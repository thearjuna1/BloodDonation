package bloodDonation.example.BloodDonation.dto;

import bloodDonation.example.BloodDonation.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDto {

    @Data
    public static class RegisterRequest {
        @NotBlank
        private String fullName;

        @NotBlank @Email
        private String email;

        @NotBlank
        private String phone;

        @NotBlank @Size(min = 8)
        private String password;

        @NotNull
        private User.Role role;

        private User.BloodGroup bloodGroup;

        private String aadhaar;
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
        private boolean approved;
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
        private boolean adminVerified;
        private LocalDate nextEligibleDonationDate;
        private LocalDateTime createdAt;
    }
}