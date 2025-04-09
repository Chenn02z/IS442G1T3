package IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers;

import IS442.G1T3.IDPhotoGenerator.dto.ComplianceCheckResponse;
import IS442.G1T3.IDPhotoGenerator.model.ImageNewEntity;

/**
 * This is in line with the Chain of responsibility Behavioural gangOf4 pattern
 */
public interface ComplianceChecker {
    ComplianceCheckResponse checkFailed(ImageNewEntity photo);

    void nextComplianceChecker(ComplianceChecker nextComplianceChecker);

    ComplianceChecker getNextComplianceChecker();
}