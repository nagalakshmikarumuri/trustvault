package com.trustvault.dto;

import com.trustvault.entity.Document;

/**
 * View model for document rows with server-computed authorization flags.
 */
public class DocumentListItem {

    private final Document document;
    private final boolean owner;
    private final boolean canDownload;
    private final boolean canEdit;
    private final boolean canDelete;
    private final boolean canManagePermissions;
    private final String permissionLabel;

    public DocumentListItem(Document document,
                            boolean owner,
                            boolean canDownload,
                            boolean canEdit,
                            boolean canDelete,
                            boolean canManagePermissions,
                            String permissionLabel) {
        this.document = document;
        this.owner = owner;
        this.canDownload = canDownload;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
        this.canManagePermissions = canManagePermissions;
        this.permissionLabel = permissionLabel;
    }

    public Document getDocument() {
        return document;
    }

    public boolean isOwner() {
        return owner;
    }

    public boolean isCanDownload() {
        return canDownload;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public boolean isCanManagePermissions() {
        return canManagePermissions;
    }

    public String getPermissionLabel() {
        return permissionLabel;
    }
}
