package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.dto.CropEditResponseDTO;
import IS442.G1T3.IDPhotoGenerator.dto.CropResponseDTO;
import IS442.G1T3.IDPhotoGenerator.model.CropEntity;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import org.springframework.core.io.Resource;

import java.util.UUID;

public interface ImageCropNewService {


    ImageNewEntity getImageForEditing(UUID imageId);

    ImageNewEntity saveCrop(UUID imageId, int x, int y, int width, int height);
}
