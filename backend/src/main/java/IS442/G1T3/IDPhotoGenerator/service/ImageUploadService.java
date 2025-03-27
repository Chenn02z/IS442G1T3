package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface ImageUploadService {

    /**
     * Processes the image upload by saving files and persisting metadata.
     *
     * @param imageFile        The original uploaded image.
     * @param userId The uploader's UUID
     * @return The persisted ImageEntity.
     * @throws IOException If an I/O error occurs during file saving.
     */
    ImageNewEntity processImage(
            MultipartFile imageFile, UUID userId
    ) throws IOException;
}
