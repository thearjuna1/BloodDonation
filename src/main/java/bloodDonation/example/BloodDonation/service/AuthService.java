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

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Transactional
    public UserDto.UserResponse register(@Valid UserDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new BusinessException("Email already registered");
        if (userRepository.existsByPhone(request.getPhone()))
            throw new BusinessException("Phone number already registered");
        if (request.getRole() == User.Role.DONOR && request.getBloodGroup() == null)
            throw new BusinessException("Blood group is required for donors");

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .bloodGroup(request.getBloodGroup())
                .emailVerified(true) // Auto-verify — no OTP needed
                .build();

        User saved = userRepository.save(user);
        auditService.log("USER_REGISTERED", saved.getId().toString(), saved.getId().toString());
        log.info("New user registered: {} with role: {}", saved.getEmail(), saved.getRole());
        return userService.mapToResponse(saved);
    }

    public AuthDto.LoginResponse login(@Valid AuthDto.LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) auth.getPrincipal();

        if (user.isAccountLocked())
            throw new BusinessException("Account is locked. Contact support.");

        String token = jwtUtil.generateToken(user);
        auditService.log("USER_LOGIN", user.getId().toString(), user.getId().toString());

        return new AuthDto.LoginResponse(token, user.getEmail(), user.getRole().name(), jwtExpirationMs / 1000);
    }
}