package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageStatus;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.FileStorageService;
import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

@Service
@Slf4j
public class FloodFillServiceImpl implements FloodFillService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;

    @Value("${image.storage.path}")
    private String storagePath;

    public FloodFillServiceImpl(ImageRepository imageRepository, FileStorageService fileStorageService) {
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public ImageEntity removeBackground(UUID imageId, String seedPointsJson, int tolerance) throws IOException {
        // Get original image from repository
        ImageEntity imageEntity = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found with id: " + imageId));

        // Load the original image
        Path originalPath = Paths.get(imageEntity.getSavedFilePath());
        if (!originalPath.isAbsolute()) {
            originalPath = Paths.get(System.getProperty("user.dir")).resolve(imageEntity.getSavedFilePath()).normalize();
        }

        // Process the image
        BufferedImage originalImage = ImageIO.read(originalPath.toFile());
        List<Point> seedPoints = parsePoints(seedPointsJson);
        BufferedImage processedImage = floodFill(originalImage, seedPoints, tolerance);

        // Save the processed image
        String processedFileName = "floodfill_" + imageId + ".png";
        Path processedPath = originalPath.getParent().resolve(processedFileName);
        ImageIO.write(processedImage, "PNG", processedPath.toFile());

        // Update image entity with new path and status
        imageEntity.setSavedFilePath(processedPath.toString());
        imageEntity.setStatus(ImageStatus.COMPLETED.toString());
        
        return imageRepository.save(imageEntity);
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