package IS442.G1T3.IDPhotoGenerator.service.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import IS442.G1T3.IDPhotoGenerator.dto.CropParams;
import IS442.G1T3.IDPhotoGenerator.factory.CropImageFactory;
import IS442.G1T3.IDPhotoGenerator.factory.ImageFactorySelector;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageOperationType;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropNewService;
import lombok.extern.slf4j.Slf4j;

import IS442.G1T3.IDPhotoGenerator.service.FileStorageService;
import IS442.G1T3.IDPhotoGenerator.service.ImageVersionControlService;

@Slf4j
@Service
public class ImageCropNewServiceImpl implements ImageCropNewService {

    @Value("${image.storage.path}")
    private String storagePath;

    private final ImageNewRepository imageNewRepository;
    private final FileStorageService fileStorageService;
    private final ImageVersionControlService imageVersionControlService;
    private final ImageFactorySelector factorySelector;

    public ImageCropNewServiceImpl(
            ImageNewRepository imageNewRepository,
            FileStorageService fileStorageService,
            ImageVersionControlService imageVersionControlService,
            ImageFactorySelector factorySelector
    ) {
        this.imageNewRepository = imageNewRepository;
        this.fileStorageService = fileStorageService;
        this.imageVersionControlService = imageVersionControlService;
        this.factorySelector = factorySelector;
    }

    @Override
    public ImageNewEntity getImageForEditing(UUID imageId) {
        return imageVersionControlService.getLatestImageVersion(imageId);
    }

    @Override
    public ImageNewEntity saveCrop(UUID imageId, int x, int y, int width, int height) {
        log.info("Received crop request with params - imageId: {}, x: {}, y: {}, width: {}, height: {}",
                imageId, x, y, width, height);

        validateCropDimensions(x, y, width, height);

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

        String baseImageUrl = imageVersionControlService.getBaseImageUrl(imageId, currentEntity);
        String sourceImageUrl = determineSourceImageUrl(currentEntity);

        log.info("Using sourceImageUrl for cropping: {}", sourceImageUrl);
        log.info("Preserving baseImageUrl for future reference: {}", baseImageUrl);

        // Calculate the next version number and determine the base image URL
        int newVersion = imageVersionControlService.getNextVersion(imageId);

        // Create a new entity for the cropped version using builder pattern
        CropParams cropParams = CropParams.builder()
                .x(x)
                .y(y)
                .width(width)
                .height(height)
                .build();

        CropImageFactory cropFactory = (CropImageFactory) factorySelector.getFactory(ImageOperationType.CROP);
        ImageNewEntity newEntity = cropFactory.create(
                imageId,
                currentEntity.getUserId(),
                currentEntity.getVersion() + 1,
                baseImageUrl,
                cropParams);

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

            // Create the cropped image
            log.info("Creating subimage with: x={}, y={}, width={}, height={}", x, y, width, height);
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);

            log.info("Cropped image dimensions: {}x{}", croppedImage.getWidth(), croppedImage.getHeight());

            // Save cropped image to file storage
            String croppedFilename = fileStorageService.saveVersionedImage(imageId, newEntity.getVersion(), croppedImage);
            // Set the current image URL to the new cropped image
            newEntity.setCurrentImageUrl(croppedFilename);

            log.info("Crop operation successful. New image saved: {}", croppedFilename);

            // Update photo session - add new version to undo stack and clear redo stack
            imageVersionControlService.updatePhotoSession(imageId, newEntity.getVersion());

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

    public void validateCropDimensions(int x, int y, int width, int height) {
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
    }

    public String determineSourceImageUrl(ImageNewEntity currentEntity) {
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
        return sourceImageUrl;
    }
}
