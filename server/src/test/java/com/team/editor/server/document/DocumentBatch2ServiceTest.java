package com.team.editor.server.document;

import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.common.protocol.dto.document.OpenDocumentRequest;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;
import com.team.editor.server.repository.InMemoryDocumentRepository;
import com.team.editor.server.validation.DocumentRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentBatch2ServiceTest {
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
    void openDocumentByCodeWithEditorCodeReturnsEditorResponse() {
        CreateDocumentResponse created = createOmarDocument();

        OpenDocumentResponse opened = openByCode("biso", created.getEditorCode());

        assertEquals(created.getDocumentId(), opened.getDocumentId());
        assertEquals(DocumentRole.EDITOR, opened.getRole());
        assertTrue(opened.isCanEdit());
        assertFalse(opened.isCanShare());
        assertNull(opened.getEditorCode());
        assertNull(opened.getViewerCode());
    }

    @Test
    void openDocumentByCodeWithViewerCodeReturnsViewerResponse() {
        CreateDocumentResponse created = createOmarDocument();

        OpenDocumentResponse opened = openByCode("hossam", created.getViewerCode());

        assertEquals(created.getDocumentId(), opened.getDocumentId());
        assertEquals(DocumentRole.VIEWER, opened.getRole());
        assertFalse(opened.isCanEdit());
        assertFalse(opened.isCanShare());
        assertNull(opened.getEditorCode());
        assertNull(opened.getViewerCode());
    }

    @Test
    void ownerCanGetShareCodes() {
        CreateDocumentResponse created = createOmarDocument();

        ShareCodeResponse response = queryService.getShareCodes(created.getDocumentId(), "omar");

        assertTrue(response.getEditorCode().startsWith("EDT-"));
        assertTrue(response.getViewerCode().startsWith("VWR-"));
        assertTrue(response.isCanShare());
    }

    @Test
    void viewerCannotGetShareCodes() {
        CreateDocumentResponse created = createOmarDocument();
        openByCode("viewer-user", created.getViewerCode());

        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> queryService.getShareCodes(created.getDocumentId(), "viewer-user")
        );

        assertEquals("VIEWER_CANNOT_VIEW_SHARE_CODES", exception.getErrorCode());
    }

    @Test
    void editorCannotGetShareCodesInThisBatch() {
        CreateDocumentResponse created = createOmarDocument();
        openByCode("editor-user", created.getEditorCode());

        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> queryService.getShareCodes(created.getDocumentId(), "editor-user")
        );

        assertEquals("PERMISSION_DENIED", exception.getErrorCode());
    }

    @Test
    void invalidShareCodeThrowsShareCodeNotFound() {
        createOmarDocument();

        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> openByCode("biso", "EDT-NOPE01")
        );

        assertEquals("SHARE_CODE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void editorUserCanOpenByIdAfterOpeningByEditorCode() {
        CreateDocumentResponse created = createOmarDocument();
        openByCode("biso", created.getEditorCode());

        OpenDocumentResponse opened = queryService.openDocumentById(created.getDocumentId(), "biso");

        assertEquals(DocumentRole.EDITOR, opened.getRole());
        assertTrue(opened.isCanEdit());
        assertFalse(opened.isCanShare());
        assertNull(opened.getEditorCode());
        assertNull(opened.getViewerCode());
    }

    @Test
    void viewerUserCanOpenByIdAfterOpeningByViewerCode() {
        CreateDocumentResponse created = createOmarDocument();
        openByCode("hossam", created.getViewerCode());

        OpenDocumentResponse opened = queryService.openDocumentById(created.getDocumentId(), "hossam");

        assertEquals(DocumentRole.VIEWER, opened.getRole());
        assertFalse(opened.isCanEdit());
        assertFalse(opened.isCanShare());
        assertNull(opened.getEditorCode());
        assertNull(opened.getViewerCode());
    }

    @Test
    void unknownUserOpeningByIdStillThrowsPermissionDenied() {
        CreateDocumentResponse created = createOmarDocument();

        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> queryService.openDocumentById(created.getDocumentId(), "unknown")
        );

        assertEquals("PERMISSION_DENIED", exception.getErrorCode());
    }

    private CreateDocumentResponse createOmarDocument() {
        CreateDocumentRequest request = new CreateDocumentRequest(
                new UserId("omar"),
                "Untitled Document",
                null
        );

        return commandService.createDocument(request);
    }

    private OpenDocumentResponse openByCode(String userId, String shareCode) {
        OpenDocumentRequest request = new OpenDocumentRequest(
                null,
                new UserId(userId),
                userId,
                shareCode
        );

        return queryService.openDocumentByCode(request);
    }

    @Test
    void userOpeningEditorCodeGetsEditorRoleAndIsInEditorList() {
        CreateDocumentResponse created = createOmarDocument();

        OpenDocumentResponse opened = openByCode("omar2", created.getEditorCode());

        assertEquals(DocumentRole.EDITOR, opened.getRole());
        assertTrue(opened.isCanEdit());

        // Verify via open-by-id that role sticks
        OpenDocumentResponse byId = queryService.openDocumentById(created.getDocumentId(), "omar2");
        assertEquals(DocumentRole.EDITOR, byId.getRole());
    }

    @Test
    void userSwitchesFromEditorToViewerByUsingViewerCode() {
        CreateDocumentResponse created = createOmarDocument();

        // First open as editor
        OpenDocumentResponse editorOpen = openByCode("omar2", created.getEditorCode());
        assertEquals(DocumentRole.EDITOR, editorOpen.getRole());

        // Now open with viewer code -> must switch to viewer
        OpenDocumentResponse viewerOpen = openByCode("omar2", created.getViewerCode());
        assertEquals(DocumentRole.VIEWER, viewerOpen.getRole());
        assertFalse(viewerOpen.isCanEdit());

        // Verify via open-by-id that role updated to VIEWER
        OpenDocumentResponse byId = queryService.openDocumentById(created.getDocumentId(), "omar2");
        assertEquals(DocumentRole.VIEWER, byId.getRole());
    }

    @Test
    void userSwitchesFromViewerBackToEditorByUsingEditorCode() {
        CreateDocumentResponse created = createOmarDocument();

        // Open as editor
        openByCode("omar2", created.getEditorCode());
        // Switch to viewer
        openByCode("omar2", created.getViewerCode());
        // Switch back to editor
        OpenDocumentResponse editorAgain = openByCode("omar2", created.getEditorCode());

        assertEquals(DocumentRole.EDITOR, editorAgain.getRole());
        assertTrue(editorAgain.isCanEdit());

        // Verify via open-by-id
        OpenDocumentResponse byId = queryService.openDocumentById(created.getDocumentId(), "omar2");
        assertEquals(DocumentRole.EDITOR, byId.getRole());
    }

    @Test
    void openingSameCodeMultipleTimesIsIdempotent() {
        CreateDocumentResponse created = createOmarDocument();

        // Open editor code 3 times
        openByCode("omar2", created.getEditorCode());
        openByCode("omar2", created.getEditorCode());
        OpenDocumentResponse third = openByCode("omar2", created.getEditorCode());

        assertEquals(DocumentRole.EDITOR, third.getRole());
        assertTrue(third.isCanEdit());
    }
}
