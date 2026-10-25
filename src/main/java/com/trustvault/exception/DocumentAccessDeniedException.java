package com.trustvault.exception;

/**
 * Raised when a user must not learn whether a document exists.
 * Handled as a generic not-found response.
 */
public class DocumentAccessDeniedException extends RuntimeException {

    public DocumentAccessDeniedException() {
        super("Document not found.");
    }
}
