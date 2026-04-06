package bloodDonation.example.BloodDonation.controller;

import bloodDonation.example.BloodDonation.dto.AuthDto;
import bloodDonation.example.BloodDonation.dto.UserDto;
import bloodDonation.example.BloodDonation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Standard registration — DONOR or PATIENT only.
     * Accepts JSON body.
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto.UserResponse> register(
            @Valid @RequestBody UserDto.RegisterRequest request) {

        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Doctor registration — requires two document uploads.
     * Accepts multipart/form-data.
     *
     * Fields expected:
     *   fullName, email, phone, password  (text parts)
     *   medicalLicence                    (file part)
     *   degreeCertificate                 (file part)
     */
    @PostMapping(value = "/register-doctor", consumes = "multipart/form-data")
    public ResponseEntity<UserDto.UserResponse> registerDoctor(
            @RequestPart("fullName")          String fullName,
            @RequestPart("email")             String email,
            @RequestPart("phone")             String phone,
            @RequestPart("password")          String password,
            @RequestPart("medicalLicence")    MultipartFile medicalLicence,
            @RequestPart("degreeCertificate") MultipartFile degreeCertificate) {

        UserDto.RegisterDoctorRequest req = new UserDto.RegisterDoctorRequest();
        req.setFullName(fullName);
        req.setEmail(email);
        req.setPhone(phone);
        req.setPassword(password);

        return ResponseEntity.ok(
                authService.registerDoctor(req, medicalLicence, degreeCertificate));
    }

    /**
     * Login — all roles.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthDto.LoginResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }
}