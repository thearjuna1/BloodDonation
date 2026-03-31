package bloodDonation.example.BloodDonation.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action;       // e.g. REQUEST_APPROVED, DONATION_COMPLETED

    @Column(nullable = false)
    private String actorId;      // Who performed the action

    @Column(nullable = false)
    private String targetId;     // What was affected

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    private String ipAddress;    // Optional: captured from request
    private String details;      // Optional: additional context

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    // ✅ No setters on this entity — immutable by design.
    // No update or delete operations are permitted.
}
