package com.trustvault.service;

import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.entity.PermissionType;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.DocumentPermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAccessServiceTest {

    @Mock
    private DocumentPermissionRepository documentPermissionRepository;

    @InjectMocks
    private DocumentAccessService documentAccessService;

    private User owner;
    private User reader;
    private User editor;
    private User stranger;
    private Document document;

    @BeforeEach
    void setUp() {
        owner = new User("owner", "hash", Role.OWNER, true);
        owner.setId(1L);
        reader = new User("reader", "hash", Role.VIEWER, true);
        reader.setId(2L);
        editor = new User("editor", "hash", Role.VIEWER, true);
        editor.setId(3L);
        stranger = new User("stranger", "hash", Role.OWNER, true);
        stranger.setId(4L);
        document = new Document("a.pdf", "uuid.pdf", owner, "Legal", "application/pdf", 10L);
        document.setId(50L);
    }

    @Test
    @DisplayName("Owner can download, edit, delete, and manage permissions")
    void ownerHasFullAccess() {
        assertThat(documentAccessService.canDownload(owner, document)).isTrue();
        assertThat(documentAccessService.canEdit(owner, document)).isTrue();
        assertThat(documentAccessService.canDelete(owner, document)).isTrue();
        assertThat(documentAccessService.canManagePermissions(owner, document)).isTrue();
    }

    @Test
    @DisplayName("READ user can download but cannot edit, delete, or manage permissions")
    void readUserCannotEditOrDelete() {
        when(documentPermissionRepository.findByDocumentIdAndUserId(50L, 2L))
                .thenReturn(Optional.of(new DocumentPermission(document, reader, PermissionType.READ, owner)));

        assertThat(documentAccessService.canDownload(reader, document)).isTrue();
        assertThat(documentAccessService.canEdit(reader, document)).isFalse();
        assertThat(documentAccessService.canDelete(reader, document)).isFalse();
        assertThat(documentAccessService.canManagePermissions(reader, document)).isFalse();
    }

    @Test
    @DisplayName("EDIT user can download and edit but cannot delete or manage permissions")
    void editUserCannotManagePermissions() {
        when(documentPermissionRepository.findByDocumentIdAndUserId(50L, 3L))
                .thenReturn(Optional.of(new DocumentPermission(document, editor, PermissionType.EDIT, owner)));

        assertThat(documentAccessService.canDownload(editor, document)).isTrue();
        assertThat(documentAccessService.canEdit(editor, document)).isTrue();
        assertThat(documentAccessService.canDelete(editor, document)).isFalse();
        assertThat(documentAccessService.canManagePermissions(editor, document)).isFalse();
    }

    @Test
    @DisplayName("Unrelated user has no document access")
    void strangerHasNoAccess() {
        when(documentPermissionRepository.findByDocumentIdAndUserId(50L, 4L)).thenReturn(Optional.empty());

        assertThat(documentAccessService.canDownload(stranger, document)).isFalse();
        assertThat(documentAccessService.canEdit(stranger, document)).isFalse();
        assertThat(documentAccessService.canDelete(stranger, document)).isFalse();
    }
}
