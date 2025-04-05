package IS442.G1T3.IDPhotoGenerator.service;

import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

public interface FileStorageService {
    /**
     * Saves the original uploaded image to the 'original' subdirectory.
     *
     * @param file    The uploaded MultipartFile.
     * @param imageId The unique identifier for the image.
     * @return The file path where the image is stored.
     * @throws IOException If an I/O error occurs during file saving.
     */
    String saveOriginalImage(MultipartFile file, UUID imageId) throws IOException;

    /**
     * Saves a cropped (or modified) image and returns its generated filename.
     *
     * @param imageId      The ID of the image.
     * @param version      The version number to include in the filename.
     * @param croppedImage The cropped BufferedImage.
     * @return The filename of the saved image.
     * @throws IOException If saving the image fails.
     */
    String saveVersionedImage(UUID imageId, int version, BufferedImage croppedImage) throws IOException;
}
