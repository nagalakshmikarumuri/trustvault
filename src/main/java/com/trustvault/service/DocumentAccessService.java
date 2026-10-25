package com.trustvault.service;

import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.entity.PermissionType;
import com.trustvault.entity.User;
import com.trustvault.repository.DocumentPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * Server-side authorization for document operations.
 * Ownership and grants are never taken from the client.
 */
@Service
@Transactional(readOnly = true)
public class DocumentAccessService {

    private final DocumentPermissionRepository documentPermissionRepository;

    public DocumentAccessService(DocumentPermissionRepository documentPermissionRepository) {
        this.documentPermissionRepository = documentPermissionRepository;
    }

    public boolean isOwner(User user, Document document) {
        return user != null && document != null && document.getOwner() != null
                && Objects.equals(document.getOwner().getId(), user.getId());
    }

    public boolean canDownload(User user, Document document) {
        return isOwner(user, document) || hasAtLeast(user, document, PermissionType.READ);
    }

    public boolean canEdit(User user, Document document) {
        return isOwner(user, document) || hasAtLeast(user, document, PermissionType.EDIT);
    }

    public boolean canDelete(User user, Document document) {
        return isOwner(user, document);
    }

    public boolean canManagePermissions(User user, Document document) {
        return isOwner(user, document);
    }

    public Optional<DocumentPermission> findGrant(User user, Document document) {
        if (user == null || document == null || document.getId() == null || user.getId() == null) {
            return Optional.empty();
        }
        return documentPermissionRepository.findByDocumentIdAndUserId(document.getId(), user.getId());
    }

    public boolean hasAtLeast(User user, Document document, PermissionType required) {
        if (isOwner(user, document)) {
            return true;
        }
        Optional<DocumentPermission> grant = findGrant(user, document);
        if (grant.isEmpty()) {
            return false;
        }
        PermissionType actual = grant.get().getPermissionType();
        if (required == PermissionType.READ) {
            return actual == PermissionType.READ || actual == PermissionType.EDIT;
        }
        return actual == PermissionType.EDIT;
    }
}
