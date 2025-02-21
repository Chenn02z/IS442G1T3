package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.service.impl.BackgroundRemovalServiceImpl;
import IS442.G1T3.IDPhotoGenerator.service.impl.CartooniseServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/background-removal")
public class BackgroundRemovalController {

    @Autowired
    private BackgroundRemovalServiceImpl backgroundRemovalServiceImpl;
    @Autowired
    private CartooniseServiceImpl cartooniseServiceImpl;

    @PostMapping("/remove")
    public ResponseEntity<ImageEntity> removeBackground(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam("backgroundOption") String backgroundOption) {
        try {
            ImageEntity processedImage = backgroundRemovalServiceImpl.removeBackground(imageFile, userId, backgroundOption);
            return ResponseEntity.ok(processedImage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cartoonise")
    public ResponseEntity<?> cartooniseImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "userId", required = false) UUID userId) {
        try {
            // Validate input
            if (imageFile == null || imageFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Image file is required");
            }

            byte[] processedImage = cartooniseServiceImpl.cartooniseImage(imageFile, userId);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"cartoonised_" + imageFile.getOriginalFilename() + "\"")
                    .body(processedImage);
        } catch (Exception e) {
            log.error("Error processing image: ", e);
            return ResponseEntity.internalServerError().body("Error processing image: " + e.getMessage());
        }
    }
}

