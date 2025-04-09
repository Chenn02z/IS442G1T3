package IS442.G1T3.IDPhotoGenerator.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import IS442.G1T3.IDPhotoGenerator.dto.CropParams;
import IS442.G1T3.IDPhotoGenerator.factory.CropImageFactory; // Import the missing enum
import IS442.G1T3.IDPhotoGenerator.factory.ImageFactorySelector;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity; // Import the missing class
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageOperationType;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropNewService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImageCropNewServiceImpl implements ImageCropNewService {

    @Value("${image.storage.path}")
    private String storagePath;

    private final ImageNewRepository imageNewRepository;
    private final PhotoSessionRepository photoSessionRepository;
    private final ImageFactorySelector factorySelector;

    public ImageCropNewServiceImpl(
            ImageNewRepository imageNewRepository,
            PhotoSessionRepository photoSessionRepository,
            ImageFactorySelector factorySelector
    ) {
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
        this.factorySelector = factorySelector;
    }

    @Override
    public ImageNewEntity getImageForEditing(UUID imageId) {
        // First, check if we have a photo session for this image
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        
        if (photoSession == null) {
            log.warn("No PhotoSession found for imageId: {}, falling back to latest version", imageId);
            // If no photo session, try to get the latest image directly
            ImageNewEntity latestEntity = imageNewRepository.findLatestRowByImageId(imageId);
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
                ImageNewEntity imageEntity = imageNewRepository.findByCurrentImageUrl(imageUrl);
                
                if (imageEntity != null) {
                    log.info("Found entity with currentImageUrl: {}", imageUrl);
                    return imageEntity;
                }
                
                // Try without the .png extension
                imageUrl = imageId.toString() + "_" + latestVersion;
                log.debug("Trying to find entity with currentImageUrl (without extension): {}", imageUrl);
                imageEntity = imageNewRepository.findByCurrentImageUrl(imageUrl);
                
                if (imageEntity != null) {
                    log.info("Found entity with currentImageUrl (without extension): {}", imageUrl);
                    return imageEntity;
                }
                
                // Try finding by imageId and version
                log.debug("Trying to find entity by imageId and version: {}, {}", imageId, Integer.parseInt(latestVersion));
                try {
                    imageEntity = imageNewRepository.findByImageIdAndVersion(imageId, Integer.parseInt(latestVersion));
                    
                    if (imageEntity != null) {
                        log.info("Found entity by imageId and version: {}, {}", imageId, latestVersion);
                        return imageEntity;
                    }
                } catch (Exception ex) {
                    log.warn("Method findByImageIdAndVersion not available or failed: {}", ex.getMessage());
                    
                    // Manual implementation as fallback - find entity with matching imageId and version
                    List<ImageNewEntity> allEntities = imageNewRepository.findByImageId(imageId);
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
                List<ImageNewEntity> allEntities = imageNewRepository.findByImageId(imageId);
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
        ImageNewEntity imageEntity = imageNewRepository.findLatestRowByImageId(imageId);
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }
        
        log.info("Found latest entity: imageId={}, version={}, currentImageUrl={}, baseImageUrl={}", 
                imageEntity.getImageId(), imageEntity.getVersion(), 
                imageEntity.getCurrentImageUrl(), imageEntity.getBaseImageUrl());
                
        return imageEntity;
    }

    @Override
    public ImageNewEntity saveCrop(UUID imageId, int x, int y, int width, int height) {
        log.info("Received crop request with params - imageId: {}, x: {}, y: {}, width: {}, height: {}",
                imageId, x, y, width, height);

        // Validate crop parameters
        validateCropDimensions(x, y, width, height);

        // Get current entity (with all your existing robust code)
        ImageNewEntity currentEntity = getImageForEditing(imageId);
        
        // Find latest entity for version calculation
        ImageNewEntity latestEntity = imageNewRepository.findLatestRowByImageId(imageId);
        if (latestEntity == null) {
            latestEntity = currentEntity;
        }
        
        int newVersion = latestEntity.getVersion() + 1;
        String baseImageUrl = determineBaseImageUrl(currentEntity, imageId);
        String sourceImageUrl = determineSourceImageUrl(currentEntity);
        
        // Create or get photo session
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            photoSession = new PhotoSession();
            photoSession.setImageId(imageId);
            photoSession.setUndoStack("1");
            photoSession.setRedoStack("");
        }

        // Use the factory pattern to create the entity
        CropImageFactory cropFactory = (CropImageFactory) factorySelector.getFactory(ImageOperationType.CROP);
        CropParams cropParams = new CropParams(x, y, width, height);
        
        ImageNewEntity newEntity = cropFactory.create(
                imageId,
                currentEntity.getUserId(),
                newVersion,
                baseImageUrl,
                cropParams
        );
        
        try {
            // Read source image with all your robust file handling logic
            String originalImagePath = resolveImagePath(sourceImageUrl, imageId);
            BufferedImage originalImage = ImageIO.read(new File(originalImagePath));
            
            if (originalImage == null) {
                throw new IOException("Could not read original image: " + originalImagePath);
            }
            
            // Validate crop bounds against actual image dimensions
            validateCropBounds(originalImage, x, y, width, height);
            
            // Create cropped image
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            
            // Save cropped image
            String croppedFilename = imageId + "_" + newVersion + ".png";
            File outputFile = new File(Paths.get(storagePath, croppedFilename).toString());
            boolean saved = ImageIO.write(croppedImage, "png", outputFile);
            
            if (!saved || !outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("Failed to save cropped image");
            }
            
            // Set the current image URL
            newEntity.setCurrentImageUrl(croppedFilename);
            
            // Update photo session
            updatePhotoSession(photoSession, newVersion);
            
        } catch (IOException e) {
            log.error("Error creating cropped image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create cropped image: " + e.getMessage(), e);
        }
        
        // Save and return the entity
        return imageNewRepository.save(newEntity);
    }
    
    // Helper method to update the photo session's undo/redo stacks
    private void updatePhotoSession(PhotoSession photoSession, int newVersion) {
        List<String> undoVersions = new ArrayList<>();
        
        if (photoSession.getUndoStack() != null && !photoSession.getUndoStack().isEmpty()) {
            undoVersions = new ArrayList<>(Arrays.asList(photoSession.getUndoStack().split(",")));
        }
        
        // Add the new version to the undo stack
        undoVersions.add(String.valueOf(newVersion));
        photoSession.setUndoStack(String.join(",", undoVersions));
        
        // Clear the redo stack since we've created a new edit
        photoSession.setRedoStack("");
        
        log.info("Updated undo stack: {}", photoSession.getUndoStack());
        
        // Save the PhotoSession
        photoSessionRepository.save(photoSession);
    }

    private String determineBaseImageUrl(ImageNewEntity currentEntity, UUID imageId) {
        String baseImageUrl = currentEntity.getBaseImageUrl();
        if (baseImageUrl == null || baseImageUrl.isEmpty()) {
            List<ImageNewEntity> allVersions = imageNewRepository.findByImageId(imageId);
            if (!allVersions.isEmpty()) {
                allVersions.sort((a, b) -> a.getVersion() - b.getVersion());
                ImageNewEntity originalEntity = allVersions.get(0);
                baseImageUrl = originalEntity.getCurrentImageUrl();
            }
            
            if (baseImageUrl == null || baseImageUrl.isEmpty()) {
                baseImageUrl = currentEntity.getCurrentImageUrl();
            }
        }
        return baseImageUrl;
    }

    private String determineSourceImageUrl(ImageNewEntity currentEntity) {
        if ("Crop".equals(currentEntity.getLabel()) && 
            currentEntity.getBaseImageUrl() != null && 
            !currentEntity.getBaseImageUrl().isEmpty()) {
            return currentEntity.getBaseImageUrl();
        }
        return currentEntity.getCurrentImageUrl();
    }

    private String resolveImagePath(String imageUrl, UUID imageId) throws IOException {
        String imagePath = Paths.get(storagePath, imageUrl).toString();
        File file = new File(imagePath);
        
        if (file.exists() && file.isFile()) {
            return imagePath;
        }
        
        // Your existing fallback logic for finding files
        // [include all the file finding code from your current implementation]
        
        throw new IOException("Source image file not found: " + imageUrl);
    }

    private void validateCropDimensions(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                String.format("Invalid crop dimensions: width=%d, height=%d. Both must be positive.", width, height)
            );
        }
        
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException(
                String.format("Invalid crop position: x=%d, y=%d. Both must be non-negative.", x, y)
            );
        }
    }

    private void validateCropBounds(BufferedImage image, int x, int y, int width, int height) {
        if (x + width > image.getWidth() || y + height > image.getHeight()) {
            throw new IllegalArgumentException(
                String.format("Crop dimensions (x=%d, y=%d, width=%d, height=%d) exceed image bounds (%dx%d)",
                    x, y, width, height, image.getWidth(), image.getHeight())
            );
        }
    }
}