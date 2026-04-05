package bloodDonation.example.BloodDonation.dto;

import bloodDonation.example.BloodDonation.entity.RequestStatus;
import bloodDonation.example.BloodDonation.entity.User;
import lombok.Data;
import java.time.LocalDateTime;

public class BloodRequestDto {

    @Data
    public static class CreateRequest {
        private User.BloodGroup bloodGroup;
        private String hospitalName;
        private String hospitalAddress;
        private Integer unitsRequired;
    }

    @Data
    public static class BloodRequestResponse {
        private Long id;
        private Long patientId;
        private String patientName;
        private User.BloodGroup bloodGroup;
        // FIX: Changed from BloodRequest.RequestStatus to just RequestStatus
        private RequestStatus status;
        private String hospitalName;
        private String hospitalAddress;
        private Integer unitsRequired;
        private boolean documentsVerified;
        private boolean hasDocuments;
        private String prescriptionDocUrl;
        private String hospitalProofUrl;
        private String rejectionReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}