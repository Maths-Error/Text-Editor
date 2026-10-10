package com.team.editor.server.document;

import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.common.protocol.dto.document.ListDocumentsResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentRequest;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;
import com.team.editor.server.persistence.MongoDocumentRecord;
import com.team.editor.server.repository.DocumentRepository;
import com.team.editor.server.validation.DocumentRequestValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Handles document read queries for Batch 1 and 2.
 */
@Service
public class DocumentQueryService {
    private final DocumentRepository documentRepository;
    private final DocumentAccessService documentAccessService;
    private final DocumentRequestValidator validator;
    private final DocumentMapper documentMapper;

    public DocumentQueryService(
            DocumentRepository documentRepository,
            DocumentAccessService documentAccessService,
            DocumentRequestValidator validator,
            DocumentMapper documentMapper
    ) {
        this.documentRepository = documentRepository;
        this.documentAccessService = documentAccessService;
        this.validator = validator;
        this.documentMapper = documentMapper;
    }

    public ListDocumentsResponse listDocuments(String userId) {
        validator.validateUserId(userId);

        List<MongoDocumentRecord> documents = documentRepository.findByOwnerId(userId.trim());
        return documentMapper.toListResponse(documents);
    }

    public OpenDocumentResponse openDocumentById(String documentId, String userId) {
        validator.validateDocumentId(documentId);
        validator.validateUserId(userId);

        MongoDocumentRecord document = findExistingDocument(documentId.trim());
        String trimmedUserId = userId.trim();

        if (!documentAccessService.canOpenById(document, trimmedUserId)) {
            throw new DocumentApiException("PERMISSION_DENIED", "User cannot open this document.", 403);
        }

        return mapOpenResponseForUser(document, trimmedUserId);
    }

    public OpenDocumentResponse openDocumentByCode(OpenDocumentRequest request) {
        validator.validateOpenByCodeRequest(request);

        String shareCode = request.getShareCode().trim();
        String userId = request.getUserId().getValue();
        Optional<MongoDocumentRecord> documentOptional = documentRepository.findByShareCode(shareCode);

        if (documentOptional.isEmpty()) {
            throw new DocumentApiException("SHARE_CODE_NOT_FOUND", "Share code was not found.", 404);
        }

        MongoDocumentRecord document = documentOptional.get();
        if (document.isDeleted()) {
            throw new DocumentApiException("DOCUMENT_NOT_FOUND", "Document was not found.", 404);
        }

        if (shareCode.equals(document.getEditorCode())) {
            document.addEditorUserId(userId);
            documentRepository.save(document);
            return documentMapper.toEditorOpenResponse(document);
        }

        if (shareCode.equals(document.getViewerCode())) {
            document.addViewerUserId(userId);
            documentRepository.save(document);
            return documentMapper.toViewerOpenResponse(document);
        }

        throw new DocumentApiException("SHARE_CODE_NOT_FOUND", "Share code was not found.", 404);
    }

    public ShareCodeResponse getShareCodes(String documentId, String requesterUserId) {
        validator.validateDocumentId(documentId);
        validator.validateUserId(requesterUserId);

        MongoDocumentRecord document = findExistingDocument(documentId.trim());
        String trimmedUserId = requesterUserId.trim();

        if (documentAccessService.canShare(document, trimmedUserId)) {
            return documentMapper.toShareCodeResponse(document);
        }

        if (documentAccessService.isViewer(document, trimmedUserId)) {
            throw new DocumentApiException(
                    "VIEWER_CANNOT_VIEW_SHARE_CODES",
                    "Viewers cannot view document share codes.",
                    403
            );
        }

        throw new DocumentApiException("PERMISSION_DENIED", "User cannot view document share codes.", 403);
    }

    private MongoDocumentRecord findExistingDocument(String documentId) {
        Optional<MongoDocumentRecord> documentOptional = documentRepository.findById(documentId);

        if (documentOptional.isEmpty() || documentOptional.get().isDeleted()) {
            throw new DocumentApiException("DOCUMENT_NOT_FOUND", "Document was not found.", 404);
        }

        return documentOptional.get();
    }

    private OpenDocumentResponse mapOpenResponseForUser(MongoDocumentRecord document, String userId) {
        DocumentRole role = documentAccessService.resolveRole(document, userId);

        if (role == DocumentRole.OWNER) {
            return documentMapper.toOwnerOpenResponse(document);
        }

        if (role == DocumentRole.EDITOR) {
            return documentMapper.toEditorOpenResponse(document);
        }

        if (role == DocumentRole.VIEWER) {
            return documentMapper.toViewerOpenResponse(document);
        }

        throw new DocumentApiException("PERMISSION_DENIED", "User cannot open this document.", 403);
    }
}
