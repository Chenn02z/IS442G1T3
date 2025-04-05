package IS442.G1T3.IDPhotoGenerator.service.complianceChecker;

import org.springframework.stereotype.Component;

import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.BackgroundComplianceChecker;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.ComplianceChecker;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.DimensionsComplianceChecker;
import IS442.G1T3.IDPhotoGenerator.service.complianceChecker.checkers.FaceCenteringComplianceChecker;

/**
 * Builds a chain of compliance checkers following the Chain of Responsibility pattern.
 */
@Component
public class ComplianceChainBuilder {

    private final DimensionsComplianceChecker dimensionsComplianceChecker;
    private final BackgroundComplianceChecker backgroundComplianceChecker;
    private final FaceCenteringComplianceChecker faceCenteringComplianceChecker;

    public ComplianceChainBuilder(
            DimensionsComplianceChecker dimensionsComplianceChecker,
            BackgroundComplianceChecker backgroundComplianceChecker,
            FaceCenteringComplianceChecker faceCenteringComplianceChecker
    ) {
        this.dimensionsComplianceChecker = dimensionsComplianceChecker;
        this.backgroundComplianceChecker = backgroundComplianceChecker;
        this.faceCenteringComplianceChecker = faceCenteringComplianceChecker;
    }

    /**
     * Builds and returns a chain of compliance checkers (Chain of Responsibility Behavioural Design Pattern).
     * The order of the chain is important - simpler/faster checks should be first.
     *
     * @return The head of the compliance checker chain
     */
    public ComplianceChecker buildComplianceChain() {
        // Build the chain
        dimensionsComplianceChecker.nextComplianceChecker(backgroundComplianceChecker);
        backgroundComplianceChecker.nextComplianceChecker(faceCenteringComplianceChecker);

        // Return the head of the chain
        return dimensionsComplianceChecker;
    }
}
