package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.service.ClothesReplacementService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replace-clothes")
public class ClothesReplacementController {

    @Autowired
    private ClothesReplacementService clothesReplacementService;


    /**
     * Endpoint to process an image by overlaying clothes.
     * @param imageId The UUID of the image to process.
     * @return The updated ImageNewEntity after processing.
     * @throws Exception if processing fails.
     */
    @GetMapping("/{imageId}")
    public ImageNewEntity replaceClothes(@PathVariable("imageId") UUID imageId) throws Exception {
        return clothesReplacementService.OverlaidImage(imageId);
    }
}
