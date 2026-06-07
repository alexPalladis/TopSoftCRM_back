package com.topsoft.topsoftcrm_backend.service;

import com.topsoft.topsoftcrm_backend.model.AuditLog;
import com.topsoft.topsoftcrm_backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuditLogService
 *
 * Γράφει audit entries ασύγχρονα (fire-and-forget) ώστε:
 * 1. Να μην επιβαρύνει την κύρια transactional λειτουργία.
 * 2. Ένα τυχαίο σφάλμα logging να μην κάνει rollback την κύρια πράξη.
 *
 * Χρήση από άλλους services:
 *   auditLogService.log(actorId, actorRole, actorName,
 *                       "DEALER", dealer.getId(), dealer.getEponymia(),
 *                       "CREATE", "Δημιουργία dealer");
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Γράφει μια audit εγγραφή.
     * Τρέχει σε νέα transaction (REQUIRES_NEW) ώστε το commit να γίνει
     * αμέσως, ανεξάρτητα από την calling transaction.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            String actorId,
            String actorRole,
            String actorName,
            String entityType,
            String entityId,
            String entityName,
            String action,
            String description) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorId(actorId)
                    .actorRole(actorRole)
                    .actorName(actorName)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .action(action)
                    .description(description)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Ποτέ δεν σκοτώνουμε τη ροή για χάρη του logging
            log.error("Audit log αποτυχία: {}", e.getMessage());
        }
    }
}