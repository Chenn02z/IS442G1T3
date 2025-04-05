package IS442.G1T3.IDPhotoGenerator.service.impl;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageDownloadService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ImageDownloadServiceImpl implements ImageDownloadService {

    private final ImageNewRepository imageNewRepository;
    private final PhotoSessionRepository photoSessionRepository;

    @Value("${image.storage.path}")
    private String storagePath;

    public ImageDownloadServiceImpl(ImageNewRepository imageNewRepository,
                                   PhotoSessionRepository photoSessionRepository) {
        this.imageNewRepository = imageNewRepository;    
        this.photoSessionRepository = photoSessionRepository;
    }

    @Override
    public Resource processDownloadRequest(UUID imageId) {
        // Get the latest version of the image
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        String undoStack = photoSession.getUndoStack();
        int currVersion = 1;
        if (undoStack != null && !undoStack.isBlank()) {
            String[] versions = undoStack.split(",");
            currVersion = Integer.parseInt(versions[versions.length - 1]);
        }

        ImageNewEntity imageEntity = imageNewRepository.findByImageIdAndVersion(imageId, currVersion);
        

        // Convert relative path to absolute path
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        String currentFileName = imageEntity.getCurrentImageUrl();
        String filePath = saveDir + File.separator + currentFileName;
        
        log.info("Processing image download from {}", filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found on server at: " + filePath);
        }
        return new FileSystemResource(file);
    }

    @Override
    public Resource processSizedDownloadRequest(UUID imageId, Integer width, Integer height, Integer dpi, String unit) {
        // Get the latest version of the image
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        String undoStack = photoSession.getUndoStack();
        int currVersion = 1;
        if (undoStack != null && !undoStack.isBlank()) {
            String[] versions = undoStack.split(",");
            currVersion = Integer.parseInt(versions[versions.length - 1]);
        }

        ImageNewEntity imageEntity = imageNewRepository.findByImageIdAndVersion(imageId, currVersion);
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Convert relative path to absolute path
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        String currentFileName = imageEntity.getCurrentImageUrl();
        String filePath = saveDir + File.separator + currentFileName;
        
        log.info("Processing sized image download from {}", filePath);
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            throw new RuntimeException("File not found on server at: " + filePath);
        }
        
        try {
            // Calculate pixel dimensions based on physical dimensions
            int pixelWidth, pixelHeight;
            
            if ("mm".equalsIgnoreCase(unit)) {
                // Convert mm to inches (1 inch = 25.4 mm)
                double widthInches = width / 25.4;
                double heightInches = height / 25.4;
                
                // Calculate pixel dimensions
                pixelWidth = (int) Math.round(widthInches * dpi);
                pixelHeight = (int) Math.round(heightInches * dpi);
            } else if ("in".equalsIgnoreCase(unit) || "inch".equalsIgnoreCase(unit)) {
                // Direct inch to pixel conversion
                pixelWidth = width * dpi;
                pixelHeight = height * dpi;
            } else {
                // Assume pixels directly
                pixelWidth = width;
                pixelHeight = height;
            }
            
            // Create a temp file for the resized image
            File resizedFile = File.createTempFile("sized_", "_" + sourceFile.getName());
            
            // Use BufferedImage to resize
            BufferedImage originalImage = ImageIO.read(sourceFile);
            BufferedImage resizedImage = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_RGB);
            
            // Draw the original image resized
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, pixelWidth, pixelHeight, null);
            g.dispose();
            
            // Write to the temp file
            ImageIO.write(resizedImage, "jpg", resizedFile);
            
            return new FileSystemResource(resizedFile);
        } catch (IOException e) {
            log.error("Error resizing image: {}", e.getMessage());
            throw new RuntimeException("Failed to resize image: " + e.getMessage());
        }
    }

    @Override
    public File zipSelectedImages(List<UUID> imageIds) throws IOException {
        File zipFile = File.createTempFile("selected_images_", ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            for (UUID imageId : imageIds) {
                try {
                    // Get the latest version of the image  
                    PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
                    if (photoSession == null) {
                        log.warn("Image not found for imageId: {}", imageId);
                        continue;
                    }

                    String undoStack = photoSession.getUndoStack();
                    int currVersion = 1;
                    if (undoStack != null && !undoStack.isBlank()) {
                        String[] versions = undoStack.split(",");
                        currVersion = Integer.parseInt(versions[versions.length - 1]);
                    }

                    ImageNewEntity imageEntity = imageNewRepository.findByImageIdAndVersion(imageId, currVersion);

                    // Get the file path
                    String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
                    String currentFileName = imageEntity.getCurrentImageUrl();
                    String filePath = saveDir + File.separator + currentFileName;
                    File fileToZip = new File(filePath);

                    if (fileToZip.exists()) {
                        try (FileInputStream fis = new FileInputStream(fileToZip)) {
                            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                            zipOut.putNextEntry(zipEntry);
                            byte[] bytes = new byte[1024];
                            int length;
                            while ((length = fis.read(bytes)) >= 0) {
                                zipOut.write(bytes, 0, length);
                            }
                        }
                    } else {
                        log.warn("File not found for imageId {}: {}", imageId, filePath);
                    }
                } catch (Exception ex) {
                    log.error("Error processing imageId {}: {}", imageId, ex.getMessage());
                }
            }
        }
        return zipFile;
    }

    @Override
    public Resource processPixelDownloadRequest(UUID imageId, Integer widthPx, Integer heightPx) {
        // Get the latest version of the image
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        String undoStack = photoSession.getUndoStack();
        int currVersion = 1;
        if (undoStack != null && !undoStack.isBlank()) {
            String[] versions = undoStack.split(",");
            currVersion = Integer.parseInt(versions[versions.length - 1]);
        }

        ImageNewEntity imageEntity = imageNewRepository.findByImageIdAndVersion(imageId, currVersion);
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Convert relative path to absolute path
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        String currentFileName = imageEntity.getCurrentImageUrl();
        String filePath = saveDir + File.separator + currentFileName;
        
        log.info("Processing pixel-based image download from {}", filePath);
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            throw new RuntimeException("File not found on server at: " + filePath);
        }
        
        try {
            // Create a temp file for the resized image
            File resizedFile = File.createTempFile("pixel_sized_", "_" + sourceFile.getName());
            
            // Use BufferedImage to resize
            BufferedImage originalImage = ImageIO.read(sourceFile);
            BufferedImage resizedImage = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB);
            
            // Draw the original image resized
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, widthPx, heightPx, null);
            g.dispose();
            
            // Write to the temp file
            ImageIO.write(resizedImage, "jpg", resizedFile);
            
            return new FileSystemResource(resizedFile);
        } catch (IOException e) {
            log.error("Error resizing image: {}", e.getMessage());
            throw new RuntimeException("Failed to resize image: " + e.getMessage());
        }
    }
}
