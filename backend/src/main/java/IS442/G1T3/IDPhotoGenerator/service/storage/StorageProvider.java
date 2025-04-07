package IS442.G1T3.IDPhotoGenerator.service.storage;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.UUID;

public interface StorageProvider {
    String saveImage(UUID imageId, int version, BufferedImage image);
    InputStream getImage(String imageUrl);
    boolean deleteImage(String imageUrl);
    String getPublicUrl(String imageUrl);
    
}