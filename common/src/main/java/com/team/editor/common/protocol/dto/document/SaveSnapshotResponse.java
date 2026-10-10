package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response returned after a document snapshot is saved.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveSnapshotResponse {
    private String documentId;
    private long snapshotVersion;
    private String updatedAt;

    public SaveSnapshotResponse() {
    }

    public SaveSnapshotResponse(String documentId, long snapshotVersion, String updatedAt) {
        this.documentId = documentId;
        this.snapshotVersion = snapshotVersion;
        this.updatedAt = updatedAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
