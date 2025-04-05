package IS442.G1T3.IDPhotoGenerator.dto;

import java.util.ArrayList;
import java.util.List;

import IS442.G1T3.IDPhotoGenerator.model.enums.ComplianceCheckStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplianceCheckResponse {
    private ComplianceCheckStatus complianceCheckStatus;
    private String message;
    
    @Builder.Default
    private List<ComplianceDetail> details = new ArrayList<>();
    
    /**
     * Details of individual compliance checks
     */
    @Data
    @Builder
    public static class ComplianceDetail {
        private String checkName;
        private ComplianceCheckStatus status;
        private String message;
        private String category;  // e.g., "dimensions", "background", "face"
    }
}

