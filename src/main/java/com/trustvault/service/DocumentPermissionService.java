package com.trustvault.service;

import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.entity.PermissionType;
import com.trustvault.entity.User;
import com.trustvault.exception.DocumentAccessDeniedException;
import com.trustvault.repository.DocumentPermissionRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class DocumentPermissionService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository documentPermissionRepository;
    private final UserRepository userRepository;
    private final DocumentAccessService documentAccessService;
    private final AuditService auditService;

    public DocumentPermissionService(DocumentRepository documentRepository,
                                     DocumentPermissionRepository documentPermissionRepository,
                                     UserRepository userRepository,
                                     DocumentAccessService documentAccessService,
                                     AuditService auditService) {
        this.documentRepository = documentRepository;
        this.documentPermissionRepository = documentPermissionRepository;
        this.userRepository = userRepository;
        this.documentAccessService = documentAccessService;
        this.auditService = auditService;
    }

    private Document requireManagedDocument(User actor, Long documentId, String deniedDetail) {
        Document document = documentRepository.findByIdAndDeletedFalse(documentId).orElse(null);
        if (document == null || !documentAccessService.canManagePermissions(actor, document)) {
            auditService.record(actor, AuditAction.ACCESS_DENIED, document, null,
                    AuditOutcome.DENIED, deniedDetail);
            throw new DocumentAccessDeniedException();
        }
        return document;
    }

    public List<DocumentPermission> listPermissions(User actor, Long documentId) {
        Document document = requireManagedDocument(actor, documentId, "Unauthorized permission list");
        return documentPermissionRepository.findByDocumentIdWithUsers(document.getId());
    }

    public List<DocumentPermission> listSharedWith(User user) {
        return documentPermissionRepository.findActiveSharedWithUser(user.getId());
    }

    @Transactional
    public DocumentPermission grant(User actor, Long documentId, String targetUsername, PermissionType type) {
        Document document = requireManagedDocument(actor, documentId, "Unauthorized permission grant");
        if (type == null) {
            throw new IllegalArgumentException("Permission type must be READ or EDIT.");
        }
        if (targetUsername == null || targetUsername.isBlank()) {
            throw new IllegalArgumentException("Target username is required.");
        }
        User target = userRepository.findByUsername(targetUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("Target user does not exist."));
        if (Objects.equals(target.getId(), document.getOwner().getId())) {
            throw new IllegalArgumentException("The document owner already has full access.");
        }

        DocumentPermission permission = documentPermissionRepository
                .findByDocumentIdAndUserId(documentId, target.getId())
                .orElseGet(() -> new DocumentPermission(document, target, type, actor));
        permission.setPermissionType(type);
        permission.setGrantedBy(actor);
        DocumentPermission saved = documentPermissionRepository.save(permission);

        auditService.record(actor, AuditAction.PERMISSION_GRANT, document, target,
                AuditOutcome.SUCCESS, "Granted " + type.name());
        return saved;
    }

    @Transactional
    public void revoke(User actor, Long documentId, Long permissionId) {
        Document document = requireManagedDocument(actor, documentId, "Unauthorized permission revoke");
        DocumentPermission permission = documentPermissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found."));
        if (!Objects.equals(permission.getDocument().getId(), documentId)) {
            auditService.record(actor, AuditAction.ACCESS_DENIED, document, null,
                    AuditOutcome.DENIED, "Permission does not belong to document");
            throw new DocumentAccessDeniedException();
        }
        User target = permission.getUser();
        documentPermissionRepository.delete(permission);
        auditService.record(actor, AuditAction.PERMISSION_REVOKE, document, target,
                AuditOutcome.SUCCESS, "Revoked " + permission.getPermissionType().name());
    }
}
