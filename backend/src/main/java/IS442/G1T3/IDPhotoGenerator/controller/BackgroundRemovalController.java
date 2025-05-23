package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.BackgroundRemovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/background-removal")
public class BackgroundRemovalController {

    // Use interface to adhere to Dependency Inversion Principle
    @Autowired
    private BackgroundRemovalService BackgroundRemovalService;

    @PostMapping("/{imageId}/auto")
    public ResponseEntity<ImageNewEntity> cartooniseImage(@PathVariable UUID imageId) {
        try {
            log.info("Received background removal request for imageId: {}", imageId);
            ImageNewEntity processedImage = BackgroundRemovalService.removeBackground(imageId);
            return ResponseEntity.ok(processedImage);
        } catch (Exception e) {
            log.error("Error processing image", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

