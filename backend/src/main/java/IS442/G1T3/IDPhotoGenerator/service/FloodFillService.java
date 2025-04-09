package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import java.io.IOException;
import java.util.UUID;

public interface FloodFillService {
    /**
     * Applies flood fill algorithm to remove background from an image.
     *
     * @param imageId The unique identifier of the image
     * @param seedPointsJson JSON string containing an array of points where flood fill should start
     * @param tolerance Color tolerance for the flood fill algorithm
     * @param algorithm The algorithm to use (bfs or dfs)
     * @return The processed image entity
     * @throws IOException If an error occurs during file operations
     */
    ImageNewEntity removeBackground(UUID imageId, String seedPointsJson, int tolerance, String algorithm) throws IOException;
}