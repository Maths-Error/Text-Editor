package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;

/**
 * Request DTO that can represent opening by id or opening by share code.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenDocumentRequest {
    private String documentId;
    private UserId userId;
    private String displayName;
    private String shareCode;

    public OpenDocumentRequest() {
    }

    public OpenDocumentRequest(String documentId, UserId userId, String displayName, String shareCode) {
        this.documentId = documentId;
        this.userId = userId;
        this.displayName = displayName;
        this.shareCode = shareCode;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShareCode() {
        return shareCode;
    }

    public void setShareCode(String shareCode) {
        this.shareCode = shareCode;
    }
}
