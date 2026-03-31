package bloodDonation.example.BloodDonation.service;

import bloodDonation.example.BloodDonation.entity.AuditLog;
import bloodDonation.example.BloodDonation.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Logs an audit event. Uses REQUIRES_NEW so it always persists
     * even if the parent transaction rolls back — audit must never be lost.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String actorId, String targetId) {
        log(action, actorId, targetId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String actorId, String targetId, String details) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .targetId(targetId)
                .details(details)
                .build();

        auditLogRepository.save(entry);
        log.debug("AUDIT: action={} actor={} target={}", action, actorId, targetId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByActor(String actorId) {
        return auditLogRepository.findByActorIdOrderByTimestampDesc(actorId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByTarget(String targetId) {
        return auditLogRepository.findByTargetIdOrderByTimestampDesc(targetId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByDateRange(LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(from, to);
    }
}
