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
        GoogleAuthorizationCodeFlow flow = getFlow();
        flow.createAndStoreCredential(
                flow.newTokenRequest(authCode).setRedirectUri(redirectUrl).execute(),
                userId);
    }
    
    /**
     * Create or get Drive service for user
     */
    private Drive getDriveService(String userId) throws IOException, GeneralSecurityException {
        return userDriveServices.computeIfAbsent(userId, id -> {
            try {
                NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                Credential credential = getFlow().loadCredential(id);
                
                if (credential == null) {
                    throw new IllegalStateException("User not authenticated with Google Drive");
                }
                
                return new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create Google Drive service", e);
            }
        });
    }
    
    /**
     * Create OAuth flow
     */
    private GoogleAuthorizationCodeFlow getFlow() throws IOException, GeneralSecurityException {
        // Create client secrets manually instead of loading from file
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        
        // Set redirect URIs if needed
        // String[] uriArray = redirectUris.split(",");
        // details.setRedirectUris(Arrays.asList(uriArray));
        
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);
        
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
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
        Drive driveService = getDriveService(userId);
        
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        
        java.io.File localFile = new java.io.File(filePath);
        if (!localFile.exists()) {
            throw new IOException("Local file not found: " + filePath);
        }
        
        InputStreamContent mediaContent = new InputStreamContent(
                mimeType, 
                new FileInputStream(localFile));
        
        // Set the content length to avoid upload errors
        mediaContent.setLength(localFile.length());
        
        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();
        
        return file.getId();
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

    // Add this method to your GoogleDriveService class
    public boolean isUserAuthenticated(String userId) {
        try {
            // Check if credentials exist and are valid
            Credential credentials = getUserCredentials(userId);
            if (credentials == null) {
                return false;
            }
            
            // Check if token is expired and can't be refreshed
            if (credentials.getExpiresInSeconds() != null && 
                credentials.getExpiresInSeconds() <= 0 && 
                !credentials.refreshToken()) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            // Log the exception but don't throw it
            System.err.println("Error checking authentication status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public File getFile(String userId, String fileId) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        return driveService.files().get(fileId).execute();
    }

    /**
     * Make a file publicly accessible and return its web view link
     */
    public String makeFilePublic(String userId, String fileId) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService(userId);
        
        // Create a public permission
        Permission permission = new Permission();
        permission.setType("anyone");
        permission.setRole("reader");
        
        // Apply the permission to the file
        driveService.permissions().create(fileId, permission).execute();
        
        // Get the updated file with webViewLink
        File file = driveService.files().get(fileId)
            .setFields("id,webViewLink,webContentLink")
            .execute();
        
        // Return the direct link for viewing/downloading
        return file.getWebContentLink();
    }
}