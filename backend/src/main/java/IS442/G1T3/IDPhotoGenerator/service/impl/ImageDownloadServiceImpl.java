package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


@Slf4j
@Service
public class ImageDownloadServiceImpl implements ImageDownloadService {

    private final ImageRepository imageRepository;

    public ImageDownloadServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public Resource processDownloadRequest(UUID imageId) {
        // Query for saved path with imageId
        String savedFilePath = imageRepository.findSavedFilePathByImageId(imageId);
        Path filePath = Paths.get(System.getProperty("user.dir")).resolve(savedFilePath.replace("\\", "/")).normalize();
        log.info("Processing image of imageId {} download from {}", imageId, filePath);
        File file = filePath.toFile();
        if (!file.exists()) {
            throw new RuntimeException("File not found on server");
        }
        return new FileSystemResource(file);
    }

    public File zipSelectedImages(List<UUID> imageIds) throws IOException {
        File zipFile = File.createTempFile("selected_images_", ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            for (UUID imageId : imageIds) {
                try {
                    // Get the file path of the image
                    String savedFilePath = processDownloadRequest(imageId).getFile().getAbsolutePath();
                    Path filePath = Paths.get(savedFilePath);
                    File fileToZip = filePath.toFile();

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
                        log.warn("File not found for imageId {}: {}", imageId, savedFilePath);
                    }
                } catch (Exception ex) {
                    log.error("Error processing imageId {}: {}", imageId, ex.getMessage());
                }
            }
        }
        return zipFile;
    }
}
