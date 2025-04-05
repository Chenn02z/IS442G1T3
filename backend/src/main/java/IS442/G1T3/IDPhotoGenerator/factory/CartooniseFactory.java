package IS442.G1T3.IDPhotoGenerator.factory;

import java.util.UUID;

import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

@Component
public class CartooniseFactory implements ImageEntityFactory<Void> {
    @Override
    public ImageNewEntity create(UUID imageId, UUID userId, int version, String baseImageUrl, Void params) {
        String processedFileName = imageId.toString() + "_" + version + ".png";

        return ImageNewEntity.builder()
                .imageId(imageId)
                .userId(userId)
                .version(version)
                .label("Cartoonise")
                .baseImageUrl(baseImageUrl)
                .currentImageUrl(processedFileName)
                .build();
    }
}
