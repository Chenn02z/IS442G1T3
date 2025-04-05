package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.FileStorageService;
import IS442.G1T3.IDPhotoGenerator.service.ImageUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class ImageUploadServiceImpl implements ImageUploadService {

    private final FileStorageService fileStorageService;
    private final ImageNewRepository imageNewRepository;
    private final PhotoSessionRepository photoSessionRepository;

    @Value("${image.storage.path}")
    private String storagePath;

    public ImageUploadServiceImpl(
            FileStorageService fileStorageService,
            ImageNewRepository imageNewRepository,
            PhotoSessionRepository photoSessionRepository) {
        this.fileStorageService = fileStorageService;
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
    }

    @Override
    public ImageNewEntity processImage(MultipartFile imageFile, UUID userId) throws IOException {
        UUID imageId = UUID.randomUUID();

        // Ensure storage directory exists
        Path uploadPath = Paths.get(storagePath);
        Files.createDirectories(uploadPath);

        // Save the original image with version 1
        String fileExtension = ".png";  // Always save as PNG
        String fileName = imageId.toString() + "_1" + fileExtension;
        String savedFilePath = fileStorageService.saveOriginalImage(imageFile, imageId);
        log.info("Saving Image to: " + savedFilePath);

        // Create and save the image entity
        ImageNewEntity imageEntity = ImageNewEntity.builder()
                .imageId(imageId)
                .userId(userId)
                .version(1)
                .label("Original")
                .baseImageUrl(fileName)
                .currentImageUrl(fileName)
                .build();

        // Create and save photo session with initial version tracking
        PhotoSession photoSession = new PhotoSession();
        photoSession.setImageId(imageId);
        photoSession.setUndoStack("1");  // Start with version 1
        photoSession.setRedoStack("");
        photoSessionRepository.save(photoSession);

        // Save and return the image entity
        return imageNewRepository.save(imageEntity);
    }
}
