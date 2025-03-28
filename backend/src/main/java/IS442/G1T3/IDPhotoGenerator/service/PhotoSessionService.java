package IS442.G1T3.IDPhotoGenerator.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import IS442.G1T3.IDPhotoGenerator.dto.StateManagementResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

public interface PhotoSessionService {
    // State management operations
    StateManagementResponse undo(UUID imageId);
    StateManagementResponse redo(UUID imageId);
    ImageNewEntity confirm(UUID imageId);
    Map<String, List<String>> getHistory(UUID imageId);
    ImageNewEntity getLatestVersion(UUID imageId);
    List<UUID> getUserImages(UUID userId);
    List<ImageNewEntity> getUserLatestList(UUID userId);
    void deleteImage(UUID imageId);
}
