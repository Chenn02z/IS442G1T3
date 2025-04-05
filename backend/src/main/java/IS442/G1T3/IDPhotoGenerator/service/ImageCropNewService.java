package IS442.G1T3.IDPhotoGenerator.service;


import java.util.UUID;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

public interface ImageCropNewService {


    ImageNewEntity getImageForEditing(UUID imageId);

    ImageNewEntity saveCrop(UUID imageId, int x, int y, int width, int height);
}
