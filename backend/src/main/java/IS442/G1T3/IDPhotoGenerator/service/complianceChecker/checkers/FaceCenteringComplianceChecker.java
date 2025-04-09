package IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.dto.ComplianceCheckResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ComplianceCheckStatus;

/**
 * Checks that the face is properly centered in the ID photo.
 * Uses OpenCV for face detection and calculates if the face is centered
 * within acceptable thresholds.
 */
@Component
public class FaceCenteringComplianceChecker implements ComplianceChecker {

    @Value("${image.storage.path}")
    private String storagePath;

    private ComplianceChecker nextComplianceChecker;

    // Tolerance percentage for how much the face can deviate from center (as a percentage of image dimensions)
    private static final double CENTER_TOLERANCE_PERCENT = 10.0;

    static {
        // Load the OpenCV native library
        nu.pattern.OpenCV.loadLocally();
    }

    /**
     * Checks if the face in the photo is properly centered.
     * Uses Haar Cascade classifier for face detection and determines if the
     * face is within an acceptable distance from the center of the image.
     */
    @Override
    public ComplianceCheckResponse checkFailed(ImageNewEntity photo, String countryCode) {
        String imagePath = String.format("%s/%s", storagePath, photo.getCurrentImageUrl());

        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                throw new RuntimeException("File does not exist at path: " + imagePath);
            }

            // Load the image using OpenCV
            Mat image = Imgcodecs.imread(imagePath);
            if (image.empty()) {
                throw new RuntimeException("Unable to read the image at path: " + imagePath);
            }

            // Load the face cascade classifier
            URL cascadeUrl = getClass().getResource("/opencv/haarcascade_frontalface_default.xml");
            File cascadeFile = Paths.get(cascadeUrl.toURI()).toFile();
            CascadeClassifier faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());

            if (faceDetector.empty()) {
                throw new RuntimeException("Failed to load face cascade classifier ONE.");
            }

            // Convert to grayscale for face detection
            Mat grayImage = new Mat();
            Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(grayImage, grayImage);

            // Detect faces
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(grayImage, faces);
            Rect[] facesArray = faces.toArray();

            if (facesArray.length == 0) {
                return ComplianceCheckResponse.builder()
                        .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                        .message("No face detected in the image.")
                        .build();
            }

            if (facesArray.length > 1) {
                return ComplianceCheckResponse.builder()
                        .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                        .message("Multiple faces detected in the image. ID photo should have exactly one face.")
                        .build();
            }

            // Get the detected face
            Rect face = facesArray[0];

            // Calculate face center
            Point faceCenter = new Point(
                    face.x + face.width / 2.0,
                    face.y + face.height / 2.0
            );

            // Calculate image center
            Point imageCenter = new Point(
                    image.width() / 2.0,
                    image.height() / 2.0
            );

            // Calculate allowed deviation in pixels
            double maxHorizontalDeviation = image.width() * (CENTER_TOLERANCE_PERCENT / 100.0);
            double maxVerticalDeviation = image.height() * (CENTER_TOLERANCE_PERCENT / 100.0);

            // Check if face is centered within tolerance
            double horizontalDeviation = Math.abs(faceCenter.x - imageCenter.x);
            double verticalDeviation = Math.abs(faceCenter.y - imageCenter.y);

            if (horizontalDeviation > maxHorizontalDeviation || verticalDeviation > maxVerticalDeviation) {
                return ComplianceCheckResponse.builder()
                        .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                        .message("Face is not properly centered in the image.")
                        .build();
            }

        } catch (Exception e) {
            return ComplianceCheckResponse.builder()
                    .complianceCheckStatus(ComplianceCheckStatus.FAIL)
                    .message(String.format("Face centering compliance check failed: %s", e.getMessage()))
                    .build();
        }

        return ComplianceCheckResponse.builder()
                .complianceCheckStatus(ComplianceCheckStatus.PASS)
                .message("Face centering compliance check passed")
                .build();
    }

    @Override
    public void nextComplianceChecker(ComplianceChecker nextComplianceChecker) {
        this.nextComplianceChecker = nextComplianceChecker;
    }

    @Override
    public ComplianceChecker getNextComplianceChecker() {
        return nextComplianceChecker;
    }
} 