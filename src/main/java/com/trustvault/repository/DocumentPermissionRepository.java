package com.trustvault.repository;

import com.trustvault.entity.DocumentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    Optional<DocumentPermission> findByDocumentIdAndUserId(Long documentId, Long userId);

    @Query("""
            SELECT p FROM DocumentPermission p
            JOIN FETCH p.user
            JOIN FETCH p.grantedBy
            WHERE p.document.id = :documentId
            ORDER BY p.createdAt ASC
            """)
    List<DocumentPermission> findByDocumentIdWithUsers(@Param("documentId") Long documentId);

    @Query("""
            SELECT p FROM DocumentPermission p
            JOIN FETCH p.document d
            JOIN FETCH d.owner
            WHERE p.user.id = :userId AND d.deleted = false
            ORDER BY d.createdAt DESC
            """)
    List<DocumentPermission> findActiveSharedWithUser(@Param("userId") Long userId);
}
