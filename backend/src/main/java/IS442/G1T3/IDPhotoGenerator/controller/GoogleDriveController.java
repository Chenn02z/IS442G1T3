package IS442.G1T3.IDPhotoGenerator.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.UserStoragePreference;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.UserStoragePreferenceRepository;
import IS442.G1T3.IDPhotoGenerator.service.FileStorageService;
import IS442.G1T3.IDPhotoGenerator.service.GoogleDriveService;
import IS442.G1T3.IDPhotoGenerator.service.ImageUploadService;

@RestController
@RequestMapping("/api/google-drive")
public class GoogleDriveController {

    private final GoogleDriveService driveService;
    private final UserStoragePreferenceRepository preferenceRepository;
    private final FileStorageService fileStorageService;
    private final ImageUploadService imageUploadService;
    private final ImageNewRepository imageNewRepository;

    @Value("${image.storage.path}")
    private String storagePath;
    public GoogleDriveController(
            GoogleDriveService driveService,
            UserStoragePreferenceRepository preferenceRepository,
            FileStorageService fileStorageService,
            ImageUploadService imageUploadService,
            ImageNewRepository imageNewRepository) {
        this.driveService = driveService;
        this.preferenceRepository = preferenceRepository;
        this.fileStorageService = fileStorageService;
        this.imageUploadService = imageUploadService;
        this.imageNewRepository = imageNewRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImageToDrive(
            @RequestParam("image") MultipartFile image,
            @RequestParam UUID userId) {
        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select an image to upload.");
            }

            String fileName = image.getOriginalFilename();
            String mimeType = image.getContentType();

            // Save the file locally
            String localFileName = UUID.randomUUID().toString() + "_" + fileName;
            String localFilePath = System.getProperty("user.dir") + File.separator + storagePath + File.separator + localFileName;
            
            java.io.File localFile = new java.io.File(localFilePath);
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                fos.write(image.getBytes());
            }

            // Upload to Google Drive
            String fileId = driveService.uploadFileFromPath(userId.toString(), fileName, mimeType, localFilePath);
            
            // Make the file public and get its direct link
            String publicUrl = driveService.makeFilePublic(userId.toString(), fileId);

            Map<String, String> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("url", publicUrl);
            response.put("message", "Image uploaded successfully to Google Drive.");

            // Clean up local file
            localFile.delete();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    @PostMapping("/upload-by-id")
    public ResponseEntity<?> uploadImageToDriveById(
            @RequestParam String imageId,
            @RequestParam UUID userId) {
        try {
            // Convert the String imageId to UUID
            UUID imageUuid = UUID.fromString(imageId);
            
            // Find the latest version of the image using Optional
            Optional<ImageNewEntity> imageOptional = imageNewRepository.findTopByImageIdOrderByVersionDesc(imageUuid);
            
            if (imageOptional.isEmpty()) {
                return ResponseEntity.badRequest().body("Image not found with ID: " + imageId);
            }
            
            ImageNewEntity image = imageOptional.get();
            String filename = image.getCurrentImageUrl();
            
            // Build the path to the file
            Path filePath = Paths.get(storagePath).resolve(filename);
            File localFile = filePath.toFile();
            
            if (!localFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Image file not found: " + filename);
            }
            
            // Determine MIME type
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) {
                // Default to PNG if can't determine
                mimeType = "image/png";
            }
            
            // Upload to Google Drive
            String fileId = driveService.uploadFileFromPath(
                userId.toString(),
                "Photo_" + (image.getLabel() != null ? image.getLabel() : imageId),
                mimeType,
                localFile.getAbsolutePath()
            );
            
            // Make the file public 
            String publicUrl = driveService.makeFilePublic(userId.toString(), fileId);
            
            Map<String, String> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("url", publicUrl);
            response.put("message", "Image uploaded successfully to Google Drive.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid image ID format: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload image: " + e.getMessage());
        }
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl(
            @RequestParam UUID userId,
            @RequestParam String redirectUrl) {
        try {
            String authUrl = driveService.getAuthorizationUrl(userId.toString(), redirectUrl);
            Map<String, String> response = new HashMap<>();
            response.put("authUrl", authUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/auth-callback")
    public ResponseEntity<?> handleAuthCallback(
            @RequestParam UUID userId,
            @RequestParam String code,
            @RequestParam String redirectUrl,
            @RequestParam(required = false, defaultValue = "googleDriveStorageProvider") String providerType) {
        try {
            driveService.handleAuthorizationCode(userId.toString(), code, redirectUrl);
            
            // Save/update user's storage preference
            UserStoragePreference preference = preferenceRepository.findByUserId(userId);
            if (preference == null) {
                preference = new UserStoragePreference();
                preference.setUserId(userId);
                preference.setProviderType(providerType);
                preference.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
                preference.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            } else {
                preference.setProviderType(providerType);
                preference.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            }
            preferenceRepository.save(preference);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Log the specific error for debugging
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Add new endpoint to check user's storage preference
    @GetMapping("/storage/preference/{userId}")
    public ResponseEntity<?> getUserStoragePreference(@PathVariable UUID userId) {
        try {
            UserStoragePreference preference = preferenceRepository.findByUserId(userId);
            if (preference == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("provider", preference.getProviderType());
            response.put("updatedAt", preference.getUpdatedAt().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<?> getFileInfo(
            @PathVariable String fileId,
            @RequestParam UUID userId) {
        try {
            // Make sure the file is publicly accessible
            String publicUrl = driveService.makeFilePublic(userId.toString(), fileId);
            
            Map<String, String> response = new HashMap<>();
            response.put("fileId", fileId);
            response.put("url", publicUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve file: " + e.getMessage());
        }
    }
}