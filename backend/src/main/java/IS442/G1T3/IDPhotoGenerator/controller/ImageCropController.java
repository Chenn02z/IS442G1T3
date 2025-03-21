package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.dto.CropEditResponseDTO;
import IS442.G1T3.IDPhotoGenerator.dto.CropRequestDTO;
import IS442.G1T3.IDPhotoGenerator.dto.CropResponseDTO;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Validated
@RestController
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

    @PostMapping("/{imageId}/crop")
    public ResponseEntity<CropResponseDTO> cropImage(
            @PathVariable UUID imageId,
            @Valid @RequestBody CropRequestDTO cropRequest) {
        CropResponseDTO response = imageCropService.saveCrop(
                imageId,
                cropRequest.getX(),
                cropRequest.getY(),
                cropRequest.getWidth(),
                cropRequest.getHeight());
        return ResponseEntity.ok(response);
    }
}

