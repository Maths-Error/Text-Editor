package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;

/**
 * Request body for renaming a document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RenameDocumentRequest {
    private UserId requesterUserId;
    private String title;

    public RenameDocumentRequest() {
    }

    public RenameDocumentRequest(UserId requesterUserId, String title) {
        this.requesterUserId = requesterUserId;
        this.title = title;
    }

    public UserId getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(UserId requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
