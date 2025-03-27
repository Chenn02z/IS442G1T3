package IS442.G1T3.IDPhotoGenerator.service;

import java.util.UUID;
import java.util.List;
import java.util.Map;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

public interface PhotoSessionService {
    // State management operations
    ImageNewEntity undo(UUID imageId);
    ImageNewEntity redo(UUID imageId);
    ImageNewEntity confirm(UUID imageId);
    Map<String, List<String>> getHistory(UUID imageId);
    ImageNewEntity getLatestVersion(UUID imageId);
    List<UUID> getUserImages(UUID userId);
    List<ImageNewEntity> getUserLatestList(UUID userId);
    void deleteImage(UUID imageId);
}
