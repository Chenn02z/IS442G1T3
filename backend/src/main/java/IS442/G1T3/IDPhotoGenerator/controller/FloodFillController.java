package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import lombok.extern.slf4j.Slf4j;
import java.awt.Point;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
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
            @RequestParam("seedPoints") String seedPointsJson,
            @RequestParam(value = "tolerance", defaultValue = "10") int tolerance) {
        try {
            byte[] processedImage = FloodFillService.removeBackground(file, seedPointsJson, tolerance);

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