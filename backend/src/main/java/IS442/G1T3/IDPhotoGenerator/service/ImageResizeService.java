package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

/**
 * Interface for the service that resizes images to meet dimension requirements.
 */
public interface ImageResizeService {
    ImageNewEntity resizeImage(
            ImageNewEntity originalImage, 
            int targetWidth, 
            int targetHeight,
            boolean maintainAspectRatio,
            boolean allowCropping);
}