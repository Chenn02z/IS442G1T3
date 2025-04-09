package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import java.io.File;
import java.util.UUID;

import IS442.G1T3.IDPhotoGenerator.service.ClothesReplacementService;
import IS442.G1T3.IDPhotoGenerator.service.ImageVersionControlService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClothesReplacementServiceImpl implements ClothesReplacementService {

    private final ImageNewRepository imageNewRepository;
    // private final PhotoSessionRepository photoSessionRepository; // Not used directly here.
    private final ImageVersionControlService imageVersionControlService;

    // Configured storage path (e.g., defined in application.properties)
    @Value("${image.storage.path}")
    private String storagePath;

    private int neckTop;

    // Constructor: Note that photoSessionRepository is injected but not used directly.
    public ClothesReplacementServiceImpl(ImageNewRepository imageNewRepository,
                                         PhotoSessionRepository photoSessionRepository,
                                         ImageVersionControlService imageVersionControlService) {
        this.imageNewRepository = imageNewRepository;
        // this.photoSessionRepository = photoSessionRepository;
        this.imageVersionControlService = imageVersionControlService;
    }

    public ImageNewEntity OverlaidImage(UUID imageId) throws Exception {
        // --- Load Image and Session Info ---
        // Retrieve the latest image version using the image version control service.
        ImageNewEntity currentEntity = imageVersionControlService.getLatestImageVersion(imageId);
        if (currentEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Convert the relative storage path to an absolute path.
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        File storageDirFile = new File(saveDir);
        if (!storageDirFile.exists()) {
            storageDirFile.mkdirs();
        }

        // Resolve the input image path using the current image's URL.
        String currentImageFileName = currentEntity.getCurrentImageUrl();
        String inputPath = saveDir + File.separator + currentImageFileName;
        log.info("Loading image from: {}", inputPath);

        // Get the next version number from the image version control service.
        int nextVersion = imageVersionControlService.getNextVersion(imageId);

        // Load the image file.
        Mat image = Imgcodecs.imread(inputPath);
        if (image.empty()) {
            throw new RuntimeException("Failed to load image from: " + inputPath);
        }

        // --- Face Detection to Compute Shoulder Trapezoid ---
        CascadeClassifier faceDetector = new CascadeClassifier();
        // Ensure the cascade file path is correct.
        if (!faceDetector.load("src/main/resources/opencv/haarcascade_frontalface_default.xml")) {
            throw new Exception("Failed to load face cascade classifier");
        }
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections);
        Rect[] faces = faceDetections.toArray();
        if (faces.length == 0) {
            throw new Exception("No face detected for clothes overlay");
        }
        // Choose the largest face if multiple are detected.
        Rect face = faces[0];
        for (Rect r : faces) {
            if (r.area() > face.area()) {
                face = r;
            }
        }
        // Create elliptical mask for face instead of rectangle - moved slightly higher to capture hair
        Point center = new Point(face.x + face.width / 2,
                face.y + (double) face.height / 2 - (face.height * 0.1)); // Moved up slightly to include hair
        Size axes = new Size(face.width * 0.4, face.height * 0.625); // Slightly taller to capture more hair

        // Create trapezoid for shoulders/neck below the face - wider base, shorter height, narrower top
        this.neckTop = face.y + face.height;
        int shoulderWidth = (int) (face.width * 2.5); // Wider base
        int shoulderHeight = (int) (face.height * 0.75); // Shorter height
        int topWidth = (int) (face.width * 0.8); // Narrower top width

        // Get image dimensions
        int imageWidth = image.cols();
        int imageHeight = image.rows();
        // Bottom left point (x=0, y=height)
        Point bottomLeft = new Point(0, imageHeight);
        // Bottom right point (x=width, y=height)
        Point bottomRight = new Point(imageWidth, imageHeight);

        // Trapezoid points
        Point[] shoulderPoints = new Point[4];
        shoulderPoints[0] = new Point(center.x - topWidth * 0.2, neckTop * 1.1); // Top left - narrower
        shoulderPoints[1] = new Point(center.x + topWidth * 0.2, neckTop * 1.1); // Top right - narrower
        shoulderPoints[2] = bottomRight;
        shoulderPoints[3] = bottomLeft;
        // --- Overlay Clothes (without warping) ---
        // Simply overlay the shirt PNG onto the image using the bounding rectangle defined by the shoulderPoints.
        String clothesImagePath = "public/officewear-testing3.png";  // Ensure this PNG has the desired transparency.
        Mat finalImage = overlayClothes(image, clothesImagePath, shoulderPoints);

        // --- Save Processed Image ---
        String processedFileName = imageId.toString() + "_" + nextVersion + ".png";
        String outputPath = saveDir + File.separator + processedFileName;
        log.info("Saving processed image to: {}", outputPath);
        boolean saved = Imgcodecs.imwrite(outputPath, finalImage);
        if (!saved) {
            throw new RuntimeException("Failed to save processed image");
        }

        // --- Update Photo Session via ImageVersionControlService ---
        // This call updates the photo session's undo/redo stacks with the new version.
        imageVersionControlService.updatePhotoSession(imageId, nextVersion);

        // --- Persist New Image Entity ---
        ImageNewEntity processedImage = ImageNewEntity.builder()
                .imageId(imageId)
                .userId(currentEntity.getUserId())
                .version(nextVersion)
                .label("Clothes Overlay")
                .baseImageUrl(processedFileName)
                .currentImageUrl(processedFileName)
                .build();

        return imageNewRepository.save(processedImage);
    }

    /**
     * Overlays a clothes image (e.g., a shirt PNG) onto the provided foreground image.
     * This implementation does not warp the overlay image but uses the shoulder points to compute
     * a bounding rectangle where the overlay image will be resized and applied.
     *
     * @param foreground     The person image (foreground) onto which the shirt will be placed.
     * @param clothesPath    The file path to the shirt image (PNG with alpha recommended).
     * @param shoulderPoints An array of 4 Points defining the region where the shirt should be placed.
     * @return               The resulting image with the shirt overlay.
     */
    public Mat overlayClothes(Mat foreground, String clothesPath, Point[] shoulderPoints) {
        if (shoulderPoints == null || shoulderPoints.length != 4) {
            throw new IllegalArgumentException("Exactly four shoulder points are required.");
        }

        // Load the clothes image with alpha channel
        Mat clothes = Imgcodecs.imread(clothesPath, Imgcodecs.IMREAD_UNCHANGED);
        if (clothes.empty()) {
            throw new RuntimeException("Could not load clothes image from: " + clothesPath);
        }

        // Ensure the clothes image is in BGRA
        if (clothes.channels() == 3) {
            Imgproc.cvtColor(clothes, clothes, Imgproc.COLOR_BGR2BGRA);
        }

        // Calculate neck position more accurately based on shoulder points
        // Assuming the first two points are the top shoulders
        int neckTop = (int)Math.min(shoulderPoints[0].y, shoulderPoints[1].y);
        neckTop -= (int)(foreground.rows() * 0.52);

        // Calculate the width between shoulders for better scaling
        double shoulderWidth = Math.abs(shoulderPoints[0].x - shoulderPoints[1].x);

        int roiX = 0;
        int roiY = neckTop; // Start at neck position
        int roiWidth = foreground.cols();
        int roiHeight = foreground.rows() - neckTop;

        // Ensure ROI is within bounds
//        roiX = Math.max(0, roiX);
        roiY = Math.max(0, roiY);
        if (roiX + roiWidth > foreground.cols()) {
            roiWidth = foreground.cols() - roiX;
        }
        if (roiY + roiHeight > foreground.rows()) {
            roiHeight = foreground.rows() - roiY;
        }

        // Calculate better scale factor based on shoulder width
        double widthScaleFactor = shoulderWidth / (clothes.cols() * 0.8); // Adjust 0.8 as needed
        double heightScaleFactor = roiHeight / (double) clothes.rows();

        // Use the larger scale factor to ensure proper fit
        double scaleFactor = Math.max(widthScaleFactor, heightScaleFactor);

        // Apply a multiplier to make the clothes fit better
        scaleFactor *= 1.05; // Adjust this value based on results

        int newOverlayWidth = (int) Math.round(clothes.cols() * scaleFactor);
        int newOverlayHeight = (int) Math.round(clothes.rows() * scaleFactor);

        Mat resizedOverlay = new Mat(); // rounded corners updates from here
        Imgproc.resize(clothes, resizedOverlay, new Size(newOverlayWidth, newOverlayHeight));



        Mat overlayCanvas = Mat.zeros(roiHeight, roiWidth, clothes.type());

        // Center horizontally and position vertically with a small offset
        int offsetX = (roiWidth - newOverlayWidth) / 2;
        int offsetY = 0; // You might want to adjust this based on testing

        // Ensure offsets don't go out of bounds
        offsetX = Math.max(0, offsetX);
//        offsetY = Math.max(0, offsetY);

        // Adjust ROI if needed to ensure it stays within bounds
        int endY = Math.min(offsetY + newOverlayHeight, overlayCanvas.rows());
        int endX = Math.min(offsetX + newOverlayWidth, overlayCanvas.cols());

        Mat canvasROI = overlayCanvas.submat(offsetY, endY, offsetX, endX);

        // Create a submat of resizedOverlay that matches canvasROI dimensions
        Mat overlaySubmat = resizedOverlay.submat(0, endY - offsetY, 0, endX - offsetX);

        overlaySubmat.copyTo(canvasROI);

        if (foreground.channels() == 3) {
            Imgproc.cvtColor(foreground, foreground, Imgproc.COLOR_BGR2BGRA);
        }

        Mat roi = foreground.submat(roiY, roiY + roiHeight, roiX, roiX + roiWidth);
        Mat blendedROI = alphaBlend(roi, overlayCanvas);
        blendedROI.copyTo(roi);

        return foreground;
    }

    /**
     * Performs per-pixel alpha blending between the background (ROI) and the foreground (overlay/clothes) images.
     * Both images must be the same size and have 4 channels (BGRA).
     *
     * @param background The background image (ROI) in BGRA.
     * @param foreground The overlay (clothes) image in BGRA.
     * @return           The blended image.
     */
    private Mat alphaBlend(Mat background, Mat foreground) {
        Mat result = background.clone();

        for (int y = 0; y < background.rows(); y++) {
            for (int x = 0; x < background.cols(); x++) {
                double[] bgPixel = background.get(y, x); // BGRA
                double[] fgPixel = foreground.get(y, x);   // BGRA

                // Compute the alpha factor from the overlay pixel.
                double alpha = fgPixel[3] / 255.0;
                double invAlpha = 1.0 - alpha;

                double[] blendedPixel = new double[4];
                blendedPixel[0] = fgPixel[0] * alpha + bgPixel[0] * invAlpha;
                blendedPixel[1] = fgPixel[1] * alpha + bgPixel[1] * invAlpha;
                blendedPixel[2] = fgPixel[2] * alpha + bgPixel[2] * invAlpha;
                blendedPixel[3] = 255; // Fully opaque

                result.put(y, x, blendedPixel);
            }
        }
        return result;
    }
}