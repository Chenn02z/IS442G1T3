package IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents standard photo dimensions for various countries' ID and passport photos.
 * All dimensions are in pixels at standard DPI (300).
 */
@Getter
@AllArgsConstructor
public class PhotoDimensionStandard {
    private final String countryCode;
    private final String countryName;
    private final int width;
    private final int height;
    private final int minWidth;
    private final int minHeight;
    private final int maxWidth;
    private final int maxHeight;

    /**
     * Creates a standard with exact dimensions (no tolerance)
     */
    public PhotoDimensionStandard(String countryCode, String countryName, int width, int height) {
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.width = width;
        this.height = height;
        this.minWidth = width;
        this.minHeight = height;
        this.maxWidth = width;
        this.maxHeight = height;
    }

    /**
     * Checks if the given dimensions comply with this standard
     */
    public boolean compliesWith(int imageWidth, int imageHeight) {
        return imageWidth >= minWidth && imageWidth <= maxWidth
                && imageHeight >= minHeight && imageHeight <= maxHeight;
    }

    /**
     * Returns human-readable dimension requirements
     */
    public String getDimensionRequirements() {
        if (width == minWidth && width == maxWidth && height == minHeight && height == maxHeight) {
            return String.format("Exactly %dx%d pixels", width, height);
        } else {
            return String.format("Between %dx%d and %dx%d pixels", minWidth, minHeight, maxWidth, maxHeight);
        }
    }
} 