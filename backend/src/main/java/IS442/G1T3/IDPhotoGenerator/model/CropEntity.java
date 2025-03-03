package IS442.G1T3.IDPhotoGenerator.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CropEntity {

    @Id
    @Column(name = "crop_id", updatable = false, nullable = false)
    private UUID cropId;


    @Column(name = "crop_x", nullable = false)
    private double x;

    @Column(name = "crop_y", nullable = false)
    private double y;

    @Column(name = "crop_width", nullable = false)
    private double width;

    @Column(name = "crop_height", nullable = false)
    private double height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private ImageEntity image;  // Foreign key reference to ImageEntity

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
