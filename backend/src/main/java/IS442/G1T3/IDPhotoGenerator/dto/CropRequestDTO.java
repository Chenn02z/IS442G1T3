package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CropRequestDTO {
    private int x;
    private int y;
    private int width;
    private int height;

}
