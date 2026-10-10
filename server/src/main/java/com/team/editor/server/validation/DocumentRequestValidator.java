package com.team.editor.server.validation;

import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.OpenDocumentRequest;
import com.team.editor.common.protocol.dto.document.RenameDocumentRequest;
import com.team.editor.common.protocol.dto.document.SaveSnapshotRequest;
import com.team.editor.server.document.DocumentApiException;
import org.springframework.stereotype.Component;

/**
 * Basic request validation for the Batch 1 document API.
 */
@Component
public class DocumentRequestValidator {
    public void validateCreateRequest(CreateDocumentRequest request) {
        if (request == null) {
            throwInvalidRequest("Create document request is required.");
        }

        if (request.getOwnerId() == null || request.getOwnerId().getValue() == null
                || request.getOwnerId().getValue().isBlank()) {
            throwInvalidRequest("ownerId is required.");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throwInvalidRequest("title is required.");
        }
    }

    public void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throwInvalidRequest("userId is required.");
        }
    }

    public void validateDocumentId(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            throwInvalidRequest("documentId is required.");
        }
    }

    public void validateOpenByCodeRequest(OpenDocumentRequest request) {
        if (request == null) {
            throwInvalidRequest("Open document by code request is required.");
        }

        if (request.getUserId() == null || request.getUserId().getValue() == null
                || request.getUserId().getValue().isBlank()) {
            throwInvalidRequest("userId is required.");
        }

        validateShareCode(request.getShareCode());
    }

    public void validateShareCode(String shareCode) {
        if (shareCode == null || shareCode.isBlank()) {
            throwInvalidRequest("shareCode is required.");
        }
    }

    public void validateRenameRequest(String documentId, RenameDocumentRequest request) {
        validateDocumentId(documentId);

        if (request == null) {
            throwInvalidRequest("Rename document request is required.");
        }

        if (request.getRequesterUserId() == null || request.getRequesterUserId().getValue() == null
                || request.getRequesterUserId().getValue().isBlank()) {
            throwInvalidRequest("requesterUserId is required.");
        }

        validateTitle(request.getTitle());
    }

    public void validateDeleteRequest(String documentId, String requesterUserId) {
        validateDocumentId(documentId);
        validateUserId(requesterUserId);
    }

    public void validateSaveSnapshotRequest(String documentId, SaveSnapshotRequest request) {
        validateDocumentId(documentId);

        if (request == null) {
            throwInvalidRequest("Save snapshot request is required.");
        }

        if (request.getRequesterUserId() == null || request.getRequesterUserId().getValue() == null
                || request.getRequesterUserId().getValue().isBlank()) {
            throwInvalidRequest("requesterUserId is required.");
        }

        if (request.getSnapshot() == null) {
            throwInvalidRequest("snapshot is required.");
        }

    }

    public void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throwInvalidRequest("title is required.");
        }
    }

    private void throwInvalidRequest(String message) {
        throw new DocumentApiException("INVALID_REQUEST", message, 400);
    }
}
