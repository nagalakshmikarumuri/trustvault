package com.trustvault.repository;

import com.trustvault.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DocumentRepository
 *
 * Spring Data JPA repository for the Document entity.
 * Provides persistence and query operations for document metadata.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Retrieves all non-deleted documents owned by a specific user.
     *
     * @param ownerId the user ID of the document owner
     * @return a list of active documents belonging to the specified user
     */
    List<Document> findByOwnerIdAndDeletedFalse(Long ownerId);

    /**
     * Retrieves an active (non-deleted) document by its unique ID.
     *
     * @param id the document ID
     * @return an Optional containing the Document if found and not soft-deleted, or empty otherwise
     */
    Optional<Document> findByIdAndDeletedFalse(Long id);
}
