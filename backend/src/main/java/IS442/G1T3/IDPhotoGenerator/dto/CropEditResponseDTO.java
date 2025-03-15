package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CropEditResponseDTO {

    private UUID imageId;
    private String originalImageUrl;
    private double x;       // can be null if no crop exists yet
    private double y;
    private double width;
    private double height;
    private String croppedFilePath;

    // No-argument constructor
//    public CropEditResponseDTO() {
//    }

    // Parameterized constructor
//    public CropEditResponseDTO(UUID imageId, String originalImageUrl, Integer x, Integer y, Integer width, Integer height, String croppedFilePath) {
//        this.imageId = imageId;
//        this.originalImageUrl = originalImageUrl;
//        this.x = x;
//        this.y = y;
//        this.width = width;
//        this.height = height;
//        this.croppedFilePath = croppedFilePath;
//    }



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
