package IS442.G1T3.IDPhotoGenerator.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;

public interface ImageDownloadService {

    /**
     * Processes the image upload by saving files and persisting metadata.
     *
     * @param imageId The imageId of the image.
     * @return The Download Resource.
     */
    Resource processDownloadRequest(UUID imageId);

    Resource processSizedDownloadRequest(UUID imageId, Integer width, Integer height, Integer dpi, String unit);

    File zipSelectedImages(List<UUID> imageIds) throws IOException;

    Resource processPixelDownloadRequest(UUID imageId, Integer widthPx, Integer heightPx);
}
