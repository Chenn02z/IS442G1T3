package IS442.G1T3.IDPhotoGenerator.factory;

import java.util.UUID;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

public interface ImageEntityFactory<T> {
    ImageNewEntity create(UUID imageId, UUID userId, int version, String baseImageUrl, T params);
}
