package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.dto.CropEditResponseDTO;
import IS442.G1T3.IDPhotoGenerator.dto.CropResponseDTO;
import IS442.G1T3.IDPhotoGenerator.model.CropEntity;
import IS442.G1T3.IDPhotoGenerator.repository.CropRepository;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ImageCropServiceImpl implements ImageCropService {

    @Value("${image.storage.path}")
    private String storagePath;

    private final ImageRepository imageRepository;
    private final CropRepository cropRepository;

    public ImageCropServiceImpl(ImageRepository imageRepository, CropRepository cropRepository) {
        this.imageRepository = imageRepository;
        this.cropRepository = cropRepository;
    }

    // Returns the original image and any existing crop parameters
    public CropEditResponseDTO getImageForEditing(UUID imageId) {
        ImageEntity imageEntity = imageRepository.findByImageIdWithOriginalFilePath(imageId);
//                .orElseThrow(() -> new RuntimeException("Image not found"));
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }
        // Assume the original image URL/path is stored in a field (e.g., originalFilePath)
        String originalImageUrl = imageEntity.getSavedFilePath();

        // Fetch existing crop data for this image if it exists
        CropEntity cropEntity = cropRepository.findByImageImageId(imageId);

        CropEditResponseDTO dto = new CropEditResponseDTO();
        dto.setImageId(imageId);
        dto.setOriginalImageUrl(originalImageUrl);

        // If cropEntity exists, set its parameters in the DTO; otherwise, leave them null
        if (cropEntity != null) {
            dto.setX(cropEntity.getX());
            dto.setY(cropEntity.getY());
            dto.setWidth(cropEntity.getWidth());
            dto.setHeight(cropEntity.getHeight());
            dto.setCroppedFilePath(cropEntity.getCroppedFilePath());
        }
        return dto;
    }

    // Saves a new crop record (or updates an existing one) when the user clicks Save
    // public CropResponseDTO saveCrop(UUID imageId, int x, int y, int width, int height) {
    public CropResponseDTO saveCrop(UUID imageId, int x, int y, int width, int height) {
        // Retrieve the image entity or throw an exception if not found.
//        ImageEntity imageEntity = imageRepository.findById(imageId)
//         ImageEntity imageEntity = imageRepository.findByImageId(imageId)
//                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));
        Optional<ImageEntity> optionalEntity = Optional.ofNullable(imageRepository.findByImageId(imageId));
        ImageEntity imageEntity;
        if (optionalEntity.isPresent()) {
            imageEntity = optionalEntity.get();
        } else {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Get the saved file path from the image entity.
        String savedFilePath = imageEntity.getSavedFilePath();
        log.info("Saved file path from DB: {}", savedFilePath);

        // Determine the original path.
        // If savedFilePath is not absolute, resolve it relative to the current working directory.
        Path originalPath = Paths.get(savedFilePath);
        if (!originalPath.isAbsolute()) {
            originalPath = Paths.get(System.getProperty("user.dir")).resolve(savedFilePath).normalize();
        }
        log.info("Resolved original image path: {}", originalPath.toString());

        File originalFile = originalPath.toFile();
        if (!originalFile.exists()) {
            throw new RuntimeException("Original image file not found on server at: " + originalPath.toString());
        }

        // Read the original image and validate crop dimensions.
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(originalFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the original image file.", e);
        }

        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
                x + width > originalImage.getWidth() || y + height > originalImage.getHeight()) {
            throw new IllegalArgumentException("Invalid crop dimensions.");
        }

        // Define a file name and path for the cropped image.
        String croppedFileName = "cropped_" + imageId + "_" + x + "_" + y + "_" + width + "_" + height + ".png";
        Path croppedAbsoluteFilePath = originalPath.getParent().resolve(croppedFileName);
        File croppedFile = croppedAbsoluteFilePath.toFile();

        // Ensure the parent directory exists.
        File parentDir = croppedFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Perform cropping and save the new image.
        try {
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            ImageIO.write(croppedImage, "png", croppedFile);
        } catch (IOException e) {
            throw new RuntimeException("Error during cropping", e);
        }

        // Update an existing crop record or create a new one.
        CropEntity cropEntity = cropRepository.findByImageImageId(imageId);
        if (cropEntity != null) {
            cropEntity.setX(x);
            cropEntity.setY(y);
            cropEntity.setWidth(width);
            cropEntity.setHeight(height);
            cropEntity.setCroppedFilePath(croppedFileName);
            cropRepository.save(cropEntity);
        } else {
            cropEntity = new CropEntity();
            cropEntity.setCropId(UUID.randomUUID());
            cropEntity.setX(x);
            cropEntity.setY(y);
            cropEntity.setWidth(width);
            cropEntity.setHeight(height);
            cropEntity.setCroppedFilePath(croppedFileName);
            cropEntity.setImage(imageEntity);  // Set the foreign key reference
            cropRepository.save(cropEntity);
        }

        // TO DO: Update saved_file_path row in Images repo
        // e.g. imageRepo.save(imageEntity)

        // Build and return the response DTO.
        String croppedSavedFilePath = String.join("/", storagePath, croppedFileName);
        CropResponseDTO response = new CropResponseDTO(croppedSavedFilePath);

        return response;
    }

    
}

