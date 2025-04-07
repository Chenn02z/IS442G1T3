package IS442.G1T3.IDPhotoGenerator.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_storage_preferences")
public class UserStoragePreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "drive_app_folder_id")
    private String driveAppFolderId;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "token_expiry")
    private java.sql.Timestamp tokenExpiry;

    @Column(name = "created_at")
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;

    // Getters and setters here
}
