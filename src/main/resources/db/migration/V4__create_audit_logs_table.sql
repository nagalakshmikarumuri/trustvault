-- ===================================================================
-- TrustVault Database Migration V4
-- Description: Immutable application audit log
-- ===================================================================

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    actor_username VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    document_id BIGINT NULL,
    target_user_id BIGINT NULL,
    target_username VARCHAR(50) NULL,
    outcome VARCHAR(20) NOT NULL,
    details VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT fk_audit_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_audit_target_user FOREIGN KEY (target_user_id) REFERENCES users (id),
    CONSTRAINT chk_audit_outcome CHECK (outcome IN ('SUCCESS', 'DENIED', 'FAILURE')),
    INDEX idx_audit_actor_id (actor_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_document_id (document_id),
    INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
