package IS442.G1T3.IDPhotoGenerator.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;

@Repository
public interface PhotoSessionRepository extends JpaRepository<PhotoSession, String> {
    PhotoSession findByImageId(UUID imageId);
}
