package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response returned after a document title is changed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RenameDocumentResponse {
    private String documentId;
    private String title;
    private String updatedAt;

    public RenameDocumentResponse() {
    }

    public RenameDocumentResponse(String documentId, String title, String updatedAt) {
        this.documentId = documentId;
        this.title = title;
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

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
