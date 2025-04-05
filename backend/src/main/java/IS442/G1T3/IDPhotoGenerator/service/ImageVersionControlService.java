package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

import java.util.UUID;

public interface ImageVersionControlService {
    /**
     * Create phot0 session for an image with version == 0
     */
    void initialisePhotoSession(UUID imageId);

    /**
     * Retrieves the image entity of the latest version
     */
    ImageNewEntity getLatestImageVersion(UUID imageId);

    /**
     * Returns the next version number for a new edit.
     */
    int getNextVersion(UUID imageId);

    /**
     * Updates the photo session's undo and redo stacks with the new version.
     */
    void updatePhotoSession(UUID imageId, int newVersion);

    /**
     * Determines the base image URL to be preserved during cropping.
     */
    String getBaseImageUrl(UUID imageId, ImageNewEntity currentEntity);
}
