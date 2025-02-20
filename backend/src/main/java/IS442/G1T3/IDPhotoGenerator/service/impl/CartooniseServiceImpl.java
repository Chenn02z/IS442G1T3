package IS442.G1T3.IDPhotoGenerator.service.impl;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.BackgroundRemovalService;
import IS442.G1T3.IDPhotoGenerator.service.CartoonisationService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CartooniseServiceImpl implements CartoonisationService {

	private final ImageRepository imageRepository;

	@Value("${app.upload.dir:${user.home}}")
	private String uploadDir;

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
	public byte[] cartooniseImage(MultipartFile file, UUID userId) throws Exception {
		// Convert MultipartFile to Mat
		Mat image = multipartFileToMat(file);

		// Perform background removal
		Mat result = removeBackgroundUsingGrabCut(image);

		// Save the processed image to the desktop
		String processedFileName = saveProcessedImageToDesktop(result, file.getOriginalFilename());

		// Convert Mat to byte array for response
		MatOfByte mob = new MatOfByte();
		Imgcodecs.imencode(".png", result, mob);
		byte[] imageData = mob.toArray();

		// Save the processed image to the original upload directory (optional)
		String fileName = saveImage(result, userId);

		return imageData;
	}

	private String saveProcessedImageToDesktop(Mat image, String originalFilename) throws Exception {
		Path uploadPath = Paths.get(System.getProperty("user.home"), "Desktop");
		Files.createDirectories(uploadPath);

		String savedFileName = UUID.randomUUID() + ".png";
		String processedFileName = "bg_removed_" + savedFileName;
		Path processedPath = uploadPath.resolve(processedFileName);

		Imgcodecs.imwrite(processedPath.toString(), image);

		return processedFileName;
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

	private Mat multipartFileToMat(MultipartFile file) throws Exception {
		byte[] bytes = file.getBytes();
		String tempFileName = UUID.randomUUID().toString() + ".jpg";
		Path path = Paths.get(uploadDir, tempFileName);
		Files.write(path, bytes);
		Mat image = Imgcodecs.imread(path.toString());
		Files.delete(path);
		return image;
	}

	private String saveImage(Mat image, UUID userId) throws Exception {
		String fileName = UUID.randomUUID().toString() + ".jpg";
		Path path;
		
		if (userId != null) {
			// If userId exists, save in user-specific directory
			path = Paths.get(uploadDir, userId.toString(), fileName);
		} else {
			// If userId is null, save in a general directory
			path = Paths.get(uploadDir, "anonymous", fileName);
		}
		
		Files.createDirectories(path.getParent());
		Imgcodecs.imwrite(path.toString(), image);
		return fileName;
	}
}

