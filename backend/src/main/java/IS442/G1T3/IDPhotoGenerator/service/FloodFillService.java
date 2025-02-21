package IS442.G1T3.IDPhotoGenerator.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FloodFillService {
    /**
     * Processes the crop request for an image.
     *
     * @param file       The multipart file of the image to process.
     * @param seedPoints The list of seedpoint coordinates to remove the background from.
     * @param tolerance  The tolerance of colour difference between surrounding pixels.
     * @return           The processed image with pixels in a byte array.
     */
    byte[] removeBackground(MultipartFile file, String seedPointsJson, int tolerance) throws IOException;
}