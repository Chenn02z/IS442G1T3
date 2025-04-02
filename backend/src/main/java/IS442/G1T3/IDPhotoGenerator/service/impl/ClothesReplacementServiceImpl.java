package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import java.io.File;
import java.util.UUID;

import IS442.G1T3.IDPhotoGenerator.service.ClothesReplacementService;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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
    private final PhotoSessionRepository photoSessionRepository;
    // This is your configured storage path (e.g., via application.properties)
    @Value("${image.storage.path}")
    private String storagePath;

    // Corrected constructor name matching the class name.
    public ClothesReplacementServiceImpl(ImageNewRepository imageNewRepository,
                                         PhotoSessionRepository photoSessionRepository
                                         ) {
        this.imageNewRepository = imageNewRepository;
        this.photoSessionRepository = photoSessionRepository;
    }


    public ImageNewEntity OverlaidImage(UUID imageId) throws Exception {
        // --- Load Image and Session Info ---
        ImageNewEntity latestImage = imageNewRepository.findLatestRowByImageId(imageId);
        if (latestImage == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        PhotoSession photoSession = photoSessionRepository.findByImageId(imageId);
        if (photoSession == null) {
            photoSession = new PhotoSession();
            photoSession.setImageId(imageId);
            photoSession.setUndoStack("1");
        }

        String undoStack = photoSession.getUndoStack();
        int nextVersion = 1;
        if (undoStack != null && !undoStack.isBlank()) {
            String[] versions = undoStack.split(",");
            nextVersion = Integer.parseInt(versions[versions.length - 1]) + 1;
        }

        // Convert relative path to absolute path.
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        File storageDirFile = new File(saveDir);
        if (!storageDirFile.exists()) {
            storageDirFile.mkdirs();
        }

        // Resolve the input image path (use the latest version)
        String currentImageFileName = latestImage.getCurrentImageUrl();
        String inputPath = saveDir + File.separator + currentImageFileName;
        log.info("Loading image from: {}", inputPath);

        // Load the image.
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
        // Compute the center and region parameters (adjust these factors as needed).
        Point center = new Point(face.x + face.width / 2.0, face.y + face.height / 2.0 - face.height * 0.18);
        int neckTop = face.y + face.height;
        int shoulderWidth = (int) (face.width * 2.5);
        int shoulderHeight = (int) (face.height * 0.65);
        int topWidth = (int) (face.width * 0.8);
        // Define the trapezoid for the shoulder region.
        Point[] shoulderPoints = new Point[4];
        shoulderPoints[0] = new Point(center.x - topWidth * 0.32, neckTop * 1.15);  // Top left
        shoulderPoints[1] = new Point(center.x + topWidth * 0.32, neckTop * 1.15);  // Top right
        shoulderPoints[2] = new Point(center.x + shoulderWidth * 0.5, neckTop + shoulderHeight);  // Bottom right
        shoulderPoints[3] = new Point(center.x - shoulderWidth * 0.5, neckTop + shoulderHeight);  // Bottom left

        // --- Overlay Clothes ---
        // Call the overlayClothes method (using the current class) to add clothes to the image.
        String clothesImagePath = "public/officewear-testing.jpg";  // Update this path accordingly.
        Mat finalImage = overlayClothes(image, clothesImagePath, shoulderPoints);

        // --- Save Processed Image ---
        String processedFileName = imageId.toString() + "_" + nextVersion + ".png";
        String outputPath = saveDir + File.separator + processedFileName;
        log.info("Saving processed image to: {}", outputPath);
        boolean saved = Imgcodecs.imwrite(outputPath, finalImage);
        if (!saved) {
            throw new RuntimeException("Failed to save processed image");
        }

        // --- Update Photo Session and Persist New Image Entity ---
        String newUndoStack = (undoStack == null || undoStack.isBlank())
                ? String.valueOf(nextVersion)
                : undoStack + "," + nextVersion;
        photoSession.setUndoStack(newUndoStack);
        photoSession.setRedoStack("");
        photoSessionRepository.save(photoSession);

        ImageNewEntity processedImage = ImageNewEntity.builder()
                .imageId(imageId)
                .userId(latestImage.getUserId())
                .version(nextVersion)
                .label("Clothes Overlay")
                .baseImageUrl(processedFileName)
                .currentImageUrl(processedFileName)
                .build();

        return imageNewRepository.save(processedImage);
    }

    /**
     * Overlays a clothes image (e.g., office wear) onto the provided foreground image.
     *
     * @param foreground     The subject image (foreground) with background removed.
     * @param clothesPath    The file path to the clothes image (PNG with alpha recommended).
     * @param shoulderPoints An array of 4 Points defining the trapezoid (shoulder region) where the clothes will be mapped.
     * @return               A new Mat image with the clothes overlay applied.
     */
    public Mat overlayClothes(Mat foreground, String clothesPath, Point[] shoulderPoints) {
        if (shoulderPoints == null || shoulderPoints.length != 4) {
            throw new IllegalArgumentException("Exactly four shoulder points are required for the trapezoid.");
        }

        // Load the clothes image with unchanged flag to keep the alpha channel if present.
        Mat clothes = Imgcodecs.imread(clothesPath, Imgcodecs.IMREAD_UNCHANGED);
        if (clothes.empty()) {
            throw new RuntimeException("Could not load clothes image from: " + clothesPath);
        }

        // Define source points (corners of the clothes image) using org.opencv.core.Point.
        Point[] srcPoints = new Point[] {
                new Point(0, 0),
                new Point(clothes.cols(), 0),
                new Point(clothes.cols(), clothes.rows()),
                new Point(0, clothes.rows())
        };

        // Create MatOfPoint2f objects directly from the Point arrays.
        MatOfPoint2f srcMat = new MatOfPoint2f(srcPoints);
        MatOfPoint2f dstMat = new MatOfPoint2f(shoulderPoints);

        // Compute the perspective transform.
        Mat transform = Imgproc.getPerspectiveTransform(srcMat, dstMat);

        // Warp the clothes image to fit the trapezoid region.
        Mat warpedClothes = new Mat();
        Imgproc.warpPerspective(clothes, warpedClothes, transform, foreground.size(),
                Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, new Scalar(0, 0, 0, 0));

        // Ensure both images are 4-channel (BGRA) for proper alpha blending.
        if (foreground.channels() == 3) {
            Imgproc.cvtColor(foreground, foreground, Imgproc.COLOR_BGR2BGRA);
        }
        if (warpedClothes.channels() == 3) {
            Imgproc.cvtColor(warpedClothes, warpedClothes, Imgproc.COLOR_BGR2BGRA);
        }

        // Blend the warped clothes image with the foreground image.
        Mat finalImage = alphaBlend(foreground, warpedClothes);

        return finalImage;
    }

    /**
     * Performs per-pixel alpha blending between the background and foreground images.
     * Both images must be of the same size and have 4 channels (BGRA).
     *
     * @param background The background image (subject) in BGRA.
     * @param foreground The foreground image (clothes) in BGRA.
     * @return           The blended image.
     */
    private Mat alphaBlend(Mat background, Mat foreground) {
        Mat result = background.clone();

        for (int y = 0; y < background.rows(); y++) {
            for (int x = 0; x < background.cols(); x++) {
                double[] bgPixel = background.get(y, x); // BGRA
                double[] fgPixel = foreground.get(y, x);   // BGRA

                // Compute the alpha factor from the foreground pixel.
                double alpha = fgPixel[3] / 255.0;
                double invAlpha = 1.0 - alpha;

                double[] blendedPixel = new double[4];
                blendedPixel[0] = fgPixel[0] * alpha + bgPixel[0] * invAlpha; // Blue
                blendedPixel[1] = fgPixel[1] * alpha + bgPixel[1] * invAlpha; // Green
                blendedPixel[2] = fgPixel[2] * alpha + bgPixel[2] * invAlpha; // Red
                blendedPixel[3] = 255; // Fully opaque

                result.put(y, x, blendedPixel);
            }
        }
        return result;
    }
}
