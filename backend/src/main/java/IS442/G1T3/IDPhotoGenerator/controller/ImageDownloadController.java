package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.service.ImageDownloadService;
import IS442.G1T3.IDPhotoGenerator.service.impl.ImageDownloadServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import org.springframework.core.io.InputStreamResource;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
@Validated
public class ImageDownloadController {

    // Use interface to adhere to Dependency Inversion Principle
    // Use final to prevent bugs
    private final ImageDownloadService imageDownloadService;


    @GetMapping("/download/{imageId}")
    public ResponseEntity<Resource> download(
            @PathVariable UUID imageId
    ) {
        try {
            log.info("Processing download request for imageId: {}", imageId);
            Resource fileResource = imageDownloadService.processDownloadRequest(imageId);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG) // Or detect dynamically
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileResource.getFilename() + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            log.error(e.toString());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/download/multiple")
    public ResponseEntity<Resource> downloadSelectedImages(@RequestBody List<UUID> imageIds) {
        try {
            log.info("Processing request to download selected images: {}", imageIds);

            // Call the service to zip selected images
            File zipFile = imageDownloadService.zipSelectedImages(imageIds);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(zipFile));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=selected_images.zip")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("Error while processing multi-image download: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

}
