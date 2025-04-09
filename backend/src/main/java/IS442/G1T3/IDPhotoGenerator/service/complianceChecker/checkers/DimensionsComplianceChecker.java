package IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.dto.ComplianceCheckResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ComplianceCheckStatus;

/**
 * Checks that the dimensions fits to the passport ID dimensions standards
 * based on the country specified or the default standard.
 */
@Component
public class DimensionsComplianceChecker implements ComplianceChecker {
    @Value("${image.storage.path}")
    private String storagePath;

    @Value("${default.country.code:SG}")
    private String defaultCountryCode;

    private ComplianceChecker nextComplianceChecker;

    @Autowired
    private Map<String, PhotoDimensionStandard> photoDimensionStandards;

    @Override
    public ComplianceCheckResponse checkFailed(ImageNewEntity photo, String countryCode) {
        String imagePath = String.format("%s/%s", storagePath, photo.getCurrentImageUrl());

        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                throw new RuntimeException("File does not exist at path: " + imagePath);
            }

            BufferedImage bufferedImage = ImageIO.read(file);
            if (bufferedImage == null) {
                throw new RuntimeException("Unable to read the image at path: " + imagePath);
            }

            int actualWidth = bufferedImage.getWidth();
            int actualHeight = bufferedImage.getHeight();

            // If countryCode is null, empty, or "ANY", check against any standard
            if (countryCode == null || countryCode.isEmpty() || "ANY".equalsIgnoreCase(countryCode)) {
                // Check if the photo complies with any of the standards
                boolean compliesWithAnyStandard = false;
                PhotoDimensionStandard matchedStandard = null;

                for (PhotoDimensionStandard standard : photoDimensionStandards.values()) {
                    if (standard.compliesWith(actualWidth, actualHeight)) {
                        compliesWithAnyStandard = true;
                        matchedStandard = standard;
                        break;
                    }
                }

                if (!compliesWithAnyStandard) {
                    // Use default country code for error message if no standards match
                    PhotoDimensionStandard defaultStandard = photoDimensionStandards.get(defaultCountryCode);
                    if (defaultStandard == null) {
                        throw new RuntimeException("No dimension standard found for default country code: " + defaultCountryCode);
                    }

                    throw new RuntimeException(String.format(
                            "Photo dimensions do not comply with any standard. Default requirement is %s for %s. Found: %dx%d",
                            defaultStandard.getDimensionRequirements(), defaultStandard.getCountryName(),
                            actualWidth, actualHeight
                    ));
                }

                return ComplianceCheckResponse.builder()
                        .complianceCheckStatus(ComplianceCheckStatus.PASS)
                        .message("Dimensions comply with " + matchedStandard.getCountryName() + " standard")
                        .build();
            } else {
                // Check against the specific country standard
                PhotoDimensionStandard standard = photoDimensionStandards.get(countryCode.toUpperCase());
                if (standard == null) {
                    throw new RuntimeException("No dimension standard found for country code: " + countryCode);
                }

                if (!standard.compliesWith(actualWidth, actualHeight)) {
                    throw new RuntimeException(String.format(
                            "Photo dimensions do not comply with %s standard. Required: %s. Found: %dx%d",
                            standard.getCountryName(), standard.getDimensionRequirements(),
                            actualWidth, actualHeight
                    ));
                }

                return ComplianceCheckResponse.builder()
                        .complianceCheckStatus(ComplianceCheckStatus.PASS)
                        .message("Dimensions comply with " + standard.getCountryName() + " standard")
                        .build();
            }

        } catch (IOException e) {
            return ComplianceCheckResponse.builder()
                    .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                    .message(String.format("An unexpected error occurred while checking image dimensions: %s", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return ComplianceCheckResponse.builder()
                    .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                    .message(String.format("Dimensions Compliance check failed: %s", e.getMessage()))
                    .build();
        }
    }

    @Override
    public void nextComplianceChecker(ComplianceChecker nextComplianceChecker) {
        this.nextComplianceChecker = nextComplianceChecker;
    }

    @Override
    public ComplianceChecker getNextComplianceChecker() {
        return nextComplianceChecker;
    }
}
