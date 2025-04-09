package IS442.G1T3.IDPhotoGenerator.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.PhotoDimensionStandard;

/**
 * Configuration class that defines photo dimension standards for different countries.
 */
@Configuration
public class PhotoStandardsConfig {

    @Bean
    public Map<String, PhotoDimensionStandard> photoDimensionStandards() {
        Map<String, PhotoDimensionStandard> standards = new HashMap<>();

        // US passport photo (2x2 inches at 300 DPI)
        standards.put("US", new PhotoDimensionStandard(
                "US", "United States", 600, 600
        ));

        // EU/Schengen standard (35x45mm at 300 DPI)
        standards.put("EU", new PhotoDimensionStandard(
                "EU", "European Union", 413, 531,
                // Allow +/- 5% tolerance
                392, 504, 434, 558
        ));

        // UK passport (35x45mm at 300 DPI)
        standards.put("GB", new PhotoDimensionStandard(
                "GB", "United Kingdom", 413, 531
        ));

        // Singapore passport photo (35x45mm)
        standards.put("SG", new PhotoDimensionStandard(
                "SG", "Singapore", 400, 514
        ));

        // Indian passport (35x45mm)
        standards.put("IN", new PhotoDimensionStandard(
                "IN", "India", 413, 531
        ));

        // Chinese passport (33x48mm)
        standards.put("CN", new PhotoDimensionStandard(
                "CN", "China", 390, 567
        ));

        // Canada passport (50x70mm)
        standards.put("CA", new PhotoDimensionStandard(
                "CA", "Canada", 590, 826
        ));

        // Australia passport (35x45mm)
        standards.put("AU", new PhotoDimensionStandard(
                "AU", "Australia", 413, 531
        ));

        // Add more countries as needed

        return standards;
    }
}