package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropNewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/image-crop-new")
public class ImageCropNewController {

    private final ImageCropNewService imageCropNewService;

    public ImageCropNewController(ImageCropNewService imageCropNewService) {
        this.imageCropNewService = imageCropNewService;
    }

    // Endpoint to retrieve the image for editing based on its imageId.
    @GetMapping("/edit/{imageId}")
    public ResponseEntity<ImageNewEntity> getImageForEditing(@PathVariable UUID imageId) {
        ImageNewEntity imageEntity = imageCropNewService.getImageForEditing(imageId);
        return ResponseEntity.ok(imageEntity);
    }

    // Endpoint to crop an image. Expects imageId and crop parameters as request parameters.
    @PostMapping("/crop")
    public ResponseEntity<ImageNewEntity> saveCrop(
            @RequestParam UUID imageId,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height
    ) {
        ImageNewEntity updatedEntity = imageCropNewService.saveCrop(imageId, x, y, width, height);
        return ResponseEntity.ok(updatedEntity);
    }
}
