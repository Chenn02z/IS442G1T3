package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageVersionControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ImageVersionControlServiceImpl implements ImageVersionControlService {
    private final ImageNewRepository imageRepository;
    private final PhotoSessionRepository photoSessionRepository;

    public ImageVersionControlServiceImpl(ImageNewRepository imageRepository, PhotoSessionRepository photoSessionRepository) {
        this.imageRepository = imageRepository;
        this.photoSessionRepository = photoSessionRepository;
    }

    @Override
    public ImageNewEntity getLatestImageVersion(UUID imageId) {
        // First, check if we have a photo session for this image
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);

        if (photoSession == null) {
            log.warn("No PhotoSession found for imageId: {}, falling back to latest version", imageId);
            // If no photo session, try to get the latest image directly
            ImageNewEntity latestEntity = imageRepository.findLatestRowByImageId(imageId);
            if (latestEntity != null) {
                log.info("Found latest entity for imageId: {}, version: {}, currentImageUrl: {}, baseImageUrl: {}",
                        imageId, latestEntity.getVersion(), latestEntity.getCurrentImageUrl(), latestEntity.getBaseImageUrl());
                return latestEntity;
            }
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        log.info("Found PhotoSession for imageId: {} with undoStack: {}, redoStack: {}",
                imageId, photoSession.getUndoStack(), photoSession.getRedoStack());

        if (photoSession.getUndoStack() != null && !photoSession.getUndoStack().isEmpty()) {
            // Get the latest version from the undo stack
            String[] versions = photoSession.getUndoStack().split(",");
            String latestVersion = versions[versions.length - 1];

            log.info("Latest version from undo stack: {} for imageId: {}", latestVersion, imageId);

            // Find the image entity with this version - try multiple patterns
            try {
                // First try with full URL pattern
                String imageUrl = imageId.toString() + "_" + latestVersion + ".png";
                log.debug("Trying to find entity with currentImageUrl: {}", imageUrl);
                ImageNewEntity imageEntity = imageRepository.findByCurrentImageUrl(imageUrl);

                if (imageEntity != null) {
                    log.info("Found entity with currentImageUrl: {}", imageUrl);
                    return imageEntity;
                }

                // Try without the .png extension
                imageUrl = imageId.toString() + "_" + latestVersion;
                log.debug("Trying to find entity with currentImageUrl (without extension): {}", imageUrl);
                imageEntity = imageRepository.findByCurrentImageUrl(imageUrl);

                if (imageEntity != null) {
                    log.info("Found entity with currentImageUrl (without extension): {}", imageUrl);
                    return imageEntity;
                }

                // Try finding by imageId and version
                log.debug("Trying to find entity by imageId and version: {}, {}", imageId, Integer.parseInt(latestVersion));
                try {
                    imageEntity = imageRepository.findByImageIdAndVersion(imageId, Integer.parseInt(latestVersion));

                    if (imageEntity != null) {
                        log.info("Found entity by imageId and version: {}, {}", imageId, latestVersion);
                        return imageEntity;
                    }
                } catch (Exception ex) {
                    log.warn("Method findByImageIdAndVersion not available or failed: {}", ex.getMessage());

                    // Manual implementation as fallback - find entity with matching imageId and version
                    List<ImageNewEntity> allEntities = imageRepository.findByImageId(imageId);
                    if (allEntities != null) {
                        log.debug("Manually searching through {} entities for version {}", allEntities.size(), latestVersion);
                        int versionToFind = Integer.parseInt(latestVersion);
                        for (ImageNewEntity entity : allEntities) {
                            if (entity.getVersion() == versionToFind) {
                                log.info("Found entity by manual search with imageId: {} and version: {}",
                                        imageId, latestVersion);
                                return entity;
                            }
                        }
                    }
                }

                // Last resort - try to get any entity with this imageId
                List<ImageNewEntity> allEntities = imageRepository.findByImageId(imageId);
                if (!allEntities.isEmpty()) {
                    ImageNewEntity latestEntity = allEntities.get(allEntities.size() - 1);
                    log.warn("Using latest entity found by imageId: {}, version: {}",
                            imageId, latestEntity.getVersion());
                    return latestEntity;
                }

                log.error("Could not find any entity for imageId: {} and version: {}", imageId, latestVersion);
            } catch (Exception e) {
                log.error("Error finding entity for editing: {}", e.getMessage(), e);
            }
        } else {
            log.warn("PhotoSession exists but undoStack is empty for imageId: {}", imageId);
        }

        // Fallback to latest version if no photo session or entity found
        log.info("No valid entity found via PhotoSession, falling back to findLatestRowByImageId for image ID {}", imageId);
        ImageNewEntity imageEntity = imageRepository.findLatestRowByImageId(imageId);
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        log.info("Found latest entity: imageId={}, version={}, currentImageUrl={}, baseImageUrl={}",
                imageEntity.getImageId(), imageEntity.getVersion(),
                imageEntity.getCurrentImageUrl(), imageEntity.getBaseImageUrl());

        return imageEntity;
    }

    @Override
    public int getNextVersion(UUID imageId) {
        ImageNewEntity latestEntity = imageRepository.findLatestRowByImageId(imageId);
        return (latestEntity == null ? 0 : latestEntity.getVersion()) + 1;
    }

    @Override
    public void updatePhotoSession(UUID imageId, int newVersion) {
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            log.info("No photo session found for imageId: {}. Creating new session.", imageId);
            photoSession = new PhotoSession();
            photoSession.setImageId(imageId);
            // Starting with version 1 by default
            photoSession.setUndoStack("1");
            photoSession.setRedoStack("");
        }

        List<String> undoVersions = new ArrayList<>();
        if (photoSession.getUndoStack() != null && !photoSession.getUndoStack().isEmpty()) {
            undoVersions = new ArrayList<>(Arrays.asList(photoSession.getUndoStack().split(",")));
        }
        // Append the new version to the undo stack
        undoVersions.add(String.valueOf(newVersion));
        photoSession.setUndoStack(String.join(",", undoVersions));
        // Clear the redo stack since a new edit has been made
        photoSession.setRedoStack("");
        photoSessionRepository.save(photoSession);
    }

    @Override
    public String getBaseImageUrl(UUID imageId, ImageNewEntity currentEntity) {
        String baseImageUrl = currentEntity.getBaseImageUrl();
        if (baseImageUrl == null || baseImageUrl.isEmpty()) {
            // If the base URL isn't set, find the very first version (assumed to be the original)
            List<ImageNewEntity> allVersions = imageRepository.findByImageId(imageId);
            if (!allVersions.isEmpty()) {
                allVersions.sort((a, b) -> a.getVersion() - b.getVersion());
                ImageNewEntity originalEntity = allVersions.get(0);
                baseImageUrl = originalEntity.getCurrentImageUrl();
                log.info("Found original version (version {}) using its currentImageUrl as baseImageUrl: {}",
                        originalEntity.getVersion(), baseImageUrl);
            }
            if (baseImageUrl == null || baseImageUrl.isEmpty()) {
                log.warn("No baseImageUrl found, setting the same as current image URL");
                baseImageUrl = currentEntity.getCurrentImageUrl();
            }
        }
        return baseImageUrl;
    }
}
