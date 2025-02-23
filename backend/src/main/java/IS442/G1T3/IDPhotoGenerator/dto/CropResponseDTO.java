package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CropResponseDTO {
    private UUID cropId;
    private UUID imageId;
    private String croppedImageUrl;
    private double x;
    private double y;
    private double width;
    private double height;


}