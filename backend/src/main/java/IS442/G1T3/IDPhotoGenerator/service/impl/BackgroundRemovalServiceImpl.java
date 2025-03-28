package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.BackgroundRemovalService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// Use javax.imageio instead of jakarta.imageio
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BackgroundRemovalServiceImpl implements BackgroundRemovalService {

    private static final int COLOR_SIMILARITY_THRESHOLD = 2;
    private static final int MIN_SIMILAR_NEIGHBORS = 3;
    private static final String BLUE_HEX = "#0000FF";
    private static final String WHITE_HEX = "#FFFFFF";
    private static final String TRANSPARENT_HEX = "#00000000";

    private final ImageNewRepository imageNewRepository;
    private final PhotoSessionRepository photoSessionRepository;

    @Value("${image.storage.path}")
    private String storagePath;

    public BackgroundRemovalServiceImpl(ImageNewRepository imageNewRepository,
                                      PhotoSessionRepository photoSessionRepository) {
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
    }

    @Override
    public ImageNewEntity removeBackground(MultipartFile file, UUID userId, String backgroundOption) throws Exception {
        try {
            log.info("Starting background removal process for backgroundOption: {}", backgroundOption);
            
            UUID imageId = UUID.randomUUID();
            
            // Convert relative path to absolute path if needed
            Path uploadPath = Paths.get(storagePath);
            if (!uploadPath.isAbsolute()) {
                uploadPath = Paths.get(System.getProperty("user.dir")).resolve(storagePath);
            }
            
            // Make sure directory exists
            Files.createDirectories(uploadPath);
            log.info("Using storage path: {}", uploadPath.toAbsolutePath());
            
            String fileExtension = ".png";  // Always save as PNG
            
            // Save original image first
            String baseFileName = imageId.toString() + "_1" + fileExtension;
            Path originalPath = uploadPath.resolve(baseFileName);
            
            log.info("Reading original image from multipart file");
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            
            if (originalImage == null) {
                throw new IOException("Failed to read image from uploaded file");
            }
            
            log.info("Writing original image to: {}", originalPath);
            File originalFile = originalPath.toFile();
            boolean originalImageSaved = ImageIO.write(originalImage, "PNG", originalFile);
            
            if (!originalImageSaved) {
                throw new IOException("Failed to save original image to file system");
            }
            
            log.info("Processing image with background removal algorithm");
            BufferedImage processedImage = processImage(originalImage, backgroundOption);
            
            String processedFileName = imageId.toString() + "_2" + fileExtension;
            Path processedPath = uploadPath.resolve(processedFileName);
            
            log.info("Writing processed image to: {}", processedPath);
            File processedFile = processedPath.toFile();
            boolean processedImageSaved = ImageIO.write(processedImage, "PNG", processedFile);
            
            if (!processedImageSaved) {
                throw new IOException("Failed to save processed image to file system");
            }
            
            // Create and save the original image entity
            ImageNewEntity originalImageEntity = ImageNewEntity.builder()
                    .imageId(imageId)
                    .userId(userId)
                    .version(1)
                    .label("Original")
                    .baseImageUrl(baseFileName)
                    .currentImageUrl(baseFileName)
                    .build();
                    
            log.info("Saving original image entity to database");
            imageNewRepository.save(originalImageEntity);
            
            // Create and save the processed image entity
            ImageNewEntity processedImageEntity = ImageNewEntity.builder()
                    .imageId(imageId)
                    .userId(userId)
                    .version(2)
                    .label("Background Removal")
                    .baseImageUrl(baseFileName)
                    .currentImageUrl(processedFileName)
                    .build();
                    
            log.info("Saving processed image entity to database");
            ImageNewEntity savedEntity = imageNewRepository.save(processedImageEntity);
            
            // Create and save photo session with version tracking
            PhotoSession photoSession = new PhotoSession();
            photoSession.setImageId(imageId);
            photoSession.setUndoStack("1,2");  // Original is version 1, processed is version 2
            photoSession.setRedoStack("");
            
            log.info("Saving photo session to database");
            photoSessionRepository.save(photoSession);
            
            log.info("Background removal process completed successfully");
            return savedEntity;
        } catch (IOException e) {
            log.error("IO error during background removal: {}", e.getMessage(), e);
            throw new Exception("Failed to process the image: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during background removal: {}", e.getMessage(), e);
            throw new Exception("An unexpected error occurred: " + e.getMessage(), e);
        }
    }

    private BufferedImage processImage(BufferedImage original, String backgroundOption) {
        BufferedImage processedImage = removeBackgroundInternal(original, backgroundOption);
        BufferedImage mask = createMaskFromOutlines(processedImage, backgroundOption);
        BufferedImage subjectImage = applyMaskToImage(original, mask);
        return subjectImage; // Skip second background removal
    }

    private BufferedImage removeBackgroundInternal(BufferedImage original, String backgroundOption) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackgroundPixel(original, x, y)) {
                    String hexColor = switch (backgroundOption.toUpperCase()) {
                        case "TRANSPARENT" -> TRANSPARENT_HEX;
                        case "BLUE" -> BLUE_HEX;
                        case "WHITE" -> WHITE_HEX;
                        default -> TRANSPARENT_HEX;
                    };
                    result.setRGB(x, y, hexToRgb(hexColor));
                } else {
                    result.setRGB(x, y, original.getRGB(x, y));
                }
            }
        }
        return result;
    }

    private BufferedImage createMaskFromOutlines(BufferedImage processedImage, String backgroundOption) {
        String bgHex = switch (backgroundOption.toUpperCase()) {
            case "TRANSPARENT" -> TRANSPARENT_HEX;
            case "BLUE" -> BLUE_HEX;
            case "WHITE" -> WHITE_HEX;
            default -> TRANSPARENT_HEX;
        };
        int bgRgb = hexToRgb(bgHex);

        int width = processedImage.getWidth();
        int height = processedImage.getHeight();
        boolean[][] outlineMatrix = new boolean[width][height];
        List<Point> outlinePixels = new ArrayList<>();

        // Detect outline pixels (subject adjacent to background)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int currentRgb = processedImage.getRGB(x, y);
                if (currentRgb != bgRgb) {
                    boolean isOutline = false;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                int neighborRgb = processedImage.getRGB(nx, ny);
                                if (neighborRgb == bgRgb) {
                                    isOutline = true;
                                    break;
                                }
                            }
                        }
                        if (isOutline) break;
                    }
                    if (isOutline) {
                        outlinePixels.add(new Point(x, y));
                        outlineMatrix[x][y] = true;
                    }
                }
            }
        }

        int minX = outlinePixels.stream().mapToInt(p -> p.x).min().orElse(0);
        int minY = outlinePixels.stream().mapToInt(p -> p.y).min().orElse(0);
        int maxX = outlinePixels.stream().mapToInt(p -> p.x).max().orElse(width - 1);
        int maxY = outlinePixels.stream().mapToInt(p -> p.y).max().orElse(height - 1);

        Point startPoint = findStartingPoint(processedImage, minX, minY, maxX, maxY, bgRgb, outlineMatrix);
        Set<Point> subjectPixels = floodFill(processedImage, startPoint.x, startPoint.y, bgRgb, outlineMatrix);

        BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.setColor(new Color(0, 0, 0, 0)); // Transparent
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(255, 255, 255, 255)); // Opaque
        subjectPixels.forEach(p -> g.fillRect(p.x, p.y, 1, 1));
        g.dispose();

        return mask;
    }

    private Point findStartingPoint(BufferedImage image, int minX, int minY, int maxX, int maxY, int bgRgb, boolean[][] outlineMatrix) {
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                    int rgb = image.getRGB(x, y);
                    if (rgb != bgRgb && !outlineMatrix[x][y]) {
                        return new Point(x, y);
                    }
                }
            }
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int rgb = image.getRGB(x, y);
                if (rgb != bgRgb && !outlineMatrix[x][y]) {
                    return new Point(x, y);
                }
            }
        }

        return new Point(centerX, centerY);
    }

    private Set<Point> floodFill(BufferedImage image, int startX, int startY, int bgRgb, boolean[][] outlineMatrix) {
        Set<Point> filledPoints = new HashSet<>();
        Queue<Point> queue = new LinkedBlockingQueue<>();
        Point start = new Point(startX, startY);
        queue.add(start);
        filledPoints.add(start);

        int width = image.getWidth();
        int height = image.getHeight();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            for (int[] dir : directions) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    Point neighbor = new Point(nx, ny);
                    int rgb = image.getRGB(nx, ny);
                    if (rgb != bgRgb && !outlineMatrix[nx][ny] && !filledPoints.contains(neighbor)) {
                        filledPoints.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return filledPoints;
    }

    private BufferedImage applyMaskToImage(BufferedImage original, BufferedImage mask) {
        BufferedImage result = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
        g.drawImage(mask, 0, 0, null);
        g.dispose();
        return result;
    }

    private boolean isBackgroundPixel(BufferedImage image, int x, int y) {
        String centerHex = rgbToHex(image.getRGB(x, y));
        int similarNeighbors = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < image.getWidth() && ny >= 0 && ny < image.getHeight()) {
                    String neighborHex = rgbToHex(image.getRGB(nx, ny));
                    if (isHexColorSimilar(centerHex, neighborHex)) {
                        similarNeighbors++;
                    }
                }
            }
        }

        return similarNeighbors >= MIN_SIMILAR_NEIGHBORS;
    }

    private String rgbToHex(int rgb) {
        Color color = new Color(rgb, true);
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private int hexToRgb(String hexColor) {
        if (hexColor.equals(TRANSPARENT_HEX)) {
            return new Color(0, 0, 0, 0).getRGB();
        }
        return Color.decode(hexColor).getRGB();
    }

    private boolean isHexColorSimilar(String hex1, String hex2) {
        Color color1 = Color.decode(hex1);
        Color color2 = Color.decode(hex2);
        return Math.abs(color1.getRed() - color2.getRed()) < COLOR_SIMILARITY_THRESHOLD &&
                Math.abs(color1.getGreen() - color2.getGreen()) < COLOR_SIMILARITY_THRESHOLD &&
                Math.abs(color1.getBlue() - color2.getBlue()) < COLOR_SIMILARITY_THRESHOLD;
    }

    static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}