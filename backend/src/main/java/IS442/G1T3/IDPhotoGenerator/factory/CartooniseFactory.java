package IS442.G1T3.IDPhotoGenerator.factory;

import java.util.UUID;

import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

@Component
public class CartooniseFactory implements ImageEntityFactory<Void> {
    @Override
    public ImageNewEntity create(UUID imageId, UUID userId, int version, String processedFileName, Void params) {
        String fileName = imageId.toString() + "_" + version + ".png";

        return ImageNewEntity.builder()
                .imageId(imageId)
                .userId(userId)
                .version(version)
                .label("Cartoonise")
                .baseImageUrl(processedFileName)
                .currentImageUrl(processedFileName)
                .build();
    }
}
