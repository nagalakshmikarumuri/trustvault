package com.trustvault.service;

import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.entity.PermissionType;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.exception.DocumentAccessDeniedException;
import com.trustvault.repository.DocumentPermissionRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPermissionServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentPermissionRepository documentPermissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentAccessService documentAccessService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private DocumentPermissionService documentPermissionService;

    private User owner;
    private User editor;
    private User target;
    private Document document;

    @BeforeEach
    void setUp() {
        owner = new User("owner", "hash", Role.OWNER, true);
        owner.setId(1L);
        editor = new User("editor", "hash", Role.VIEWER, true);
        editor.setId(2L);
        target = new User("bob", "hash", Role.VIEWER, true);
        target.setId(3L);
        document = new Document("a.pdf", "uuid.pdf", owner, "Legal", "application/pdf", 10L);
        document.setId(50L);
    }

    @Test
    @DisplayName("Owner can grant READ permission to an existing user")
    void ownerCanGrant() {
        when(documentRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(document));
        when(documentAccessService.canManagePermissions(owner, document)).thenReturn(true);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
        when(documentPermissionRepository.findByDocumentIdAndUserId(50L, 3L)).thenReturn(Optional.empty());
        when(documentPermissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentPermission granted = documentPermissionService.grant(owner, 50L, "bob", PermissionType.READ);

        assertThat(granted.getUser()).isEqualTo(target);
        assertThat(granted.getPermissionType()).isEqualTo(PermissionType.READ);
        verify(auditService).record(eq(owner), eq(AuditAction.PERMISSION_GRANT), eq(document), eq(target),
                eq(AuditOutcome.SUCCESS), any());
    }

    @Test
    @DisplayName("Grant is rejected when the target user does not exist")
    void cannotGrantToMissingUser() {
        when(documentRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(document));
        when(documentAccessService.canManagePermissions(owner, document)).thenReturn(true);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentPermissionService.grant(owner, 50L, "ghost", PermissionType.READ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
        verify(documentPermissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("EDIT user cannot grant permissions")
    void editUserCannotGrant() {
        when(documentRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(document));
        when(documentAccessService.canManagePermissions(editor, document)).thenReturn(false);

        assertThatThrownBy(() -> documentPermissionService.grant(editor, 50L, "bob", PermissionType.READ))
                .isInstanceOf(DocumentAccessDeniedException.class);
        verify(auditService).record(eq(editor), eq(AuditAction.ACCESS_DENIED), eq(document), eq(null),
                eq(AuditOutcome.DENIED), any());
        verify(documentPermissionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Owner can revoke a permission")
    void ownerCanRevoke() {
        DocumentPermission permission = new DocumentPermission(document, target, PermissionType.READ, owner);
        permission.setId(9L);
        when(documentRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(document));
        when(documentAccessService.canManagePermissions(owner, document)).thenReturn(true);
        when(documentPermissionRepository.findById(9L)).thenReturn(Optional.of(permission));

        documentPermissionService.revoke(owner, 50L, 9L);

        verify(documentPermissionRepository).delete(permission);
        verify(auditService).record(eq(owner), eq(AuditAction.PERMISSION_REVOKE), eq(document), eq(target),
                eq(AuditOutcome.SUCCESS), any());
    }

    @Test
    @DisplayName("Unauthorized user cannot revoke")
    void unauthorizedCannotRevoke() {
        when(documentRepository.findByIdAndDeletedFalse(50L)).thenReturn(Optional.of(document));
        when(documentAccessService.canManagePermissions(editor, document)).thenReturn(false);

        assertThatThrownBy(() -> documentPermissionService.revoke(editor, 50L, 9L))
                .isInstanceOf(DocumentAccessDeniedException.class);
        verify(documentPermissionRepository, never()).delete(any());
        verify(auditService).record(eq(editor), eq(AuditAction.ACCESS_DENIED), eq(document), eq(null),
                eq(AuditOutcome.DENIED), any());
    }
}
