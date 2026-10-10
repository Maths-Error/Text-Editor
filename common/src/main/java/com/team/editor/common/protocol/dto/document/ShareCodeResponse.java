package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response containing document share codes for users who can share.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShareCodeResponse {
    private String documentId;
    private String editorCode;
    private String viewerCode;
    private boolean canShare;

    public ShareCodeResponse() {
    }

    public ShareCodeResponse(String documentId, String editorCode, String viewerCode, boolean canShare) {
        this.documentId = documentId;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.canShare = canShare;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
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

    public boolean isCanShare() {
        return canShare;
    }

    public void setCanShare(boolean canShare) {
        this.canShare = canShare;
    }
}
