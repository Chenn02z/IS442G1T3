package IS442.G1T3.IDPhotoGenerator.controller;

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
@RestController
@RequestMapping("/api/images/crop")
@Validated
public class ImageCropController {

    private final ImageCropServiceImpl imageCropServiceImpl;

    public ImageCropController(ImageCropServiceImpl imageCropServiceImpl) {
        this.imageCropServiceImpl = imageCropServiceImpl;
    }
    @PostMapping("/{imageId}")
    public ResponseEntity<Resource> cropImage(
            @PathVariable UUID imageId,
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam int width,
            @RequestParam int height
    ) {
        try {
            log.info("Processing crop request for imageId: {}", imageId);
            Resource croppedImageResource = imageCropServiceImpl.processCropRequest(imageId, x, y, width, height);

            // Dynamically detect content type
            Path croppedFilePath = croppedImageResource.getFile().toPath();
            String contentType = Files.probeContentType(croppedFilePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + croppedImageResource.getFilename() + "\"")
                    .body(croppedImageResource);
        } catch (IllegalArgumentException e) {
            log.error("Invalid crop request: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Unexpected error while processing crop request: {}", e.toString());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }
}
