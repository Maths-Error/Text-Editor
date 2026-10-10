package com.team.editor.common.protocol.dto.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Response wrapper for a user's visible document list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ListDocumentsResponse {
    private List<DocumentSummaryDto> documents;

    public ListDocumentsResponse() {
        this.documents = new ArrayList<>();
    }

    public ListDocumentsResponse(List<DocumentSummaryDto> documents) {
        this.documents = documents == null ? new ArrayList<>() : documents;
    }

    public List<DocumentSummaryDto> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentSummaryDto> documents) {
        this.documents = documents == null ? new ArrayList<>() : documents;
    }
}
