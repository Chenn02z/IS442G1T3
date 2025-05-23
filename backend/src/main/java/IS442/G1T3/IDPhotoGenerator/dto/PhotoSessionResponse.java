package IS442.G1T3.IDPhotoGenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoSessionResponse<T> {
    private String status;
    private T data;
} 