package IS442.G1T3.IDPhotoGenerator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")  // Exposes files via /images/
                .addResourceLocations("file:images/")  // Maps to actual images/ directory
                .setCachePeriod(0);  // Disable caching (for testing)
    }
}
