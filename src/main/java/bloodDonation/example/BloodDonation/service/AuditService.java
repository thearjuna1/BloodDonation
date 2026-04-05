package bloodDonation.example.BloodDonation.service;

import bloodDonation.example.BloodDonation.entity.AuditLog;
import bloodDonation.example.BloodDonation.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String actorId, String targetId) {
        AuditLog entry = AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .targetId(targetId)
                .build();
        auditLogRepository.save(entry);
        log.debug("AUDIT: action={} actor={} target={}", action, actorId, targetId);
    }
}