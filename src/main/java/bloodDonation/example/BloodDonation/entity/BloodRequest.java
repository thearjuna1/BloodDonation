package bloodDonation.example.BloodDonation.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class BloodRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Enumerated(EnumType.STRING)
    private User.BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    private String hospitalName;
    private String hospitalAddress;
    private Integer unitsRequired;

    // ADD THESE FIELDS
    private String prescriptionDocUrl;
    private String hospitalProofUrl;

    @Builder.Default
    private boolean hasDocuments = false;

    @Builder.Default
    private boolean documentsVerified = false;

    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = RequestStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}