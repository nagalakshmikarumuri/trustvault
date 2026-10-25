package com.trustvault.service;

import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    @DisplayName("record persists actor, action, document, outcome, and safe details")
    void recordCreatesAuditEntry() {
        User actor = new User("alice", "hash", Role.OWNER, true);
        actor.setId(1L);
        Document document = new Document("a.pdf", "uuid.pdf", actor, "Legal", "application/pdf", 4L);
        document.setId(10L);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.record(actor, AuditAction.DOWNLOAD, document, null, AuditOutcome.SUCCESS, "Document downloaded");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getActorUsername()).isEqualTo("alice");
        assertThat(saved.getAction()).isEqualTo(AuditAction.DOWNLOAD);
        assertThat(saved.getDocument()).isEqualTo(document);
        assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(saved.getDetails()).doesNotContain("uuid.pdf");
    }

    @Test
    @DisplayName("denied access is recorded with DENIED outcome")
    void deniedAccessIsRecorded() {
        User actor = new User("bob", "hash", Role.VIEWER, true);
        actor.setId(2L);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.record(actor, AuditAction.ACCESS_DENIED, null, null, AuditOutcome.DENIED, "Unauthorized download");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(captor.getValue().getOutcome()).isEqualTo(AuditOutcome.DENIED);
        assertThat(captor.getValue().getActorUsername()).isEqualTo("bob");
    }
}
