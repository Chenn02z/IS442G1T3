package IS442.G1T3.IDPhotoGenerator.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/helloworld")
public class helloworld {
    @GetMapping("")
    public String index() {
        log.info("Handling request for /api/helloworld");
        return "Greetings from Spring Boot!";
    }
}
