package IS442.G1T3.IDPhotoGenerator.service.storage.impl;

import IS442.G1T3.IDPhotoGenerator.service.storage.StorageProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.UUID;

@Service("localStorageProvider")
public class LocalStorageProvider implements StorageProvider {

    @Value("${image.storage.path}")
    private String storagePath;
    
    @Override
    public String saveImage(UUID imageId, int version, BufferedImage image) {
        try {
            String fileName = imageId.toString() + "_" + version + ".png";
            String fullPath = System.getProperty("user.dir") + File.separator + 
                             storagePath + File.separator + fileName;
            File outputFile = new File(fullPath);
            ImageIO.write(image, "png", outputFile);
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save image locally", e);
        }
    }
    
    @Override
    public InputStream getImage(String imageUrl) {
        try {
            String fullPath = System.getProperty("user.dir") + File.separator + 
                             storagePath + File.separator + imageUrl;
            return new FileInputStream(fullPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get image", e);
        }
    }
    
    @Override
    public boolean deleteImage(String imageUrl) {
        String fullPath = System.getProperty("user.dir") + File.separator + 
                         storagePath + File.separator + imageUrl;
        return new File(fullPath).delete();
    }
    
    @Override
    public String getPublicUrl(String imageUrl) {
        return "/api/images/view/" + imageUrl;
    }
}