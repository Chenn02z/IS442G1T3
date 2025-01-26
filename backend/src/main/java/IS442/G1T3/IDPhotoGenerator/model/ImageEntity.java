package IS442.G1T3.IDPhotoGenerator.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class ImageEntity {

    @Id
    @Column(name = "image_id", updatable = false, nullable = false)
    private UUID imageId;

    @Column(name = "user_id", nullable = true)
    private UUID userId;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "saved_file_path", nullable = false)
    private String savedFilePath;

    @Column(name = "background_option", nullable = false, length = 20)
    private String backgroundOption;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
