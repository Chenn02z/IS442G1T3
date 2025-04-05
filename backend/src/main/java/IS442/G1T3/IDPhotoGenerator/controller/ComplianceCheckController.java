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
import IS442.G1T3.IDPhotoGenerator.model.enums.ComplianceCheckStatus;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.ComplianceService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/compliance")
@Tag(name = "Compliance Check", description = "APIs for checking photo compliance with ID/passport standards")
public class ComplianceCheckController {

    @Value("${default.country.code:SG}")
    private String defaultCountryCode;

    private final ComplianceService complianceService;
    private final ImageNewRepository imageRepository;

    public ComplianceCheckController(ComplianceService complianceService, ImageNewRepository imageRepository) {
        this.complianceService = complianceService;
        this.imageRepository = imageRepository;
    }

    @PostMapping("/check")
    public ResponseEntity<ComplianceCheckResponse> checkCompliance(
            @Parameter(description = "The image ID to check")
            @RequestParam("imageId") UUID imageId,

            @Parameter(description = "Country code to check against (e.g., 'US', 'EU', 'SG'). Defaults to 'SG' if not provided.")
            @RequestParam(value = "countryCode", required = false) String countryCode) {

        // Find the image by ID
        ImageNewEntity image = imageRepository.findLatestRowByImageId(imageId);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        // Use the defaultCountryCode if countryCode is not provided
        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = defaultCountryCode;
        }

        // Run compliance checks with the specified country code
        ComplianceCheckResponse result = complianceService.checkCompliance(image, countryCode);

        if (result.getComplianceCheckStatus() == ComplianceCheckStatus.PASS) {
            return ResponseEntity.ok(result);
        } else {
            // Still return 200 OK but with failure details in the body
            return ResponseEntity.ok(result);
        }
    }
} 