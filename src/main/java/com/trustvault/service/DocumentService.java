package com.trustvault.service;

import com.trustvault.entity.Document;
import com.trustvault.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * DocumentService
 *
 * Core service layer for managing document operations in TrustVault.
 * Encapsulates business logic for querying active (non-deleted) documents
 * and delegating persistence calls to DocumentRepository.
 */
@Service
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    /**
     * Lists all active (non-deleted) documents belonging to a specific owner.
     *
     * @param ownerId the user ID of the document owner
     * @return a list of active documents owned by the user
     */
    public List<Document> getDocumentsByOwnerId(Long ownerId) {
        return documentRepository.findByOwnerIdAndDeletedFalse(ownerId);
    }

    /**
     * Finds an active (non-deleted) document by its unique ID.
     *
     * @param id the document ID
     * @return an Optional containing the active Document, or empty if not found or soft-deleted
     */
    public Optional<Document> findDocumentById(Long id) {
        return documentRepository.findByIdAndDeletedFalse(id);
    }

    /**
     * Retrieves an active (non-deleted) document by its unique ID, throwing NoSuchElementException
     * if the document does not exist or has been soft-deleted.
     *
     * @param id the document ID
     * @return the active Document entity
     * @throws NoSuchElementException if the document is not found or is soft-deleted
     */
    public Document getDocumentById(Long id) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Active document not found with ID: " + id));
    }

    /**
     * Persists a new Document record.
     *
     * <p>This method runs in a write transaction. The caller is responsible for
     * populating all required fields on the entity (name, storedFilename, owner,
     * contentType, fileSize) before invoking this method.
     *
     * @param document the fully-populated Document entity to save
     * @return the saved Document with its generated ID populated
     */
    @Transactional
    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    /**
     * Soft-deletes a document. Caller must have already authorized ownership.
     */
    @Transactional
    public Document softDelete(Document document) {
        document.setDeleted(true);
        return documentRepository.save(document);
    }

    /**
     * Updates allowed metadata only. Owner identity cannot be changed here.
     */
    @Transactional
    public Document updateMetadata(Document document, String name, String category) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Document name is required.");
        }
        String trimmedName = name.trim();
        if (trimmedName.length() > 255) {
            throw new IllegalArgumentException("Document name is too long.");
        }
        document.setName(trimmedName);
        if (category == null || category.isBlank()) {
            document.setCategory(null);
        } else {
            String trimmedCategory = category.trim();
            if (trimmedCategory.length() > 100) {
                throw new IllegalArgumentException("Category is too long.");
            }
            document.setCategory(trimmedCategory);
        }
        return documentRepository.save(document);
    }

    public List<Document> searchAuthorized(Long userId,
                                           String name,
                                           String ownerUsername,
                                           String category,
                                           LocalDate from,
                                           LocalDate to) {
        return documentRepository.searchAuthorized(
                userId,
                blankToNull(name),
                blankToNull(ownerUsername),
                blankToNull(category),
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
