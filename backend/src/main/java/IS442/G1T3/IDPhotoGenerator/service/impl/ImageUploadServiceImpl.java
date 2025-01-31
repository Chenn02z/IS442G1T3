package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageStatus;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class ImageUploadServiceImpl implements ImageUploadService {

    private final FileStorageServiceImpl fileStorageServiceImpl;
    private final ImageRepository imageRepository;

    public ImageUploadServiceImpl(FileStorageServiceImpl fileStorageServiceImpl, ImageRepository imageRepository) {
        this.fileStorageServiceImpl = fileStorageServiceImpl;
        this.imageRepository = imageRepository;
    }

    @Override
    public ImageEntity processImage(
            MultipartFile imageFile, String backgroundOption, @Nullable MultipartFile customBackground
    ) throws IOException {

        UUID imageId = UUID.randomUUID();

        // Save the original image
        String savedFilePath = fileStorageServiceImpl.saveOriginalImage(imageFile, imageId);
        log.info("Saving Image to:" + savedFilePath);
        // Create ImageEntity with initial status
        ImageEntity imageEntity = ImageEntity.builder()
                .imageId(imageId)
                .originalFileName(imageFile.getOriginalFilename())
                .savedFilePath(savedFilePath)
                .backgroundOption(backgroundOption)
                .status(ImageStatus.UPLOADED.toString())
                .build();

        // Persist metadata to the database and return the saved ImageEntity
        return imageRepository.save(imageEntity);
    }
}
