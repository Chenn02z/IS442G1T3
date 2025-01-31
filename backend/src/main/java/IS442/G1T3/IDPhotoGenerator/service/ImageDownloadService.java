package IS442.G1T3.IDPhotoGenerator.service;

import org.springframework.core.io.Resource;

import java.util.UUID;

public interface ImageDownloadService {

    /**
     * Processes the image upload by saving files and persisting metadata.
     *
     * @param imageId The imageId of the image.
     * @return The Download Resource.
     */
    Resource processDownloadRequest(UUID imageId);
}
