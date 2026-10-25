package com.trustvault.repository;

import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            LEFT JOIN FETCH a.actor
            LEFT JOIN FETCH a.targetUser
            LEFT JOIN FETCH a.document
            WHERE (:action IS NULL OR a.action = :action)
              AND (:actorUsername IS NULL OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :actorUsername, '%')))
              AND (:fromTs IS NULL OR a.createdAt >= :fromTs)
              AND (:toTs IS NULL OR a.createdAt < :toTs)
            ORDER BY a.createdAt DESC
            """)
    List<AuditLog> search(
            @Param("action") AuditAction action,
            @Param("actorUsername") String actorUsername,
            @Param("fromTs") LocalDateTime fromTs,
            @Param("toTs") LocalDateTime toTs);
}
