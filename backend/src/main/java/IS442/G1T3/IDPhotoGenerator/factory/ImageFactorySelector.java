package IS442.G1T3.IDPhotoGenerator.factory;

import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.model.enums.ImageOperationType;

@Component
public class ImageFactorySelector {
    public static ImageEntityFactory<?> getFactory(ImageOperationType type) {
        switch (type) {
            case ORIGINAL:
                return new OriginalImageFactory();
            case CROP:
                return new CropImageFactory();
            case BACKGROUND_REMOVAL:
                return new BackgroundRemovalFactory();
            case CARTOONISE:
                return new CartooniseFactory();
            case FLOODFILL:
                return new FloodFillFactory();
            default:
                throw new IllegalArgumentException("Unknown operation type: " + type);
        }
    }
}
