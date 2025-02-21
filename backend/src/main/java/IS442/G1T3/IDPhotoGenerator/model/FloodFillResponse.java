package IS442.G1T3.IDPhotoGenerator.model;

import lombok.Data;

@Data
public class FloodFillResponse {
    private int[] data;

    public FloodFillResponse(int[] data) {
        this.data = data;
    }
}