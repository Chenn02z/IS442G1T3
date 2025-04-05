package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.PhotoDimensionStandard;
import IS442.G1T3.IDPhotoGenerator.service.ImageResizeService;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Resize", description = "APIs for resizing images to meet standards")
public class ImageResizeController {

    private final ImageResizeService imageResizeService;
    private final ImageNewRepository imageRepository;
    private final Map<String, PhotoDimensionStandard> photoDimensionStandards;

    public ImageResizeController(
            ImageResizeService imageResizeService,
            ImageNewRepository imageRepository,
            Map<String, PhotoDimensionStandard> photoDimensionStandards
    ) {
        this.imageResizeService = imageResizeService;
        this.imageRepository = imageRepository;
        this.photoDimensionStandards = photoDimensionStandards;
    }

    @PostMapping("/resize")
    public ResponseEntity<ImageNewEntity> resizeImage(
            @Parameter(description = "The image ID to resize")
            @RequestParam("imageId") UUID imageId,

            @Parameter(description = "The country code to define the target dimensions (e.g., 'US', 'EU', 'SG')")
            @RequestParam(value = "countryCode", defaultValue = "SG") String countryCode,

            @Parameter(description = "Whether to maintain the original aspect ratio (if false, image will be stretched)")
            @RequestParam(value = "maintainAspectRatio", defaultValue = "true") boolean maintainAspectRatio,

            @Parameter(description = "Whether to crop the image if needed to fit the target dimensions")
            @RequestParam(value = "allowCropping", defaultValue = "true") boolean allowCropping) {
        try {
            // Find the image to resize
            ImageNewEntity image = imageRepository.findLatestRowByImageId(imageId);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }

            // Get the target dimension standard
            PhotoDimensionStandard standard = photoDimensionStandards.get(countryCode.toUpperCase());
            if (standard == null) {
                return ResponseEntity.badRequest().body(null);
            }

            // Resize
            ImageNewEntity resizedImage = imageResizeService.resizeImage(
                    image,
                    standard.getWidth(),
                    standard.getHeight(),
                    maintainAspectRatio,
                    allowCropping
            );
            return ResponseEntity.ok(resizedImage);
        } catch (Exception e) {
            log.error("Error while resizing image", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 