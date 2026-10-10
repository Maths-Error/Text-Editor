package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;

/**
 * Response returned after a document is created.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateDocumentResponse {
    private String documentId;
    private String title;
    private UserId ownerId;
    private DocumentRole role;
    private boolean canEdit;
    private boolean canShare;
    private String editorCode;
    private String viewerCode;
    private CrdtSnapshotDto snapshot;
    private long snapshotVersion;
    private String createdAt;
    private String updatedAt;

    public CreateDocumentResponse() {
    }

    public CreateDocumentResponse(
            String documentId,
            String title,
            UserId ownerId,
            DocumentRole role,
            boolean canEdit,
            boolean canShare,
            String editorCode,
            String viewerCode,
            CrdtSnapshotDto snapshot,
            String createdAt,
            String updatedAt
    ) {
        this(
                documentId,
                title,
                ownerId,
                role,
                canEdit,
                canShare,
                editorCode,
                viewerCode,
                snapshot,
                0,
                createdAt,
                updatedAt
        );
    }

    public CreateDocumentResponse(
            String documentId,
            String title,
            UserId ownerId,
            DocumentRole role,
            boolean canEdit,
            boolean canShare,
            String editorCode,
            String viewerCode,
            CrdtSnapshotDto snapshot,
            long snapshotVersion,
            String createdAt,
            String updatedAt
    ) {
        this.documentId = documentId;
        this.title = title;
        this.ownerId = ownerId;
        this.role = role;
        this.canEdit = canEdit;
        this.canShare = canShare;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.snapshot = snapshot;
        this.snapshotVersion = snapshotVersion;
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

    public String getEditorCode() {
        return editorCode;
    }

    public void setEditorCode(String editorCode) {
        this.editorCode = editorCode;
    }

    public String getViewerCode() {
        return viewerCode;
    }

    public void setViewerCode(String viewerCode) {
        this.viewerCode = viewerCode;
    }

    public CrdtSnapshotDto getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(CrdtSnapshotDto snapshot) {
        this.snapshot = snapshot;
    }

    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
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
