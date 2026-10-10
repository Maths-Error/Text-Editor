package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;

/**
 * Request body for saving the latest CRDT snapshot for a document.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveSnapshotRequest {
    private UserId requesterUserId;
    private long baseSnapshotVersion;
    private CrdtSnapshotDto snapshot;

    public SaveSnapshotRequest() {
    }

    public SaveSnapshotRequest(UserId requesterUserId, long baseSnapshotVersion, CrdtSnapshotDto snapshot) {
        this.requesterUserId = requesterUserId;
        this.baseSnapshotVersion = baseSnapshotVersion;
        this.snapshot = snapshot;
    }

    public UserId getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(UserId requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public long getBaseSnapshotVersion() {
        return baseSnapshotVersion;
    }

    public void setBaseSnapshotVersion(long baseSnapshotVersion) {
        this.baseSnapshotVersion = baseSnapshotVersion;
    }

    public CrdtSnapshotDto getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(CrdtSnapshotDto snapshot) {
        this.snapshot = snapshot;
    }
}
