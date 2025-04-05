package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageOperationType;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.FileStorageService;
import IS442.G1T3.IDPhotoGenerator.service.ImageUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import IS442.G1T3.IDPhotoGenerator.factory.ImageFactorySelector;
import IS442.G1T3.IDPhotoGenerator.factory.OriginalImageFactory;
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
    private final ImageFactorySelector factorySelector;

    @Value("${image.storage.path}")
    private String storagePath;

    public ImageUploadServiceImpl(
            FileStorageService fileStorageService,
            ImageNewRepository imageNewRepository,
            PhotoSessionRepository photoSessionRepository,
            ImageFactorySelector factorySelector
    ) {
        this.fileStorageService = fileStorageService;
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
        this.factorySelector = factorySelector;
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
        OriginalImageFactory originalFactory = (OriginalImageFactory) factorySelector.getFactory(ImageOperationType.ORIGINAL);
        ImageNewEntity imageEntity = originalFactory.create(imageId, userId, 1, fileName, null);


        // Create and save photo session with initial version tracking

        // Make use of Builder pattern for cleaner code
        PhotoSession photoSession = PhotoSession.builder()
                .imageId(imageId)
                .undoStack("1")
                .redoStack("")
                .build();
        photoSessionRepository.save(photoSession);

        // Save and return the image entity
        return imageNewRepository.save(imageEntity);
    }
}
