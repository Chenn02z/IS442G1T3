package IS442.G1T3.IDPhotoGenerator;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.ImageCropNewService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

/**
 * Manual test for verifying the crop logic.
 * This can be run with the 'manual-crop-test' profile.
 * Example: java -Dspring.profiles.active=manual-crop-test -jar application.jar
 */
@Configuration
@Profile("manual-crop-test")
public class ManualCropTest {

    @Bean
    public CommandLineRunner testCropLogic(ImageCropNewService imageCropNewService) {
        return args -> {
            System.out.println("=== MANUAL CROP LOGIC TEST ===");
            
            // You can specify an existing image ID here to test with
            // For example, if you have an image with ID "123e4567-e89b-12d3-a456-426614174000"
            
            // 1. First try a test with an image that has a "Crop" label
            try {
                UUID imageId = UUID.fromString("REPLACE_WITH_REAL_UUID"); // Replace with a real image ID
                
                System.out.println("Fetching image for editing with ID: " + imageId);
                ImageNewEntity currentEntity = imageCropNewService.getImageForEditing(imageId);
                System.out.println("Current entity: " +
                        "id=" + currentEntity.getImageId() +
                        ", version=" + currentEntity.getVersion() +
                        ", label=" + currentEntity.getLabel() +
                        ", currentImageUrl=" + currentEntity.getCurrentImageUrl() +
                        ", baseImageUrl=" + currentEntity.getBaseImageUrl());
                
                if ("Crop".equals(currentEntity.getLabel())) {
                    System.out.println("Entity has 'Crop' label - will use baseImageUrl: " +
                            currentEntity.getBaseImageUrl());
                } else {
                    System.out.println("Entity has '" + currentEntity.getLabel() + 
                            "' label - will use currentImageUrl: " + currentEntity.getCurrentImageUrl());
                }
                
                System.out.println("Testing crop operation with example parameters (100, 100, 200, 200)...");
                ImageNewEntity result = imageCropNewService.saveCrop(imageId, 100, 100, 200, 200);
                System.out.println("Crop operation result: " +
                        "id=" + result.getImageId() +
                        ", version=" + result.getVersion() +
                        ", label=" + result.getLabel() +
                        ", currentImageUrl=" + result.getCurrentImageUrl() +
                        ", baseImageUrl=" + result.getBaseImageUrl());
                
                System.out.println("Test completed successfully!");
            } catch (Exception e) {
                System.err.println("Error during test: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }
} 