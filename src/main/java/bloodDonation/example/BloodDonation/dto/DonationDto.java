package bloodDonation.example.BloodDonation.dto;

import bloodDonation.example.BloodDonation.entity.Donation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class DonationDto {

    @Data
    public static class CreateDonationRequest {
        @NotNull
        private Long bloodRequestId;

        @NotNull @Min(1)
        private Integer unitsDonated;
    }

    @Data
    public static class CompleteDonationRequest {
        @NotNull
        private Long donationId;
    }

    @Data
    public static class DonationResponse {
        private Long id;
        private Long donorId;
        private String donorName;
        private Long bloodRequestId;
        private Donation.DonationStatus status;
        private Integer unitsDonated;
        private LocalDateTime donatedAt;
        private LocalDateTime createdAt;
    }
}