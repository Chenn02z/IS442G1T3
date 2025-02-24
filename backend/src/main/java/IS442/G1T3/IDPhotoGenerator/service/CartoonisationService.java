package IS442.G1T3.IDPhotoGenerator.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;

public interface CartoonisationService {
    /**
     * Converts an existing image into a cartoon style image.
     *
     * @param imageId The UUID of the image to cartoonize
     * @return ImageEntity containing the processed image details
     * @throws Exception If any error occurs during processing
     */
    ImageEntity cartooniseImage(UUID imageId) throws Exception;
}

