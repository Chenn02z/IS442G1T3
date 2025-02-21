package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.model.ImageUploadResponse;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageStatus;
import IS442.G1T3.IDPhotoGenerator.service.impl.ImageUploadServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/images")
@Validated // do we want to use this?
public class ImageUploadController {

    private final ImageUploadServiceImpl imageUploadServiceImpl;

    public ImageUploadController(ImageUploadServiceImpl imageUploadServiceImpl) {
        this.imageUploadServiceImpl = imageUploadServiceImpl;
    }

    private static final String[] ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png"};

    @PostMapping("/upload")
    public ResponseEntity<ImageUploadResponse> upload(
            @RequestParam MultipartFile imageFile,
            @RequestParam UUID userId
    ) {
        // Backend validation; 2nd layer of safety after frontend validation
        try {
            validateImageFile(imageFile);

            ImageEntity imageEntity = imageUploadServiceImpl.processImage(imageFile, userId);
            ImageUploadResponse response = new ImageUploadResponse(imageEntity.getImageId(), imageEntity.getSavedFilePath(), imageEntity.getStatus(), "Image Uploaded Successfully.");
            log.info("File uploaded successfully.");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            log.error(e.toString());
            ImageUploadResponse response = new ImageUploadResponse(ImageStatus.UPLOAD_FAILED.toString(), e.toString());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            log.error(e.toString());
            ImageUploadResponse response = new ImageUploadResponse(ImageStatus.UPLOAD_FAILED.toString(), e.toString());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private void validateImageFile(MultipartFile imageFile) {
        if (!isAllowedContentType(imageFile.getContentType())) {
            throw new IllegalArgumentException("Invalid image file type. Allowed types are JPEG and PNG.");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        for (String allowedContentType : ALLOWED_CONTENT_TYPES) {
            if (contentType.equalsIgnoreCase(allowedContentType)) {
                return true;
            }
        }
        return false;
    }
}
