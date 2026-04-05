package bloodDonation.example.BloodDonation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String actorId;

    @Column(nullable = false)
    private String targetId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    private String details;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}