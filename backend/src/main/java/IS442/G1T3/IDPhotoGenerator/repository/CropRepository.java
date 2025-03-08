package IS442.G1T3.IDPhotoGenerator.repository;

import IS442.G1T3.IDPhotoGenerator.model.CropEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CropRepository extends JpaRepository<CropEntity, UUID> {

    // Find single crop entity by imageId
    // CropEntity findByImageId(UUID imageId);

    // Find crop entity by looking at imageId in image
   CropEntity findByImageImageId(UUID imageId);


    // Find original imageId by cropId
//    @Query("SELECT c.imageId FROM CropEntity c WHERE c.cropId=:cropId")
//    UUID findImageIdByCropId(UUID cropId);
}
