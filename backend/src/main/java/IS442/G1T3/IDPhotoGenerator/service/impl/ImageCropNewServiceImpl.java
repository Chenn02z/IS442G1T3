package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropNewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class ImageCropNewServiceImpl implements ImageCropNewService {

    @Value("${image.storage.path}")
    private String storagePath;

    private final ImageNewRepository imageNewRepository;

    public ImageCropNewServiceImpl(ImageNewRepository imageNewRepository) {
        this.imageNewRepository = imageNewRepository;
    }

    @Override
    public ImageNewEntity getImageForEditing(UUID imageId) {
        // Retrieve the latest image record for the given imageId
        ImageNewEntity imageEntity = imageNewRepository.findLatestRowByImageId(imageId);
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }
        return imageEntity;
    }

    @Override
    public ImageNewEntity saveCrop(UUID imageId, int x, int y, int width, int height) {
        // Retrieve the latest image record for the given imageId
        ImageNewEntity imageEntity = imageNewRepository.findLatestRowByImageId(imageId);
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Get the base image URL (original file location)
        String baseImageUrl = imageEntity.getBaseImageUrl();

        // Resolve the absolute file path.
        // If the baseImageUrl is not absolute, resolve it relative to the "backend" folder in the current working directory.
        Path originalPath = Paths.get(baseImageUrl);
        if (!originalPath.isAbsolute()) {
            originalPath = Paths.get(System.getProperty("user.dir"))
                    .resolve("images")
                    .resolve(baseImageUrl)
                    .normalize();
        }

        File originalFile = originalPath.toFile();
        if (!originalFile.exists()) {
            throw new RuntimeException("Original image file not found on server at: " + originalPath);
        }

        // Read the original image
        BufferedImage originalImage;
        try {
            originalImage = ImageIO.read(originalFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the original image file.", e);
        }

        // Validate crop dimensions
        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
                x + width > originalImage.getWidth() || y + height > originalImage.getHeight()) {
            throw new IllegalArgumentException("Invalid crop dimensions.");
        }

        // Define a new file name for the cropped image
        String croppedFileName = "cropped_" + imageId + "_" + x + "_" + y + "_" + width + "_" + height + ".png";
        Path croppedAbsoluteFilePath = originalPath.getParent().resolve(croppedFileName);
        File croppedFile = croppedAbsoluteFilePath.toFile();

        // Ensure the parent directory exists
        File parentDir = croppedFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Perform the cropping and save the cropped image
        try {
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            ImageIO.write(croppedImage, "png", croppedFile);
        } catch (IOException e) {
            throw new RuntimeException("Error during cropping", e);
        }

        // Build a JSON string for the cropData field
        String newCropData = String.format("%d,%d,%d,%d", x, y, width, height);
        imageEntity.setCropData(newCropData);

        // Increment the version (if your logic is to create a new version on crop update)
        imageEntity.setVersion(imageEntity.getVersion() + 1);

        // Set the new current image URL using the image id and the new version.
        // Adjust the format as needed (here we use a .png extension).
        String newCurrentImageUrl = String.format("%s_%d.png", imageId.toString(), imageEntity.getVersion());
        imageEntity.setCurrentImageUrl(newCurrentImageUrl);

        // Set the label to always be "save crop"
        imageEntity.setLabel("CROP");

        // Save the updated entity
        imageNewRepository.save(imageEntity);

        return imageEntity;
    }
}
