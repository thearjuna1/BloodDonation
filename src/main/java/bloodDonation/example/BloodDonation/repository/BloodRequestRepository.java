package bloodDonation.example.BloodDonation.repository;

import bloodDonation.example.BloodDonation.entity.BloodRequest;
import bloodDonation.example.BloodDonation.entity.RequestStatus;
import bloodDonation.example.BloodDonation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    // FIX: Removed BloodRequest. prefix from RequestStatus
    List<BloodRequest> findByStatusOrderByCreatedAtAsc(RequestStatus status);

    // FIX: Removed BloodRequest. prefix from RequestStatus
    List<BloodRequest> findByBloodGroupAndStatus(User.BloodGroup bloodGroup, RequestStatus status);

    @Query("SELECT COUNT(r) FROM BloodRequest r WHERE r.patient.id = :patientId " +
            "AND r.createdAt >= CURRENT_TIMESTAMP - 1 HOUR")
    long countRecentRequestsByPatient(@Param("patientId") Long patientId);

    @Query("SELECT r FROM BloodRequest r WHERE r.status = bloodDonation.example.BloodDonation.entity.RequestStatus.PENDING")
    List<BloodRequest> findAllPendingRequests();

    @Query("SELECT r FROM BloodRequest r WHERE r.status = bloodDonation.example.BloodDonation.entity.RequestStatus.APPROVED")
    List<BloodRequest> findApprovedRequests();

    List<BloodRequest> findByStatus(RequestStatus status);
}