package bloodDonation.example.BloodDonation.entity;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;
    private String password;
    private String fullName;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private BloodGroup bloodGroup;

    @Builder.Default
    private boolean emailVerified = false;
    @Builder.Default
    private boolean adminVerified = false;
    @Builder.Default
    private boolean accountLocked = false;

    private LocalDate nextEligibleDonationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }

    public boolean isDonorEligible() {
        return nextEligibleDonationDate == null ||
                !java.time.LocalDate.now().isBefore(nextEligibleDonationDate);
    }

    public boolean isVerifiedDoctor() {
        return role == Role.DOCTOR && adminVerified;
    }

    public enum Role {ADMIN, DOCTOR, DONOR, PATIENT}

    public enum BloodGroup {
        O_NEG, A_NEG, B_NEG, O_POS, A_POS, B_POS, AB_NEG, AB_POS;

        @JsonCreator
        public static BloodGroup fromString(String value) {
            return switch (value.toUpperCase()) {
                case "B_POSITIVE", "B_POS" -> B_POS;
                case "A_POSITIVE", "A_POS" -> A_POS;
                case "O_POSITIVE", "O_POS" -> O_POS;
                case "AB_POSITIVE", "AB_POS" -> AB_POS;
                case "B_NEGATIVE", "B_NEG" -> B_NEG;
                case "A_NEGATIVE", "A_NEG" -> A_NEG;
                case "O_NEGATIVE", "O_NEG" -> O_NEG;
                case "AB_NEGATIVE", "AB_NEG" -> AB_NEG;
                default -> throw new IllegalArgumentException("Invalid blood group: " + value);
            };
        }
    }
}