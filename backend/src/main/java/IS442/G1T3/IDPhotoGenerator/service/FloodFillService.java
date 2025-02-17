package IS442.G1T3.IDPhotoGenerator.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FloodFillService {
    byte[] removeBackground(MultipartFile file, int seedX, int seedY, int tolerance) throws IOException;
}