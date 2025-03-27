package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
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
public class UserImageController {
    private final ImageNewRepository imageNewRepository;

    public UserImageController(ImageNewRepository imageNewRepository) {
        this.imageNewRepository = imageNewRepository;
    }

    /**
     * Retrieves a mapping of image IDs to their corresponding current image URLs for the specified user.
     *
     * <p>This endpoint fetches all images associated with the provided user ID, and returns a map where
     * the key is the image's unique identifier (UUID) and the value is the current image URL.
     *
     * @param userId the UUID of the user whose images are being retrieved
     * @return a map where each key is an image ID and each value is the corresponding current image URL
     */
    @Operation(summary = "Get User Images' id and currentImageUrl", description = "Retrieves a mapping of image IDs to their corresponding current image URLs for the specified user. eg. {'image-id' : 'current-image-url'}")
    @GetMapping("/userimages/{userId}")
    public Map<UUID, String> getUserImages(@PathVariable UUID userId) {
        List<ImageNewEntity> userImages = imageNewRepository.findByUserId(userId);
        Map<UUID, String> imageMap = new HashMap<>();
        for (ImageNewEntity imageEntity : userImages) {
            imageMap.put(imageEntity.getImageId(), imageEntity.getCurrentImageUrl());
        }
        return imageMap;
    }
}
