package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@Slf4j
public class FloodFillController {

    private final FloodFillService FloodFillService;

    public FloodFillController(FloodFillService FloodFillService) {
        this.FloodFillService = FloodFillService;
    }

    @PostMapping("/remove-background")
    public ResponseEntity<?> removeBackground(
            @RequestParam("file") MultipartFile file,
            @RequestParam("seedX") int seedX,
            @RequestParam("seedY") int seedY,
            @RequestParam(value = "tolerance", defaultValue = "10") int tolerance) {
        try {
            byte[] processedImage = FloodFillService.removeBackground(file, seedX, seedY, tolerance);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"processed_" + file.getOriginalFilename() + "\"")
                    .body(processedImage);

        } catch (IOException e) {
            log.error("Error processing image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing image: " + e.getMessage());
        }
    }
}