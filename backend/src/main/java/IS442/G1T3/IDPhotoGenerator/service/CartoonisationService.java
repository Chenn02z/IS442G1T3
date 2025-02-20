package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface CartoonisationService {
    /**
     * Converts an uploaded image into a cartoon style image.
     *
     * @param file    The uploaded MultipartFile to be cartoonized.
     * @param userId  The unique identifier for the user.
     * @return The persisted image entity with processing results.
     * @throws Exception If any error occurs during processing.
     */
    ImageEntity cartooniseImage(MultipartFile file, UUID userId) throws Exception;
}

