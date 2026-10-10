package com.team.editor.server.document;

import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DeleteDocumentResponse;
import com.team.editor.common.protocol.dto.document.RenameDocumentRequest;
import com.team.editor.common.protocol.dto.document.RenameDocumentResponse;
import com.team.editor.common.protocol.dto.document.SaveSnapshotRequest;
import com.team.editor.common.protocol.dto.document.SaveSnapshotResponse;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import com.team.editor.server.persistence.MongoDocumentRecord;
import com.team.editor.server.repository.DocumentRepository;
import com.team.editor.server.validation.DocumentRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles document write commands for Phase 3.
 */
@Service
public class DocumentCommandService {
    private final DocumentRepository documentRepository;
    private final ShareCodeService shareCodeService;
    private final DocumentAccessService documentAccessService;
    private final DocumentRequestValidator validator;
    private final DocumentMapper documentMapper;

    @Autowired
    public DocumentCommandService(
            DocumentRepository documentRepository,
            ShareCodeService shareCodeService,
            DocumentAccessService documentAccessService,
            DocumentRequestValidator validator,
            DocumentMapper documentMapper
    ) {
        this.documentRepository = documentRepository;
        this.shareCodeService = shareCodeService;
        this.documentAccessService = documentAccessService;
        this.validator = validator;
        this.documentMapper = documentMapper;
    }

    public DocumentCommandService(
            DocumentRepository documentRepository,
            ShareCodeService shareCodeService,
            DocumentRequestValidator validator,
            DocumentMapper documentMapper
    ) {
        this(documentRepository, shareCodeService, new DocumentAccessService(), validator, documentMapper);
    }

    public CreateDocumentResponse createDocument(CreateDocumentRequest request) {
        validator.validateCreateRequest(request);

        CrdtSnapshotDto snapshot = request.getInitialSnapshot();
        if (snapshot == null) {
            snapshot = createDefaultSnapshot();
        }

        String documentId = "doc_" + UUID.randomUUID();
        String editorCode = generateUniqueEditorCode();
        String viewerCode = generateUniqueViewerCode();
        String now = Instant.now().toString();

        MongoDocumentRecord document = new MongoDocumentRecord(
                documentId,
                request.getTitle().trim(),
                request.getOwnerId(),
                editorCode,
                viewerCode,
                snapshot,
                now,
                now,
                false
        );

        documentRepository.save(document);
        return documentMapper.toCreateResponse(document);
    }

    public RenameDocumentResponse renameDocument(String documentId, RenameDocumentRequest request) {
        validator.validateRenameRequest(documentId, request);

        MongoDocumentRecord document = findExistingDocument(documentId.trim());
        String requesterUserId = request.getRequesterUserId().getValue();

        if (!documentAccessService.canRename(document, requesterUserId)) {
            throw new DocumentApiException("PERMISSION_DENIED", "User cannot rename this document.", 403);
        }

        String now = Instant.now().toString();
        document.setTitle(request.getTitle().trim());
        document.setUpdatedAt(now);
        documentRepository.save(document);

        return documentMapper.toRenameResponse(document);
    }

    public DeleteDocumentResponse deleteDocument(String documentId, String requesterUserId) {
        validator.validateDeleteRequest(documentId, requesterUserId);

        MongoDocumentRecord document = findExistingDocument(documentId.trim());
        String trimmedRequesterUserId = requesterUserId.trim();

        if (!documentAccessService.canDelete(document, trimmedRequesterUserId)) {
            throw new DocumentApiException("PERMISSION_DENIED", "User cannot delete this document.", 403);
        }

        document.setDeleted(true);
        document.setUpdatedAt(Instant.now().toString());
        documentRepository.save(document);

        return documentMapper.toDeleteResponse(document);
    }

    public SaveSnapshotResponse saveSnapshot(String documentId, SaveSnapshotRequest request) {
        validator.validateSaveSnapshotRequest(documentId, request);

        String trimmedDocumentId = documentId.trim();
        MongoDocumentRecord document = findExistingDocument(trimmedDocumentId);
        String requesterUserId = request.getRequesterUserId().getValue();

        if (!documentAccessService.canSaveSnapshot(document, requesterUserId)) {
            throw new DocumentApiException("PERMISSION_DENIED", "User cannot save this document snapshot.", 403);
        }

        String now = Instant.now().toString();
        Optional<MongoDocumentRecord> updatedDocument = documentRepository.updateSnapshot(
                trimmedDocumentId,
                request.getSnapshot(),
                now
        );

        if (updatedDocument.isEmpty()) {
            throw new DocumentApiException("DOCUMENT_NOT_FOUND", "Document was not found.", 404);
        }

        return documentMapper.toSaveSnapshotResponse(updatedDocument.get());
    }

    private CrdtSnapshotDto createDefaultSnapshot() {
        CrdtSnapshotDto snapshot = new CrdtSnapshotDto();
        snapshot.setFormatVersion(1);
        snapshot.setBlocks(new ArrayList<>());
        snapshot.setMetadata(new HashMap<>());
        return snapshot;
    }

    private String generateUniqueEditorCode() {
        String code = shareCodeService.generateEditorCode();
        int attempts = 0;

        while (documentRepository.codeExists(code) && attempts < 5) {
            code = shareCodeService.generateEditorCode();
            attempts++;
        }

        return code;
    }

    private String generateUniqueViewerCode() {
        String code = shareCodeService.generateViewerCode();
        int attempts = 0;

        while (documentRepository.codeExists(code) && attempts < 5) {
            code = shareCodeService.generateViewerCode();
            attempts++;
        }

        return code;
    }

    private MongoDocumentRecord findExistingDocument(String documentId) {
        Optional<MongoDocumentRecord> documentOptional = documentRepository.findById(documentId);

        if (documentOptional.isEmpty() || documentOptional.get().isDeleted()) {
            throw new DocumentApiException("DOCUMENT_NOT_FOUND", "Document was not found.", 404);
        }

        return documentOptional.get();
    }
}
