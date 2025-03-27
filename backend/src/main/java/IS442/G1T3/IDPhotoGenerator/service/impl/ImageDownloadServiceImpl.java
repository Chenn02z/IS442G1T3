package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ImageDownloadServiceImpl implements ImageDownloadService {

    private final ImageNewRepository imageNewRepository;

    @Value("${image.storage.path}")
    private String storagePath;

    public ImageDownloadServiceImpl(ImageNewRepository imageNewRepository) {
        this.imageNewRepository = imageNewRepository;
    }

    @Override
    public Resource processDownloadRequest(UUID imageId) {
        // Get the latest version of the image
        ImageNewEntity imageEntity = imageNewRepository.findLatestRowByImageId(imageId);
        if (imageEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

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

    public File zipSelectedImages(List<UUID> imageIds) throws IOException {
        File zipFile = File.createTempFile("selected_images_", ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            for (UUID imageId : imageIds) {
                try {
                    // Get the latest version of the image
                    ImageNewEntity imageEntity = imageNewRepository.findLatestRowByImageId(imageId);
                    if (imageEntity == null) {
                        log.warn("Image not found for imageId: {}", imageId);
                        continue;
                    }

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
}
