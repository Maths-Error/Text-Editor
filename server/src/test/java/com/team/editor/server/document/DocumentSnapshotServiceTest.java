package com.team.editor.server.document;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentRequest;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.document.SaveSnapshotRequest;
import com.team.editor.common.protocol.dto.document.SaveSnapshotResponse;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import com.team.editor.server.repository.InMemoryDocumentRepository;
import com.team.editor.server.validation.DocumentRequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentSnapshotServiceTest {
    private DocumentCommandService commandService;
    private DocumentQueryService queryService;

    @BeforeEach
    void setUp() {
        InMemoryDocumentRepository repository = new InMemoryDocumentRepository();
        ShareCodeService shareCodeService = new ShareCodeService();
        DocumentRequestValidator validator = new DocumentRequestValidator();
        DocumentAccessService accessService = new DocumentAccessService();
        DocumentMapper mapper = new DocumentMapper();

        commandService = new DocumentCommandService(repository, shareCodeService, accessService, validator, mapper);
        queryService = new DocumentQueryService(repository, accessService, validator, mapper);
    }

    @Test
    void ownerCanSaveSnapshotAndOpenReturnsLatestVersion() {
        CreateDocumentResponse created = createOmarDocument();

        SaveSnapshotResponse saved = commandService.saveSnapshot(
                created.getDocumentId(),
                saveRequest("omar", 0, snapshotWithText("owner save"))
        );

        assertEquals(created.getDocumentId(), saved.getDocumentId());
        assertEquals(1, saved.getSnapshotVersion());
        assertNotNull(saved.getUpdatedAt());

        OpenDocumentResponse opened = queryService.openDocumentById(created.getDocumentId(), "omar");
        assertEquals(1, opened.getSnapshotVersion());
        assertEquals("owner save", opened.getSnapshot().getMetadata().get("text"));
    }

    @Test
    void editorCanSaveSnapshot() {
        CreateDocumentResponse created = createOmarDocument();
        openByCode("biso", created.getEditorCode());

        SaveSnapshotResponse saved = commandService.saveSnapshot(
                created.getDocumentId(),
                saveRequest("biso", 0, snapshotWithText("editor save"))
        );

        assertEquals(1, saved.getSnapshotVersion());
    }

    @Test
    void viewerCannotSaveSnapshot() {
        CreateDocumentResponse created = createOmarDocument();
        openByCode("abdo", created.getViewerCode());

        DocumentApiException exception = assertThrows(
                DocumentApiException.class,
                () -> commandService.saveSnapshot(
                        created.getDocumentId(),
                        saveRequest("abdo", 0, snapshotWithText("viewer save"))
                )
        );

        assertEquals("PERMISSION_DENIED", exception.getErrorCode());
    }

    @Test
    void staleBaseSnapshotVersionStillSavesLatestSnapshot() {
        CreateDocumentResponse created = createOmarDocument();
        openByCode("biso", created.getEditorCode());

        SaveSnapshotResponse firstSave = commandService.saveSnapshot(
                created.getDocumentId(),
                saveRequest("omar", 0, snapshotWithText("first save"))
        );

        assertEquals(1, firstSave.getSnapshotVersion());

        SaveSnapshotResponse secondSave = commandService.saveSnapshot(
                created.getDocumentId(),
                saveRequest("biso", 0, snapshotWithText("stale save"))
        );

        assertEquals(2, secondSave.getSnapshotVersion());

        OpenDocumentResponse opened = queryService.openDocumentById(created.getDocumentId(), "omar");
        assertEquals(2, opened.getSnapshotVersion());
        assertEquals("stale save", opened.getSnapshot().getMetadata().get("text"));
    }

    @Test
    void negativeBaseSnapshotVersionIsInformational() {
        CreateDocumentResponse created = createOmarDocument();

        SaveSnapshotResponse saved = commandService.saveSnapshot(
                created.getDocumentId(),
                saveRequest("omar", -1, snapshotWithText("negative version save"))
        );

        assertEquals(1, saved.getSnapshotVersion());

        OpenDocumentResponse opened = queryService.openDocumentById(created.getDocumentId(), "omar");
        assertEquals("negative version save", opened.getSnapshot().getMetadata().get("text"));
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

    private SaveSnapshotRequest saveRequest(String userId, long baseSnapshotVersion, CrdtSnapshotDto snapshot) {
        return new SaveSnapshotRequest(new UserId(userId), baseSnapshotVersion, snapshot);
    }

    private CrdtSnapshotDto snapshotWithText(String text) {
        CrdtSnapshotDto snapshot = new CrdtSnapshotDto();
        snapshot.setFormatVersion(1);
        snapshot.setBlocks(new ArrayList<>());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("text", text);
        snapshot.setMetadata(metadata);
        return snapshot;
    }
}
