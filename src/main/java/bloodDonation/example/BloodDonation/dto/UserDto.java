package bloodDonation.example.BloodDonation.dto;

import bloodDonation.example.BloodDonation.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDto {

    // ── Standard registration (DONOR / PATIENT) ───────────────────────────
    @Data
    public static class RegisterRequest {
        @NotBlank
        private String fullName;

        @Email @NotBlank
        private String email;

        @NotBlank
        private String phone;

        @NotBlank @Size(min = 8)
        private String password;

        private User.Role role;
        private User.BloodGroup bloodGroup;
    }

    // ── Doctor registration (multipart — documents required) ──────────────
    @Data
    public static class RegisterDoctorRequest {
        @NotBlank
        private String fullName;

        @Email @NotBlank
        private String email;

        @NotBlank
        private String phone;

        @NotBlank @Size(min = 8)
        private String password;

        // Documents are passed as MultipartFile in the controller,
        // not bound here — kept separate to allow @RequestPart binding.
    }

    // ── Profile update ────────────────────────────────────────────────────
    @Data
    public static class UpdateProfileRequest {
        @NotBlank
        private String fullName;
        private User.BloodGroup bloodGroup;
    }

    // ── Admin: verify / reject a doctor ──────────────────────────────────
    @Data
    public static class VerifyDoctorRequest {
        private Long doctorId;
        private boolean approved;
    }

    // ── Response ──────────────────────────────────────────────────────────
    @Data
    public static class UserResponse {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private User.Role role;
        private User.BloodGroup bloodGroup;
        private boolean emailVerified;
        private boolean adminVerified;
        private LocalDate nextEligibleDonationDate;
        private LocalDateTime createdAt;

        // Doctor document URLs (null for non-doctors)
        private String medicalLicenceUrl;
        private String degreeCertificateUrl;
    }
}