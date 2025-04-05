package IS442.G1T3.IDPhotoGenerator.factory;

import java.util.UUID;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import org.springframework.stereotype.Component;

@Component
public class FloodFillFactory implements ImageEntityFactory<Void> {
    @Override
    public ImageNewEntity create(UUID imageId, UUID userId, int version, String baseImageUrl, Void params) {
        String fileName = imageId.toString() + "_" + version + ".png";

        return ImageNewEntity.builder()
                .imageId(imageId)
                .userId(userId)
                .version(version)
                .label("Flood Fill")
                .baseImageUrl(baseImageUrl)
                .currentImageUrl(fileName)
                .build();
    }
}
