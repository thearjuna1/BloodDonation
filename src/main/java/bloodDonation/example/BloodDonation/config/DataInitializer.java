package bloodDonation.example.BloodDonation.config;

import bloodDonation.example.BloodDonation.entity.User;
import bloodDonation.example.BloodDonation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "sahil@12gmail.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .fullName("Sahil Badoni")
                    .email(adminEmail)
                    .phone("9999999999")
                    .password(passwordEncoder.encode("arjun123"))
                    .role(User.Role.ADMIN)
                    .emailVerified(true)
                    .adminVerified(true)
                    .build();

            userRepository.save(admin);
            log.info("✅ Admin user 'Sahil Badoni' created with email: {}", adminEmail);
        } else {
            log.info("ℹ️ Admin user already exists, skipping seed.");
        }
    }
}