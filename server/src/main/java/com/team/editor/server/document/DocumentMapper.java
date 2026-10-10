package com.team.editor.server.document;

import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DeleteDocumentResponse;
import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.common.protocol.dto.document.DocumentSummaryDto;
import com.team.editor.common.protocol.dto.document.ListDocumentsResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.RenameDocumentResponse;
import com.team.editor.common.protocol.dto.document.SaveSnapshotResponse;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;
import com.team.editor.server.persistence.MongoDocumentRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts stored document records into API DTOs.
 */
@Component
public class DocumentMapper {
    public CreateDocumentResponse toCreateResponse(MongoDocumentRecord document) {
        return new CreateDocumentResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getOwnerId(),
                DocumentRole.OWNER,
                true,
                true,
                document.getEditorCode(),
                document.getViewerCode(),
                document.getSnapshot(),
                document.getSnapshotVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public OpenDocumentResponse toOwnerOpenResponse(MongoDocumentRecord document) {
        return new OpenDocumentResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getOwnerId(),
                DocumentRole.OWNER,
                true,
                true,
                document.getEditorCode(),
                document.getViewerCode(),
                document.getSnapshot(),
                document.getSnapshotVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public OpenDocumentResponse toEditorOpenResponse(MongoDocumentRecord document) {
        return new OpenDocumentResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getOwnerId(),
                DocumentRole.EDITOR,
                true,
                false,
                null,
                null,
                document.getSnapshot(),
                document.getSnapshotVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public OpenDocumentResponse toViewerOpenResponse(MongoDocumentRecord document) {
        return new OpenDocumentResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getOwnerId(),
                DocumentRole.VIEWER,
                false,
                false,
                null,
                null,
                document.getSnapshot(),
                document.getSnapshotVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public DocumentSummaryDto toOwnerSummary(MongoDocumentRecord document) {
        return new DocumentSummaryDto(
                document.getDocumentId(),
                document.getTitle(),
                document.getOwnerId(),
                DocumentRole.OWNER,
                true,
                true,
                "",
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    public ListDocumentsResponse toListResponse(List<MongoDocumentRecord> documents) {
        List<DocumentSummaryDto> summaries = new ArrayList<>();

        if (documents != null) {
            for (MongoDocumentRecord document : documents) {
                summaries.add(toOwnerSummary(document));
            }
        }

        return new ListDocumentsResponse(summaries);
    }

    public ShareCodeResponse toShareCodeResponse(MongoDocumentRecord document) {
        return new ShareCodeResponse(
                document.getDocumentId(),
                document.getEditorCode(),
                document.getViewerCode(),
                true
        );
    }

    public RenameDocumentResponse toRenameResponse(MongoDocumentRecord document) {
        return new RenameDocumentResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getUpdatedAt()
        );
    }

    public DeleteDocumentResponse toDeleteResponse(MongoDocumentRecord document) {
        return new DeleteDocumentResponse(
                document.getDocumentId(),
                true
        );
    }

    public SaveSnapshotResponse toSaveSnapshotResponse(MongoDocumentRecord document) {
        return new SaveSnapshotResponse(
                document.getDocumentId(),
                document.getSnapshotVersion(),
                document.getUpdatedAt()
        );
    }
}
