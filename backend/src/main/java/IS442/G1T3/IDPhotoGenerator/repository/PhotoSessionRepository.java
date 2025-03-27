package IS442.G1T3.IDPhotoGenerator.repository;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;
import IS442.G1T3.IDPhotoGenerator.model.PhotoSession;

@Repository
public interface PhotoSessionRepository extends JpaRepository<PhotoSession, String> {
    PhotoSession findByImageId(UUID imageId);
}
