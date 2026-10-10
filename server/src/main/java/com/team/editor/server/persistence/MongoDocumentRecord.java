package com.team.editor.server.persistence;

import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * Stored document model for Phase 3 MongoDB persistence.
 */
@Document(collection = "documents")
public class MongoDocumentRecord {
    @Id
    private String documentId;
    private String title;
    private UserId ownerId;
    private String editorCode;
    private String viewerCode;
    private CrdtSnapshotDto snapshot;
    private long snapshotVersion;
    private String createdAt;
    private String updatedAt;
    private boolean deleted;
    private Set<String> editorUserIds;
    private Set<String> viewerUserIds;

    public MongoDocumentRecord() {
        this.editorUserIds = new HashSet<>();
        this.viewerUserIds = new HashSet<>();
    }

    public MongoDocumentRecord(
            String documentId,
            String title,
            UserId ownerId,
            String editorCode,
            String viewerCode,
            CrdtSnapshotDto snapshot,
            String createdAt,
            String updatedAt,
            boolean deleted
    ) {
        this(documentId, title, ownerId, editorCode, viewerCode, snapshot, createdAt, updatedAt, deleted, 0, null, null);
    }

    public MongoDocumentRecord(
            String documentId,
            String title,
            UserId ownerId,
            String editorCode,
            String viewerCode,
            CrdtSnapshotDto snapshot,
            String createdAt,
            String updatedAt,
            boolean deleted,
            Set<String> editorUserIds,
            Set<String> viewerUserIds
    ) {
        this(
                documentId,
                title,
                ownerId,
                editorCode,
                viewerCode,
                snapshot,
                createdAt,
                updatedAt,
                deleted,
                0,
                editorUserIds,
                viewerUserIds
        );
    }

    public MongoDocumentRecord(
            String documentId,
            String title,
            UserId ownerId,
            String editorCode,
            String viewerCode,
            CrdtSnapshotDto snapshot,
            String createdAt,
            String updatedAt,
            boolean deleted,
            long snapshotVersion,
            Set<String> editorUserIds,
            Set<String> viewerUserIds
    ) {
        this.documentId = documentId;
        this.title = title;
        this.ownerId = ownerId;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.snapshot = snapshot;
        this.snapshotVersion = snapshotVersion;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
        this.editorUserIds = editorUserIds == null ? new HashSet<>() : editorUserIds;
        this.viewerUserIds = viewerUserIds == null ? new HashSet<>() : viewerUserIds;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Set<String> getEditorUserIds() {
        if (editorUserIds == null) {
            editorUserIds = new HashSet<>();
        }

        return editorUserIds;
    }

    public void setEditorUserIds(Set<String> editorUserIds) {
        this.editorUserIds = editorUserIds == null ? new HashSet<>() : editorUserIds;
    }

    public Set<String> getViewerUserIds() {
        if (viewerUserIds == null) {
            viewerUserIds = new HashSet<>();
        }

        return viewerUserIds;
    }

    public void setViewerUserIds(Set<String> viewerUserIds) {
        this.viewerUserIds = viewerUserIds == null ? new HashSet<>() : viewerUserIds;
    }

    public void addEditorUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        getEditorUserIds().add(userId);
        getViewerUserIds().remove(userId);
    }

    public void addViewerUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        getViewerUserIds().add(userId);
        getEditorUserIds().remove(userId);
    }
}
