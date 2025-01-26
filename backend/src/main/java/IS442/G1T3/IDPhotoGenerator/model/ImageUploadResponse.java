package IS442.G1T3.IDPhotoGenerator.model;

import java.util.UUID;

public class ImageUploadResponse {
    private UUID imageId;
    private String status;
    private String message;

    public ImageUploadResponse(UUID imageId, String status, String message) {
        this.imageId = imageId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
