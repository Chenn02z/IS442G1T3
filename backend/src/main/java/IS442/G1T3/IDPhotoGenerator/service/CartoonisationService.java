package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

import java.io.IOException;
import java.util.UUID;

public interface CartoonisationService {
    /**
     * Converts an existing image into a cartoon style image.
     *
     * @param imageId The UUID of the image to cartoonize
     * @return ImageNewEntity containing the processed image details
     * @throws Exception If any error occurs during processing
     * @throws IOException If an I/O error occurs during file operations
     */
    ImageNewEntity cartooniseImage(UUID imageId) throws Exception, IOException;
}

