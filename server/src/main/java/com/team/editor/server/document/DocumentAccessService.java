package com.team.editor.server.document;

import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.server.persistence.MongoDocumentRecord;
import org.springframework.stereotype.Service;

/**
 * Simple Phase 3 access checks.
 */
@Service
public class DocumentAccessService {
    public boolean isOwner(MongoDocumentRecord document, String userId) {
        if (document == null || document.getOwnerId() == null || userId == null || userId.isBlank()) {
            return false;
        }

        return userId.equals(document.getOwnerId().getValue());
    }

    public boolean isEditor(MongoDocumentRecord document, String userId) {
        if (document == null || userId == null || userId.isBlank()) {
            return false;
        }

        return document.getEditorUserIds().contains(userId);
    }

    public boolean isViewer(MongoDocumentRecord document, String userId) {
        if (document == null || userId == null || userId.isBlank()) {
            return false;
        }

        return document.getViewerUserIds().contains(userId);
    }

    public DocumentRole resolveRole(MongoDocumentRecord document, String userId) {
        if (isOwner(document, userId)) {
            return DocumentRole.OWNER;
        }

        if (isEditor(document, userId)) {
            return DocumentRole.EDITOR;
        }

        if (isViewer(document, userId)) {
            return DocumentRole.VIEWER;
        }

        return null;
    }

    public boolean canOpenById(MongoDocumentRecord document, String userId) {
        return resolveRole(document, userId) != null;
    }

    public boolean canShare(MongoDocumentRecord document, String userId) {
        return isOwner(document, userId);
    }

    public boolean canRename(MongoDocumentRecord document, String userId) {
        return isOwner(document, userId);
    }

    public boolean canDelete(MongoDocumentRecord document, String userId) {
        return isOwner(document, userId);
    }

    public boolean canSaveSnapshot(MongoDocumentRecord document, String userId) {
        return isOwner(document, userId) || isEditor(document, userId);
    }
}
