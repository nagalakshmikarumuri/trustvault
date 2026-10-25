package com.trustvault.controller;

import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.AuditService;
import com.trustvault.service.DocumentAccessService;
import com.trustvault.service.DocumentPermissionService;
import com.trustvault.service.DocumentService;
import com.trustvault.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentOperationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private DocumentService documentService;
    @MockBean
    private DocumentStorageService documentStorageService;
    @MockBean
    private DocumentAccessService documentAccessService;
    @MockBean
    private DocumentPermissionService documentPermissionService;
    @MockBean
    private AuditService auditService;

    private User owner;
    private User reader;
    private User editor;
    private User stranger;
    private User admin;
    private Document document;

    @BeforeEach
    void setUp() {
        owner = user("alice", Role.OWNER, 1L);
        reader = user("bob", Role.VIEWER, 2L);
        editor = user("carol", Role.VIEWER, 3L);
        stranger = user("dave", Role.OWNER, 4L);
        admin = user("admin", Role.ADMIN, 5L);
        document = new Document("Report.pdf", "uuid-report.pdf", owner, "Finance", "application/pdf", 2048L);
        document.setId(10L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(reader));
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(editor));
        when(userRepository.findByUsername("dave")).thenReturn(Optional.of(stranger));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(documentService.findDocumentById(10L)).thenReturn(Optional.of(document));
    }

    @Test
    @DisplayName("Owner can download own document")
    @WithMockUser(username = "alice")
    void ownerCanDownloadOwnDocument() throws Exception {
        when(documentAccessService.canDownload(owner, document)).thenReturn(true);
        when(documentStorageService.loadAsResource("uuid-report.pdf"))
                .thenReturn(new ByteArrayResource("pdf-bytes".getBytes()));

        mockMvc.perform(get("/documents/10/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("Report.pdf")))
                .andExpect(content().bytes("pdf-bytes".getBytes()));

        verify(documentStorageService).loadAsResource("uuid-report.pdf");
        verify(auditService).record(eq(owner), eq(AuditAction.DOWNLOAD), eq(document), isNull(),
                eq(AuditOutcome.SUCCESS), any());
    }

    @Test
    @DisplayName("Unrelated user cannot download another user's document")
    @WithMockUser(username = "dave")
    void unrelatedUserCannotDownload() throws Exception {
        when(documentAccessService.canDownload(stranger, document)).thenReturn(false);

        mockMvc.perform(get("/documents/10/download"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("uuid-report.pdf"))));

        verify(documentStorageService, never()).loadAsResource(any());
        verify(auditService).record(eq(stranger), eq(AuditAction.ACCESS_DENIED), eq(document), isNull(),
                eq(AuditOutcome.DENIED), any());
    }

    @Test
    @DisplayName("READ user can download")
    @WithMockUser(username = "bob")
    void readUserCanDownload() throws Exception {
        when(documentAccessService.canDownload(reader, document)).thenReturn(true);
        when(documentStorageService.loadAsResource("uuid-report.pdf"))
                .thenReturn(new ByteArrayResource("pdf-bytes".getBytes()));

        mockMvc.perform(get("/documents/10/download"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("READ user cannot edit")
    @WithMockUser(username = "bob")
    void readUserCannotEdit() throws Exception {
        when(documentAccessService.canEdit(reader, document)).thenReturn(false);

        mockMvc.perform(post("/documents/10/edit")
                        .param("name", "Hacked.pdf")
                        .param("category", "Nope")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        verify(documentService, never()).updateMetadata(any(), any(), any());
        verify(auditService).record(eq(reader), eq(AuditAction.ACCESS_DENIED), eq(document), isNull(),
                eq(AuditOutcome.DENIED), any());
    }

    @Test
    @DisplayName("EDIT user can edit metadata")
    @WithMockUser(username = "carol")
    void editUserCanEdit() throws Exception {
        when(documentAccessService.canEdit(editor, document)).thenReturn(true);
        when(documentService.updateMetadata(document, "Updated.pdf", "Legal")).thenReturn(document);

        mockMvc.perform(post("/documents/10/edit")
                        .param("name", "Updated.pdf")
                        .param("category", "Legal")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        verify(documentService).updateMetadata(document, "Updated.pdf", "Legal");
        verify(auditService).record(eq(editor), eq(AuditAction.UPDATE), eq(document), isNull(),
                eq(AuditOutcome.SUCCESS), any());
    }

    @Test
    @DisplayName("EDIT user cannot manage permissions")
    @WithMockUser(username = "carol")
    void editUserCannotManagePermissions() throws Exception {
        when(documentPermissionService.listPermissions(eq(editor), eq(10L)))
                .thenThrow(new com.trustvault.exception.DocumentAccessDeniedException());

        mockMvc.perform(get("/documents/10/permissions"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Owner can grant permission")
    @WithMockUser(username = "alice")
    void ownerCanGrantPermission() throws Exception {
        mockMvc.perform(post("/documents/10/permissions/grant")
                        .param("username", "bob")
                        .param("permissionType", "READ")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents/10/permissions"));

        verify(documentPermissionService).grant(owner, 10L, "bob", com.trustvault.entity.PermissionType.READ);
    }

    @Test
    @DisplayName("Owner can revoke permission")
    @WithMockUser(username = "alice")
    void ownerCanRevokePermission() throws Exception {
        mockMvc.perform(post("/documents/10/permissions/9/revoke").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents/10/permissions"));

        verify(documentPermissionService).revoke(owner, 10L, 9L);
    }

    @Test
    @DisplayName("Unauthorized user cannot grant permission")
    @WithMockUser(username = "dave")
    void unauthorizedCannotGrant() throws Exception {
        when(documentPermissionService.grant(eq(stranger), eq(10L), eq("bob"), any()))
                .thenThrow(new com.trustvault.exception.DocumentAccessDeniedException());

        mockMvc.perform(post("/documents/10/permissions/grant")
                        .param("username", "bob")
                        .param("permissionType", "READ")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));
    }

    @Test
    @DisplayName("Unauthorized user cannot delete")
    @WithMockUser(username = "dave")
    void unauthorizedCannotDelete() throws Exception {
        when(documentAccessService.canDelete(stranger, document)).thenReturn(false);

        mockMvc.perform(post("/documents/10/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        verify(documentService, never()).softDelete(any());
        verify(auditService).record(eq(stranger), eq(AuditAction.ACCESS_DENIED), eq(document), isNull(),
                eq(AuditOutcome.DENIED), any());
    }

    @Test
    @DisplayName("Search does not expose another user's documents")
    @WithMockUser(username = "dave")
    void searchDoesNotExposeOtherDocuments() throws Exception {
        when(documentService.searchAuthorized(eq(4L), any(), any(), any(), any(), any())).thenReturn(List.of());
        when(documentPermissionService.listSharedWith(any())).thenReturn(List.of());

        mockMvc.perform(get("/documents/search").param("name", "Report"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Report.pdf"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("uuid-report"))));
    }

    @Test
    @DisplayName("Admin can access audit view")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAccessAuditView() throws Exception {
        AuditLog log = new AuditLog();
        log.setActorUsername("alice");
        log.setAction(AuditAction.UPLOAD);
        log.setOutcome(AuditOutcome.SUCCESS);
        log.setCreatedAt(java.time.LocalDateTime.now());
        when(auditService.search(any(), any(), any(), any())).thenReturn(List.of(log));

        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isOk())
                .andExpect(view().name("audit"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Audit activity")));
    }

    @Test
    @DisplayName("Non-admin cannot access audit view")
    @WithMockUser(username = "alice", roles = "OWNER")
    void nonAdminCannotAccessAuditView() throws Exception {
        mockMvc.perform(get("/admin/audit"))
                .andExpect(status().isForbidden());
    }

    private static User user(String username, Role role, Long id) {
        User user = new User(username, "hash", role, true);
        user.setId(id);
        return user;
    }
}
