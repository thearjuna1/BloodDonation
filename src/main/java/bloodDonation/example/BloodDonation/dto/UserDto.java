package bloodDonation.example.BloodDonation.dto;

import bloodDonation.example.BloodDonation.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.validation.constraints.Pattern;


import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDto {

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Email
        private String email;

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank
        private String fullName;

        @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian phone number")
        private String phone;

        @NotNull
        private User.Role role;

        private User.BloodGroup bloodGroup; // Required for DONOR

        private String aadhaar; // Will be hashed before storage
    }

    @Data
    public static class UserResponse {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private User.Role role;
        private User.BloodGroup bloodGroup;
        private boolean emailVerified;
        private boolean phoneVerified;
        private boolean adminVerified;
        private LocalDate nextEligibleDonationDate;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank
        private String fullName;

        private User.BloodGroup bloodGroup;
    }

    @Data
    public static class VerifyDoctorRequest {
        @NotNull
        private Long doctorId;

        @NotNull
        private boolean approved;

        private String rejectionReason;
    }
}
