package IS442.G1T3.IDPhotoGenerator.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.opencv.core.Core;
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

import IS442.G1T3.IDPhotoGenerator.factory.CartooniseFactory;
import IS442.G1T3.IDPhotoGenerator.factory.ImageFactorySelector;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageOperationType;
import IS442.G1T3.IDPhotoGenerator.repository.ImageNewRepository;
import IS442.G1T3.IDPhotoGenerator.service.CartoonisationService;
import IS442.G1T3.IDPhotoGenerator.service.ImageVersionControlService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CartooniseServiceImpl implements CartoonisationService {

    private final ImageNewRepository imageNewRepository;
    private final ImageVersionControlService imageVersionControlService;
    private final ImageFactorySelector imageFactorySelector; // Inject the selector

    @Value("${image.storage.path}")
    private String storagePath;

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
        } catch (Exception e) {
            log.error("Error loading OpenCV native library: {}", e.getMessage());
        }
    }

    public CartooniseServiceImpl(
            ImageNewRepository imageNewRepository,
            ImageVersionControlService imageVersionControlService,
            ImageFactorySelector imageFactorySelector
    ) {
        this.imageNewRepository = imageNewRepository;
        this.imageVersionControlService = imageVersionControlService;
        this.imageFactorySelector = imageFactorySelector;

    }

    @Override
    public ImageNewEntity cartooniseImage(UUID imageId) {
        // ------
        // STEP 1
        // ------
        // Get current image for editing using version control service
        ImageNewEntity currentEntity = imageVersionControlService.getLatestImageVersion(imageId);
        if (currentEntity == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Convert relative path to absolute path
        String saveDir = System.getProperty("user.dir") + File.separator + storagePath;
        File storageDirFile = new File(saveDir);
        if (!storageDirFile.exists()) {
            storageDirFile.mkdirs();
        }

        // ------
        // STEP 2
        // ------
        // Resolve the input image path using currentImageUrl from currentEntity
        String currentImageFileName = currentEntity.getCurrentImageUrl();
        String inputPath = saveDir + File.separator + currentImageFileName;
        log.info("Loading image from: {}", inputPath);

        // ------
        // STEP 3
        // ------
        // Get next version from version control service
        int nextVersion = imageVersionControlService.getNextVersion(imageId);

        // Load & Process the image
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

        // ------
        // Step 4
        // ------
        // Update photo session using version control service
        imageVersionControlService.updatePhotoSession(imageId, nextVersion);

        // ------
        // Step 5
        // ------
        // Get base image URL from version control service
        String baseImageUrl = imageVersionControlService.getBaseImageUrl(imageId, currentEntity);

        // Create and save the new image entity
        CartooniseFactory cartooniseFactory = (CartooniseFactory) imageFactorySelector.getFactory(ImageOperationType.CARTOONISE);
        ImageNewEntity processedImage = cartooniseFactory.create(imageId, currentEntity.getUserId(), nextVersion, baseImageUrl, null);


        return imageNewRepository.save(processedImage);
    }

    private Mat removeBackgroundUsingGrabCut(Mat image) {
        // Create initial mask for GrabCut
        Mat mask = new Mat(image.size(), CvType.CV_8UC1, Scalar.all(Imgproc.GC_BGD)); // Set all to background initially
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        Mat foreground = null;

        // Create a copy of the image for visualization
        Mat visualizationImg = image.clone();

        // Convert to grayscale for better edge detection
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);

        // Apply Gaussian blur to reduce noise
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);

        // Use Canny edge detection to find edges
        Mat edges = new Mat();
        Imgproc.Canny(grayImage, edges, 100, 150);

        // Dilate edges to make them more prominent
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
        Imgproc.dilate(edges, edges, kernel);

        try {
            // Load the face detector
            CascadeClassifier faceDetector = new CascadeClassifier();
            boolean isLoaded = faceDetector.load("src/main/resources/opencv/haarcascade_frontalface_default.xml");

            if (!isLoaded) {
                throw new Exception("Failed to load face cascade classifier");
            }

            // Detect faces
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(image, faceDetections);
            Rect[] faces = faceDetections.toArray();

            if (faces.length > 0) {
                // Get the face (we expect only one, but take the largest if multiple are detected)
                Rect face = faces[0];
                if (faces.length > 1) {
                    // Find the largest face in case multiple are detected
                    face = getLargestRect(faces);
                }

                // Create elliptical mask for face instead of rectangle - moved slightly higher to capture hair
                Point center = new Point(face.x + face.width / 2,
                        face.y + (double) face.height / 2 - (face.height * 0.1)); // Moved up slightly to include hair
                Size axes = new Size(face.width * 0.4, face.height * 0.625); // Slightly taller to capture more hair

                // Create face mask for capturing edges
                Mat faceMask = new Mat(mask.size(), CvType.CV_8UC1, Scalar.all(0));
                Imgproc.ellipse(faceMask, center, axes, 0, 0, 360, new Scalar(255), -1);  // Use 255 for white

                // Draw red ellipse outline on visualization image
                Imgproc.ellipse(visualizationImg, center, axes, 0, 0, 360, new Scalar(0, 0, 255), 2);

                // Create trapezoid for shoulders/neck below the face - wider base, shorter height, narrower top
                int neckTop = face.y + face.height;
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
                shoulderPoints[0] = new Point(center.x - topWidth * 0.2, neckTop); // Top left - narrower
                shoulderPoints[1] = new Point(center.x + topWidth * 0.2, neckTop); // Top right - narrower
                shoulderPoints[2] = bottomRight;
                shoulderPoints[3] = bottomLeft;

                // Create shoulder mask for capturing edges
                Mat shoulderMask = new Mat(mask.size(), CvType.CV_8UC1, Scalar.all(0));
                List<MatOfPoint> contours = new ArrayList<>();
                MatOfPoint shoulder = new MatOfPoint(shoulderPoints);
                contours.add(shoulder);
                Imgproc.fillPoly(shoulderMask, contours, new Scalar(255));  // Use 255 for white

                // Draw red trapezoid outline on visualization image
                for (int i = 0; i < shoulderPoints.length; i++) {
                    Imgproc.line(visualizationImg,
                            shoulderPoints[i],
                            shoulderPoints[(i + 1) % shoulderPoints.length],
                            new Scalar(0, 0, 255), 2);
                }

                // Combine face and shoulder masks to get the region of interest
                Mat combinedMask = new Mat();
                Core.bitwise_or(faceMask, shoulderMask, combinedMask);

                // Save combined mask for visualization (optional)
                Imgcodecs.imwrite("images/masks/combined_mask.jpg", combinedMask);

                // Now capture edges only within the masks
                Mat maskedEdges = new Mat();
                Core.bitwise_and(edges, combinedMask, maskedEdges);

                // Save masked edges for visualization (optional)
                Imgcodecs.imwrite("images/masks/masked_edges.jpg", maskedEdges);

                // Initialize the GrabCut mask based on the masked edges
                // Areas with edges will be marked as probable foreground
                // First create an expanded region around the face and shoulders
                int expandedTop = Math.max(0, face.y - face.height / 2);
                int expandedBottom = Math.min(image.height(), neckTop + shoulderHeight + face.height / 2);
                int expandedLeft = (int) Math.max(0, center.x - shoulderWidth * 0.6);
                int expandedRight = (int) Math.min(image.width(), center.x + shoulderWidth * 0.6);
                Rect region = new Rect(expandedLeft, expandedTop, expandedRight - expandedLeft, expandedBottom - expandedTop);

                // For all pixels in the mask, set initial values
                for (int y = 0; y < mask.rows(); y++) {
                    for (int x = 0; x < mask.cols(); x++) {
                        // If pixel is outside our expanded region, it's definite background
                        if (y < expandedTop || y >= expandedBottom || x < expandedLeft || x >= expandedRight) {
                            mask.put(y, x, Imgproc.GC_BGD);
                        }
                        // If pixel is within masks and is an edge
                        else if (maskedEdges.get(y, x)[0] > 0) {
                            mask.put(y, x, Imgproc.GC_FGD);  // Mark as definite foreground
                        }
                        // If pixel is within masks but not an edge
                        else if (combinedMask.get(y, x)[0] > 0) {
                            mask.put(y, x, Imgproc.GC_PR_FGD);  // Mark as probable foreground
                        }
                        // Otherwise, it's probable background
                        else {
                            mask.put(y, x, Imgproc.GC_PR_BGD);
                        }
                    }
                }

                // Save the initial GrabCut mask for visualization (optional)
                Mat maskVis = new Mat();
                mask.convertTo(maskVis, CvType.CV_8U, 63.75); // Scale values for visualization
                Imgcodecs.imwrite("images/masks/initial_grabcut_mask.jpg", maskVis);

                // Run GrabCut with our edge-enhanced mask
                Imgproc.grabCut(image, mask, region, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_MASK);

                // Clean up resources we're done with
                if (faceMask != null && !faceMask.empty()) faceMask.release();
                if (shoulderMask != null && !shoulderMask.empty()) shoulderMask.release();
                if (combinedMask != null && !combinedMask.empty()) combinedMask.release();
                if (maskedEdges != null && !maskedEdges.empty()) maskedEdges.release();
                shoulder.release();
            } else {
                throw new Exception("No face detected");
            }
        } catch (Exception e) {
            System.err.println("Face detection failed: " + e.getMessage());

            // Fallback to basic rect if no face detected or error occurred
            Rect rect = new Rect(1, 1, image.width() - 2, image.height() - 2);
            Imgproc.grabCut(image, mask, rect, bgModel, fgModel, 5, Imgproc.GC_INIT_WITH_RECT);
        }

        try {
            // Create a mask for foreground pixels (both definitely and probably foreground)
            Mat foregroundMask = new Mat();
            Core.compare(mask, new Scalar(Imgproc.GC_PR_FGD), foregroundMask, Core.CMP_EQ);
            Mat foregroundMask2 = new Mat();
            Core.compare(mask, new Scalar(Imgproc.GC_FGD), foregroundMask2, Core.CMP_EQ);
            Core.bitwise_or(foregroundMask, foregroundMask2, foregroundMask);

            // Create the foreground image
            foreground = new Mat(image.size(), CvType.CV_8UC3, new Scalar(255, 255, 255));
            image.copyTo(foreground, foregroundMask);

            // Save visualization and edge images if they're valid
            if (visualizationImg != null && !visualizationImg.empty()) {
                try {
                    Imgcodecs.imwrite("images/masks/visualization.jpg", visualizationImg);
                } catch (Exception e) {
                    System.err.println("Failed to save visualization image: " + e.getMessage());
                }
            }

            if (edges != null && !edges.empty()) {
                try {
                    Imgcodecs.imwrite("images/masks/edges.jpg", edges);
                } catch (Exception e) {
                    System.err.println("Failed to save edges image: " + e.getMessage());
                }
            }

            // Clean up resources
            if (foregroundMask != null && !foregroundMask.empty()) foregroundMask.release();
            if (foregroundMask2 != null && !foregroundMask2.empty()) foregroundMask2.release();

        } catch (Exception e) {
            System.err.println("Error in final processing: " + e.getMessage());
        } finally {
            // Clean up all resources properly
            if (mask != null && !mask.empty()) mask.release();
            if (bgModel != null && !bgModel.empty()) bgModel.release();
            if (fgModel != null && !fgModel.empty()) fgModel.release();
            if (grayImage != null && !grayImage.empty()) grayImage.release();
            if (edges != null && !edges.empty()) edges.release();
            if (kernel != null && !kernel.empty()) kernel.release();
            if (visualizationImg != null && !visualizationImg.empty()) visualizationImg.release();
        }

        return foreground;
    }

    // Helper method to find the largest rectangle (in case multiple faces are detected)
    private Rect getLargestRect(Rect[] rects) {
        if (rects.length == 0) {
            return null;
        }

        Rect largest = rects[0];
        for (Rect rect : rects) {
            if (rect.area() > largest.area()) {
                largest = rect;
            }
        }

        return largest;
    }
}

