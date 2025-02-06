package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
public class ImageCropServiceImpl implements ImageCropService {

    private final ImageRepository imageRepository;

    public ImageCropServiceImpl(ImageRepository imageRepository){
        this.imageRepository = imageRepository;
    }

    @Override
    public Resource processCropRequest(UUID imageId, int x, int y, int width, int height) {
        // 1️⃣ Fetch the image file path from the database
        String savedFilePath = imageRepository.findSavedFilePathByImageId(imageId);
        Path filePath = Paths.get(System.getProperty("user.dir")).resolve(savedFilePath).normalize();
        log.info("Processing crop request for imageId {} from path {}", imageId, filePath);

        File originalFile = filePath.toFile();
        if (!originalFile.exists()) {
            throw new RuntimeException("Original image file not found on server.");
        }

        try {
            // 2️⃣ Load the original image
            BufferedImage originalImage = ImageIO.read(originalFile);

            // Validate crop dimensions
            if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
                    x + width > originalImage.getWidth() || y + height > originalImage.getHeight()) {
                throw new IllegalArgumentException("Invalid crop dimensions.");
            }

            // 3️⃣ Perform cropping
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);

            // 4️⃣ Save the cropped image to a new file
            String croppedFileName = "cropped_" + imageId + ".png";
            Path croppedFilePath = filePath.getParent().resolve(croppedFileName);
            File croppedFile = croppedFilePath.toFile();
            ImageIO.write(croppedImage, "png", croppedFile);

            log.info("Cropped image saved successfully at {}", croppedFilePath);

            // 5️⃣ Optional: Update the image repository with new cropped file path
            ImageEntity imageEntity = imageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("Image metadata not found in database."));
            imageEntity.setSavedFilePath(croppedFilePath.toString());
            imageRepository.save(imageEntity);

            // 6️⃣ Return the cropped image as a Resource (for download or further processing)
            return new FileSystemResource(croppedFile);

        } catch (IOException e) {
            throw new RuntimeException("Error while processing the crop operation.", e);
        }
    }
}
