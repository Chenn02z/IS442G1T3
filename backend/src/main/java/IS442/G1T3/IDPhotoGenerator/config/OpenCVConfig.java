// package IS442.G1T3.IDPhotoGenerator.config;

// import org.opencv.core.Core;
// import org.springframework.context.annotation.Configuration;
// import jakarta.annotation.PostConstruct;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import java.io.File;
// import java.io.FileOutputStream;
// import java.io.IOException;
// import java.io.InputStream;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.Arrays;

// @Configuration
// public class OpenCVConfig {
    
//     private static final Logger log = LoggerFactory.getLogger(OpenCVConfig.class);
    
//     @PostConstruct
//     public void loadLibrary() {
//         try {
//             // Get system properties
//             String osName = System.getProperty("os.name").toLowerCase();
//             String osArch = System.getProperty("os.arch").toLowerCase();
//             log.info("OS: {}, Architecture: {}", osName, osArch);
            
//             // For Apple Silicon Macs
//             if (osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm"))) {
//                 log.info("Detected Apple Silicon Mac (ARM64 architecture)");
                
//                 // Create a marker file to avoid initialization issues with OpenCV
//                 // but continue the application without OpenCV functionality
//                 System.setProperty("java.awt.headless", "true");
//                 log.warn("OpenCV native library is not compatible with ARM64 architecture.");
//                 log.warn("Application will run with limited image processing functionality.");
                
//                 // Explicitly mark OpenCV as unavailable
//                 System.setProperty("opencv.unavailable", "true");
//                 return;
//             }
            
//             // For other architectures, continue with normal loading
//             log.info("Attempting to load OpenCV native library...");
            
//             try {
//                 System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//                 log.info("OpenCV loaded successfully using System.loadLibrary()");
//                 return;
//             } catch (UnsatisfiedLinkError e) {
//                 log.warn("Could not load OpenCV library using System.loadLibrary(): {}", e.getMessage());
//             }
            
//             // Try extracting and loading from the dependency jar
//             try {
//                 String libPath;
//                 String libName;
                
//                 if (osName.contains("win")) {
//                     libPath = "/nu/pattern/opencv/windows/x86_64/opencv_java249.dll";
//                     libName = "opencv_java249.dll";
//                 } else if (osName.contains("mac")) {
//                     libPath = "/nu/pattern/opencv/osx/x86_64/libopencv_java249.dylib";
//                     libName = "libopencv_java249.dylib";
//                 } else if (osName.contains("linux")) {
//                     libPath = "/nu/pattern/opencv/linux/x86_64/libopencv_java249.so";
//                     libName = "libopencv_java249.so";
//                 } else {
//                     throw new RuntimeException("Unsupported operating system: " + osName);
//                 }
                
//                 // Extract library to a temporary file
//                 Path tempDir = Files.createTempDirectory("opencv_lib");
//                 File tempFile = new File(tempDir.toFile(), libName);
                
//                 try (InputStream in = getClass().getResourceAsStream(libPath);
//                      FileOutputStream out = new FileOutputStream(tempFile)) {
                    
//                     if (in == null) {
//                         throw new IOException("Could not find library resource: " + libPath);
//                     }
                    
//                     byte[] buffer = new byte[1024];
//                     int bytesRead;
//                     while ((bytesRead = in.read(buffer)) != -1) {
//                         out.write(buffer, 0, bytesRead);
//                     }
//                 }
                
//                 System.load(tempFile.getAbsolutePath());
//                 log.info("OpenCV loaded successfully from extracted library at: {}", tempFile.getAbsolutePath());
                
//             } catch (Exception ex) {
//                 log.error("Failed to load OpenCV from extracted library: {}", ex.getMessage());
//                 throw new RuntimeException("Failed to load OpenCV native library", ex);
//             }
            
//         } catch (Exception e) {
//             log.error("Error initializing OpenCV: {}", e.getMessage(), e);
//             // Set a property to indicate OpenCV is unavailable
//             System.setProperty("opencv.unavailable", "true");
//         }
//     }
// }