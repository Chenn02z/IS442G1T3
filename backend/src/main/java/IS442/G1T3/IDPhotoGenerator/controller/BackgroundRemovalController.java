package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.BackgroundRemovalService;
import IS442.G1T3.IDPhotoGenerator.service.CartoonisationService;
import IS442.G1T3.IDPhotoGenerator.service.impl.BackgroundRemovalServiceImpl;
import IS442.G1T3.IDPhotoGenerator.service.impl.CartooniseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/background-removal")
public class BackgroundRemovalController {

    // Use interface to adhere to Dependency Inversion Principle
    // Use final for more defensive code
    private final BackgroundRemovalService backgroundRemovalService;


    // Use interface to adhere to Dependency Inversion Principle
    @Autowired
    private CartoonisationService cartoonisationService;

    @PostMapping("/remove")
    public ResponseEntity<ImageNewEntity> removeBackground(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam("backgroundOption") String backgroundOption) {
        try {
            log.info("Received background removal request for backgroundOption: {}", backgroundOption);
            
            // If userId is not provided, generate a temporary one
            UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();
            log.info("Using userId: {}", effectiveUserId);
            
            // Validate input
            if (imageFile == null || imageFile.isEmpty()) {
                log.error("No image file provided");
                return ResponseEntity.badRequest().build();
            }
            
            if (backgroundOption == null || backgroundOption.trim().isEmpty()) {
                log.error("No background option provided");
                return ResponseEntity.badRequest().build();
            }
            
            ImageNewEntity processedImage = backgroundRemovalService.removeBackground(
                imageFile, 
                effectiveUserId, 
                backgroundOption
            );
            
            log.info("Background removal completed successfully for imageId: {}", processedImage.getImageId());
            return ResponseEntity.ok(processedImage);
            
        } catch (Exception e) {
            log.error("Error removing background: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{imageId}/auto")
    public ResponseEntity<ImageNewEntity> cartooniseImage(@PathVariable UUID imageId) {
        try {
            log.info("Received cartoonise request for imageId: {}", imageId);
            ImageNewEntity processedImage = cartoonisationService.cartooniseImage(imageId);
            return ResponseEntity.ok(processedImage);
        } catch (Exception e) {
            log.error("Error processing image: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

