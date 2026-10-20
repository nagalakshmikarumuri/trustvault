package com.trustvault.entity;

/**
 * Role
 *
 * Defines the user authorization roles supported across TrustVault.
 * Exactly supports:
 * - ADMIN: Administrative user with system-wide management capabilities.
 * - OWNER: Document owner with ownership and management rights over their documents.
 * - VIEWER: Read-only access to documents shared with them.
 */
public enum Role {
    ADMIN,
    OWNER,
    VIEWER
}
