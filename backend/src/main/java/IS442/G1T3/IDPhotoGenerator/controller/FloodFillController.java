package IS442.G1T3.IDPhotoGenerator.controller;

import IS442.G1T3.IDPhotoGenerator.model.FloodFillRequest;
import IS442.G1T3.IDPhotoGenerator.model.FloodFillResponse;
import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FloodFillController {

    private final FloodFillService floodFillService;

    @Autowired
    public FloodFillController(FloodFillService floodFillService) {
        this.floodFillService = floodFillService;
    }

    @PostMapping("/api/background-remover/floodfill")
    public FloodFillResponse processImage(@RequestBody FloodFillRequest request) {
        return floodFillService.processImage(request);
    }
}