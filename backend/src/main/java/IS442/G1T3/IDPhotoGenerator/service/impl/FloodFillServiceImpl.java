package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.FloodFillService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.Queue;

@Service
@Slf4j
public class FloodFillServiceImpl implements FloodFillService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ImageNewRepository imageNewRepository;
    private final PhotoSessionRepository photoSessionRepository;
    private final boolean isOpenCVAvailable;

    @Value("${image.storage.path}")
    private String storagePath;

    public FloodFillServiceImpl(ImageNewRepository imageNewRepository,
                              PhotoSessionRepository photoSessionRepository) {
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
        this.isOpenCVAvailable = !"true".equals(System.getProperty("opencv.unavailable"));
        log.info("FloodFillServiceImpl initialized with OpenCV available: {}", isOpenCVAvailable);
    }

    @Override
    public ImageNewEntity removeBackground(UUID imageId, String seedPointsJson, int tolerance) throws IOException {
        try {
            // Get original image from repository
            ImageNewEntity originalEntity = imageNewRepository.findLatestRowByImageId(imageId);
            if (originalEntity == null) {
                throw new RuntimeException("Image not found with id: " + imageId);
            }

            // Get photo session for version tracking
            PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
            if (photoSession == null) {
                photoSession = new PhotoSession();
                photoSession.setImageId(imageId);
                photoSession.setUndoStack("1");
            }

            // Get the next version number
            String undoStack = photoSession.getUndoStack();
            int nextVersion = originalEntity.getVersion() + 1;
            int currVersion = 1;
            if (undoStack != null && !undoStack.isBlank()) {
                String[] versions = undoStack.split(",");
                currVersion = Integer.parseInt(versions[versions.length - 1]);
            }
            log.info("Current version: {}", currVersion);
            log.info("imageId: {}", imageId);
            ImageNewEntity currImage = imageNewRepository.findByImageIdAndVersion(imageId, currVersion);

            // Convert relative path to absolute path
            String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
            File storageDirFile = new File(saveDir);
            if (!storageDirFile.exists()) {
                storageDirFile.mkdirs();
            }
            // Resolve the input image path using currentImageUrl
            String currentFileName = currImage.getCurrentImageUrl();
            String inputPath = saveDir + File.separator + currentFileName;
            File originalFile = new File(inputPath);

            // Check if the file exists
            if (!originalFile.exists()) {
                throw new IOException("Original image file not found on server at: " + inputPath);
            }

            // Process the image
            BufferedImage originalImage = ImageIO.read(originalFile);
            List<Point> seedPoints = parsePoints(seedPointsJson);
            
            // Apply flood fill
            BufferedImage processedImage = floodFill(originalImage, seedPoints, tolerance);

            // Save the processed image
            String processedFileName = imageId.toString() + "_" + nextVersion + ".png";
            String outputPath = saveDir + File.separator + processedFileName;

            log.info("Saving processed image to: {}", outputPath);
            File outputFile = new File(outputPath);
            boolean saved = ImageIO.write(processedImage, "PNG", outputFile);
            if (!saved) {
                throw new RuntimeException("Failed to save processed image");
            }

            // Update undo stack
            String newUndoStack = undoStack == null || undoStack.isBlank() ? 
                String.valueOf(nextVersion) : undoStack + "," + nextVersion;
            photoSession.setUndoStack(newUndoStack);
            photoSession.setRedoStack("");
            photoSessionRepository.save(photoSession);

            // Create and save the new image entity
            ImageNewEntity processedEntity = ImageNewEntity.builder()
                    .imageId(imageId)
                    .userId(originalEntity.getUserId())
                    .version(nextVersion)
                    .label("Flood Fill")
                    // .baseImageUrl(originalEntity.getCurrentImageUrl()) // Changed from baseImageUrl to currentImageUrl
                    .baseImageUrl(processedFileName)
                    .currentImageUrl(processedFileName)
                    .build();

            return imageNewRepository.save(processedEntity);
        } catch (JsonProcessingException e) {
            log.error("Error parsing seed points JSON: {}", e.getMessage());
            throw new IOException("Error processing JSON data: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error during background removal: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during background removal: {}", e.getMessage());
            throw new IOException("Error during background removal: " + e.getMessage(), e);
        }
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