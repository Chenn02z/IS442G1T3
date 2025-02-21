package IS442.G1T3.IDPhotoGenerator.model;

import lombok.Data;

@Data
public class FloodFillRequest {
    private int[] imageData;
    private int width;
    private int height;
    private int startX;
    private int startY;
    private int tolerance;
}


