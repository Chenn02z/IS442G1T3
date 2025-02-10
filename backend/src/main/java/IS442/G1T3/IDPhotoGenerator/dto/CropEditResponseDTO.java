package IS442.G1T3.IDPhotoGenerator.dto;

import java.util.Objects;
import java.util.UUID;

public class CropEditResponseDTO {

    private UUID imageId;
    private String originalImageUrl;
    private Integer x;       // can be null if no crop exists yet
    private Integer y;
    private Integer width;
    private Integer height;

    // No-argument constructor
    public CropEditResponseDTO() {
    }

    // Parameterized constructor
    public CropEditResponseDTO(UUID imageId, String originalImageUrl, Integer x, Integer y, Integer width, Integer height) {
        this.imageId = imageId;
        this.originalImageUrl = originalImageUrl;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Getters and Setters
    public UUID getImageId() {
        return imageId;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public void setOriginalImageUrl(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    // Override toString() for debugging and logging
    @Override
    public String toString() {
        return "CropEditResponseDTO{" +
                "imageId=" + imageId +
                ", originalImageUrl='" + originalImageUrl + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    // Override equals() to compare DTO objects
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CropEditResponseDTO)) return false;
        CropEditResponseDTO that = (CropEditResponseDTO) o;
        return Objects.equals(imageId, that.imageId) &&
                Objects.equals(originalImageUrl, that.originalImageUrl) &&
                Objects.equals(x, that.x) &&
                Objects.equals(y, that.y) &&
                Objects.equals(width, that.width) &&
                Objects.equals(height, that.height);
    }

    // Override hashCode() for consistent hashing behavior
    @Override
    public int hashCode() {
        return Objects.hash(imageId, originalImageUrl, x, y, width, height);
    }
}
