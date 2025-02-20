package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.dto.CropEditResponseDTO;
import IS442.G1T3.IDPhotoGenerator.dto.CropResponseDTO;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropService;
import IS442.G1T3.IDPhotoGenerator.service.impl.ImageCropServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@CrossOrigin(origins = "http://localhost:3000")
@Validated@RestController
@RequestMapping("/api/images")
public class ImageCropController {

    private final ImageCropService imageCropService;

    public ImageCropController(ImageCropService imageCropService) {
        this.imageCropService = imageCropService;
    }

    // Endpoint 1: When users click the edit button
    // Returns the original image URL (or file path) along with the current crop parameters (if any)
    @GetMapping("/{imageId}/edit")
    public ResponseEntity<CropEditResponseDTO> getImageForEditing(@PathVariable UUID imageId) {
        CropEditResponseDTO response = imageCropService.getImageForEditing(imageId);
        return ResponseEntity.ok(response);
    }

    // Endpoint 2: When users click save after editing
    // Performs the crop operation and saves/updates the CropEntity in the crops repository.
    // It returns a response with the new crop parameters and cropped image URL.
    @PostMapping("/{imageId}/crop")
    public ResponseEntity<CropResponseDTO> cropImage(
            @PathVariable UUID imageId,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height) {
        CropResponseDTO response = imageCropService.saveCrop(imageId, x, y, width, height);
        return ResponseEntity.ok(response);
    }
}

