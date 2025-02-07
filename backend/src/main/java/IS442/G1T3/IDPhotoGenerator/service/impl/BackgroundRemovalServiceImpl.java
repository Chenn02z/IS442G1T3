package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
//import org.springframework.beans.factory.annotation.Autowired;
import IS442.G1T3.IDPhotoGenerator.service.BackgroundRemovalService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class BackgroundRemovalServiceImpl implements BackgroundRemovalService {

    private final ImageRepository imageRepository;

    @Value("${app.upload.dir:${user.home}}")
    private String uploadDir;

    public BackgroundRemovalServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public ImageEntity removeBackground(MultipartFile file, UUID userId, String backgroundOption) throws Exception {
        Path uploadPath = Paths.get(uploadDir).resolve("Desktop");
        Files.createDirectories(uploadPath);  // Ensure directory exists

        String originalFilename = file.getOriginalFilename();
        assert originalFilename != null;
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFileName = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(savedFileName);

        // Save original file
        File tempFile = filePath.toFile();
        file.transferTo(tempFile);

        // Process image
        BufferedImage originalImage = ImageIO.read(tempFile);
        BufferedImage processedImage = processBackground(originalImage, backgroundOption);

        // Save processed image
        String processedFileName = "processed_" + savedFileName;
        Path processedPath = uploadPath.resolve(processedFileName);
        ImageIO.write(processedImage, "PNG", processedPath.toFile());

        // Create entity
        ImageEntity imageEntity = ImageEntity.builder()
                .imageId(UUID.randomUUID())
                .userId(userId)
                .originalFileName(originalFilename)
                .savedFilePath(processedPath.toString())
                .backgroundOption(backgroundOption)
                .status("COMPLETED")
                .build();

        return imageRepository.save(imageEntity);
    }

    private BufferedImage processBackground(BufferedImage original, String backgroundOption) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Convert to grayscale and apply threshold
        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscale.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // Simple threshold-based segmentation
        int threshold = calculateOutsThreshold(grayscale);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = grayscale.getRGB(x, y) & 0xFF;
                if (gray > threshold) {
                    // Background pixel
                    switch (backgroundOption.toUpperCase()) {
                        case "BLUE":
                            processed.setRGB(x, y, Color.BLUE.getRGB());
                            break;
                        case "TRANSPARENT":
                            processed.setRGB(x, y, 0x00FFFFFF); // Transparent
                            break;
                        case "WHITE":
                        default:
                            processed.setRGB(x, y, Color.WHITE.getRGB());
                    }
                } else {
                    // Foreground pixel
                    processed.setRGB(x, y, original.getRGB(x, y));
                }
            }
        }

        return processed;
    }

    private int calculateOutsThreshold(BufferedImage image) {
        int[] histogram = new int[256];

        // Calculate histogram
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                histogram[image.getRGB(x, y) & 0xFF]++;
            }
        }

        int total = image.getWidth() * image.getHeight();
        float sum = 0;
        for (int i = 0; i < 256; i++) sum += i * histogram[i];

        float sumB = 0;
        int wB = 0;
        int wF;
        float varMax = 0;
        int threshold = 0;

        for (int i = 0; i < 256; i++) {
            wB += histogram[i];
            if (wB == 0) continue;
            wF = total - wB;
            if (wF == 0) break;

            sumB += i * histogram[i];
            float mB = sumB / wB;
            float mF = (sum - sumB) / wF;

            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = i;
            }
        }

        return threshold;
    }
}
