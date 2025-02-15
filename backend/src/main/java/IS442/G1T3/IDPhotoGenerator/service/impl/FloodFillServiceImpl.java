package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.FloodFillRequest;
import IS442.G1T3.IDPhotoGenerator.model.FloodFillResponse;
import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import org.springframework.stereotype.Service;

@Service
public class FloodFillServiceImpl implements FloodFillService {

    @Override
    public FloodFillResponse processImage(FloodFillRequest request) {
        int width = request.getWidth();
        int height = request.getHeight();
        int[] imageData = request.getImageData();
        int startX = request.getStartX();
        int startY = request.getStartY();
        int tolerance = request.getTolerance();

        boolean[][] visited = new boolean[height][width];
        int targetColor = getPixel(imageData, startX, startY, width);

        // Perform flood fill
        floodFill(imageData, startX, startY, targetColor, width, height, tolerance, visited);

        // Create transparent background
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!visited[y][x]) {
                    setPixelTransparent(imageData, x, y, width);
                }
            }
        }

        return new FloodFillResponse(imageData);
    }

    private void floodFill(int[] imageData, int x, int y, int targetColor, int width, int height,
                           int tolerance, boolean[][] visited) {
        if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]) {
            return;
        }

        int currentColor = getPixel(imageData, x, y, width);
        if (!isColorSimilar(currentColor, targetColor, tolerance)) {
            return;
        }

        visited[y][x] = true;

        // Recursive flood fill in 4 directions
        floodFill(imageData, x + 1, y, targetColor, width, height, tolerance, visited);
        floodFill(imageData, x - 1, y, targetColor, width, height, tolerance, visited);
        floodFill(imageData, x, y + 1, targetColor, width, height, tolerance, visited);
        floodFill(imageData, x, y - 1, targetColor, width, height, tolerance, visited);
    }

    private int getPixel(int[] imageData, int x, int y, int width) {
        int index = (y * width + x) * 4;
        return (imageData[index] << 24) | (imageData[index + 1] << 16) |
                (imageData[index + 2] << 8) | imageData[index + 3];
    }

    private void setPixelTransparent(int[] imageData, int x, int y, int width) {
        int index = (y * width + x) * 4;
        imageData[index + 3] = 0; // Set alpha to 0
    }

    private boolean isColorSimilar(int color1, int color2, int tolerance) {
        int r1 = (color1 >> 24) & 0xFF;
        int g1 = (color1 >> 16) & 0xFF;
        int b1 = (color1 >> 8) & 0xFF;

        int r2 = (color2 >> 24) & 0xFF;
        int g2 = (color2 >> 16) & 0xFF;
        int b2 = (color2 >> 8) & 0xFF;

        return Math.abs(r1 - r2) <= tolerance &&
                Math.abs(g1 - g2) <= tolerance &&
                Math.abs(b1 - b2) <= tolerance;
    }
}