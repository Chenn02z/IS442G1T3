package IS442.G1T3.IDPhotoGenerator.dto;

import java.util.UUID;

public class ImageUploadResponse {
    private UUID imageId;
    private String savedFilePath;
    private String status;
    private String message;

    public ImageUploadResponse(UUID imageId, String savedFilePath, String status, String message) {
        this.imageId = imageId;
        this.savedFilePath = savedFilePath;
        this.status = status;
        this.message = message;
    }

    public ImageUploadResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getImageId() {
        return imageId;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }

    public String getSavedFilePath() {
        return savedFilePath;
    }

    public void setSavedFilePath(String savedFilePath) {
        this.savedFilePath = savedFilePath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
