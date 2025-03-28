package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.CartoonisationService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CartooniseServiceImpl implements CartoonisationService {

    private final ImageNewRepository imageNewRepository;
    private final PhotoSessionRepository photoSessionRepository;

    @Value("${image.storage.path}")
    private String storagePath;

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
        } catch (Exception e) {
            log.error("Error loading OpenCV native library: {}", e.getMessage());
        }
    }

    public CartooniseServiceImpl(ImageNewRepository imageNewRepository,
                               PhotoSessionRepository photoSessionRepository) {
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
    }

    @Override
    public ImageNewEntity cartooniseImage(UUID imageId) throws Exception {
        // Find the latest image directly by imageId
        ImageNewEntity latestImage = imageNewRepository.findLatestRowByImageId(imageId);
        if (latestImage == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Get photo session for version tracking
        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            // Create new photo session if it doesn't exist
            photoSession = new PhotoSession();
            photoSession.setImageId(imageId);
            photoSession.setUndoStack("1");
        }

        // Get the next version number
        String undoStack = photoSession.getUndoStack();
        int nextVersion = 1;
        if (undoStack != null && !undoStack.isBlank()) {
            String[] versions = undoStack.split(",");
            nextVersion = Integer.parseInt(versions[versions.length - 1]) + 1;
        }

        // Convert relative path to absolute path
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        File storageDirFile = new File(saveDir);
        if (!storageDirFile.exists()) {
            storageDirFile.mkdirs();
        }

        // Resolve the input image path - use currentImageUrl instead of baseImageUrl
        // to work with the latest version of the image
        String currentImageFileName = latestImage.getCurrentImageUrl();
        String inputPath = saveDir + File.separator + currentImageFileName;
        log.info("Loading image from: {}", inputPath);

        // Load and process the image
        Mat image = Imgcodecs.imread(inputPath);
        if (image.empty()) {
            throw new RuntimeException("Failed to load image from: " + inputPath);
        }

        // Apply cartoon effect
        Mat result = removeBackgroundUsingGrabCut(image);

        // Save the processed image
        String processedFileName = imageId.toString() + "_" + nextVersion + ".png";
        String outputPath = saveDir + File.separator + processedFileName;

        log.info("Saving processed image to: {}", outputPath);
        boolean saved = Imgcodecs.imwrite(outputPath, result);
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
        ImageNewEntity processedImage = ImageNewEntity.builder()
                .imageId(imageId)
                .userId(latestImage.getUserId())
                .version(nextVersion)
                .label("Cartoonise")
                .baseImageUrl(latestImage.getCurrentImageUrl()) // Use currentImageUrl as the baseImageUrl for this new version
                .currentImageUrl(processedFileName)
                .build();

        return imageNewRepository.save(processedImage);
    }

    private Mat removeBackgroundUsingGrabCut(Mat image) {
        Mat mask = new Mat(image.size(), CvType.CV_8UC1, Scalar.all(0));
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        Rect rect = new Rect(1, 1, image.width() - 2, image.height() - 2);

        // GrabCut segmentation
        Imgproc.grabCut(image, mask, rect, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_RECT);

        // Create a mask of probably and definitely foreground pixels
        Mat foregroundMask = new Mat();
        Core.compare(mask, new Scalar(Imgproc.GC_PR_FGD), foregroundMask, Core.CMP_EQ);

        // Create the foreground image
        Mat foreground = new Mat(image.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
        image.copyTo(foreground, foregroundMask);

        return foreground;
    }
}

