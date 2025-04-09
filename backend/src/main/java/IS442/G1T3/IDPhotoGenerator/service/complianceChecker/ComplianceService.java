package IS442.G1T3.IDPhotoGenerator.service.complianceChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import IS442.G1T3.IDPhotoGenerator.dto.ComplianceCheckResponse;
import IS442.G1T3.IDPhotoGenerator.dto.ComplianceCheckResponse.ComplianceDetail;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ComplianceCheckStatus;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.ComplianceChecker;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.PhotoDimensionStandard;

/**
 * Service that handles compliance checking for ID photos.
 */
@Service
public class ComplianceService {

    private ComplianceChainBuilder complianceChainBuilder;
    private Map<String, PhotoDimensionStandard> photoDimensionStandards;

    public ComplianceService(ComplianceChainBuilder complianceChainBuilder, Map<String, PhotoDimensionStandard> photoDimensionStandards) {
        this.complianceChainBuilder = complianceChainBuilder;
        this.photoDimensionStandards = photoDimensionStandards;
    }
    /**
     * Checks if an image complies with ID photo standards.
     *
     * @param image       The image to check
     * @param countryCode Optional country code to check against. If null, checks against all standards.
     * @return The compliance check result
     */
    public ComplianceCheckResponse checkCompliance(ImageNewEntity image, String countryCode) {
        // Get the head of the compliance checker chain
        ComplianceChecker complianceChain = complianceChainBuilder.buildComplianceChain();

        // Collect results from all checkers in the chain
        List<ComplianceDetail> checkDetails = new ArrayList<>();
        ComplianceCheckStatus finalStatus = ComplianceCheckStatus.PASS;
        StringBuilder summaryMessage = new StringBuilder();

        // Start at the head of the chain
        ComplianceChecker currentChecker = complianceChain;
        while (currentChecker != null) {
            String checkerName = currentChecker.getClass().getSimpleName();
            String category = getCheckerCategory(checkerName);

            // Pass the country code to each checker
            ComplianceCheckResponse checkResult = currentChecker.checkFailed(image, countryCode);

            // Create a detail for this check
            ComplianceDetail detail = ComplianceDetail.builder()
                    .checkName(checkerName)
                    .status(checkResult.getComplianceCheckStatus())
                    .message(checkResult.getMessage())
                    .category(category)
                    .build();

            checkDetails.add(detail);

            // If any check fails, the overall status is a fail
            if (checkResult.getComplianceCheckStatus() == ComplianceCheckStatus.FAIL) {
                finalStatus = ComplianceCheckStatus.FAIL;
                if (summaryMessage.length() > 0) {
                    summaryMessage.append("; ");
                }
                summaryMessage.append(checkResult.getMessage());
            }

            // Move to the next checker in the chain
            currentChecker = currentChecker.getNextComplianceChecker();
        }

        // Create the final combined result
        ComplianceCheckResponse finalResult = ComplianceCheckResponse.builder()
                .complianceCheckStatus(finalStatus)
                .details(checkDetails)
                .build();

        // Set the summary message
        if (finalStatus == ComplianceCheckStatus.PASS) {
            finalResult.setMessage("All compliance checks passed");

            // Add country information if provided
            if (countryCode != null && !countryCode.isEmpty()) {
                PhotoDimensionStandard standard = photoDimensionStandards.get(countryCode.toUpperCase());
                if (standard != null) {
                    finalResult.setMessage(finalResult.getMessage() +
                            String.format(" (Complies with %s standard)", standard.getCountryName()));
                }
            }
        } else {
            finalResult.setMessage(summaryMessage.toString());
        }

        return finalResult;
    }

    /**
     * Extracts a user-friendly category from the checker class name.
     */
    private String getCheckerCategory(String checkerName) {
        if (checkerName.contains("Dimensions")) {
            return "dimensions";
        } else if (checkerName.contains("Background")) {
            return "background";
        } else if (checkerName.contains("Face")) {
            return "face";
        } else {
            return "other";
        }
    }
} 