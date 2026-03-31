package bloodDonation.example.BloodDonation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private String tokenType = "Bearer";
        private String email;
        private String role;
        private long expiresIn;

        public LoginResponse(String token, String email, String role, long expiresIn) {
            this.token = token;
            this.email = email;
            this.role = role;
            this.expiresIn = expiresIn;
        }
    }

    @Data
    public static class OtpVerifyRequest {
        @NotBlank
        private String phone;

        @NotBlank
        private String otp;
    }

    @Data
    public static class EmailVerifyRequest {
        @NotBlank
        private String token;
    }
}
