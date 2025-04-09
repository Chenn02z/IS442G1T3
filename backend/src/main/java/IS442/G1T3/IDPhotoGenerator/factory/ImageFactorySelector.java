package IS442.G1T3.IDPhotoGenerator.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.model.enums.ImageOperationType;

@Component
public class ImageFactorySelector {
    
    private final OriginalImageFactory originalImageFactory;
    private final CropImageFactory cropImageFactory;
    private final CartooniseFactory cartooniseFactory;
    private final FloodFillFactory floodFillFactory;
    
    @Autowired
    public ImageFactorySelector(
            OriginalImageFactory originalImageFactory,
            CropImageFactory cropImageFactory,
            CartooniseFactory cartooniseFactory,
            FloodFillFactory floodFillFactory) {
        this.originalImageFactory = originalImageFactory;
        this.cropImageFactory = cropImageFactory;
        this.cartooniseFactory = cartooniseFactory;
        this.floodFillFactory = floodFillFactory;
    }
    
    public ImageEntityFactory<?> getFactory(ImageOperationType type) {
        switch (type) {
            case ORIGINAL:
                return originalImageFactory;
            case CROP:
                return cropImageFactory;
            case CARTOONISE:
                return cartooniseFactory;
            case FLOODFILL:
                return floodFillFactory;
            default:
                throw new IllegalArgumentException("Unknown operation type: " + type);
        }
    }
}
