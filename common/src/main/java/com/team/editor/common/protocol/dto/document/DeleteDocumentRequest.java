package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;

/**
 * Request body for deleting a document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteDocumentRequest {
    private UserId requesterUserId;

    public DeleteDocumentRequest() {
    }

    public DeleteDocumentRequest(UserId requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public UserId getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(UserId requesterUserId) {
        this.requesterUserId = requesterUserId;
    }
}
