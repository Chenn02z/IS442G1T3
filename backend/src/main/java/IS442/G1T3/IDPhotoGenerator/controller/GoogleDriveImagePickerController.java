package IS442.G1T3.IDPhotoGenerator.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.model.File;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.GoogleDriveService;
import IS442.G1T3.IDPhotoGenerator.service.ImageUploadService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/google-drive")
@RequiredArgsConstructor
public class GoogleDriveImagePickerController {

    private final GoogleDriveService driveService;
    private final ImageUploadService imageUploadService;

    
    @GetMapping("/list-images")
    public ResponseEntity<?> listImages(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            // Check if we have a valid user ID
            if (userId == null) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "User ID is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check if the user is authenticated first
            boolean isAuthenticated = driveService.isUserAuthenticated(userId.toString());
            if (!isAuthenticated) {
                Map<String, Object> response = new HashMap<>();
                response.put("authenticated", false);
                response.put("error", "User not authenticated with Google Drive");
                response.put("authRequired", true);
                return ResponseEntity.status(401).body(response);
            }

            // Query for image files only (JPG and PNG)
            String query = "(mimeType = 'image/jpeg' or mimeType = 'image/png') and trashed=false";
            List<File> allFiles = driveService.listFiles(userId.toString(), query);

            if (allFiles == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, allFiles.size());
            List<File> files = allFiles.subList(start, end);

            List<Map<String, String>> response = files.stream()
                .map(file -> {
                    Map<String, String> fileMap = new HashMap<>();
                    fileMap.put("id", file.getId());
                    fileMap.put("name", file.getName());
                    try {
                        // Generate thumbnail data URL
                        String thumbnailDataUrl = generateThumbnailDataUrl(userId.toString(), file.getId());
                        fileMap.put("thumbnail", thumbnailDataUrl);
                    } catch (IOException | GeneralSecurityException e) {
                        System.err.println("Error generating thumbnail for file " + file.getId() + ": " + e.getMessage());
                        fileMap.put("thumbnail", null); // Or a default placeholder
                    }
                    return fileMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> pagedResponse = new HashMap<>();
            pagedResponse.put("content", response);
            pagedResponse.put("page", page);
            pagedResponse.put("size", size);
            pagedResponse.put("totalElements", allFiles.size());
            pagedResponse.put("totalPages", (int) Math.ceil((double) allFiles.size() / size));

            return ResponseEntity.ok(pagedResponse);
        } catch (GoogleJsonResponseException e) {
            // Handle Google API specific errors
            if (e.getStatusCode() == 401) {
                Map<String, Object> response = new HashMap<>();
                response.put("authenticated", false);
                response.put("error", "Google Drive authentication failed: " + e.getDetails().getMessage());
                response.put("authRequired", true);
                return ResponseEntity.status(401).body(response);
            }

            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getDetails().getMessage());
            errorResponse.put("code", String.valueOf(e.getStatusCode()));
            return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            // Return a proper error response with details
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorType", e.getClass().getName());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/import-image")
    public ResponseEntity<?> importImageFromDrive(
            @RequestParam UUID userId,
            @RequestParam String fileId) {
        try {
            // Validate parameters
            if (userId == null || fileId == null || fileId.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "User ID and file ID are required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 1. Download the file from Google Drive
            InputStream fileStream = driveService.downloadFile(userId.toString(), fileId);
            
            // 2. Get file metadata to determine name and type
            File driveFile = driveService.getFile(userId.toString(), fileId);
            
            // 3. Create a temporary file to store the image
            java.io.File tempFile = java.io.File.createTempFile("drive-import-", ".jpg");
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            // 4. Create a MultipartFile from this temp file
            MultipartFile multipartFile = new MockMultipartFile(
                driveFile.getName(), 
                driveFile.getName(),
                driveFile.getMimeType(),
                new FileInputStream(tempFile)
            );
            
            // 5. Use your existing image upload service
            ImageNewEntity imageEntity = imageUploadService.processImage(multipartFile, userId);
            
            // 6. Return the appropriate response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("id", imageEntity.getImageId().toString());
            response.put("imageUrl", imageEntity.getCurrentImageUrl());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorType", e.getClass().getName());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/thumbnail-proxy")
    public ResponseEntity<byte[]> proxyThumbnail(
            @RequestParam UUID userId,
            @RequestParam String fileId) {
        try {
            // Get the file metadata to find the thumbnail URL
            File file = driveService.getFile(userId.toString(), fileId);
            String thumbnailUrl = file.getThumbnailLink();
            
            if (thumbnailUrl == null || thumbnailUrl.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Get user credentials to access the thumbnail
            Credential credentials = driveService.getUserCredentials(userId.toString());
            
            // Create an HTTP client
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(thumbnailUrl))
                .header("Authorization", "Bearer " + credentials.getAccessToken())
                .GET()
                .build();
            
            // Fetch the thumbnail
            HttpResponse<byte[]> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofByteArray());
            
            if (response.statusCode() == 200) {
                // Determine content type
                String contentType = "image/jpeg"; // Default
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(contentType));
                
                return new ResponseEntity<>(response.body(), headers, HttpStatus.OK);
            } else {
                return ResponseEntity.status(response.statusCode()).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String generateThumbnailDataUrl(String userId, String fileId) throws IOException, GeneralSecurityException {
        try {
            // 1. Get the file metadata
            File file = driveService.getFile(userId, fileId);
            String thumbnailUrl = file.getThumbnailLink();

            // 2. If a thumbnail link exists, use it
            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                // Download the thumbnail from the URL
                byte[] imageBytes = downloadThumbnail(userId, thumbnailUrl);
                if (imageBytes != null) {
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    return "data:image/jpeg;base64," + base64Image;
                }
            }

            // 3. If no thumbnail link or download fails, generate a thumbnail from the full image
            try (InputStream fileStream = driveService.downloadFile(userId, fileId)) {
                BufferedImage originalImage = ImageIO.read(fileStream);
                if (originalImage == null) {
                    throw new IOException("Could not read image from stream");
                }

                int width = originalImage.getWidth();
                int height = originalImage.getHeight();

                // Calculate thumbnail dimensions (e.g., 200x200)
                int thumbWidth = 200;
                int thumbHeight = 200;

                // Create a scaled thumbnail
                BufferedImage thumbnail = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
                thumbnail.getGraphics().drawImage(originalImage.getScaledInstance(thumbWidth, thumbHeight, java.awt.Image.SCALE_SMOOTH), 0, 0, null);

                // Convert to data URL
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageIO.write(thumbnail, "jpeg", byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                return "data:image/jpeg;base64," + base64Image;
            }
        } catch (Exception e) {
            System.err.println("Error generating thumbnail for file " + fileId + ": " + e.getMessage());
            throw e; // Re-throw the exception to be handled in listImages
        }
    }

    private byte[] downloadThumbnail(String userId, String thumbnailUrl) throws IOException, GeneralSecurityException {
        try {
            // Get user credentials
            Credential credentials = driveService.getUserCredentials(userId);

            // Create an HTTP client
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(thumbnailUrl))
                .header("Authorization", "Bearer " + credentials.getAccessToken())
                .GET()
                .build();

            // Fetch the thumbnail
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Failed to download thumbnail from URL: " + thumbnailUrl + ", status code: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error downloading thumbnail from URL: " + thumbnailUrl + ": " + e.getMessage());
            return null;
        }
    }
}