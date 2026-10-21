package com.trustvault.controller;

import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.DocumentService;
import com.trustvault.service.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DocumentControllerTest
 *
 * Integration-style MockMvc tests for DocumentController.
 * Verifies:
 * - Access control (unauthenticated requests are redirected to login).
 * - GET /documents lists only the authenticated owner's documents.
 * - POST /documents/upload stores a file and saves metadata for the correct owner.
 * - Upload validation errors surface as flash messages without saving anything.
 * - The owner is resolved exclusively from the Spring Security context,
 *   not from any request parameter.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private DocumentStorageService documentStorageService;

    private User testUser;
    private Document doc1;
    private Document doc2;

    @BeforeEach
    void setUp() {
        testUser = new User("alice", "$2a$10$someHashedPassword", Role.OWNER, true);
        testUser.setId(1L);

        doc1 = new Document("Report.pdf", "uuid-report.pdf", testUser, "Finance", "application/pdf", 2048L);
        doc1.setId(10L);
        doc2 = new Document("Contract.docx", "uuid-contract.docx", testUser, "Legal", "application/msword", 4096L);
        doc2.setId(11L);
    }

    // ─── Access control ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /documents without authentication should redirect to login")
    void unauthenticatedGetShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("POST /documents/upload without authentication should redirect to login")
    void unauthenticatedPostShouldRedirectToLogin() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/documents/upload").file(file).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // ─── GET /documents ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /documents should render documents view with owner's documents")
    @WithMockUser(username = "alice")
    void shouldListOwnerDocumentsForAuthenticatedUser() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(documentService.getDocumentsByOwnerId(1L)).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attribute("username", "alice"))
                .andExpect(model().attributeExists("documents"));

        verify(documentService).getDocumentsByOwnerId(1L);
    }

    @Test
    @DisplayName("GET /documents should show empty list when user has no documents")
    @WithMockUser(username = "alice")
    void shouldShowEmptyListWhenNoDocuments() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(documentService.getDocumentsByOwnerId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(view().name("documents"))
                .andExpect(model().attribute("documents", List.of()));
    }

    // ─── POST /documents/upload ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /documents/upload with valid file should store and save document, redirect with success")
    @WithMockUser(username = "alice")
    void shouldUploadValidFileAndRedirectWithSuccessMessage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf",
                "application/pdf", "PDF data".getBytes());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(documentStorageService.store(any())).thenReturn("uuid-stored.pdf");
        when(documentService.saveDocument(any(Document.class))).thenReturn(doc1);

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .param("category", "Finance")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify the storage service was called
        verify(documentStorageService).store(any());
        // Verify a document was persisted
        verify(documentService).saveDocument(any(Document.class));
    }

    @Test
    @DisplayName("POST /documents/upload with empty file should redirect with error, nothing saved")
    @WithMockUser(username = "alice")
    void shouldRejectEmptyFileWithErrorMessage() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf",
                "application/pdf", new byte[0]);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(documentStorageService.store(any()))
                .thenThrow(new IllegalArgumentException("Upload rejected: file is empty or missing."));

        mockMvc.perform(multipart("/documents/upload")
                        .file(emptyFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Document must NOT have been saved to the database.
        verify(documentService, never()).saveDocument(any());
    }

    @Test
    @DisplayName("POST /documents/upload with unsafe filename should redirect with error, nothing saved")
    @WithMockUser(username = "alice")
    void shouldRejectUnsafeFilenameWithErrorMessage() throws Exception {
        MockMultipartFile dangerousFile = new MockMultipartFile("file", "../etc/passwd",
                "text/plain", "evil".getBytes());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(documentStorageService.store(any()))
                .thenThrow(new IllegalArgumentException("Upload rejected: filename contains unsafe characters."));

        mockMvc.perform(multipart("/documents/upload")
                        .file(dangerousFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(documentService, never()).saveDocument(any());
    }

    @Test
    @DisplayName("POST /documents/upload should assign authenticated user as owner (never from request)")
    @WithMockUser(username = "alice")
    void shouldAlwaysSetOwnerFromSecurityContextNotFromRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf",
                "application/pdf", "data".getBytes());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(documentStorageService.store(any())).thenReturn("uuid.pdf");

        // Capture the document passed to saveDocument
        when(documentService.saveDocument(any(Document.class))).thenAnswer(invocation -> {
            Document saved = invocation.getArgument(0);
            // Assert the owner ID comes from the authenticated user (alice, id=1), NOT from any request param.
            org.assertj.core.api.Assertions.assertThat(saved.getOwner().getId()).isEqualTo(1L);
            org.assertj.core.api.Assertions.assertThat(saved.getOwner().getUsername()).isEqualTo("alice");
            return saved;
        });

        mockMvc.perform(multipart("/documents/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/documents"));

        verify(documentService).saveDocument(any(Document.class));
    }

    @Test
    @DisplayName("POST /documents/upload without CSRF token should return 403 Forbidden")
    @WithMockUser(username = "alice")
    void uploadWithoutCsrfShouldBeForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf",
                "application/pdf", "data".getBytes());

        // No .with(csrf()) — Spring Security should reject with 403.
        mockMvc.perform(multipart("/documents/upload").file(file))
                .andExpect(status().isForbidden());

        verify(documentService, never()).saveDocument(any());
    }
}
