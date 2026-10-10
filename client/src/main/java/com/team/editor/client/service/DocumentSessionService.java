package com.team.editor.client.service;

import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;

public class DocumentSessionService {
    private String documentId;
    private String title;
    private String role;
    private boolean canEdit;
    private boolean canShare;
    private long snapshotVersion;
    private CrdtSnapshotDto snapshot;
    private String editorCode;
    private String viewerCode;

    public void loadFromCreateResponse(CreateDocumentResponse response) {
        if (response == null) {
            clear();
            return;
        }

        load(
                response.getDocumentId(),
                response.getTitle(),
                response.getRole(),
                response.isCanEdit(),
                response.isCanShare(),
                response.getSnapshotVersion(),
                response.getSnapshot(),
                response.getEditorCode(),
                response.getViewerCode()
        );
    }

    public void loadFromOpenResponse(OpenDocumentResponse response) {
        if (response == null) {
            clear();
            return;
        }

        load(
                response.getDocumentId(),
                response.getTitle(),
                response.getRole(),
                response.isCanEdit(),
                response.isCanShare(),
                response.getSnapshotVersion(),
                response.getSnapshot(),
                response.getEditorCode(),
                response.getViewerCode()
        );
    }

    public void updateSnapshotVersion(long snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public void updateSnapshot(CrdtSnapshotDto snapshot, long snapshotVersion) {
        this.snapshot = snapshot;
        this.snapshotVersion = snapshotVersion;
    }

    public boolean hasOpenDocument() {
        return documentId != null && !documentId.isBlank();
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public String getRole() {
        return role;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean isCanShare() {
        return canShare;
    }

    public boolean canShare() {
        return canShare;
    }

    public long getSnapshotVersion() {
        return snapshotVersion;
    }

    public CrdtSnapshotDto getSnapshot() {
        return snapshot;
    }

    public String getEditorCode() {
        return editorCode;
    }

    public String getViewerCode() {
        return viewerCode;
    }

    public void loadShareCodes(ShareCodeResponse response) {
        if (response == null) {
            return;
        }
        this.editorCode = response.getEditorCode();
        this.viewerCode = response.getViewerCode();
        this.canShare = response.isCanShare();
    }

    public boolean hasShareCodes() {
        return (editorCode != null && !editorCode.isBlank())
                || (viewerCode != null && !viewerCode.isBlank());
    }

    private void load(
            String documentId,
            String title,
            DocumentRole role,
            boolean canEdit,
            boolean canShare,
            long snapshotVersion,
            CrdtSnapshotDto snapshot,
            String editorCode,
            String viewerCode
    ) {
        this.documentId = documentId;
        this.title = title;
        this.role = role == null ? null : role.name();
        this.canEdit = canEdit;
        this.canShare = canShare;
        this.snapshotVersion = snapshotVersion;
        this.snapshot = snapshot;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
    }

    public void clear() {
        this.documentId = null;
        this.title = null;
        this.role = null;
        this.canEdit = false;
        this.canShare = false;
        this.snapshotVersion = 0;
        this.snapshot = null;
        this.editorCode = null;
        this.viewerCode = null;
    }
}
