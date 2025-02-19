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
	public ImageEntity cartooniseImage(MultipartFile file, UUID userId) throws Exception {
		Path uploadPath = Paths.get(uploadDir).resolve("Desktop");
		Files.createDirectories(uploadPath);

		String originalFilename = file.getOriginalFilename();
		String savedFileName = UUID.randomUUID() + ".png";
		String processedFileName = "bg_removed_" + savedFileName;
		Path processedPath = uploadPath.resolve(processedFileName);
		String originalFileExtension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
		File tempInputFile = File.createTempFile("input_", originalFileExtension);

		try {
			file.transferTo(tempInputFile);
			Mat img = Imgcodecs.imread(tempInputFile.getAbsolutePath());
			if (img.empty()) {
				throw new RuntimeException("Failed to read image: " + tempInputFile.getAbsolutePath());
			}

			Mat mask = new Mat(img.size(), CvType.CV_8UC1, Scalar.all(0));
			Mat bgdModel = new Mat();
			Mat fgdModel = new Mat();

			// Adjust initial rectangle to a central area (50% of image size)
			int width = img.cols();
			int height = img.rows();
			int rectWidth = width / 2;
			int rectHeight = height / 2;
			Rect rect = new Rect(width / 4, height / 4, rectWidth, rectHeight);

			Imgproc.grabCut(img, mask, rect, bgdModel, fgdModel, 3, Imgproc.GC_INIT_WITH_RECT);

			// Create binary mask combining sure and possible foreground
			Mat mask1 = new Mat();
			Mat mask3 = new Mat();
			Core.compare(mask, new Scalar(1), mask1, Core.CMP_EQ); // Sure foreground
			Core.compare(mask, new Scalar(3), mask3, Core.CMP_EQ); // Possible foreground
			Mat binaryMask = new Mat();
			Core.bitwise_or(mask1, mask3, binaryMask);

			// Merge BGR channels with binary mask as alpha
			List<Mat> bgrChannels = new ArrayList<>();
			Core.split(img, bgrChannels);
			bgrChannels.add(binaryMask);
			Mat result = new Mat();
			Core.merge(bgrChannels, result);

			// Save the result as PNG
			if (!Imgcodecs.imwrite(processedPath.toString(), result)) {
				throw new RuntimeException("Failed to save image: " + processedPath);
			}

			return imageRepository.save(ImageEntity.builder()
					.imageId(UUID.randomUUID())
					.userId(userId)
					.originalFileName(originalFilename)
					.savedFilePath(processedPath.toString())
					.status("COMPLETED")
					.build());

		} finally {
			tempInputFile.delete();
		}
	}
}
