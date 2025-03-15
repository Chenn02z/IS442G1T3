package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.dto.CropEditResponseDTO;
import IS442.G1T3.IDPhotoGenerator.dto.CropResponseDTO;
import IS442.G1T3.IDPhotoGenerator.model.CropEntity;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface ImageCropService {

    /**
     * Retrieves the original image along with any existing crop parameters for editing.
     *
     * @param imageId the UUID of the image to be edited
     * @return a CropEditResponseDTO containing the image URL/path and crop parameters (if available)
     */
    CropEditResponseDTO getImageForEditing(UUID imageId);

    /**
     * Performs the cropping operation on the image, saves or updates the crop record,
     * and returns the updated crop details including the URL/path of the cropped image.
     *
     * @param imageId the UUID of the image to crop
     * @param x the x-coordinate for the crop
     * @param y the y-coordinate for the crop
     * @param width the width of the crop area
     * @param height the height of the crop area
     * @return a CropResponseDTO containing the crop details and cropped image URL/path
     */
    // CropResponseDTO saveCrop(UUID imageId, int x, int y, int width, int height);
    CropResponseDTO saveCrop(UUID imageId, int x, int y, int width, int height);

}
