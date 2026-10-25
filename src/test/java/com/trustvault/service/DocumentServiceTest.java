package com.trustvault.service;

import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import com.trustvault.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DocumentServiceTest
 *
 * Focused unit tests for DocumentService using a mocked DocumentRepository.
 * Verifies document listing by owner and active document retrieval by ID.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    private User sampleOwner;
    private Document sampleDoc1;
    private Document sampleDoc2;

    @BeforeEach
    void setUp() {
        sampleOwner = new User("owner_user", "hashed_pwd", Role.OWNER, true);
        sampleOwner.setId(101L);

        sampleDoc1 = new Document("Resume.pdf", "uuid_resume.pdf", sampleOwner, "Personal", "application/pdf", 2048L);
        sampleDoc1.setId(1L);

        sampleDoc2 = new Document("Tax.pdf", "uuid_tax.pdf", sampleOwner, "Finance", "application/pdf", 4096L);
        sampleDoc2.setId(2L);
    }

    @Test
    @DisplayName("getDocumentsByOwnerId should query repository and return active documents for owner")
    void shouldListDocumentsByOwnerId() {
        when(documentRepository.findByOwnerIdAndDeletedFalse(101L))
                .thenReturn(List.of(sampleDoc1, sampleDoc2));

        List<Document> documents = documentService.getDocumentsByOwnerId(101L);

        assertThat(documents).hasSize(2).containsExactly(sampleDoc1, sampleDoc2);
        verify(documentRepository).findByOwnerIdAndDeletedFalse(101L);
    }

    @Test
    @DisplayName("getDocumentsByOwnerId should return empty list when owner has no active documents")
    void shouldReturnEmptyListWhenNoDocumentsFound() {
        when(documentRepository.findByOwnerIdAndDeletedFalse(999L))
                .thenReturn(List.of());

        List<Document> documents = documentService.getDocumentsByOwnerId(999L);

        assertThat(documents).isEmpty();
        verify(documentRepository).findByOwnerIdAndDeletedFalse(999L);
    }

    @Test
    @DisplayName("findDocumentById should return Optional containing Document when active")
    void shouldFindDocumentByIdWhenActive() {
        when(documentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleDoc1));

        Optional<Document> result = documentService.findDocumentById(1L);

        assertThat(result).isPresent().contains(sampleDoc1);
        verify(documentRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("findDocumentById should return empty Optional when document is not found or soft-deleted")
    void shouldReturnEmptyOptionalWhenNotFoundOrDeleted() {
        when(documentRepository.findByIdAndDeletedFalse(999L))
                .thenReturn(Optional.empty());

        Optional<Document> result = documentService.findDocumentById(999L);

        assertThat(result).isEmpty();
        verify(documentRepository).findByIdAndDeletedFalse(999L);
    }

    @Test
    @DisplayName("getDocumentById should return Document when found")
    void shouldGetDocumentByIdWhenFound() {
        when(documentRepository.findByIdAndDeletedFalse(1L))
                .thenReturn(Optional.of(sampleDoc1));

        Document doc = documentService.getDocumentById(1L);

        assertThat(doc).isNotNull().isEqualTo(sampleDoc1);
        verify(documentRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("getDocumentById should throw NoSuchElementException when document not found or soft-deleted")
    void shouldThrowExceptionWhenDocumentNotFound() {
        when(documentRepository.findByIdAndDeletedFalse(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Active document not found with ID: 999");

        verify(documentRepository).findByIdAndDeletedFalse(999L);
    }

    @Test
    @DisplayName("updateMetadata changes name and category but not owner")
    void updateMetadataDoesNotChangeOwner() {
        when(documentRepository.save(sampleDoc1)).thenReturn(sampleDoc1);

        Document updated = documentService.updateMetadata(sampleDoc1, "New.pdf", "Tax");

        assertThat(updated.getName()).isEqualTo("New.pdf");
        assertThat(updated.getCategory()).isEqualTo("Tax");
        assertThat(updated.getOwner().getId()).isEqualTo(101L);
        verify(documentRepository).save(sampleDoc1);
    }

    @Test
    @DisplayName("softDelete marks the document deleted")
    void softDeleteSetsFlag() {
        when(documentRepository.save(sampleDoc1)).thenReturn(sampleDoc1);

        Document deleted = documentService.softDelete(sampleDoc1);

        assertThat(deleted.isDeleted()).isTrue();
        verify(documentRepository).save(sampleDoc1);
    }

    @Test
    @DisplayName("searchAuthorized delegates to repository with the current user id")
    void searchAuthorizedUsesCurrentUser() {
        when(documentRepository.searchAuthorized(eq(101L), eq("Resume"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleDoc1));

        List<Document> results = documentService.searchAuthorized(101L, "Resume", null, null, null, null);

        assertThat(results).containsExactly(sampleDoc1);
        verify(documentRepository).searchAuthorized(eq(101L), eq("Resume"), isNull(), isNull(), isNull(), isNull());
    }
}
