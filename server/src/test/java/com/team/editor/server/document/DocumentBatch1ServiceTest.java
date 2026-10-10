package com.team.editor.server.document;

import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.common.protocol.dto.document.ListDocumentsResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.server.repository.InMemoryDocumentRepository;
import com.team.editor.server.validation.DocumentRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentBatch1ServiceTest {
    private DocumentCommandService commandService;
    private DocumentQueryService queryService;

    @BeforeEach
    void setUp() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        ShareCodeService shareCodeService = new ShareCodeService();
        DocumentRequestValidator validator = new DocumentRequestValidator();
        DocumentAccessService accessService = new DocumentAccessService();
        DocumentMapper mapper = new DocumentMapper();

        commandService = new DocumentCommandService(repository, shareCodeService, validator, mapper);
        queryService = new DocumentQueryService(repository, accessService, validator, mapper);
    }

    @Test
    void createDocumentReturnsOwnerDocumentResponse() {
        CreateDocumentResponse response = createOmarDocument();

        assertTrue(response.getDocumentId().startsWith("doc_"));
        assertEquals(DocumentRole.OWNER, response.getRole());
        assertTrue(response.isCanEdit());
        assertTrue(response.isCanShare());
        assertTrue(response.getEditorCode().startsWith("EDT-"));
        assertTrue(response.getViewerCode().startsWith("VWR-"));
        assertNotNull(response.getSnapshot());
    }

    @Test
    void listDocumentsReturnsCreatedDocumentForOwner() {
        CreateDocumentResponse created = createOmarDocument();

        ListDocumentsResponse listResponse = queryService.listDocuments("omar");

        assertEquals(1, listResponse.getDocuments().size());
        assertEquals(created.getDocumentId(), listResponse.getDocuments().get(0).getDocumentId());
        assertEquals(DocumentRole.OWNER, listResponse.getDocuments().get(0).getRole());
    }

    @Test
    void openDocumentByIdAsOwnerReturnsOwnerResponse() {
        CreateDocumentResponse created = createOmarDocument();

        OpenDocumentResponse opened = queryService.openDocumentById(created.getDocumentId(), "omar");

        assertEquals(created.getDocumentId(), opened.getDocumentId());
        assertEquals(DocumentRole.OWNER, opened.getRole());
        assertTrue(opened.isCanEdit());
        assertTrue(opened.isCanShare());
        assertEquals(created.getEditorCode(), opened.getEditorCode());
        assertEquals(created.getViewerCode(), opened.getViewerCode());
    }

    @Test
    void openDocumentByIdWithWrongUserThrowsPermissionDenied() {
        CreateDocumentResponse created = createOmarDocument();

        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> queryService.openDocumentById(created.getDocumentId(), "biso")
        );

        assertEquals("PERMISSION_DENIED", exception.getErrorCode());
    }

    @Test
    void openDocumentByIdWithMissingIdThrowsDocumentNotFound() {
        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> queryService.openDocumentById("doc_missing", "omar")
        );

        assertEquals("DOCUMENT_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void createDocumentWithBlankTitleThrowsInvalidRequest() {
        CreateDocumentRequest request = new CreateDocumentRequest(new UserId("omar"), "   ", null);

        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> commandService.createDocument(request)
        );

        assertEquals("INVALID_REQUEST", exception.getErrorCode());
    }

    private CreateDocumentResponse createOmarDocument() {
        CreateDocumentRequest request = new CreateDocumentRequest(
                new UserId("omar"),
                "Untitled Document",
                null
        );

        return commandService.createDocument(request);
    }
}
