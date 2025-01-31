package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.service.impl.ImageDownloadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@CrossOrigin(origins = "http://localhost:3000") // Allow frontend requests (local)
@RestController
@RequestMapping("/api/images")
@Validated
public class ImageDownloadController {

    private final ImageDownloadServiceImpl imageDownloadServiceImpl;

    public ImageDownloadController(ImageDownloadServiceImpl imageDownloadServiceImpl) {
        this.imageDownloadServiceImpl = imageDownloadServiceImpl;
    }

    @GetMapping("/download/{imageId}")
    public ResponseEntity<Resource> download(
            @PathVariable UUID imageId
    ) {
        try {
            log.info("Processing download request for imageId: {}", imageId);
            Resource fileResource = imageDownloadServiceImpl.processDownloadRequest(imageId);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Or detect dynamically
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            log.error(e.toString());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
