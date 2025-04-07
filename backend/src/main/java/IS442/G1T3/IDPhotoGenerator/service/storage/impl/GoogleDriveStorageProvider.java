package IS442.G1T3.IDPhotoGenerator.service.storage.impl;

import IS442.G1T3.IDPhotoGenerator.service.GoogleDriveService;
import IS442.G1T3.IDPhotoGenerator.service.storage.StorageProvider;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Service("googleDriveStorageProvider")
public class GoogleDriveStorageProvider implements StorageProvider {

    private final GoogleDriveService googleDriveService;
    private final ImageNewRepository imageRepository;
    
    @Autowired
    public GoogleDriveStorageProvider(GoogleDriveService googleDriveService, 
                                     ImageNewRepository imageRepository) {
        this.googleDriveService = googleDriveService;
        this.imageRepository = imageRepository;
    }
    
    public String getName() {
        return "google-drive";
    }
    
    @Override
    public String saveImage(UUID imageId, int version, BufferedImage image) {
        try {
            // Get userId from imageId
            String userId = getUserIdFromImageId(imageId);
            String fileName = imageId.toString() + "_" + version + ".png";
            
            // Convert BufferedImage to byte stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            
            // Upload to Google Drive and get file ID - now with userId
            String fileId = googleDriveService.uploadFile(userId, fileName, "image/png", inputStream);
            
            // Store with special prefix to identify Google Drive files
            return "gdrive:" + fileId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save image to Google Drive", e);
        }
    }
    
    @Override
    public InputStream getImage(String imageUrl) {
        try {
            // For retrieval, we need to extract the userId from the image URL
            // or pass a default user. For simplicity, we'll use a default user here.
            String defaultUserId = "default-user";
            
            // Extract file ID from URL (removing the gdrive: prefix)
            String fileId = imageUrl.substring(7);
            return googleDriveService.downloadFile(defaultUserId, fileId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get image from Google Drive", e);
        }
    }
    
    @Override
    public boolean deleteImage(String imageUrl) {
        try {
            // For deletion, use the default user ID since we don't have context
            String defaultUserId = "default-user";
            String fileId = imageUrl.substring(7);
            return googleDriveService.deleteFile(defaultUserId, fileId);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getPublicUrl(String imageUrl) {
        try {
            // For public links, use the default user ID
            String defaultUserId = "default-user";
            String fileId = imageUrl.substring(7);
            return googleDriveService.getPublicLink(defaultUserId, fileId);
        } catch (Exception e) {
            return "/api/images/view/" + imageUrl;
        }
    }
    
    /**
     * Helper method to retrieve userId from imageId
     */
    private String getUserIdFromImageId(UUID imageId) {
        return imageRepository.findTopByImageIdOrderByVersionDesc(imageId)
                .map(image -> image.getUserId().toString())
                .orElseThrow(() -> new RuntimeException("Image not found: " + imageId));
    }
}