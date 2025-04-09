package IS442.G1T3.IDPhotoGenerator.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import IS442.G1T3.IDPhotoGenerator.model.UserStoragePreference;

public interface UserStoragePreferenceRepository extends JpaRepository<UserStoragePreference, UUID> {
    UserStoragePreference findByUserId(UUID userId);
}
