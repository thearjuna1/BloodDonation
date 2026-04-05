package bloodDonation.example.BloodDonation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private BloodRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationStatus status;

    private Integer unitsDonated;

    @Column(name = "donated_at")
    private LocalDateTime donatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = DonationStatus.SCHEDULED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DonationStatus { SCHEDULED, COMPLETED, CANCELLED }
}