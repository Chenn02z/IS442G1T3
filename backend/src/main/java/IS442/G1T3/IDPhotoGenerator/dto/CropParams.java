package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CropParams {
    private int x;
    private int y;
    private int width;
    private int height;
}
