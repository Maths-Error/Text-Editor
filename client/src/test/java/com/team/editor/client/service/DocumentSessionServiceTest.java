package com.team.editor.client.service;

import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.document.CreateDocumentResponse;
import com.team.editor.common.protocol.dto.document.DocumentRole;
import com.team.editor.common.protocol.dto.document.OpenDocumentResponse;
import com.team.editor.common.protocol.dto.persistence.CrdtSnapshotDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentSessionServiceTest {

    @Test
    void loadsCreateResponseValues() {
        CrdtSnapshotDto snapshot = new CrdtSnapshotDto();
        CreateDocumentResponse response = new CreateDocumentResponse(
                "doc-1",
                "Demo",
                new UserId("omar"),
                DocumentRole.OWNER,
                true,
                true,
                "EDT-123",
                "VWR-456",
                snapshot,
                7,
                "created",
                "updated"
        );

        DocumentSessionService service = new DocumentSessionService();
        service.loadFromCreateResponse(response);

        assertTrue(service.hasOpenDocument());
        assertEquals("doc-1", service.getDocumentId());
        assertEquals("Demo", service.getTitle());
        assertEquals("OWNER", service.getRole());
        assertTrue(service.isCanEdit());
        assertTrue(service.isCanShare());
        assertEquals(7, service.getSnapshotVersion());
        assertSame(snapshot, service.getSnapshot());
        assertEquals("EDT-123", service.getEditorCode());
        assertEquals("VWR-456", service.getViewerCode());
    }

    @Test
    void loadsOpenResponseValues() {
        CrdtSnapshotDto snapshot = new CrdtSnapshotDto();
        OpenDocumentResponse response = new OpenDocumentResponse(
                "doc-2",
                "Shared",
                new UserId("owner"),
                DocumentRole.VIEWER,
                false,
                false,
                null,
                null,
                snapshot,
                3,
                "created",
                "updated"
        );

        DocumentSessionService service = new DocumentSessionService();
        service.loadFromOpenResponse(response);

        assertTrue(service.hasOpenDocument());
        assertEquals("doc-2", service.getDocumentId());
        assertEquals("VIEWER", service.getRole());
        assertFalse(service.isCanEdit());
        assertFalse(service.isCanShare());
        assertEquals(3, service.getSnapshotVersion());
        assertSame(snapshot, service.getSnapshot());
    }

    @Test
    void updatesSnapshotVersionAfterSave() {
        DocumentSessionService service = new DocumentSessionService();

        service.updateSnapshotVersion(11);

        assertEquals(11, service.getSnapshotVersion());
    }
}
