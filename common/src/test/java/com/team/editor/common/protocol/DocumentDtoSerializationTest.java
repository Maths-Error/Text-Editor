package com.team.editor.common.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.document.CreateDocumentRequest;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.common.protocol.dto.document.DocumentSummaryDto;
import com.team.editor.common.protocol.dto.document.ListDocumentsResponse;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDtoSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeAndDeserializeCreateDocumentRequestWithNullInitialSnapshot() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest(
                new UserId("omar"),
                "Untitled Document",
                null
        );

        String json = objectMapper.writeValueAsString(request);
        CreateDocumentRequest restored = objectMapper.readValue(json, CreateDocumentRequest.class);

        assertEquals("omar", restored.getOwnerId().getValue());
        assertEquals("Untitled Document", restored.getTitle());
        assertNull(restored.getInitialSnapshot());
    }

    @Test
    void shouldSerializeAndDeserializeCreateDocumentResponse() throws Exception {
        CrdtSnapshotDto snapshot = createSimpleSnapshot();
        CreateDocumentResponse response = new CreateDocumentResponse(
                "doc_abc123",
                "Untitled Document",
                new UserId("omar"),
                DocumentRole.OWNER,
                true,
                true,
                "EDT-9K2P4Q",
                "VWR-7M8X1A",
                snapshot,
                "2026-05-02T12:00:00Z",
                "2026-05-02T12:00:00Z"
        );

        String json = objectMapper.writeValueAsString(response);
        CreateDocumentResponse restored = objectMapper.readValue(json, CreateDocumentResponse.class);

        assertEquals("doc_abc123", restored.getDocumentId());
        assertEquals(DocumentRole.OWNER, restored.getRole());
        assertTrue(restored.isCanEdit());
        assertTrue(restored.isCanShare());
        assertEquals("EDT-9K2P4Q", restored.getEditorCode());
        assertEquals("VWR-7M8X1A", restored.getViewerCode());
        assertEquals(1, restored.getSnapshot().getFormatVersion());
        assertTrue(restored.getSnapshot().getBlocks() instanceof List);
    }

    @Test
    void shouldDeserializeViewerOpenDocumentResponseWithoutShareCodes() throws Exception {
        String json = """
                {
                  "documentId": "doc_abc123",
                  "title": "Untitled Document",
                  "ownerId": {
                    "value": "omar"
                  },
                  "role": "VIEWER",
                  "canEdit": false,
                  "canShare": false,
                  "snapshot": {
                    "formatVersion": 1,
                    "blocks": []
                  },
                  "createdAt": "2026-05-02T12:00:00Z",
                  "updatedAt": "2026-05-02T12:10:00Z"
                }
                """;

        OpenDocumentResponse restored = objectMapper.readValue(json, OpenDocumentResponse.class);

        assertEquals("doc_abc123", restored.getDocumentId());
        assertEquals(DocumentRole.VIEWER, restored.getRole());
        assertEquals(false, restored.isCanEdit());
        assertEquals(false, restored.isCanShare());
        assertNull(restored.getEditorCode());
        assertNull(restored.getViewerCode());
    }

    @Test
    void shouldSerializeAndDeserializeListDocumentsResponse() throws Exception {
        DocumentSummaryDto summary = new DocumentSummaryDto(
                "doc_abc123",
                "Untitled Document",
                new UserId("omar"),
                DocumentRole.OWNER,
                true,
                true,
                "Hello world...",
                "2026-05-02T12:00:00Z",
                "2026-05-02T12:10:00Z"
        );
        List<DocumentSummaryDto> documents = new ArrayList<>();
        documents.add(summary);
        ListDocumentsResponse response = new ListDocumentsResponse(documents);

        String json = objectMapper.writeValueAsString(response);
        ListDocumentsResponse restored = objectMapper.readValue(json, ListDocumentsResponse.class);

        assertEquals(1, restored.getDocuments().size());
        assertEquals("doc_abc123", restored.getDocuments().get(0).getDocumentId());
        assertEquals("Hello world...", restored.getDocuments().get(0).getVisibleTextPreview());
    }

    @Test
    void shouldSerializeAndDeserializeFlexibleCrdtSnapshot() throws Exception {
        CrdtSnapshotDto snapshot = createSimpleSnapshot();

        String json = objectMapper.writeValueAsString(snapshot);
        CrdtSnapshotDto restored = objectMapper.readValue(json, CrdtSnapshotDto.class);

        assertEquals(1, restored.getFormatVersion());
        assertTrue(restored.getBlocks() instanceof List);

        Map<?, ?> createdBy = (Map<?, ?>) restored.getMetadata().get("createdBy");
        assertEquals("omar", createdBy.get("value"));
    }

    private CrdtSnapshotDto createSimpleSnapshot() {
        List<Object> blocks = new ArrayList<>();
        Map<String, Object> block = new HashMap<>();
        block.put("blockId", "block-1");
        blocks.add(block);

        Map<String, Object> createdBy = new HashMap<>();
        createdBy.put("value", "omar");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("createdBy", createdBy);

        return new CrdtSnapshotDto(1, blocks, metadata);
    }
}
