package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response returned after a document is deleted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteDocumentResponse {
    private String documentId;
    private boolean deleted;

    public DeleteDocumentResponse() {
    }

    public DeleteDocumentResponse(String documentId, boolean deleted) {
        this.documentId = documentId;
        this.deleted = deleted;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
