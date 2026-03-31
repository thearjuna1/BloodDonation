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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final AuditService auditService;
    private final UserService userService;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Transactional
    public UserDto.UserResponse register(@Valid UserDto.RegisterRequest request) {
        // Validate uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone number already registered");
        }

        // Donors must specify blood group
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
                .aadhaarHash(request.getAadhaar() != null ?
                        userService.hashSensitiveData(request.getAadhaar()) : null)
                .build();

        User saved = userRepository.save(user);

        // Send email verification
        sendEmailVerification(saved);

        // Send phone OTP
        sendPhoneOtp(saved.getPhone());

        auditService.log("USER_REGISTERED", saved.getId().toString(), saved.getId().toString());
        log.info("New user registered: {} with role: {}", saved.getEmail(), saved.getRole());

        return userService.mapToResponse(saved);
    }

    public AuthDto.LoginResponse login(@Valid AuthDto.LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) auth.getPrincipal();

        if (!user.isEmailVerified()) {
            throw new BusinessException("Email not verified. Please check your inbox.");
        }

        if (user.isAccountLocked()) {
            throw new BusinessException("Account is locked due to suspicious activity. Contact support.");
        }

        String token = jwtUtil.generateToken(user);
        auditService.log("USER_LOGIN", user.getId().toString(), user.getId().toString());

        return new AuthDto.LoginResponse(
                token, user.getEmail(), user.getRole().name(), jwtExpirationMs / 1000
        );
    }

    @Transactional
    public void verifyEmail(String token) {
        String key = "email_verify:" + token;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            throw new BusinessException("Invalid or expired verification token");
        }

        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new BusinessException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);
        redisTemplate.delete(key);

        auditService.log("EMAIL_VERIFIED", user.getId().toString(), user.getId().toString());
        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void verifyOtp(AuthDto.OtpVerifyRequest request) {
        String key = "otp:" + request.getPhone();
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp == null) {
            throw new BusinessException("OTP expired or not found. Please request a new one.");
        }

        if (!storedOtp.equals(request.getOtp())) {
            // Increment failure count
            User user = userRepository.findByPhone(request.getPhone())
                    .orElseThrow(() -> new BusinessException("User not found"));
            userService.incrementFailedVerification(user.getId());
            throw new BusinessException("Invalid OTP");
        }

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setPhoneVerified(true);
        userRepository.save(user);
        redisTemplate.delete(key);

        auditService.log("PHONE_VERIFIED", user.getId().toString(), user.getId().toString());
    }

    private void sendEmailVerification(User user) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "email_verify:" + token,
                user.getId().toString(),
                24, TimeUnit.HOURS
        );
        // In production: send email with link containing token
        log.info("Email verification token for {}: {}", user.getEmail(), token);
    }

    private void sendPhoneOtp(String phone) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        redisTemplate.opsForValue().set(
                "otp:" + phone,
                otp,
                otpExpiryMinutes, TimeUnit.MINUTES
        );
        // In production: integrate with SMS gateway (Twilio, MSG91, etc.)
        log.info("OTP for phone {}: {}", phone, otp);
    }
}
