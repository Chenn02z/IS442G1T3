package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.CropEntity;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface ImageCropService {
    /**
     * Processes the crop request for an image.
     *
     * @param imageId The unique ID of the image to crop.
     * @param x       The x-coordinate of the top-left corner of the crop area.
     * @param y       The y-coordinate of the top-left corner of the crop area.
     * @param width   The width of the crop area.
     * @param height  The height of the crop area.
     * @return The cropped image entity with updated metadata.
     */
    Resource processCropRequest(UUID imageId, int x, int y, int width, int height);
}
