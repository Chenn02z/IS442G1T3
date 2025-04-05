package IS442.G1T3.IDPhotoGenerator.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import IS442.G1T3.IDPhotoGenerator.service.impl.ImageDownloadServiceImpl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    @PostMapping("/download/multiple")
    public ResponseEntity<Resource> downloadSelectedImages(@RequestBody List<UUID> imageIds) {
        try {
            log.info("Processing request to download selected images: {}", imageIds);

            // Call the service to zip selected images
            File zipFile = imageDownloadServiceImpl.zipSelectedImages(imageIds);
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

    @GetMapping("/download/{imageId}/sized")
    public ResponseEntity<Resource> downloadWithSize(
            @PathVariable UUID imageId,
            @RequestParam(required = true) Integer width,
            @RequestParam(required = true) Integer height,
            @RequestParam(required = false, defaultValue = "300") Integer dpi,
            @RequestParam(required = false, defaultValue = "mm") String unit
    ) {
        try {
            log.info("Processing sized download request for imageId: {}, width: {}{}, height: {}{}, dpi: {}",
                    imageId, width, unit, height, unit, dpi);
            
            Resource fileResource = imageDownloadServiceImpl.processSizedDownloadRequest(
                    imageId, width, height, dpi, unit);
                    
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileResource.getFilename() + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            log.error(e.toString());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download/{imageId}/pixel")
    public ResponseEntity<Resource> downloadWithPixelDimensions(
            @PathVariable UUID imageId,
            @RequestParam(required = true) Integer widthPx,
            @RequestParam(required = true) Integer heightPx
    ) {
        try {
            log.info("Processing pixel-based download request for imageId: {}, width: {}px, height: {}px",
                    imageId, widthPx, heightPx);
            
            Resource fileResource = imageDownloadServiceImpl.processPixelDownloadRequest(
                    imageId, widthPx, heightPx);
                    
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + fileResource.getFilename() + "\"")
                    .body(fileResource);
        } catch (Exception e) {
            log.error(e.toString());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
