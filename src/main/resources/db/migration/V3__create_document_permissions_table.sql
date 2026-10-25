-- ===================================================================
-- TrustVault Database Migration V3
-- Description: Document-level READ/EDIT permissions
-- ===================================================================

CREATE TABLE document_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    permission_type VARCHAR(20) NOT NULL,
    granted_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_dp_document FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_dp_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_dp_granted_by FOREIGN KEY (granted_by) REFERENCES users (id),
    CONSTRAINT uk_dp_document_user UNIQUE (document_id, user_id),
    CONSTRAINT chk_dp_permission_type CHECK (permission_type IN ('READ', 'EDIT')),
    INDEX idx_dp_document_id (document_id),
    INDEX idx_dp_user_id (user_id),
    INDEX idx_dp_granted_by (granted_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
