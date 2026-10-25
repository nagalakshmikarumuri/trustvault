package com.trustvault.repository;

import com.trustvault.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    @Query("SELECT d FROM Document d JOIN FETCH d.owner WHERE d.id = :id AND d.deleted = false")
    Optional<Document> findByIdAndDeletedFalse(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT d FROM Document d
            JOIN FETCH d.owner o
            WHERE d.deleted = false
              AND (o.id = :userId OR EXISTS (
                    SELECT 1 FROM DocumentPermission p
                    WHERE p.document = d AND p.user.id = :userId
              ))
              AND (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:ownerUsername IS NULL OR LOWER(o.username) LIKE LOWER(CONCAT('%', :ownerUsername, '%')))
              AND (:category IS NULL OR LOWER(d.category) LIKE LOWER(CONCAT('%', :category, '%')))
              AND (:fromTs IS NULL OR d.createdAt >= :fromTs)
              AND (:toTs IS NULL OR d.createdAt < :toTs)
            ORDER BY d.createdAt DESC
            """)
    List<Document> searchAuthorized(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("ownerUsername") String ownerUsername,
            @Param("category") String category,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);
}
