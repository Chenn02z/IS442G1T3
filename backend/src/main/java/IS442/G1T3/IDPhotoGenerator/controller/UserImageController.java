package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/images")
public class UserImageController
{
    private final ImageRepository imageRepository;

    public UserImageController(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    /**
     * Retrieves a mapping of image IDs to their corresponding saved file paths for the specified user.
     *
     * <p>This endpoint fetches all images associated with the provided user ID, and returns a map where
     * the key is the image's unique identifier (UUID) and the value is the path where the image is saved.
     *
     * @param userId the UUID of the user whose images are being retrieved
     * @return a map where each key is an image ID and each value is the corresponding saved file path
     */
    @Operation(summary = "Get User Images' id and savedFilePath", description = "Retrieves a mapping of image IDs to their corresponding saved file paths for the specified user. eg. {'image-id' : 'saved-file-path'}")
//    @GetMapping("/userimages/{userId}")
//    public Map<UUID, String> getUserImages(@PathVariable UUID userId) {
//        List<ImageEntity> userImages = imageRepository.findByUserId(userId);
//        Map<UUID, String> imageMap = new HashMap<UUID, String>();
//        for (ImageEntity imageEntity : userImages) {
//            imageMap.put(imageEntity.getImageId(), imageEntity.getSavedFilePath());
//        }
//        return imageMap;
//    }
    @GetMapping("/userimages/{userId}")
    public Map<UUID, String> getUserImages(@PathVariable UUID userId) {
        List<ImageEntity> userImages = imageRepository.findLatestVersionsByUserId(userId);
        Map<UUID, String> imageMap = new HashMap<>();
        for (ImageEntity imageEntity : userImages) {
            imageMap.put(imageEntity.getImageId(), imageEntity.getSavedFilePath());
        }
        return imageMap;
    }
}
