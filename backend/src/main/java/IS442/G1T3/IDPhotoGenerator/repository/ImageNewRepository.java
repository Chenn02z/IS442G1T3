package IS442.G1T3.IDPhotoGenerator.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

@Repository
public interface ImageNewRepository extends JpaRepository<ImageNewEntity, String> {

    // Find a single image by currentImageUrl (if needed)
    ImageNewEntity findByCurrentImageUrl(String currentImageUrl);

    @Query("SELECT i FROM ImageNewEntity i WHERE i.currentImageUrl LIKE CONCAT(:currentImageUrl, '%')")
    ImageNewEntity findByCurrentImageUrlWithoutFormat(@Param("currentImageUrl") String currentImageUrl);

    ImageNewEntity findByBaseImageUrl(String baseImageUrl);

    // Find all images by userId
    List<ImageNewEntity> findByUserId(UUID userId);

    // Find latest row by image id
    @Query("SELECT i FROM ImageNewEntity i WHERE i.imageId = :imageId AND i.version = " +
            "(SELECT MAX(i2.version) FROM ImageNewEntity i2 WHERE i2.imageId = :imageId)")
    ImageNewEntity findLatestRowByImageId(@Param("imageId") UUID imageId);

    List<ImageNewEntity> findByImageId(UUID imageId);

    // Add the missing method
    ImageNewEntity findByImageIdAndVersion(UUID imageId, int version);

    Optional<ImageNewEntity> findTopByUserIdOrderByVersionDesc(UUID userId);
    Optional<ImageNewEntity> findTopByImageIdOrderByVersionDesc(UUID imageId);

}

