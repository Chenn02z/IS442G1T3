package IS442.G1T3.IDPhotoGenerator;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import IS442.G1T3.IDPhotoGenerator.repository.ImageRepository;
import IS442.G1T3.IDPhotoGenerator.service.impl.BackgroundRemovalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class BackgroundRemovalServiceImplTest {

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private BackgroundRemovalServiceImpl backgroundRemovalServiceImpl;

    private MockMultipartFile testImage;
    private final String testUploadDir = "src/test/resources/uploads";

    @BeforeEach
    void setUp() throws Exception {
        // Create test image
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        File tempFile = File.createTempFile("test", ".png");
        ImageIO.write(image, "png", tempFile);

        testImage = new MockMultipartFile(
                "test.png",
                "test.png",
                "image/png",
                Files.readAllBytes(tempFile.toPath())
        );

        ReflectionTestUtils.setField(backgroundRemovalServiceImpl, "uploadDir", testUploadDir);

        // Mock repository
        when(imageRepository.save(any(ImageEntity.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void removeBackground_WithWhiteBackground_ShouldSucceed() throws Exception {
        ImageEntity result = backgroundRemovalServiceImpl.removeBackground(
                testImage,
                UUID.randomUUID(),
                "WHITE"
        );

        assertNotNull(result);
        assertTrue(Files.exists(Path.of(result.getSavedFilePath())));
        assertTrue(result.getSavedFilePath().contains("processed_"));
        assertEquals("COMPLETED", result.getStatus());
    }
}
