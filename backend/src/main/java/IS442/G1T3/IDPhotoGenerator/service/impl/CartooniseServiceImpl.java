package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.model.enums.ImageStatus;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.CartoonisationService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CartooniseServiceImpl implements CartoonisationService {

	private final ImageRepository imageRepository;

	@Value("${image.storage.path}")
	private String storagePath;

	static {
		try {
			nu.pattern.OpenCV.loadLocally();
		} catch (Exception e) {
			System.err.println("Error loading OpenCV native library: " + e.getMessage());
		}
	}

	public CartooniseServiceImpl(ImageRepository imageRepository) {
		this.imageRepository = imageRepository;
	}

	@Override
	public ImageEntity cartooniseImage(UUID imageId, String filePath) throws Exception {
		// Get original image from repository
		ImageEntity originalImage = imageRepository.findBySavedFilePath(filePath)
				.orElseThrow(() -> new RuntimeException("Image not found at: " + filePath));

		// Load the original image path
		Path originalPath = Paths.get(originalImage.getSavedFilePath());
		if (!originalPath.isAbsolute()) {
			originalPath = Paths.get(System.getProperty("user.dir"))
					.resolve(originalImage.getSavedFilePath())
					.normalize();
		}

		// Read the image using OpenCV
		Mat image = Imgcodecs.imread(originalPath.toString());
		if (image.empty()) {
			throw new RuntimeException("Failed to load image: " + originalPath);
		}

		// Process the image
		Mat result = removeBackgroundUsingGrabCut(image);

		// Create a unique filename for the new processed image
		// UUID newImageId = UUID.randomUUID();
		String processedFileName = "cartoonised_" + imageId + "_v" + originalImage.getProcessCount() +".png";
		String relativePath = storagePath + File.separator + processedFileName;
		String absoluteProcessedPath = new File("").getAbsolutePath() + File.separator + relativePath;

		// Ensure parent directory exists
		new File(absoluteProcessedPath).getParentFile().mkdirs();

		// Save the processed image
		Imgcodecs.imwrite(absoluteProcessedPath, result);

		// Create a new image entity instead of updating the existing one
		ImageEntity newImageEntity = new ImageEntity();
		newImageEntity.setImageId(imageId);
		newImageEntity.setUserId(originalImage.getUserId());
		newImageEntity.setOriginalFileName(originalImage.getOriginalFileName());
		newImageEntity.setSavedFilePath(relativePath);
		newImageEntity.setProcessCount(originalImage.getProcessCount());  // Start with process count 1 for the new image
		newImageEntity.setStatus(ImageStatus.COMPLETED.toString());
		newImageEntity.setBackgroundOption(originalImage.getBackgroundOption());
		
		// Save the new image entity
		return imageRepository.save(newImageEntity);
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

