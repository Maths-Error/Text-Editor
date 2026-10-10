package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;

/**
 * Compact document metadata for dashboard/list responses.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentSummaryDto {
    private String documentId;
    private String title;
    private UserId ownerId;
    private DocumentRole role;
    private boolean canEdit;
    private boolean canShare;
    private String visibleTextPreview;
    private String createdAt;
    private String updatedAt;

    public DocumentSummaryDto() {
    }

    public DocumentSummaryDto(
            String documentId,
            String title,
            UserId ownerId,
            DocumentRole role,
            boolean canEdit,
            boolean canShare,
            String visibleTextPreview,
            String createdAt,
            String updatedAt
    ) {
        this.documentId = documentId;
        this.title = title;
        this.ownerId = ownerId;
        this.role = role;
        this.canEdit = canEdit;
        this.canShare = canShare;
        this.visibleTextPreview = visibleTextPreview;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public UserId getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UserId ownerId) {
        this.ownerId = ownerId;
    }

    public DocumentRole getRole() {
        return role;
    }

    public void setRole(DocumentRole role) {
        this.role = role;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanShare() {
        return canShare;
    }

    public void setCanShare(boolean canShare) {
        this.canShare = canShare;
    }

    public String getVisibleTextPreview() {
        return visibleTextPreview;
    }

    public void setVisibleTextPreview(String visibleTextPreview) {
        this.visibleTextPreview = visibleTextPreview;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
