package bloodDonation.example.BloodDonation.service;
import bloodDonation.example.BloodDonation.dto.UserDto;
import bloodDonation.example.BloodDonation.entity.User;
import bloodDonation.example.BloodDonation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import bloodDonation.example.BloodDonation.exception.BusinessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public UserDto.UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto.UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Authenticated user not found"));
        return mapToResponse(user);
    }

    @Transactional
    public UserDto.UserResponse updateProfile(Long id, UserDto.UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setFullName(request.getFullName());
        if (request.getBloodGroup() != null) {
            user.setBloodGroup(request.getBloodGroup());
        }

        User saved = userRepository.save(user);
        auditService.log("PROFILE_UPDATED", saved.getId().toString(), saved.getId().toString());
        return mapToResponse(saved);
    }

    @Transactional
    public void verifyDoctor(UserDto.VerifyDoctorRequest request) {
        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new BusinessException("Doctor not found"));

        if (doctor.getRole() != User.Role.DOCTOR) {
            throw new BusinessException("User is not registered as a doctor");
        }

        String actorEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User admin = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new BusinessException("Admin not found"));

        if (request.isApproved()) {
            doctor.setAdminVerified(true);
            auditService.log("DOCTOR_APPROVED", admin.getId().toString(), doctor.getId().toString());
            log.info("Doctor {} approved by admin {}", doctor.getId(), admin.getId());
        } else {
            doctor.setAdminVerified(false);
            auditService.log("DOCTOR_REJECTED", admin.getId().toString(), doctor.getId().toString());
            log.info("Doctor {} rejected by admin {}", doctor.getId(), admin.getId());
        }

        userRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public List<UserDto.UserResponse> getPendingDoctors() {
        return userRepository.findPendingDoctorVerifications()
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public void incrementFailedVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setFailedVerificationAttempts(user.getFailedVerificationAttempts() + 1);

        if (user.getFailedVerificationAttempts() >= 5) {
            user.setAccountLocked(true);
            auditService.log("ACCOUNT_LOCKED_FRAUD", "SYSTEM", user.getId().toString());
            log.warn("Account locked for user {} due to repeated verification failures", userId);
        }
        userRepository.save(user);
    }

    public String hashSensitiveData(String rawData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available", e);
        }
    }

    public UserDto.UserResponse mapToResponse(User user) {
        UserDto.UserResponse res = new UserDto.UserResponse();
        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setFullName(user.getFullName());
        res.setPhone(user.getPhone());
        res.setRole(user.getRole());
        res.setBloodGroup(user.getBloodGroup());
        res.setEmailVerified(user.isEmailVerified());
        res.setPhoneVerified(user.isPhoneVerified());
        res.setAdminVerified(user.isAdminVerified());
        res.setNextEligibleDonationDate(user.getNextEligibleDonationDate());
        res.setCreatedAt(user.getCreatedAt());
        return res;
    }
}
