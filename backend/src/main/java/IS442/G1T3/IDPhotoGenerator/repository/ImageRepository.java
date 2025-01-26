package IS442.G1T3.IDPhotoGenerator.repository;

import IS442.G1T3.IDPhotoGenerator.model.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, UUID> {

    //    Find single image by imageId
    ImageEntity findByImageId(UUID imageId);

    // Find all images by userId
    List<ImageEntity> findByUserId(UUID userId);

}
