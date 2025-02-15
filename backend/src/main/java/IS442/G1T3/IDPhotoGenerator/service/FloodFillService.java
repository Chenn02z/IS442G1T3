package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.FloodFillRequest;
import IS442.G1T3.IDPhotoGenerator.model.FloodFillResponse;

public interface FloodFillService {
    /**
     * Process the image using flood fill algorithm to remove background
     * @param request Contains image data and flood fill parameters
     * @return Processed image data with transparent background
     */
    FloodFillResponse processImage(FloodFillRequest request);
}
