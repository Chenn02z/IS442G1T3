package IS442.G1T3.IDPhotoGenerator.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;

public interface FloodFillService {
    /**
     * Processes the flood fill request for an existing image.
     *
     * @param imageId The UUID of the image to process
     * @param seedPointsJson The list of seedpoint coordinates to remove the background from
     * @param tolerance The tolerance of colour difference between surrounding pixels
     * @return ImageEntity containing the processed image details
     */
    ImageEntity removeBackground(UUID imageId, String seedPointsJson, int tolerance) throws IOException;
}