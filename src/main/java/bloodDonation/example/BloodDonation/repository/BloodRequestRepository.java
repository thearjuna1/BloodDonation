package bloodDonation.example.BloodDonation.repository;
import bloodDonation.example.BloodDonation.entity.BloodRequest;
import bloodDonation.example.BloodDonation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<BloodRequest> findByStatusOrderByCreatedAtAsc(BloodRequest.RequestStatus status);

    List<BloodRequest> findByBloodGroupAndStatus(User.BloodGroup bloodGroup, BloodRequest.RequestStatus status);

    @Query("SELECT COUNT(r) FROM BloodRequest r WHERE r.patient.id = :patientId " +
            "AND r.createdAt >= CURRENT_TIMESTAMP - 1 HOUR")
    long countRecentRequestsByPatient(Long patientId);

    @Query("SELECT r FROM BloodRequest r WHERE r.status = 'PENDING' AND r.documentsVerified = true")
    List<BloodRequest> findVerifiedPendingRequests();
}
