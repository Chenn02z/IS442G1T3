package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
public class CropEditResponseDTO {

    // Getters and Setters

    private UUID imageId;
    private String originalImageUrl;
    @Setter
    private double x;       // can be null if no crop exists yet
    @Setter
    private double y;
    @Setter
    private double width;
    @Setter
    private double height;

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
        if (!(o instanceof CropEditResponseDTO that)) return false;
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
