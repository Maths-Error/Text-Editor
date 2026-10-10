package com.team.editor.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.model.UserId;
import com.team.editor.common.ops.block.InsertBlockOp;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.JoinSessionRequest;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.message.MessageType;
import com.team.editor.common.protocol.message.SessionRole;
import com.team.editor.server.document.DocumentAccessService;
import com.team.editor.server.persistence.MongoDocumentRecord;
import com.team.editor.server.repository.InMemoryDocumentRepository;
import com.team.editor.server.service.OperationRelayService;
import com.team.editor.server.service.PermissionService;
import com.team.editor.server.service.PresenceService;
import com.team.editor.server.service.SessionJoinService;
import com.team.editor.server.service.UndoRedoService;
import com.team.editor.server.session.SessionRegistry;
import com.team.editor.server.validation.MessageValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollaborationWebSocketHandlerTest {
    @Test
    void relaysOperationsAndPresenceWithinSameSession() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        context.handler.afterConnectionEstablished(aliceSession);
        context.handler.afterConnectionEstablished(bobSession);

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-1", new UserId("alice"), "Alice")
                )
        ));
        context.handler.handleTextMessage(bobSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-1", new UserId("bob"), "Bob", SessionRole.EDITOR)
                )
        ));

        assertHasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.JOIN_SESSION_RESPONSE);
        assertHasMessageType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.JOIN_SESSION_RESPONSE);
        assertPresenceEvent(aliceSession.getSentTextPayloads(), context.objectMapper, "bob", "JOINED");
        assertPresenceEvent(bobSession.getSentTextPayloads(), context.objectMapper, "alice", "JOINED");
        assertEquals(2, context.sessionRegistry.getSession("room-1").orElseThrow().getConnectedUsers().size());

        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new OperationMessage("room-1", new UserId("alice"), "INSERT_TEXT", "{\"text\":\"hello\"}")
                )
        ));

        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION));
        assertHasMessageType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);

        JsonNode relayedOperation = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(relayedOperation);
        assertEquals("alice", relayedOperation.get("senderId").get("value").asText());
        assertEquals("INSERT_TEXT", relayedOperation.get("operationKind").asText());
        assertEquals("hello", relayedOperation.get("payload").get("text").asText());

        bobSession.clearSentTextPayloads();

        context.handler.afterConnectionClosed(bobSession, CloseStatus.NORMAL);
        assertPresenceEvent(aliceSession.getSentTextPayloads(), context.objectMapper, "bob", "LEFT");

        context.handler.afterConnectionClosed(aliceSession, CloseStatus.NORMAL);
        assertTrue(context.sessionRegistry.getSession("room-1").isEmpty());
    }

    @Test
    void editorCanUndoAndServerBroadcastsGeneratedOperationToEveryone() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-undo", "alice", "Alice");
        join(context, bobSession, "room-undo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendInsertChar(context, aliceSession, "alice-op-1", "alice", 1, "a");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode aliceUndo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        JsonNode bobUndo = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(aliceUndo);
        assertNotNull(bobUndo);
        assertEquals("DELETE_CHAR", aliceUndo.get("operationKind").asText());
        assertEquals("DELETE_CHAR", bobUndo.get("operationKind").asText());
        assertEquals("SERVER", aliceUndo.get("senderId").get("value").asText());
        assertEquals("alice", aliceUndo.get("payload").get("op").get("id").get("userId").get("value").asText());
        assertEquals("alice", aliceUndo.get("payload").get("undoRedoMetadata").get("requestedByUserId").asText());
        assertEquals("UNDO", aliceUndo.get("payload").get("undoRedoMetadata").get("action").asText());
    }

    @Test
    void viewerCannotRequestUndo() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession viewerSession = new FakeWebSocketSession("ws-viewer");

        join(context, viewerSession, "room-undo-viewer", "viewer", "Viewer");
        viewerSession.clearSentTextPayloads();

        context.handler.handleTextMessage(viewerSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode errorNode = firstMessageByType(viewerSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR);
        assertNotNull(errorNode);
        assertEquals("PERMISSION_DENIED", errorNode.get("errorCode").asText());
    }

    @Test
    void emptyUndoStackIsNoOpWithoutErrorOrBroadcast() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-empty-undo", "alice", "Alice");
        join(context, bobSession, "room-empty-undo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION));
        assertFalse(hasMessageType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        assertFalse(hasMessageType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION));
        assertEquals(0, context.sessionRegistry.getSession("room-empty-undo").orElseThrow().getOperationHistorySnapshot().size());
    }

    @Test
    void emptyRedoStackIsNoOpWithoutErrorOrBroadcast() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-empty-redo", "alice", "Alice");
        join(context, bobSession, "room-empty-redo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"REDO_REQUEST"}
                """));

        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION));
        assertFalse(hasMessageType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        assertFalse(hasMessageType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION));
        assertEquals(0, context.sessionRegistry.getSession("room-empty-redo").orElseThrow().getOperationHistorySnapshot().size());
    }

    @Test
    void redoReappliesLatestUndoneOperationToEveryone() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-redo", "alice", "Alice");
        join(context, bobSession, "room-redo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendInsertChar(context, aliceSession, "alice-op-1", "alice", 1, "a");
        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"REDO_REQUEST"}
                """));

        JsonNode aliceRedo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        JsonNode bobRedo = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(aliceRedo);
        assertNotNull(bobRedo);
        assertEquals("RESTORE_CHAR", aliceRedo.get("operationKind").asText());
        assertEquals("RESTORE_CHAR", bobRedo.get("operationKind").asText());
        assertEquals("SERVER", aliceRedo.get("senderId").get("value").asText());
        assertEquals("alice", aliceRedo.get("payload").get("undoRedoMetadata").get("requestedByUserId").asText());
        assertEquals("REDO", aliceRedo.get("payload").get("undoRedoMetadata").get("action").asText());
    }

    @Test
    void redoAfterUndoingFormattedInsertRestoresSameCharacterWithFormatting() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-formatted-insert-redo", "alice", "Alice");
        join(context, bobSession, "room-formatted-insert-redo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendFormattedInsertChar(context, aliceSession, "alice-bold-insert-1", "alice", 1, "A");
        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"REDO_REQUEST"}
                """));

        JsonNode aliceRedo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        JsonNode bobRedo = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(aliceRedo);
        assertNotNull(bobRedo);
        assertEquals("RESTORE_CHAR", aliceRedo.get("operationKind").asText());
        assertEquals("RESTORE_CHAR", bobRedo.get("operationKind").asText());
        assertEquals("alice", aliceRedo.get("payload").get("op").get("id").get("userId").get("value").asText());
        assertTrue(aliceRedo.get("payload").get("op").get("formatState").get("bold").asBoolean());
        assertEquals("A", aliceRedo.get("payload").get("op").get("value").asText());
    }

    @Test
    void undoAfterDeleteCharBroadcastsRestoreCharToEveryone() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-delete-undo", "alice", "Alice");
        join(context, bobSession, "room-delete-undo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendDeleteCharWithMetadata(context, aliceSession, "alice-delete-1", "alice", 1, "z");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode aliceUndo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        JsonNode bobUndo = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(aliceUndo);
        assertNotNull(bobUndo);
        assertEquals("RESTORE_CHAR", aliceUndo.get("operationKind").asText());
        assertEquals("RESTORE_CHAR", bobUndo.get("operationKind").asText());
        assertEquals("alice", aliceUndo.get("payload").get("op").get("id").get("userId").get("value").asText());
        assertEquals(1, aliceUndo.get("payload").get("op").get("id").get("clock").asLong());
    }

    @Test
    void redoAfterUndoingDeleteCharBroadcastsDeleteCharAgain() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-delete-redo", "alice", "Alice");
        join(context, bobSession, "room-delete-redo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendDeleteCharWithMetadata(context, aliceSession, "alice-delete-1", "alice", 1, "z");
        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"REDO_REQUEST"}
                """));

        JsonNode aliceRedo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        JsonNode bobRedo = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(aliceRedo);
        assertNotNull(bobRedo);
        assertEquals("DELETE_CHAR", aliceRedo.get("operationKind").asText());
        assertEquals("DELETE_CHAR", bobRedo.get("operationKind").asText());
        assertEquals("z", aliceRedo.get("payload").get("op").get("value").asText());
    }

    @Test
    void undoDeleteCharWithMissingMetadataReturnsUnsupportedError() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");

        join(context, aliceSession, "room-delete-missing-metadata", "alice", "Alice");
        aliceSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"operationId":"old-delete","operationKind":"DELETE_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"alice"},"clock":1}}},"messageType":"OPERATION"}
                """));
        aliceSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode errorNode = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR);
        assertNotNull(errorNode);
        assertEquals("UNDO_NOT_SUPPORTED_FOR_OPERATION", errorNode.get("errorCode").asText());
    }

    @Test
    void undoAfterApplyBoldRestoresPreviousPerCharacterValues() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-format-undo", "alice", "Alice");
        join(context, bobSession, "room-format-undo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendFormatWithMetadata(context, aliceSession, "format-bold-1", "APPLY_FORMAT", "BOLD", true);
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode aliceUndo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        JsonNode bobUndo = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(aliceUndo);
        assertNotNull(bobUndo);
        assertEquals("REMOVE_FORMAT", aliceUndo.get("operationKind").asText());
        assertEquals("REMOVE_FORMAT", bobUndo.get("operationKind").asText());
        assertEquals("BOLD", aliceUndo.get("payload").get("format").asText());
        assertEquals(false, aliceUndo.get("payload").get("affectedChars").get(0).get("newValue").asBoolean());
        assertEquals(true, aliceUndo.get("payload").get("affectedChars").get(1).get("newValue").asBoolean());
        assertEquals(true, aliceUndo.get("payload").get("affectedChars").get(0).get("previousValue").asBoolean());
    }

    @Test
    void redoAfterUndoingFormatReappliesOriginalValues() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-format-redo", "alice", "Alice");
        join(context, bobSession, "room-format-redo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendFormatWithMetadata(context, aliceSession, "format-redo-1", "APPLY_FORMAT", "BOLD", true);
        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"REDO_REQUEST"}
                """));

        JsonNode aliceRedo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        JsonNode bobRedo = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(aliceRedo);
        assertNotNull(bobRedo);
        assertEquals("APPLY_FORMAT", aliceRedo.get("operationKind").asText());
        assertEquals("APPLY_FORMAT", bobRedo.get("operationKind").asText());
        assertEquals(true, aliceRedo.get("payload").get("affectedChars").get(0).get("newValue").asBoolean());
        assertEquals(false, aliceRedo.get("payload").get("affectedChars").get(0).get("previousValue").asBoolean());
    }

    @Test
    void undoAfterApplyItalicUsesFormattingMetadata() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");

        join(context, aliceSession, "room-format-italic", "alice", "Alice");
        aliceSession.clearSentTextPayloads();

        sendFormatWithMetadata(context, aliceSession, "format-italic-1", "APPLY_FORMAT", "ITALIC", true);
        aliceSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode undo = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(undo);
        assertEquals("REMOVE_FORMAT", undo.get("operationKind").asText());
        assertEquals("ITALIC", undo.get("payload").get("format").asText());
        assertEquals(false, undo.get("payload").get("affectedChars").get(0).get("newValue").asBoolean());
    }

    @Test
    void undoFormatWithMissingMetadataReturnsUnsupportedError() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");

        join(context, aliceSession, "room-format-missing-metadata", "alice", "Alice");
        aliceSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"operationId":"old-format","operationKind":"APPLY_FORMAT","payload":{"startOffset":0,"endOffset":1,"format":"BOLD"},"messageType":"OPERATION"}
                """));
        aliceSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode errorNode = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR);
        assertNotNull(errorNode);
        assertEquals("UNDO_NOT_SUPPORTED_FOR_OPERATION", errorNode.get("errorCode").asText());
    }

    @Test
    void newNormalOperationClearsRedoStack() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");

        join(context, aliceSession, "room-redo-clear", "alice", "Alice");
        aliceSession.clearSentTextPayloads();

        sendInsertChar(context, aliceSession, "alice-op-1", "alice", 1, "a");
        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));
        assertEquals(1, context.sessionRegistry.getSession("room-redo-clear").orElseThrow().getRedoStackSize());

        sendInsertChar(context, aliceSession, "alice-op-2", "alice", 2, "b");

        assertEquals(0, context.sessionRegistry.getSession("room-redo-clear").orElseThrow().getRedoStackSize());
    }

    @Test
    void undoUsesLatestDocumentOperationEvenWhenRequesterDidNotCreateIt() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        join(context, aliceSession, "room-document-undo", "alice", "Alice");
        join(context, bobSession, "room-document-undo", "bob", "Bob");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        sendInsertChar(context, aliceSession, "alice-op-1", "alice", 1, "a");
        sendInsertChar(context, bobSession, "bob-op-1", "bob", 2, "b");
        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"messageType":"UNDO_REQUEST"}
                """));

        JsonNode undoOperation = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(undoOperation);
        assertEquals("DELETE_CHAR", undoOperation.get("operationKind").asText());
        assertEquals("bob", undoOperation.get("payload").get("op").get("id").get("userId").get("value").asText());
        assertEquals(2, undoOperation.get("payload").get("op").get("id").get("clock").asLong());
    }

    @Test
    void rejectsViewerEditOperations() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession editorSession = new FakeWebSocketSession("ws-editor");
        FakeWebSocketSession viewerSession = new FakeWebSocketSession("ws-viewer");

        context.handler.handleTextMessage(editorSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-2", new UserId("editor"), "Editor", SessionRole.EDITOR)
                )
        ));
        context.handler.handleTextMessage(viewerSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-2", new UserId("viewer"), "Viewer", SessionRole.EDITOR)
                )
        ));

        editorSession.clearSentTextPayloads();
        viewerSession.clearSentTextPayloads();

        context.handler.handleTextMessage(viewerSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new OperationMessage("room-2", new UserId("viewer"), "DELETE_TEXT", "{\"count\":1}")
                )
        ));

        assertFalse(hasMessageType(editorSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION));

        JsonNode errorNode = firstMessageByType(viewerSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR);
        assertNotNull(errorNode);
        assertEquals("PERMISSION_DENIED", errorNode.get("errorCode").asText());
    }

    @Test
    void editorCanEditEvenIfJoinRequestClaimsViewerRole() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession editorSession = new FakeWebSocketSession("ws-editor");
        FakeWebSocketSession viewerSession = new FakeWebSocketSession("ws-viewer");

        context.handler.handleTextMessage(editorSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-2", new UserId("editor"), "Editor", SessionRole.VIEWER)
                )
        ));
        context.handler.handleTextMessage(viewerSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-2", new UserId("viewer"), "Viewer", SessionRole.VIEWER)
                )
        ));

        editorSession.clearSentTextPayloads();
        viewerSession.clearSentTextPayloads();

        context.handler.handleTextMessage(editorSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new OperationMessage("room-2", new UserId("editor"), "INSERT_TEXT", "{\"text\":\"ok\"}")
                )
        ));

        assertFalse(hasMessageType(editorSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        assertHasMessageType(viewerSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
    }

    @Test
    void rejectsUnknownUserForDocumentBackedWebSocketSession() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession unknownSession = new FakeWebSocketSession("ws-unknown");

        context.handler.handleTextMessage(unknownSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-1", new UserId("unknown"), "Unknown", SessionRole.EDITOR)
                )
        ));

        JsonNode errorNode = firstMessageByType(unknownSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR);
        assertNotNull(errorNode);
        assertEquals("PERMISSION_DENIED", errorNode.get("errorCode").asText());
    }

    @Test
    void rejectsDeletedDocumentWebSocketSession() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("deleted-room", new UserId("alice"), "Alice", SessionRole.EDITOR)
                )
        ));

        JsonNode errorNode = firstMessageByType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR);
        assertNotNull(errorNode);
        assertEquals("DOCUMENT_NOT_FOUND", errorNode.get("errorCode").asText());
    }

    @Test
    void relaysInsertBlockOperationsAcrossTheWebSocketSession() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-blocks", new UserId("alice"), "Alice")
                )
        ));
        context.handler.handleTextMessage(bobSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-blocks", new UserId("bob"), "Bob")
                )
        ));

        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        InsertBlockOp insertBlockOp = new InsertBlockOp(
                new BlockId(new UserId("alice"), 5),
                new BlockId(new UserId("SYSTEM"), 0)
        );
        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new OperationMessage(
                                "room-blocks",
                                new UserId("alice"),
                                "INSERT_BLOCK",
                                context.objectMapper.writeValueAsString(insertBlockOp)
                        )
                )
        ));

        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        JsonNode relayedOperation = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(relayedOperation);
        assertEquals("INSERT_BLOCK", relayedOperation.get("operationKind").asText());

        InsertBlockOp relayedPayload = context.objectMapper.readValue(
                relayedOperation.get("payload").toString(),
                InsertBlockOp.class
        );
        assertEquals(insertBlockOp.id(), relayedPayload.id());
        assertEquals(insertBlockOp.parentId(), relayedPayload.parentId());
    }

    @Test
    void relaysOperationsUsingJoinedSessionIdentityInsteadOfRejectingStaleClientIdentity() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-identity", new UserId("alice"), "Alice")
                )
        ));
        context.handler.handleTextMessage(bobSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-identity", new UserId("bob"), "Bob")
                )
        ));

        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage("""
                {"sessionId":"stale-room","senderId":{"value":"stale-user"},"operationKind":"INSERT_CHAR","payload":"{\\"blockId\\":{\\"userId\\":{\\"value\\":\\"SYSTEM\\"},\\"clock\\":0},\\"op\\":{\\"id\\":{\\"userId\\":{\\"value\\":\\"alice\\"},\\"clock\\":1},\\"parentId\\":null,\\"value\\":\\"x\\"}}","messageType":"OPERATION"}
                """));

        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        JsonNode relayedOperation = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertNotNull(relayedOperation);
        assertEquals("room-identity", relayedOperation.get("sessionId").asText());
        assertEquals("alice", relayedOperation.get("senderId").get("value").asText());
        assertEquals("INSERT_CHAR", relayedOperation.get("operationKind").asText());
        assertTrue(relayedOperation.get("payload").isObject());
        assertEquals("x", relayedOperation.get("payload").get("op").get("value").asText());
    }

    @Test
    void replaysOperationHistoryToLateJoinersInTheSameRoom() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-history", new UserId("alice"), "Alice")
                )
        ));
        aliceSession.clearSentTextPayloads();

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new OperationMessage("room-history", new UserId("alice"), "INSERT_BLOCK", "{\"block\":1}")
                )
        ));
        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new OperationMessage("room-history", new UserId("alice"), "INSERT_CHAR", "{\"char\":\"a\"}")
                )
        ));

        aliceSession.clearSentTextPayloads();

        context.handler.handleTextMessage(bobSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-history", new UserId("bob"), "Bob")
                )
        ));

        List<JsonNode> bobOperations = messagesByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.OPERATION);
        assertEquals(2, bobOperations.size());
        assertEquals("INSERT_BLOCK", bobOperations.get(0).get("operationKind").asText());
        assertEquals("INSERT_CHAR", bobOperations.get(1).get("operationKind").asText());
    }

    @Test
    void deserializesExactCursorUpdateJsonWithNullBlockId() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String rawJson = """
                {"sessionId":"demo-room","userId":{"value":"user638"},"blockId":null,"characterOffset":0,"messageType":"CURSOR_UPDATE"}
                """;

        CursorUpdateMessage cursorUpdateMessage = objectMapper.readValue(rawJson, CursorUpdateMessage.class);

        assertEquals("demo-room", cursorUpdateMessage.getSessionId());
        assertEquals(new UserId("user638"), cursorUpdateMessage.getUserId());
        assertNull(cursorUpdateMessage.getBlockId());
        assertEquals(0, cursorUpdateMessage.getCharacterOffset());
    }

    @Test
    void relaysExactCursorUpdateJsonWithoutReturningParseErrors() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-3", new UserId("alice"), "Alice")
                )
        ));
        context.handler.handleTextMessage(bobSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-3", new UserId("bob"), "Bob")
                )
        ));

        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        String rawJson = """
                {"sessionId":"demo-room","userId":{"value":"user638"},"blockId":null,"characterOffset":0,"messageType":"CURSOR_UPDATE"}
                """;
        context.handler.handleTextMessage(aliceSession, new TextMessage(rawJson));

        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        JsonNode relayedCursor = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.CURSOR_UPDATE);
        assertNotNull(relayedCursor);
        assertEquals("room-3", relayedCursor.get("sessionId").asText());
        assertEquals("alice", relayedCursor.get("userId").get("value").asText());
        assertNull(relayedCursor.get("blockId"));
        assertEquals(0, relayedCursor.get("characterOffset").asInt());
    }

    @Test
    void acceptsOffsetOnlyCursorUpdatesAndOverwritesMalformedOptionalIdentityFields() throws Exception {
        TestContext context = createContext();
        FakeWebSocketSession aliceSession = new FakeWebSocketSession("ws-alice");
        FakeWebSocketSession bobSession = new FakeWebSocketSession("ws-bob");

        context.handler.handleTextMessage(aliceSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-4", new UserId("alice"), "Alice")
                )
        ));
        context.handler.handleTextMessage(bobSession, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest("room-4", new UserId("bob"), "Bob")
                )
        ));

        aliceSession.clearSentTextPayloads();
        bobSession.clearSentTextPayloads();

        String rawJson = """
                {"sessionId":{"unexpected":true},"userId":"broken","characterOffset":7,"messageType":"CURSOR_UPDATE","extra":"ignored"}
                """;
        context.handler.handleTextMessage(aliceSession, new TextMessage(rawJson));

        assertFalse(hasMessageType(aliceSession.getSentTextPayloads(), context.objectMapper, MessageType.ERROR));
        JsonNode relayedCursor = firstMessageByType(bobSession.getSentTextPayloads(), context.objectMapper, MessageType.CURSOR_UPDATE);
        assertNotNull(relayedCursor);
        assertEquals("room-4", relayedCursor.get("sessionId").asText());
        assertEquals("alice", relayedCursor.get("userId").get("value").asText());
        assertEquals(7, relayedCursor.get("characterOffset").asInt());
    }

    private TestContext createContext() {
        ObjectMapper objectMapper = new ObjectMapper();
        MessageValidator messageValidator = new MessageValidator();
        SessionRegistry sessionRegistry = new SessionRegistry();
        PermissionService permissionService = new PermissionService();
        DocumentAccessService documentAccessService = new DocumentAccessService();
        InMemoryDocumentRepository documentRepository = new InMemoryDocumentRepository();
        seedDocuments(documentRepository);
        SessionJoinService sessionJoinService = new SessionJoinService(
                sessionRegistry,
                messageValidator,
                permissionService,
                documentRepository,
                documentAccessService
        );
        OperationRelayService operationRelayService = new OperationRelayService(objectMapper);
        UndoRedoService undoRedoService = new UndoRedoService(permissionService, operationRelayService, objectMapper);
        PresenceService presenceService = new PresenceService(objectMapper);
        CollaborationWebSocketHandler handler = new CollaborationWebSocketHandler(
                objectMapper,
                messageValidator,
                sessionJoinService,
                operationRelayService,
                undoRedoService,
                presenceService,
                permissionService
        );

        return new TestContext(objectMapper, sessionRegistry, handler);
    }

    private void seedDocuments(InMemoryDocumentRepository repository) {
        saveDocument(repository, "room-1", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-2", "owner", List.of("editor"), List.of("viewer"));
        saveDocument(repository, "room-blocks", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-identity", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-history", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-3", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-4", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-undo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-undo-viewer", "owner", List.of(), List.of("viewer"));
        saveDocument(repository, "room-empty-undo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-empty-redo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-redo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-redo-clear", "alice", List.of(), List.of());
        saveDocument(repository, "room-document-undo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-delete-undo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-delete-redo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-delete-missing-metadata", "alice", List.of(), List.of());
        saveDocument(repository, "room-formatted-insert-redo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-format-undo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-format-redo", "alice", List.of("bob"), List.of());
        saveDocument(repository, "room-format-italic", "alice", List.of(), List.of());
        saveDocument(repository, "room-format-missing-metadata", "alice", List.of(), List.of());

        MongoDocumentRecord deletedDocument = createDocument("deleted-room", "alice", List.of(), List.of());
        deletedDocument.setDeleted(true);
        repository.save(deletedDocument);
    }

    private void saveDocument(
            InMemoryDocumentRepository repository,
            String documentId,
            String ownerId,
            List<String> editorUserIds,
            List<String> viewerUserIds
    ) {
        repository.save(createDocument(documentId, ownerId, editorUserIds, viewerUserIds));
    }

    private MongoDocumentRecord createDocument(
            String documentId,
            String ownerId,
            List<String> editorUserIds,
            List<String> viewerUserIds
    ) {
        MongoDocumentRecord document = new MongoDocumentRecord(
                documentId,
                "Test Document",
                new UserId(ownerId),
                "EDT-" + documentId,
                "VWR-" + documentId,
                null,
                "2026-05-04T00:00:00Z",
                "2026-05-04T00:00:00Z",
                false
        );

        for (String editorUserId : editorUserIds) {
            document.addEditorUserId(editorUserId);
        }

        for (String viewerUserId : viewerUserIds) {
            document.addViewerUserId(viewerUserId);
        }

        return document;
    }

    private void assertHasMessageType(List<String> payloads, ObjectMapper objectMapper, MessageType messageType) throws Exception {
        assertTrue(hasMessageType(payloads, objectMapper, messageType));
    }

    private void join(TestContext context, FakeWebSocketSession session, String roomId, String userId, String displayName)
            throws Exception {
        context.handler.handleTextMessage(session, new TextMessage(
                context.objectMapper.writeValueAsString(
                        new JoinSessionRequest(roomId, new UserId(userId), displayName)
                )
        ));
    }

    private void sendInsertChar(
            TestContext context,
            FakeWebSocketSession session,
            String operationId,
            String charUserId,
            long charClock,
            String value
    ) {
        String rawJson = """
                {"operationId":"%s","operationKind":"INSERT_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"%s"},"clock":%d},"parentId":null,"value":"%s"}},"messageType":"OPERATION"}
                """.formatted(operationId, charUserId, charClock, value);
        context.handler.handleTextMessage(session, new TextMessage(rawJson));
    }

    private void sendFormattedInsertChar(
            TestContext context,
            FakeWebSocketSession session,
            String operationId,
            String charUserId,
            long charClock,
            String value
    ) {
        String rawJson = """
                {"operationId":"%s","operationKind":"INSERT_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"%s"},"clock":%d},"parentId":null,"value":"%s","formatState":{"bold":true,"italic":false,"underline":false}}},"messageType":"OPERATION"}
                """.formatted(operationId, charUserId, charClock, value);
        context.handler.handleTextMessage(session, new TextMessage(rawJson));
    }

    private void sendDeleteCharWithMetadata(
            TestContext context,
            FakeWebSocketSession session,
            String operationId,
            String charUserId,
            long charClock,
            String value
    ) {
        String rawJson = """
                {"operationId":"%s","operationKind":"DELETE_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"%s"},"clock":%d},"parentId":null,"value":"%s"}},"messageType":"OPERATION"}
                """.formatted(operationId, charUserId, charClock, value);
        context.handler.handleTextMessage(session, new TextMessage(rawJson));
    }

    private void sendFormatWithMetadata(
            TestContext context,
            FakeWebSocketSession session,
            String operationId,
            String operationKind,
            String format,
            boolean newValue
    ) {
        String rawJson = """
                {"operationId":"%s","operationKind":"%s","payload":{"startOffset":0,"endOffset":2,"format":"%s","newValue":%s,"affectedChars":[{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"charId":{"userId":{"value":"alice"},"clock":1},"previousValue":false,"newValue":%s},{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"charId":{"userId":{"value":"alice"},"clock":2},"previousValue":true,"newValue":%s}]},"messageType":"OPERATION"}
                """.formatted(operationId, operationKind, format, newValue, newValue, newValue);
        context.handler.handleTextMessage(session, new TextMessage(rawJson));
    }

    private boolean hasMessageType(List<String> payloads, ObjectMapper objectMapper, MessageType messageType) throws Exception {
        return firstMessageByType(payloads, objectMapper, messageType) != null;
    }

    private JsonNode firstMessageByType(List<String> payloads, ObjectMapper objectMapper, MessageType messageType) throws Exception {
        List<JsonNode> messages = messagesByType(payloads, objectMapper, messageType);
        return messages.isEmpty() ? null : messages.get(0);
    }

    private List<JsonNode> messagesByType(List<String> payloads, ObjectMapper objectMapper, MessageType messageType) throws Exception {
        List<JsonNode> matchingMessages = new ArrayList<>();
        for (String payload : payloads) {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode messageTypeNode = node.get("messageType");
            if (messageTypeNode != null && messageType.name().equals(messageTypeNode.asText())) {
                matchingMessages.add(node);
            }
        }
        return matchingMessages;
    }

    private void assertPresenceEvent(List<String> payloads, ObjectMapper objectMapper, String userId, String eventType) throws Exception {
        JsonNode presenceNode = firstMessageByType(payloads, objectMapper, MessageType.PRESENCE_UPDATE);
        assertNotNull(presenceNode);
        assertEquals(userId, presenceNode.get("userId").get("value").asText());
        assertEquals(eventType, presenceNode.get("eventType").asText());
    }

    private record TestContext(
            ObjectMapper objectMapper,
            SessionRegistry sessionRegistry,
            CollaborationWebSocketHandler handler
    ) {
    }

    private static final class FakeWebSocketSession implements WebSocketSession {
        private final String id;
        private final URI uri = URI.create("ws://localhost/ws/collab");
        private final HttpHeaders handshakeHeaders = new HttpHeaders();
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private final List<String> sentTextPayloads = new ArrayList<>();
        private boolean open = true;
        private int textMessageSizeLimit = 65536;
        private int binaryMessageSizeLimit = 65536;

        private FakeWebSocketSession(String id) {
            this.id = id;
        }

        private List<String> getSentTextPayloads() {
            return sentTextPayloads;
        }

        private void clearSentTextPayloads() {
            sentTextPayloads.clear();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return handshakeHeaders;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
            this.textMessageSizeLimit = messageSizeLimit;
        }

        @Override
        public int getTextMessageSizeLimit() {
            return textMessageSizeLimit;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
            this.binaryMessageSizeLimit = messageSizeLimit;
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return binaryMessageSizeLimit;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) throws IOException {
            if (message instanceof TextMessage textMessage) {
                sentTextPayloads.add(textMessage.getPayload());
                return;
            }

            if (message instanceof BinaryMessage || message instanceof PingMessage || message instanceof PongMessage) {
                throw new IOException("Only text messages are supported by this fake test session.");
            }
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public void close(CloseStatus status) {
            open = false;
        }
    }
}
