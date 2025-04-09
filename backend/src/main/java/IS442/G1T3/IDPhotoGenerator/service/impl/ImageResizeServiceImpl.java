package IS442.G1T3.IDPhotoGenerator.service.impl;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import IS442.G1T3.IDPhotoGenerator.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageResizeService;
import IS442.G1T3.IDPhotoGenerator.service.ImageVersionControlService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImageResizeServiceImpl implements ImageResizeService {

    @Value("${image.storage.path}")
    private String storagePath;

    private final ImageNewRepository imageNewRepository;
    private final FileStorageService fileStorageService;
    private final ImageVersionControlService imageVersionControlService;

    public ImageResizeServiceImpl(
            ImageNewRepository imageNewRepository,
            FileStorageService fileStorageService,
            ImageVersionControlService imageVersionControlService
    ) {
        this.imageNewRepository = imageNewRepository;
        this.fileStorageService = fileStorageService;
        this.imageVersionControlService = imageVersionControlService;
    }

    /**
     * Resizes an image to the specified dimensions in a version-controlled manner.
     *
     * @param originalImage       The original image to resize.
     * @param targetWidth         The target width in pixels.
     * @param targetHeight        The target height in pixels.
     * @param maintainAspectRatio Whether to maintain the original aspect ratio.
     * @param allowCropping       Whether to crop the image if needed to fit the target dimensions.
     * @return A new ImageNewEntity with the resized image.
     */
    public ImageNewEntity resizeImage(
            ImageNewEntity originalImage,
            int targetWidth,
            int targetHeight,
            boolean maintainAspectRatio,
            boolean allowCropping) {

        try {
            // Load the original image
            String originalImagePath = String.format("%s/%s", storagePath, originalImage.getCurrentImageUrl());
            File originalFile = new File(originalImagePath);
            BufferedImage originalBufferedImage = ImageIO.read(originalFile);

            // Get original dimensions
            int originalWidth = originalBufferedImage.getWidth();
            int originalHeight = originalBufferedImage.getHeight();

            BufferedImage resizedImage;

            if (maintainAspectRatio) {
                double widthRatio = (double) targetWidth / originalWidth;
                double heightRatio = (double) targetHeight / originalHeight;

                if (allowCropping) {
                    double ratio = Math.max(widthRatio, heightRatio);
                    int scaledWidth = (int) (originalWidth * ratio);
                    int scaledHeight = (int) (originalHeight * ratio);

                    BufferedImage tempImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = tempImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.drawImage(originalBufferedImage, 0, 0, scaledWidth, scaledHeight, null);
                    g2d.dispose();

                    int x = (scaledWidth - targetWidth) / 2;
                    int y = (scaledHeight - targetHeight) / 2;
                    x = Math.max(0, x);
                    y = Math.max(0, y);

                    // Add bounds checking to ensure the subimage dimensions stay within the original image
                    int actualWidth = Math.min(targetWidth, scaledWidth - x);
                    int actualHeight = Math.min(targetHeight, scaledHeight - y);

                    if (actualWidth <= 0 || actualHeight <= 0) {
                        // Fallback if dimensions are invalid
                        resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                        g2d = resizedImage.createGraphics();
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g2d.drawImage(originalBufferedImage, 0, 0, targetWidth, targetHeight, null);
                        g2d.dispose();
                    } else {
                        // Only attempt to create a subimage if dimensions are valid
                        resizedImage = tempImage.getSubimage(x, y, actualWidth, actualHeight);

                        // If the actual dimensions differ from target, create a new image with target dimensions
                        if (actualWidth != targetWidth || actualHeight != targetHeight) {
                            BufferedImage finalImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                            g2d = finalImage.createGraphics();
                            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g2d.drawImage(resizedImage, 0, 0, targetWidth, targetHeight, null);
                            g2d.dispose();
                            resizedImage = finalImage;
                        }
                    }
                } else {
                    double ratio = Math.min(widthRatio, heightRatio);
                    int newWidth = (int) (originalWidth * ratio);
                    int newHeight = (int) (originalHeight * ratio);

                    resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = resizedImage.createGraphics();
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.drawImage(originalBufferedImage, 0, 0, newWidth, newHeight, null);
                    g2d.dispose();
                }
            } else {
                resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resizedImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.drawImage(originalBufferedImage, 0, 0, targetWidth, targetHeight, null);
                g2d.dispose();
            }

            // Use the version control service to get the new version number and base image URL
            int newVersion = imageVersionControlService.getNextVersion(originalImage.getImageId());
            String baseImageUrl = imageVersionControlService.getBaseImageUrl(originalImage.getImageId(), originalImage);

            // Delegate file saving to the file storage service; this returns the generated filename.
            String resizedFileName = fileStorageService.saveVersionedImage(originalImage.getImageId(), newVersion, resizedImage);

            // Instead of duplicating the photo session update logic here, delegate it to the version control service.
            imageVersionControlService.updatePhotoSession(originalImage.getImageId(), newVersion);

            // Create a new image entity with versioning details
            ImageNewEntity resizedEntity = ImageNewEntity.builder()
                    .imageId(originalImage.getImageId())
                    .userId(originalImage.getUserId())
                    .version(newVersion)
                    .label("Resized")
                    .baseImageUrl(baseImageUrl)
                    .currentImageUrl(resizedFileName)
                    .cropData(originalImage.getCropData())
                    .build();

            return imageNewRepository.save(resizedEntity);

        } catch (IOException e) {
            throw new RuntimeException("Failed to resize image: " + e.getMessage(), e);
        }
    }
}
