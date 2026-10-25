package com.trustvault.service;

import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Document;
import com.trustvault.entity.User;
import com.trustvault.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Append-only audit writer. There is no update/delete API for application use.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog record(User actor,
                           AuditAction action,
                           Document document,
                           User targetUser,
                           AuditOutcome outcome,
                           String details) {
        AuditLog entry = new AuditLog();
        if (actor != null) {
            entry.setActor(actor);
            entry.setActorUsername(actor.getUsername());
        } else {
            entry.setActorUsername("unknown");
        }
        entry.setAction(action);
        entry.setDocument(document);
        if (targetUser != null) {
            entry.setTargetUser(targetUser);
            entry.setTargetUsername(targetUser.getUsername());
        }
        entry.setOutcome(outcome);
        entry.setDetails(truncate(details));
        AuditLog saved = auditLogRepository.save(entry);
        log.info("Audit {} {} actor={} outcome={}", saved.getId(), action,
                entry.getActorUsername(), outcome);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditLog> search(AuditAction action, String actorUsername, LocalDate from, LocalDate to) {
        String actorFilter = blankToNull(actorUsername);
        LocalDateTime fromTs = from == null ? null : from.atStartOfDay();
        LocalDateTime toTs = to == null ? null : to.plusDays(1).atStartOfDay();
        return auditLogRepository.search(action, actorFilter, fromTs, toTs);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String truncate(String details) {
        if (details == null) {
            return null;
        }
        return details.length() <= 500 ? details : details.substring(0, 500);
    }
}
