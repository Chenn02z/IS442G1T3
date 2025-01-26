package IS442.G1T3.IDPhotoGenerator.service;

import org.springframework.web.multipart.MultipartFile;

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
    public String saveOriginalImage(MultipartFile file, UUID imageId) throws IOException;
}
