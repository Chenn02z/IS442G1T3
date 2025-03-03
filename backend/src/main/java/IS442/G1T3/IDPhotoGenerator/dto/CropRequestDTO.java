package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CropRequestDTO {
    private double x;
    private double y;
    private double width;
    private double height;

}
