package IS442.G1T3.IDPhotoGenerator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Data
@Entity
@Table(name = "photo_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class photoSession {
    @Id
    @Column(name = "image_id", updatable = false, nullable = false)
    private UUID imageId;

    @Column(name = "undo_stack")
    private String undoStack;

    @Column(name = "redo_stack")
    private String redoStack;
}
