package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@Slf4j
public class FloodFillController {

    private final FloodFillService floodFillService;

    public FloodFillController(FloodFillService floodFillService) {
        this.floodFillService = floodFillService;
    }

    @PostMapping("/{imageId}/remove-background")
    public ResponseEntity<ImageNewEntity> removeBackground(
            @PathVariable UUID imageId,
            @RequestParam("seedPoints") String seedPointsJson,
            @RequestParam(value = "tolerance", defaultValue = "10") int tolerance) {
        try {
            ImageNewEntity processedImage = floodFillService.removeBackground(imageId, null, seedPointsJson, tolerance);
            return ResponseEntity.ok(processedImage);
        } catch (IOException e) {
            log.error("Error processing image", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}