package bloodDonation.example.BloodDonation.dto;

import bloodDonation.example.BloodDonation.entity.BloodRequest;
import bloodDonation.example.BloodDonation.entity.User;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class BloodRequestDto {

    @Data
    public static class CreateRequest {
        @NotNull
        private User.BloodGroup bloodGroup;

        @NotBlank
        private String hospitalName;

        @NotBlank
        private String hospitalAddress;

        @Min(1)
        private Integer unitsRequired;
    }

    @Data
    public static class ApprovalRequest {
        @NotNull
        private Long requestId;

        private String notes;
    }

    @Data
    public static class RejectionRequest {
        @NotNull
        private Long requestId;

        @NotBlank
        private String reason;
    }

    @Data
    public static class BloodRequestResponse {
        private Long id;
        private Long patientId;
        private String patientName;
        private User.BloodGroup bloodGroup;
        private BloodRequest.RequestStatus status;
        private String hospitalName;
        private String hospitalAddress;
        private Integer unitsRequired;
        private boolean documentsVerified;
        private String rejectionReason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
