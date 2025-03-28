package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface BackgroundRemovalService {
    /**
     * Removes the background from an uploaded image based on specified option.
     *
     * @param file             The uploaded MultipartFile.
     * @param userId           The unique identifier for the user.
     * @param backgroundOption The background replacement option (WHITE/BLUE/TRANSPARENT).
     * @return The persisted image entity with processing results.
     * @throws Exception If any error occurs during processing.
     */
    ImageNewEntity removeBackground(MultipartFile file, UUID userId, String backgroundOption) throws Exception;
}

