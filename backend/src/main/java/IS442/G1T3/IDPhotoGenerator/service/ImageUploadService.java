package IS442.G1T3.IDPhotoGenerator.service;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageUploadService {

    /**
     * Processes the image upload by saving files and persisting metadata.
     *
     * @param imageFile        The original uploaded image.
     * @param backgroundOption The selected background option.
     * @param customBackground The custom background image (if any).
     * @return The persisted ImageEntity.
     * @throws IOException If an I/O error occurs during file saving.
     */
    public ImageEntity processImage(
            MultipartFile imageFile, String backgroundOption, @Nullable MultipartFile customBackground
    ) throws IOException;
}
