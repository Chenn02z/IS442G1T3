package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import java.util.UUID;

public interface ClothesReplacementService {
    /**
     * Processes the image with clothes overlay and returns the updated image entity.
     *
     * @param imageId The unique identifier of the image.
     * @return The updated ImageNewEntity after applying the clothes overlay.
     * @throws Exception If an error occurs during processing.
     */
    ImageNewEntity OverlaidImage(UUID imageId) throws Exception;
}
