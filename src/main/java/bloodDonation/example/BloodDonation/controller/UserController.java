package bloodDonation.example.BloodDonation.controller;
import bloodDonation.example.BloodDonation.dto.UserDto;
import bloodDonation.example.BloodDonation.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserDto.UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public ResponseEntity<UserDto.UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id")
    public ResponseEntity<UserDto.UserResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(id, request));
    }

    // Admin-only: view unverified doctors
    @GetMapping("/doctors/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto.UserResponse>> getPendingDoctors() {
        return ResponseEntity.ok(userService.getPendingDoctors());
    }

    // Admin-only: approve or reject a doctor
    @PostMapping("/doctors/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> verifyDoctor(@Valid @RequestBody UserDto.VerifyDoctorRequest request) {
        userService.verifyDoctor(request);
        return ResponseEntity.ok("Doctor verification status updated");
    }
}
