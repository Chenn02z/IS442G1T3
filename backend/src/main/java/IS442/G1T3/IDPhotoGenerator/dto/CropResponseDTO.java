package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CropResponseDTO {
    private String savedFilePath;
    public CropResponseDTO(String savedFilePath) {
        this.savedFilePath = savedFilePath;
    }
}