package IS442.G1T3.IDPhotoGenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class IdPhotoGeneratorApplication {

	@Value("${image.storage.path}")
	private String storagePath;

	public static void main(String[] args) {
		SpringApplication.run(IdPhotoGeneratorApplication.class, args);
	}

	@Bean
	CommandLineRunner init() {
		return args -> {
			Path uploadPath = Paths.get(storagePath);
			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
				System.out.println("Created image storage directory at: " + uploadPath.toAbsolutePath());
			} else {
				System.out.println("Image storage directory exists at: " + uploadPath.toAbsolutePath());
			}
		};
	}
}
