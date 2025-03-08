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
import java.io.File;
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

    @Value("${image.storage.path}")
    private String storagePath;

    public FloodFillServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public ImageEntity removeBackground(UUID imageId, String filePath, String seedPointsJson, int tolerance) throws IOException {
        // Get original image from repository
        ImageEntity originalEntity = imageRepository.findBySavedFilePath(filePath)
                .orElseThrow(() -> new RuntimeException("Image not found with file path: " + filePath));

        // Increment the process count
//        imageEntity.setProcessCount(imageEntity.getProcessCount() + 1);

        // Get the saved file path
        String savedFilePath = originalEntity.getSavedFilePath();
        File originalFile = new File(savedFilePath);

        // Check if the file exists
        if (!originalFile.exists()) {
            throw new RuntimeException("Original image file not found on server at: " + savedFilePath);
        }

        // Process the image
        BufferedImage originalImage = ImageIO.read(originalFile);
        List<Point> seedPoints = parsePoints(seedPointsJson);
        BufferedImage processedImage = floodFill(originalImage, seedPoints, tolerance);

        // Create a unique filename based on the process count
        String processedFileName = "floodfill_" + imageId + "_v" + originalEntity.getProcessCount() + ".png";
        String relativePath = storagePath + File.separator + processedFileName;
        String absoluteProcessedPath = new File("").getAbsolutePath() + File.separator + relativePath;

        // Ensure directory exists
        new File(absoluteProcessedPath).getParentFile().mkdirs();

        // Save the processed image
        ImageIO.write(processedImage, "PNG", new File(absoluteProcessedPath));

        ImageEntity newImageEntity = new ImageEntity();
        newImageEntity.setImageId(imageId);
        newImageEntity.setUserId(originalEntity.getUserId());
        newImageEntity.setOriginalFileName(originalEntity.getOriginalFileName());
        newImageEntity.setSavedFilePath(relativePath);
        newImageEntity.setProcessCount(originalEntity.getProcessCount());  // Start with process count 1 for the new image
        newImageEntity.setStatus(ImageStatus.COMPLETED.toString());
        newImageEntity.setBackgroundOption(originalEntity.getBackgroundOption());

        // Save the new image entity
        return imageRepository.save(newImageEntity);
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