package IS442.G1T3.IDPhotoGenerator.dto;

import java.util.UUID;

public class CropResponseDTO {
    private UUID cropId;
    private UUID imageId;
    private String croppedImageUrl;
    private int x;
    private int y;
    private int width;
    private int height;

    // Getters and Setters
    public UUID getCropId() {
        return cropId;
    }
    public void setCropId(UUID cropId) {
        this.cropId = cropId;
    }
    public UUID getImageId() {
        return imageId;
    }
    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }
    public String getCroppedImageUrl() {
        return croppedImageUrl;
    }
    public void setCroppedImageUrl(String croppedImageUrl) {
        this.croppedImageUrl = croppedImageUrl;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
}