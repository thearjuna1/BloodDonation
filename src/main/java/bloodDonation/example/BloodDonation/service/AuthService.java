package bloodDonation.example.BloodDonation.service;

import bloodDonation.example.BloodDonation.dto.AuthDto;
import bloodDonation.example.BloodDonation.dto.UserDto;
import bloodDonation.example.BloodDonation.entity.User;
import bloodDonation.example.BloodDonation.exception.BusinessException;
import bloodDonation.example.BloodDonation.repository.UserRepository;
import bloodDonation.example.BloodDonation.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Standard registration for DONOR, PATIENT roles.
     * DOCTOR must use registerDoctor() instead (requires documents).
     */
    @Transactional
    public UserDto.UserResponse register(@Valid UserDto.RegisterRequest request) {
        if (request.getRole() == User.Role.DOCTOR) {
            throw new BusinessException("Doctors must register via the /api/auth/register-doctor endpoint with required documents.");
        }
        if (request.getRole() == User.Role.ADMIN) {
            throw new BusinessException("Admin accounts cannot be self-registered.");
        }

        validateCommonFields(request.getEmail(), request.getPhone());

        if (request.getRole() == User.Role.DONOR && request.getBloodGroup() == null) {
            throw new BusinessException("Blood group is required for donors");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .bloodGroup(request.getBloodGroup())
                .emailVerified(true)   // Auto-verify — no OTP needed
                .adminVerified(false)
                .build();

        User saved = userRepository.save(user);
        auditService.log("USER_REGISTERED", saved.getId().toString(), saved.getId().toString());
        log.info("New user registered: {} with role: {}", saved.getEmail(), saved.getRole());
        return userService.mapToResponse(saved);
    }

    /**
     * Doctor-specific registration — requires medical licence + degree certificate.
     * Account is created but adminVerified=false; the admin must approve before
     * the doctor can access any protected functionality.
     */
    @Transactional
    public UserDto.UserResponse registerDoctor(UserDto.RegisterDoctorRequest request,
                                               MultipartFile medicalLicence,
                                               MultipartFile degreeCertificate) {

        validateCommonFields(request.getEmail(), request.getPhone());

        if (medicalLicence == null || medicalLicence.isEmpty()) {
            throw new BusinessException("Medical licence document is required for doctor registration");
        }
        if (degreeCertificate == null || degreeCertificate.isEmpty()) {
            throw new BusinessException("Degree certificate is required for doctor registration");
        }

        // Store documents before saving the user so any storage failure is caught first
        String licenceUrl = fileStorageService.storeFile(medicalLicence, "doctor-docs");
        String degreeUrl  = fileStorageService.storeFile(degreeCertificate, "doctor-docs");

        User doctor = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(User.Role.DOCTOR)
                .emailVerified(true)
                .adminVerified(false)   // ← must be approved by admin before they can work
                .medicalLicenceUrl(licenceUrl)
                .degreeCertificateUrl(degreeUrl)
                .build();

        User saved = userRepository.save(doctor);
        auditService.log("DOCTOR_REGISTERED_PENDING_APPROVAL", saved.getId().toString(), saved.getId().toString());
        log.info("Doctor registered (pending admin approval): {}", saved.getEmail());
        return userService.mapToResponse(saved);
    }

    public AuthDto.LoginResponse login(@Valid AuthDto.LoginRequest request) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception ex) {
            throw new BusinessException("Invalid email or password");
        }

        User user = (User) auth.getPrincipal();

        if (user.isAccountLocked()) {
            throw new BusinessException("Account is locked. Contact support.");
        }

        // Doctors who haven't been approved yet are told explicitly
        if (user.getRole() == User.Role.DOCTOR && !user.isAdminVerified()) {
            throw new BusinessException("Your doctor account is pending admin approval. Please wait for verification.");
        }

        String token = jwtUtil.generateToken(user);
        auditService.log("USER_LOGIN", user.getId().toString(), user.getId().toString());

        return new AuthDto.LoginResponse(token, user.getEmail(), user.getRole().name(), jwtExpirationMs / 1000);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validateCommonFields(String email, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already registered");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("Phone number already registered");
        }
    }
}