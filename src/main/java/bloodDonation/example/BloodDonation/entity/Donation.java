package bloodDonation.example.BloodDonation.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private BloodRequest bloodRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collector_id")
    private User collector; // Verified blood collector

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationStatus status;

    private Integer unitsDonated;

    // Collector verification
    private String collectorGovIdHash;   // Hashed government ID
    private boolean collectorOtpVerified = false;
    private String collectorQrCode;

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

    public boolean isCollectorVerified() {
        return collectorOtpVerified && collectorGovIdHash != null;
    }

    public enum DonationStatus {
        SCHEDULED, COLLECTOR_VERIFIED, COMPLETED, CANCELLED
    }
}
