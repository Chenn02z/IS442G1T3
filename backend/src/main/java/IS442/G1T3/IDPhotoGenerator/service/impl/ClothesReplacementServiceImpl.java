package IS442.G1T3.IDPhotoGenerator.service.impl;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.repository.PhotoSessionRepository;
import IS442.G1T3.IDPhotoGenerator.service.ClothesReplacementService;
import IS442.G1T3.IDPhotoGenerator.service.ImageVersionControlService;
import lombok.extern.slf4j.Slf4j;

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
        ImageNewEntity currentEntity = imageVersionControlService.getLatestImageVersion(imageId);
        if (currentEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }
    
        // Set up directories
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        File storageDirFile = new File(saveDir);
        if (!storageDirFile.exists()) {
            storageDirFile.mkdirs();
        }
    
        String masksDir = System.getProperty("user.dir") + File.separator + "images/masks";
        File masksDirFile = new File(masksDir);
        if (!masksDirFile.exists()) {
            masksDirFile.mkdirs();
        }
    
        // Resolve the input image path
        String currentImageFileName = currentEntity.getCurrentImageUrl();
        String inputPath = saveDir + File.separator + currentImageFileName;
        log.info("Loading image from: {}", inputPath);
    
        // Get next version number
        int nextVersion = imageVersionControlService.getNextVersion(imageId);
    
        // Load the image
        Mat image = Imgcodecs.imread(inputPath);
        if (image.empty()) {
            throw new RuntimeException("Failed to load image from: " + inputPath);
        }
    
        // --- Face Detection to Compute Shoulder Region ---
        CascadeClassifier faceDetector = new CascadeClassifier();
        if (!faceDetector.load("src/main/resources/opencv/haarcascade_frontalface_default.xml")) {
            throw new Exception("Failed to load face cascade classifier");
        }
        
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections);
        Rect[] faces = faceDetections.toArray();
        if (faces.length == 0) {
            throw new Exception("No face detected for clothes overlay");
        }
        
        // Choose the largest face
        Rect face = faces[0];
        for (Rect r : faces) {
            if (r.area() > face.area()) {
                face = r;
            }
        }
        
        // Define face center
        Point center = new Point(face.x + face.width / 2, 
                                face.y + (double) face.height / 2 - (face.height * 0.1));
        
        // Set neck top position
        this.neckTop = face.y + face.height;
        
        // --- Define Upper Body Region ---
        // Get image dimensions
        int imageWidth = image.cols();
        int imageHeight = image.rows();

        // Improved shoulder estimation based on face dimensions
        double shoulderWidth = face.width * 2.5;  // shoulders wider than face
        double shoulderTop = neckTop * 1.08;       // just below the neck
        
        Point bottomLeft = new Point(0, imageHeight);
        Point bottomRight = new Point(imageWidth, imageHeight);

        // Create trapezoid for upper body
        Point[] shoulderPoints = new Point[4];
        
        // Top points (shoulders)
        shoulderPoints[0] = new Point(center.x - shoulderWidth/2, shoulderTop);  // Left shoulder
        shoulderPoints[1] = new Point(center.x + shoulderWidth/2, shoulderTop);  // Right shoulder
        
        // Bottom points (where upper body ends)
        shoulderPoints[2] = bottomRight;
        shoulderPoints[3] = bottomLeft; // Bottom left
        
        // Visualize and save masks
        visualizeAndSaveMasks(image.clone(), face, center, shoulderPoints, masksDir, imageId.toString());
        
        // --- Overlay Clothes ---
        String clothesImagePath = "public/officewear-testing4.png";
        Mat finalImage = overlayClothes(image, clothesImagePath, shoulderPoints);
    
        // --- Save Processed Image ---
        String processedFileName = imageId.toString() + "_" + nextVersion + ".png";
        String outputPath = saveDir + File.separator + processedFileName;
        log.info("Saving processed image to: {}", outputPath);
        boolean saved = Imgcodecs.imwrite(outputPath, finalImage);
        if (!saved) {
            throw new RuntimeException("Failed to save processed image");
        }
    
        // --- Update Photo Session ---
        imageVersionControlService.updatePhotoSession(imageId, nextVersion);
    
        // --- Persist New Image Entity ---
        ImageNewEntity processedImage = ImageNewEntity.builder()
                .imageId(imageId)
                .userId(currentEntity.getUserId())
                .version(nextVersion)
                .label("Upper Body Clothes Overlay")
                .baseImageUrl(processedFileName)
                .currentImageUrl(processedFileName)
                .build();
    
        return imageNewRepository.save(processedImage);
    }
    
    /**
     * Visualizes and saves masks for debugging and analysis.
     * Adapted to focus on upper body.
     */
    private void visualizeAndSaveMasks(Mat image, Rect face, Point center, 
                                      Point[] shoulderPoints, String masksDir, String imagePrefix) {
        // Create copies of the image for visualizations
        Mat faceMaskVisualization = image.clone();
        Mat shoulderMaskVisualization = image.clone();
        Mat combinedMaskVisualization = image.clone();
        
        // Create blank masks
        Mat faceMask = Mat.zeros(image.size(), CvType.CV_8UC1);
        Mat shoulderMask = Mat.zeros(image.size(), CvType.CV_8UC1);
        
        // Draw face rectangle
        Imgproc.rectangle(faceMaskVisualization, face.tl(), face.br(), new Scalar(255, 0, 0), 2);
        
        // Draw neck line
        Imgproc.line(combinedMaskVisualization, 
                    new Point(face.x + face.width * 0.3, neckTop),
                    new Point(face.x + face.width * 0.7, neckTop),
                    new Scalar(0, 255, 255), 2);
        
        // Draw shoulder points
        MatOfPoint points = new MatOfPoint();
        points.fromArray(shoulderPoints);
        
        // Fill shoulder mask
        Imgproc.fillConvexPoly(shoulderMask, points, new Scalar(255));
        
        // Draw outline on visualizations
        Imgproc.polylines(shoulderMaskVisualization, Arrays.asList(points), true, new Scalar(0, 0, 255), 2);
        Imgproc.polylines(combinedMaskVisualization, Arrays.asList(points), true, new Scalar(0, 0, 255), 2);
        
        // Save visualizations
        Imgcodecs.imwrite(masksDir + File.separator + imagePrefix + "_face_mask.png", faceMaskVisualization);
        Imgcodecs.imwrite(masksDir + File.separator + imagePrefix + "_shoulder_mask.png", shoulderMaskVisualization);
        Imgcodecs.imwrite(masksDir + File.separator + imagePrefix + "_combined_mask.png", combinedMaskVisualization);
        
        // Save binary masks
        Imgcodecs.imwrite(masksDir + File.separator + imagePrefix + "_shoulder_mask_binary.png", shoulderMask);
        
        log.info("Saved mask visualizations to directory: {}", masksDir);
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
    
        // Create a copy of the foreground image (to avoid modifying the original)
        Mat result = foreground.clone();
        if (result.channels() == 3) {
            Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2BGRA);
        }
        
        // Calculate the width and height of the target region
        double targetWidth = Math.abs(shoulderPoints[1].x - shoulderPoints[0].x);
        double targetHeight = Math.abs(shoulderPoints[2].y - shoulderPoints[0].y);
        
        // Resize the clothes image to match the target region
        Mat resizedClothes = new Mat();
        Imgproc.resize(clothes, resizedClothes, new Size(targetWidth, targetHeight));
        
        // Create mask for the upper body region
        Mat mask = Mat.zeros(result.size(), CvType.CV_8UC1);
        MatOfPoint points = new MatOfPoint();
        points.fromArray(shoulderPoints);
        Imgproc.fillConvexPoly(mask, points, new Scalar(255));
        
        // Create a slightly feathered mask for the edges
        Mat featheredMask = new Mat();
        Imgproc.GaussianBlur(mask, featheredMask, new Size(9, 9), 0);
        
        // Calculate the top-left position to place the resized clothes
        int startX = (int) shoulderPoints[0].x;
        int startY = (int) shoulderPoints[0].y;
        
        // Create a temporary result for alpha blending
        Mat tempResult = new Mat(result.size(), result.type());
        result.copyTo(tempResult);
        
        // Perform blending for each pixel
        for (int y = 0; y < result.rows(); y++) {
            for (int x = 0; x < result.cols(); x++) {
                // Get mask value at this pixel (0-255)
                double maskValue = featheredMask.get(y, x)[0];
                
                // Skip pixels outside the mask
                if (maskValue <= 0) continue;
                
                // Calculate corresponding position in resized clothes
                int clothesX = x - startX;
                int clothesY = y - startY;
                
                // Skip if outside the resized clothes bounds
                if (clothesX < 0 || clothesX >= resizedClothes.cols() || 
                    clothesY < 0 || clothesY >= resizedClothes.rows()) continue;
                
                // Get foreground (person) and clothes pixels
                double[] fgPixel = result.get(y, x);
                double[] clothesPixel = resizedClothes.get(clothesY, clothesX);
                
                // Skip transparent parts of the clothes
                if (clothesPixel[3] <= 0) continue;
                
                // Calculate alpha based on both the clothes alpha and mask feathering
                double clothesAlpha = clothesPixel[3] / 255.0;
                double maskAlpha = maskValue / 255.0;
                double finalAlpha = clothesAlpha * maskAlpha;
                
                // Skip if resulting alpha is too low
                if (finalAlpha <= 0.05) continue;
                
                // Apply alpha blending
                double invAlpha = 1.0 - finalAlpha;
                double[] blendedPixel = new double[4];
                blendedPixel[0] = clothesPixel[0] * finalAlpha + fgPixel[0] * invAlpha; // B
                blendedPixel[1] = clothesPixel[1] * finalAlpha + fgPixel[1] * invAlpha; // G
                blendedPixel[2] = clothesPixel[2] * finalAlpha + fgPixel[2] * invAlpha; // R
                blendedPixel[3] = 255; // Full opacity for the result
                
                tempResult.put(y, x, blendedPixel);
            }
        }
        
        // Copy the temporary result back to the result
        tempResult.copyTo(result);
        
        // Release resources
        clothes.release();
        resizedClothes.release();
        mask.release();
        featheredMask.release();
        tempResult.release();
        
        // Convert back to BGR if needed
        if (foreground.channels() == 3) {
            Imgproc.cvtColor(result, result, Imgproc.COLOR_BGRA2BGR);
        }
        
        return result;
    }
}