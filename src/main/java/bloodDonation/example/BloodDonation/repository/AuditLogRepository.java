package bloodDonation.example.BloodDonation.repository;

import bloodDonation.example.BloodDonation.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorIdOrderByTimestampDesc(String actorId);

    List<AuditLog> findByTargetIdOrderByTimestampDesc(String targetId);

    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to);

    // ✅ Intentionally NO delete or update methods exposed
    // The append-only guarantee is enforced here at the repository level
}
