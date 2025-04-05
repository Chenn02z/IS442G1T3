package IS442.G1T3.IDPhotoGenerator.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService implements IS442.G1T3.IDPhotoGenerator.service.FileStorageService {
    @Value("${image.storage.path}")
    private String storagePath;

    @Override
    public String saveOriginalImage(MultipartFile file, UUID imageId) throws IOException {
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        String newFileName = imageId.toString() + "_1.png";  // Always use .png extension
        String relativeSaveDir = storagePath + File.separator + newFileName;
        String filePath = saveDir + File.separator + newFileName;

        // Create directory if it doesn't exist
        File directory = new File(saveDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File newFile = new File(filePath);
        file.transferTo(newFile);
        return relativeSaveDir;
    }

    @Override
    public String saveVersionedImage(UUID imageId, int version, BufferedImage croppedImage) throws IOException {
        // Generate the filename using the imageId and version
        String croppedFilename = imageId.toString() + "_" + version + ".png";
        File outputFile = new File(Paths.get(storagePath, croppedFilename).toString());

        log.info("Saving versioned image to: {}", outputFile.getAbsolutePath());

        // Save the image using ImageIO
        boolean saved = ImageIO.write(croppedImage, "png", outputFile);
        if (!saved) {
            throw new IOException("Failed to save versioned image to " + outputFile.getAbsolutePath());
        }

        // Verify that the file was created and is not empty
        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("Failed to create versioned image file or file is empty");
        }

        log.info("Versioned image saved successfully: {}", croppedFilename);
        return croppedFilename;
    }
}
