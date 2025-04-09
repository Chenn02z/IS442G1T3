package IS442.G1T3.IDPhotoGenerator.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import IS442.G1T3.IDPhotoGenerator.dto.ComplianceCheckResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.service.ImageVersionControlService;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.ComplianceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/compliance")
@Tag(name = "Compliance Check", description = "APIs for checking photo compliance with ID/passport standards")
public class ComplianceCheckController {

    @Value("${default.country.code:SG}")
    private String defaultCountryCode;

    private final ComplianceService complianceService;
    private final ImageNewRepository imageRepository;
    private final ImageVersionControlService imageVersionControlService;

    public ComplianceCheckController(
        ComplianceService complianceService,
        ImageNewRepository imageRepository,
        ImageVersionControlService imageVersionControlService)
    {
        this.complianceService = complianceService;
        this.imageRepository = imageRepository;
        this.imageVersionControlService = imageVersionControlService;
    }

    @PostMapping("/check")
    public ResponseEntity<ComplianceCheckResponse> checkCompliance(
            @Parameter(description = "The image ID to check")
            @RequestParam("imageId") UUID imageId,

            @Parameter(description = "Country code to check against (e.g., 'US', 'EU', 'SG'). Defaults to 'SG' if not provided.")
            @RequestParam(value = "countryCode", required = false) String countryCode) {
        try {
            // Find the image by ID
            ImageNewEntity image = imageVersionControlService.getLatestImageVersion(imageId);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }
            log.info("Checking photo compliance for image with ID {} of Version: {}, countryCode: {}", imageId, image.getVersion(), countryCode == null ? "null" : countryCode);

            // Use the defaultCountryCode if countryCode is not provided
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = defaultCountryCode;
            }

            ComplianceCheckResponse result = complianceService.checkCompliance(image, countryCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error checking photo compliance", e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 