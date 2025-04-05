package IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.dto.ComplianceCheckResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ComplianceCheckStatus;

/**
 * Checks that the image background is uniformly white or off-white
 * and that there are no distracting patterns or colors.
 */
@Component
public class BackgroundComplianceChecker implements ComplianceChecker {

    @Value("${image.storage.path}")
    private String storagePath;

    private ComplianceChecker nextComplianceChecker;

    /**
     * Checks if the photo background is near-white. A simple approach is
     * to sample the corner pixels (top-left, top-right, bottom-left, bottom-right)
     * or a few columns along the edges. You can refine as needed.
     */
    @Override
    public ComplianceCheckResponse checkFailed(ImageNewEntity photo) {
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

            // Simple sampling of corner pixels (you can improve this)
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();

            // Check the top left and right corners (not checking bottom corners as it may not be white)
            Color topLeft = new Color(bufferedImage.getRGB(0, 0));
            Color topRight = new Color(bufferedImage.getRGB(width - 1, 0));

            // Define your own threshold for "white/off-white":
            // For example, each channel >= 220 => near white
            if (!isWhiteish(topLeft) || !isWhiteish(topRight)) {
                throw new RuntimeException("Background must be plain white/off-white and free of patterns.");
            }
        } catch (IOException e) {
            return ComplianceCheckResponse.builder()
                    .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                    .message(String.format("An unexpected error occurred while checking background: %s", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return ComplianceCheckResponse.builder()
                    .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                    .message(String.format("Background Compliance check failed: %s", e.getMessage()))
                    .build();
        }

        return ComplianceCheckResponse.builder()
                .complianceCheckStatus(ComplianceCheckStatus.PASS)
                .message("Background Compliance check passed")
                .build();
    }

    private boolean isWhiteish(Color color) {
        // e.g., threshold is each channel >= 220
        return color.getRed() >= 220
                && color.getGreen() >= 220
                && color.getBlue() >= 220;
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
