package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {
    @Value("${image.storage.path}")
    private String storagePath;

    @Override
    public String saveOriginalImage(MultipartFile file, UUID imageId) throws IOException {
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        String newFileName = imageId.toString() + "_1.png";  // Always use .png extension
        String relativeSaveDir = storagePath + File.separator + newFileName;
        String filePath = saveDir + File.separator + newFileName;
        
        // Create directory if it doesn't exist
        File directory = new File(saveDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        File newFile = new File(filePath);
        file.transferTo(newFile);
        return relativeSaveDir;
    }
}
