package IS442.G1T3.IDPhotoGenerator.model;

import jakarta.persistence.*;
import lombok.*;

// import java.time.LocalDateTime;
import java.util.UUID;
@Data
@Entity
@Table(name = "images_new")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ImageNewEntity {

    @Id
    @Column(name = "current_image_url", updatable = false, nullable = false)
    private String currentImageUrl;


    @Column(name = "image_id", updatable = false, nullable = false)
    private UUID imageId;


    @Column(name = "version", updatable = false, nullable = false)
    private int version;

    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(name = "label")
    private String label;

    @Column(name = "base_image_url")
    private String baseImageUrl;

    @Column(name = "crop_data")
    private String cropData;

}
