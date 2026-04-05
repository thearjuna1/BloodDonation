package bloodDonation.example.BloodDonation.repository;

import bloodDonation.example.BloodDonation.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    Optional<Donation> findByRequestId(Long requestId);

    Optional<Donation> findByRequestIdAndStatusNot(Long requestId, Donation.DonationStatus status);

    @Query("SELECT COUNT(d) FROM Donation d WHERE d.donor.id = :donorId AND d.status = 'COMPLETED'")
    long countCompletedByDonor(Long donorId);

    @Query("SELECT d FROM Donation d WHERE d.status = 'SCHEDULED'")
    List<Donation> findScheduledUnverifiedDonations();
}