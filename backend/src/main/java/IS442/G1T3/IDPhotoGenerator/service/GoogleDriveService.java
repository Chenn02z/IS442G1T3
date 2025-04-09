package IS442.G1T3.IDPhotoGenerator.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleDriveService {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String APPLICATION_NAME = "ID Photo Generator";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    
    private final ResourceLoader resourceLoader;
    private final Map<String, Drive> userDriveServices = new ConcurrentHashMap<>();

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.redirect.uris}")
    private String redirectUris;

    @Value("${image.storage.path}")
    private String storagePath;
    
    public GoogleDriveService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Get OAuth 2.0 authorization URL for a user to grant permissions
     */
    public String getAuthorizationUrl(String userId, String redirectUrl) throws IOException, GeneralSecurityException {
        GoogleAuthorizationCodeFlow flow = getFlow();
        return flow.newAuthorizationUrl()
                .setRedirectUri(redirectUrl)
                .setState(userId)
                .build();
    }
    
    /**
     * Handle OAuth callback and store credentials
     */
    public void handleAuthorizationCode(String userId, String authCode, String redirectUrl) 
            throws IOException, GeneralSecurityException {
        log.info("Handling authorization code for user: {}", userId);
        
        try {
            GoogleAuthorizationCodeFlow flow = getFlow();
            var tokenRequest = flow.newTokenRequest(authCode).setRedirectUri(redirectUrl);
            log.debug("Token request created: {}", tokenRequest);
            
            var tokenResponse = tokenRequest.execute();
            log.info("Token response received. Access token expires in: {} seconds", tokenResponse.getExpiresInSeconds());
            
            flow.createAndStoreCredential(tokenResponse, userId);
            log.info("Successfully stored credentials for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to handle authorization code for user: {}", userId, e);
            throw e;
        }
    }
    
    /**
     * Create or get Drive service for user
     */
    private Drive getDriveService(String userId) throws IOException, GeneralSecurityException {
        log.info("Getting Drive service for user: {}", userId);
        
        return userDriveServices.computeIfAbsent(userId, id -> {
            try {
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                Credential credential = getFlow().loadCredential(id);
                
                if (credential == null) {
                    log.error("User {} not authenticated with Google Drive", id);
                    throw new IllegalStateException("User not authenticated with Google Drive");
                }
                
                log.info("Credential loaded for user: {}. Token expires in: {} seconds", 
                        id, credential.getExpiresInSeconds());
                
                // Attempt to refresh token if it's about to expire
                if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 300) {
                    log.info("Token is about to expire. Attempting to refresh...");
                    boolean refreshed = credential.refreshToken();
                    log.info("Token refresh result: {}", refreshed);
                }
                
                Drive service = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                
                log.info("Drive service created successfully for user: {}", id);
                return service;
            } catch (Exception e) {
                log.error("Failed to create Google Drive service for user: {}", id, e);
                throw new RuntimeException("Failed to create Google Drive service", e);
            }
        });
    }
    
    /**
     * Create OAuth flow
     */
    private GoogleAuthorizationCodeFlow getFlow() throws IOException, GeneralSecurityException {
        log.debug("Creating Google Authorization Code Flow");
        
        // Create client secrets manually instead of loading from file
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        
        log.debug("Client ID: {}", clientId);
        log.debug("Redirect URIs: {}", redirectUris);
        
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);
        
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        log.info("Creating authorization flow with scopes: {}", SCOPES);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        
        log.debug("Authorization flow created successfully");
        return flow;
    }
    
    /**
     * List files in user's Drive
     */
    public List<File> listFiles(String userId, String query) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        FileList result = driveService.files().list()
                .setQ(query)
                .setPageSize(100)
                .setFields("files(id, name, mimeType, thumbnailLink)")
                .execute();
        return result.getFiles();
    }
    
    /**
     * Upload file to Drive
     */
    public String uploadFile(String userId, String fileName, String mimeType, InputStream inputStream) 
            throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        
        InputStreamContent mediaContent = new InputStreamContent(mimeType, inputStream);
        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
        
        return file.getId();
    }

    /**
     * Upload file to Drive from a File object
     */
    public String uploadFileFromPath(String userId, String fileName, String mimeType, String filePath) 
            throws IOException, GeneralSecurityException {
        log.info("Uploading file from path for user: {}, file: {}, path: {}", userId, fileName, filePath);
        
        Drive driveService = getDriveService(userId);
        
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        
        java.io.File localFile = new java.io.File(filePath);
        if (!localFile.exists()) {
            log.error("Local file not found: {}", filePath);
            throw new IOException("Local file not found: " + filePath);
        }
        
        log.info("Local file size: {} bytes", localFile.length());
        
        try (FileInputStream fileInputStream = new FileInputStream(localFile)) {
            InputStreamContent mediaContent = new InputStreamContent(
                    mimeType, 
                    fileInputStream);
            
            // Set the content length to avoid upload errors
            mediaContent.setLength(localFile.length());
            
            log.info("Starting file upload to Google Drive");
            File file = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id,name,size")
                    .execute();
            
            log.info("File uploaded successfully. Google Drive file ID: {}, name: {}, size: {}", 
                    file.getId(), file.getName(), file.getSize());
            
            return file.getId();
        } catch (IOException e) {
            log.error("Error uploading file to Google Drive", e);
            throw e;
        }
    }
    
    /**
     * Download file from Drive
     */
    public InputStream downloadFile(String userId, String fileId) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        return driveService.files().get(fileId)
                .executeMediaAsInputStream();
    }
    
    /**
     * Delete file from Drive
     */
    public boolean deleteFile(String userId, String fileId) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        driveService.files().delete(fileId).execute();
        return true;
    }
    
    /**
     * Make file public and get shareable link
     */
    public String getPublicLink(String userId, String fileId) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        
        // Create a public permission
        Permission permission = new Permission()
                .setType("anyone")
                .setRole("reader");
        
        driveService.permissions().create(fileId, permission).execute();
        
        // Get file to retrieve web view link
        File file = driveService.files().get(fileId)
                .setFields("webViewLink")
                .execute();
        
        return file.getWebViewLink();
    }

    public boolean isUserConnected(String userId) {
        try {
            // Check if we have valid credentials for this user
            return getUserCredentials(userId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retrieve user credentials
     */
    public Credential getUserCredentials(String userId) throws IOException, GeneralSecurityException {
        GoogleAuthorizationCodeFlow flow = getFlow();
        return flow.loadCredential(userId);
    }

    /**
     * Improved isUserAuthenticated with detailed logging
     */
    public boolean isUserAuthenticated(String userId) {
        log.info("Checking if user is authenticated: {}", userId);
        
        try {
            // Check if credentials exist and are valid
            Credential credentials = getUserCredentials(userId);
            
            if (credentials == null) {
                log.warn("No credentials found for user: {}", userId);
                return false;
            }
            
            log.info("Credentials found for user: {}. Token expires in: {} seconds", 
                    userId, credentials.getExpiresInSeconds());
            
            // Check if token is expired and can't be refreshed
            if (credentials.getExpiresInSeconds() != null && 
                credentials.getExpiresInSeconds() <= 60) {
                log.info("Token is expired or about to expire. Attempting to refresh...");
                boolean refreshSuccess = credentials.refreshToken();
                log.info("Token refresh result: {}", refreshSuccess);
                
                if (!refreshSuccess) {
                    log.warn("Failed to refresh token for user: {}", userId);
                    return false;
                }
            }
            
            log.info("User {} is authenticated", userId);
            return true;
        } catch (Exception e) {
            log.error("Error checking authentication status for user: {}", userId, e);
            return false;
        }
    }

    public File getFile(String userId, String fileId) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        return driveService.files().get(fileId).execute();
    }

    /**
     * Make a file publicly accessible with detailed logging
     */
    public String makeFilePublic(String userId, String fileId) throws IOException, GeneralSecurityException {
        log.info("Making file public for user: {}, fileId: {}", userId, fileId);
        
        Drive driveService = getDriveService(userId);
        
        try {
            // First verify the file exists and we have access
            File fileCheck = driveService.files().get(fileId).execute();
            log.info("File found: {}, name: {}", fileId, fileCheck.getName());
            
            // Create a public permission
            Permission permission = new Permission();
            permission.setType("anyone");
            permission.setRole("reader");
            
            log.info("Creating public permission for file");
            driveService.permissions().create(fileId, permission).execute();
            log.info("Public permission created successfully");
            
            // Get the updated file with webViewLink
            File file = driveService.files().get(fileId)
                .setFields("id,webViewLink,webContentLink")
                .execute();
            
            log.info("File public links - webViewLink: {}, webContentLink: {}", 
                    file.getWebViewLink(), file.getWebContentLink());
            
            // Return the direct link for viewing/downloading
            return file.getWebContentLink();
        } catch (Exception e) {
            log.error("Error making file public", e);
            throw e;
        }
    }
}