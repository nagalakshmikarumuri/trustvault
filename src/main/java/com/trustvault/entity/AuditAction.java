package com.trustvault.entity;

/**
 * Audited operations recorded by TrustVault.
 */
public enum AuditAction {
    UPLOAD,
    DOWNLOAD,
    UPDATE,
    DELETE,
    PERMISSION_GRANT,
    PERMISSION_REVOKE,
    ACCESS_DENIED
}
