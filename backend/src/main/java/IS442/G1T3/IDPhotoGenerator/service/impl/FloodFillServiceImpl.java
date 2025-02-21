package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class FloodFillServiceImpl implements FloodFillService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] removeBackground(MultipartFile file, String seedPointsJson, int tolerance) throws IOException {
        List<Point> seedPoints = parsePoints(seedPointsJson);
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        BufferedImage processedImage = floodFill(originalImage, seedPoints, tolerance);

        // Create a new BufferedImage with transparency support
        BufferedImage transparentImage = new BufferedImage(
                processedImage.getWidth(),
                processedImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        // Copy the processed image to the transparent image
        Graphics2D g2d = transparentImage.createGraphics();
        g2d.drawImage(processedImage, 0, 0, null);
        g2d.dispose();

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(transparentImage, "png", baos);
        return baos.toByteArray();
    }

    private List<Point> parsePoints(String seedPointsJson) throws JsonProcessingException {
        List<Map<String, Integer>> points = objectMapper.readValue(
            seedPointsJson, 
            new TypeReference<List<Map<String, Integer>>>() {}
        );
        
        List<Point> seedPoints = new ArrayList<>();
        for (Map<String, Integer> point : points) {
            seedPoints.add(new Point(point.get("x"), point.get("y")));
        }
        return seedPoints;
    }

    private BufferedImage floodFill(BufferedImage image, List<Point> seedPoints, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] visited = new boolean[width][height];

        for (Point seedPoint : seedPoints) {
            if (!isValidPoint(seedPoint.x, seedPoint.y, width, height)) {
                continue;
            }

            int targetColor = image.getRGB(seedPoint.x, seedPoint.y);
            Queue<Point> queue = new LinkedList<>();
            queue.add(seedPoint);

            while (!queue.isEmpty()) {
                Point p = queue.remove();
                if (isValidPoint(p.x, p.y, width, height) && !visited[p.x][p.y]) {
                    if (isColorSimilar(image.getRGB(p.x, p.y), targetColor, tolerance)) {
                        image.setRGB(p.x, p.y, 0x00FFFFFF); // Set to transparent
                        visited[p.x][p.y] = true;

                        // Add adjacent points
                        queue.add(new Point(p.x + 1, p.y));
                        queue.add(new Point(p.x - 1, p.y));
                        queue.add(new Point(p.x, p.y + 1));
                        queue.add(new Point(p.x, p.y - 1));
                    }
                }
            }
        }
        return image;
    }

    private boolean isValidPoint(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    private boolean isColorSimilar(int color1, int color2, int tolerance) {
        int r1 = (color1 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = color1 & 0xff;

        int r2 = (color2 >> 16) & 0xff;
        int g2 = (color2 >> 8) & 0xff;
        int b2 = color2 & 0xff;

        return Math.abs(r1 - r2) <= tolerance &&
                Math.abs(g1 - g2) <= tolerance &&
                Math.abs(b1 - b2) <= tolerance;
    }
}