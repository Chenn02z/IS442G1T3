package IS442.G1T3.IDPhotoGenerator.factory;

import java.util.UUID;

import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.dto.CropParams;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

@Component
public class CropImageFactory implements ImageEntityFactory<CropParams> {
    @Override
    public ImageNewEntity create(UUID imageId, UUID userId, int version, String baseImageUrl, CropParams params) {
        String fileName = imageId.toString() + "_" + version + ".png";
        String cropData = String.format("%d,%d,%d,%d", params.getX(), params.getY(), params.getWidth(), params.getHeight());

        return ImageNewEntity.builder()
                .imageId(imageId)
                .userId(userId)
                .version(version)
                .label("Crop")
                .baseImageUrl(baseImageUrl)
                .currentImageUrl(fileName)
                .cropData(cropData)
                .build();
    }
}

