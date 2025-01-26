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
        String saveDir = System.getProperty("user.dir") + "/" + storagePath;
        String newFileName = imageId.toString() + "_" + file.getOriginalFilename();
        String filePath = saveDir + File.separator + newFileName;
        File newFile = new File(filePath);
        file.transferTo(newFile);
        return filePath;
    }
}
