package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;

/**
 * Request body for creating a persistent document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateDocumentRequest {
    private UserId ownerId;
    private String title;
    private CrdtSnapshotDto initialSnapshot;

    public CreateDocumentRequest() {
    }

    public CreateDocumentRequest(UserId ownerId, String title, CrdtSnapshotDto initialSnapshot) {
        this.ownerId = ownerId;
        this.title = title;
        this.initialSnapshot = initialSnapshot;
    }

    public UserId getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UserId ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public CrdtSnapshotDto getInitialSnapshot() {
        return initialSnapshot;
    }

    public void setInitialSnapshot(CrdtSnapshotDto initialSnapshot) {
        this.initialSnapshot = initialSnapshot;
    }
}
