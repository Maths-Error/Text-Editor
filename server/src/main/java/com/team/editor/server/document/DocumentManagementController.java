package com.team.editor.server.document;

import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DeleteDocumentResponse;
import com.team.editor.common.protocol.dto.document.ListDocumentsResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentRequest;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.RenameDocumentRequest;
import com.team.editor.common.protocol.dto.document.RenameDocumentResponse;
import com.team.editor.common.protocol.dto.document.SaveSnapshotRequest;
import com.team.editor.common.protocol.dto.document.SaveSnapshotResponse;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
public class DocumentManagementController {
    private final DocumentCommandService documentCommandService;
    private final DocumentQueryService documentQueryService;

    public DocumentManagementController(
            DocumentCommandService documentCommandService,
            DocumentQueryService documentQueryService
    ) {
        this.documentCommandService = documentCommandService;
        this.documentQueryService = documentQueryService;
    }

    @PostMapping
    public CreateDocumentResponse createDocument(@RequestBody CreateDocumentRequest request) {
        return documentCommandService.createDocument(request);
    }

    @GetMapping
    public ListDocumentsResponse listDocuments(@RequestParam String userId) {
        return documentQueryService.listDocuments(userId);
    }

    @GetMapping("/{documentId}")
    public OpenDocumentResponse openDocumentById(
            @PathVariable String documentId,
            @RequestParam String userId
    ) {
        return documentQueryService.openDocumentById(documentId, userId);
    }

    @PostMapping("/open-by-code")
    public OpenDocumentResponse openDocumentByCode(@RequestBody OpenDocumentRequest request) {
        return documentQueryService.openDocumentByCode(request);
    }

    @GetMapping("/{documentId}/share-codes")
    public ShareCodeResponse getShareCodes(
            @PathVariable String documentId,
            @RequestParam String requesterUserId
    ) {
        return documentQueryService.getShareCodes(documentId, requesterUserId);
    }

    @PatchMapping("/{documentId}")
    public RenameDocumentResponse renameDocument(
            @PathVariable String documentId,
            @RequestBody RenameDocumentRequest request
    ) {
        return documentCommandService.renameDocument(documentId, request);
    }

    @PutMapping("/{documentId}/snapshot")
    public SaveSnapshotResponse saveSnapshot(
            @PathVariable String documentId,
            @RequestBody SaveSnapshotRequest request
    ) {
        return documentCommandService.saveSnapshot(documentId, request);
    }

    @DeleteMapping("/{documentId}")
    public DeleteDocumentResponse deleteDocument(
            @PathVariable String documentId,
            @RequestParam String requesterUserId
    ) {
        return documentCommandService.deleteDocument(documentId, requesterUserId);
    }
}
