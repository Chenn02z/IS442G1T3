package IS442.G1T3.IDPhotoGenerator.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/helloworld")
public class helloworld {
    @GetMapping("")
    public String index() {
        return "Greetings from Spring Boot!";
    }
}
