-- ===================================================================
-- TrustVault Database Migration V2
-- Description: Create documents table for document metadata
-- ===================================================================

CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    owner_id BIGINT NOT NULL,
    category VARCHAR(100),
    content_type VARCHAR(100),
    file_size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    INDEX idx_documents_owner_id (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
