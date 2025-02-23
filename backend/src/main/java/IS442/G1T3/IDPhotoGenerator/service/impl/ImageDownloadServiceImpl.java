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
        Path filePath = Paths.get(System.getProperty("user.dir")).resolve(savedFilePath).normalize();
        log.info("Processing image of imageId {} download from {}", imageId, filePath);
        File file = filePath.toFile();
        if (!file.exists()) {
            throw new RuntimeException("File not found on server");
        }
        return new FileSystemResource(file);
    }
}
