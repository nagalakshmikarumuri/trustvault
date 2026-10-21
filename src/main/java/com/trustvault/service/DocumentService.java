package com.trustvault.service;

import com.trustvault.entity.Document;
import com.trustvault.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
