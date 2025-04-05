package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropNewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageCropNewController {

    // Use interface to adhere to Dependency Inversion Principle
    // Use final to prevent bugs
    private final ImageCropNewService imageCropNewService;


    @GetMapping("/{imageId}/edit")
    public ResponseEntity<Map<String, Object>> getImageForEditing(@PathVariable UUID imageId) {
        ImageNewEntity imageEntity = imageCropNewService.getImageForEditing(imageId);
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        data.put("imageId", imageEntity.getImageId());
        data.put("version", imageEntity.getVersion());
        data.put("baseImageUrl", imageEntity.getBaseImageUrl());
        data.put("currentImageUrl", imageEntity.getCurrentImageUrl());
        data.put("label", imageEntity.getLabel());
        
        // Parse crop data if it exists
        if (imageEntity.getCropData() != null && !imageEntity.getCropData().isEmpty()) {
            String[] cropParams = imageEntity.getCropData().split(",");
            Map<String, Integer> crop = new HashMap<>();
            crop.put("x", Integer.parseInt(cropParams[0]));
            crop.put("y", Integer.parseInt(cropParams[1]));
            crop.put("width", Integer.parseInt(cropParams[2]));
            crop.put("height", Integer.parseInt(cropParams[3]));
            data.put("crop", crop);
        } else {
            data.put("crop", null);
        }
        
        response.put("status", "success");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{imageId}/crop")
    public ResponseEntity<Map<String, Object>> saveCrop(
            @PathVariable UUID imageId,
            @RequestBody CropRequest cropRequest
    ) {
        ImageNewEntity updatedEntity = imageCropNewService.saveCrop(
            imageId,
            cropRequest.getX(),
            cropRequest.getY(), 
            cropRequest.getWidth(),
            cropRequest.getHeight()
        );

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        
        data.put("imageId", updatedEntity.getImageId());
        data.put("version", updatedEntity.getVersion());
        data.put("baseImageUrl", updatedEntity.getBaseImageUrl());
        data.put("label", updatedEntity.getLabel());
        
        // Parse crop data
        String[] cropParams = updatedEntity.getCropData().split(",");
        Map<String, Integer> crop = new HashMap<>();
        crop.put("x", Integer.parseInt(cropParams[0]));
        crop.put("y", Integer.parseInt(cropParams[1]));
        crop.put("width", Integer.parseInt(cropParams[2]));
        crop.put("height", Integer.parseInt(cropParams[3]));
        data.put("cropData", crop);
        
        // Return the actual URL that the frontend should use
        data.put("currentImageUrl", updatedEntity.getCurrentImageUrl());
        
        response.put("status", "success");
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    static class CropRequest {
        private int x;
        private int y;
        private int width;
        private int height;

        // Getters
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }

        // Setters
        public void setX(int x) { this.x = x; }
        public void setY(int y) { this.y = y; }
        public void setWidth(int width) { this.width = width; }
        public void setHeight(int height) { this.height = height; }
    }
}
