package IS442.G1T3.IDPhotoGenerator.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import IS442.G1T3.IDPhotoGenerator.dto.PhotoSessionResponse;
import IS442.G1T3.IDPhotoGenerator.dto.StateManagementResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.PhotoSessionService;


@RestController
@RequestMapping("/api/statemanagement")
@RequiredArgsConstructor
public class PhotoSessionController {

    private final PhotoSessionService photoSessionService;


    @PostMapping("/undo")
    public ResponseEntity<?> undo(@RequestBody Map<String, String> request) {
        try {
            UUID imageId = UUID.fromString(request.get("imageId"));
            StateManagementResponse result = photoSessionService.undo(imageId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @PostMapping("/redo")
    public ResponseEntity<?> redo(@RequestBody Map<String, String> request) {
        try {
            UUID imageId = UUID.fromString(request.get("imageId"));
            StateManagementResponse result = photoSessionService.redo(imageId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestBody Map<String, String> request) {
        try {
            UUID imageId = UUID.fromString(request.get("imageId"));
            ImageNewEntity result = photoSessionService.confirm(imageId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @GetMapping("/history/{imageId}")
    public ResponseEntity<?> getHistory(@PathVariable UUID imageId) {
        try {
            Map<String, List<String>> history = photoSessionService.getHistory(imageId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", history));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @GetMapping("/latest/{imageId}")
    public ResponseEntity<?> getLatestVersion(@PathVariable UUID imageId) {
        try {
            ImageNewEntity result = photoSessionService.getLatestVersion(imageId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @GetMapping("/images/{imageId}/latest")
    public ResponseEntity<?> getLatestImageInfo(@PathVariable UUID imageId) {
        try {
            ImageNewEntity result = photoSessionService.getLatestVersion(imageId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", result));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @GetMapping("/user/images/{userId}")
    public ResponseEntity<?> getUserImages(@PathVariable UUID userId) {
        try {
            List<UUID> images = photoSessionService.getUserImages(userId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", images));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @GetMapping("/user/latest-list/{userId}")
    public ResponseEntity<?> getUserLatestList(@PathVariable UUID userId) {
        try {
            List<ImageNewEntity> images = photoSessionService.getUserLatestList(userId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", images));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{imageId}")
    public ResponseEntity<?> deleteImage(@PathVariable UUID imageId) {
        try {
            photoSessionService.deleteImage(imageId);
            return ResponseEntity.ok(new PhotoSessionResponse<>("success", "Image deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new PhotoSessionResponse<>("error", e.getMessage()));
        }
    }
}

