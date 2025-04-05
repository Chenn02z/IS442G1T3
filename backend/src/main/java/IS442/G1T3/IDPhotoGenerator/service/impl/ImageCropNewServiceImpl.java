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

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
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

    public ImageCropNewServiceImpl(ImageNewRepository imageNewRepository,
                                  PhotoSessionRepository photoSessionRepository) {
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
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
                log.debug("Trying to find entity by imageId and version: {}, {}", imageId, latestVersion);
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
                 
        // Validate input parameters first
        if (width <= 0 || height <= 0) {
            String errorMsg = String.format("Invalid crop dimensions: width=%d, height=%d. Both must be positive.", width, height);
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        
        if (x < 0 || y < 0) {
            String errorMsg = String.format("Invalid crop position: x=%d, y=%d. Both must be non-negative.", x, y);
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        
        // Get the current entity based on photo session
        ImageNewEntity currentEntity;
        try {
            currentEntity = getImageForEditing(imageId);
        } catch (RuntimeException e) {
            // If getImageForEditing fails, try to find the latest entity instead
            log.warn("Could not find entity via photo session, falling back to latest: {}", e.getMessage());
            currentEntity = imageNewRepository.findLatestRowByImageId(imageId);
            if (currentEntity == null) {
                throw new RuntimeException("Image not found with id: " + imageId);
            }
        }
        
        log.info("Found entity for cropping: id={}, version={}, currentImageUrl={}, baseImageUrl={}, label={}", 
                currentEntity.getImageId(), currentEntity.getVersion(), 
                currentEntity.getCurrentImageUrl(), currentEntity.getBaseImageUrl(), currentEntity.getLabel());
        
        // Find the latest entity to determine the next version number
        ImageNewEntity latestEntity = imageNewRepository.findLatestRowByImageId(imageId);
        if (latestEntity == null) {
            latestEntity = currentEntity; // Use current if no latest found
        }
        
        log.info("Latest entity for version calculation: id={}, version={}", 
                latestEntity.getImageId(), latestEntity.getVersion());

        // Get or create photo session
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            log.info("Creating new PhotoSession for imageId: {}", imageId);
            photoSession = new PhotoSession();
            photoSession.setImageId(imageId);
            photoSession.setUndoStack("1"); // Start with version 1
            photoSession.setRedoStack("");
        }

        // Create a new entity for the cropped version
        ImageNewEntity newEntity = new ImageNewEntity();
        newEntity.setImageId(imageId);
        newEntity.setVersion(latestEntity.getVersion() + 1);
        
        // Choose the source image URL based on the current entity's label
        String sourceImageUrl;
        
        // If current entity's label is "Crop", use its baseImageUrl (ensures we always crop from original)
        // Otherwise, use the currentImageUrl (allows cropping from other operations like background removal)
        if ("Crop".equals(currentEntity.getLabel()) && currentEntity.getBaseImageUrl() != null && !currentEntity.getBaseImageUrl().isEmpty()) {
            sourceImageUrl = currentEntity.getBaseImageUrl();
            log.info("Current label is 'Crop', using baseImageUrl: {}", sourceImageUrl);
        } else {
            sourceImageUrl = currentEntity.getCurrentImageUrl();
            log.info("Current label is not 'Crop' or baseImageUrl is missing, using currentImageUrl: {}", sourceImageUrl);
        }
        
        // Always preserve the original baseImageUrl
        String baseImageUrl = currentEntity.getBaseImageUrl();
        if (baseImageUrl == null || baseImageUrl.isEmpty()) {
            // If baseImageUrl isn't set, try to find the very first version
            List<ImageNewEntity> allVersions = imageNewRepository.findByImageId(imageId);
            if (!allVersions.isEmpty()) {
                // Sort by version to find the first one (version 1)
                allVersions.sort((a, b) -> a.getVersion() - b.getVersion());
                ImageNewEntity originalEntity = allVersions.get(0);
                baseImageUrl = originalEntity.getCurrentImageUrl();
                log.info("Found original version (version {}), using its currentImageUrl as baseImageUrl: {}", 
                        originalEntity.getVersion(), baseImageUrl);
            }
            
            if (baseImageUrl == null || baseImageUrl.isEmpty()) {
                log.warn("No baseImageUrl found, setting the same as sourceImageUrl: {}", sourceImageUrl);
                baseImageUrl = sourceImageUrl;
            }
        }
        
        log.info("Using sourceImageUrl for cropping: {}", sourceImageUrl);
        log.info("Preserving baseImageUrl for future reference: {}", baseImageUrl);
        
        newEntity.setBaseImageUrl(baseImageUrl);
        newEntity.setUserId(currentEntity.getUserId());
        newEntity.setLabel("Crop");
        
        // Set the crop data
        String cropData = String.format("%d,%d,%d,%d", x, y, width, height);
        newEntity.setCropData(cropData);

        // Create the actual cropped image file
        try {
            // Use the selected source image URL for cropping
            String originalImagePath = Paths.get(storagePath, sourceImageUrl).toString();
            
            log.info("Using image path for crop: {}", originalImagePath);
            
            // Check if the file exists first
            File originalFile = new File(originalImagePath);
            if (!originalFile.exists() || !originalFile.isFile()) {
                log.error("Source image file does not exist: {}", originalImagePath);
                
                // Try to find the file with the full path pattern
                String fullPathPattern = storagePath + File.separator + sourceImageUrl;
                File fullPathFile = new File(fullPathPattern);
                
                if (fullPathFile.exists() && fullPathFile.isFile()) {
                    log.info("Found source image at full path: {}", fullPathPattern);
                    originalImagePath = fullPathPattern;
                    originalFile = fullPathFile;
                } else {
                    // Try to find the file without extension
                    String noExtensionPath = storagePath + File.separator + sourceImageUrl.replaceAll("\\.png$|\\.jpg$|\\.jpeg$", "");
                    File noExtensionFile = new File(noExtensionPath);
                    
                    if (noExtensionFile.exists() && noExtensionFile.isFile()) {
                        log.info("Found source image at path without extension: {}", noExtensionPath);
                        originalImagePath = noExtensionPath;
                        originalFile = noExtensionFile;
                    } else {
                        // Try to list all files to help debug
                        log.error("Listing all files in {}", storagePath);
                        File storageDir = new File(storagePath);
                        File[] files = storageDir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                log.info("Found file: {}", file.getName());
                                if (file.getName().startsWith(imageId.toString())) {
                                    log.info("Found matching file by ID: {}", file.getAbsolutePath());
                                    originalImagePath = file.getAbsolutePath();
                                    originalFile = file;
                                    break;
                                }
                            }
                        }
                        
                        if (!originalFile.exists()) {
                            throw new IOException("Source image file not found: " + originalImagePath);
                        }
                    }
                }
            }
            
            // Get file size for debugging
            log.info("Original file size: {} bytes", originalFile.length());
            
            // Read the original image
            BufferedImage originalImage = ImageIO.read(originalFile);
            if (originalImage == null) {
                throw new IOException("Could not read original image: " + originalImagePath);
            }
            
            int imgWidth = originalImage.getWidth();
            int imgHeight = originalImage.getHeight();
            log.info("Original image dimensions: {}x{}", imgWidth, imgHeight);
            log.info("Cropping with parameters: x={}, y={}, width={}, height={}", x, y, width, height);
            
            // Validate crop parameters against image dimensions
            if (x < 0 || y < 0 || width <= 0 || height <= 0 || 
                x + width > imgWidth || y + height > imgHeight) {
                
                String errorMsg = String.format(
                    "Invalid crop parameters: x=%d, y=%d, width=%d, height=%d for image dimensions %dx%d", 
                    x, y, width, height, imgWidth, imgHeight
                );
                log.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            
            // Create the cropped image
            log.info("Creating subimage with: x={}, y={}, width={}, height={}", x, y, width, height);
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            
            log.info("Cropped image dimensions: {}x{}", croppedImage.getWidth(), croppedImage.getHeight());
            
            // Generate the filename for the cropped image (imageId_version.png)
            String croppedFilename = imageId.toString() + "_" + newEntity.getVersion() + ".png";
            File outputFile = new File(Paths.get(storagePath, croppedFilename).toString());
            
            log.info("Saving cropped image to: {}", outputFile.getAbsolutePath());
            
            // Save the cropped image
            boolean saved = ImageIO.write(croppedImage, "png", outputFile);
            if (!saved) {
                throw new IOException("Failed to save cropped image to " + outputFile.getAbsolutePath());
            }
            
            // Make sure the file was created
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new IOException("Failed to create cropped image file or file is empty");
            }
            
            log.info("Cropped image saved successfully. File size: {} bytes", outputFile.length());
            
            // Set the current image URL to the new cropped image
            newEntity.setCurrentImageUrl(croppedFilename);
            
            log.info("Crop operation successful. New image saved: {}", croppedFilename);
            
            // Update photo session - add new version to undo stack and clear redo stack
            updatePhotoSession(photoSession, newEntity.getVersion());
            
        } catch (Exception e) {
            log.error("Error creating cropped image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create cropped image: " + e.getMessage(), e);
        }

        // Save the new entity
        ImageNewEntity savedEntity = imageNewRepository.save(newEntity);
        log.info("Saved new entity: id={}, version={}, currentImageUrl={}", 
                savedEntity.getImageId(), savedEntity.getVersion(), savedEntity.getCurrentImageUrl());

        return savedEntity;
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
}
