package com.team.editor.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.model.UserId;
import com.team.editor.common.ops.block.InsertBlockOp;
import com.team.editor.common.ops.character.DeleteCharOp;
import com.team.editor.common.ops.character.InsertCharOp;
import com.team.editor.common.ops.character.RestoreCharOp;
import com.team.editor.common.protocol.dto.BlockOpPayload;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionResponse;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.dto.PresenceUpdateMessage;
import com.team.editor.common.protocol.dto.RedoRequestMessage;
import com.team.editor.common.protocol.dto.UndoRequestMessage;
import com.team.editor.common.protocol.message.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClientWebSocketHandlerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesFullJoinResponseThroughTheActualReceivePath() {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","accepted":true,"assignedUserId":{"value":"user638"},"message":"Joined session successfully.","messageType":"JOIN_SESSION_RESPONSE"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.joinSessionResponse);
        assertEquals("demo-room", listener.joinSessionResponse.getSessionId());
        assertEquals(new UserId("user638"), listener.joinSessionResponse.getAssignedUserId());
        assertEquals("Joined session successfully.", listener.joinSessionResponse.getMessage());
    }

    @Test
    void parsesPresenceUpdatesThatIncludeMessageType() {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","userId":{"value":"omar 1"},"displayName":"omar 1","presence":"JOINED","eventType":"JOINED","messageType":"PRESENCE_UPDATE"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.presenceUpdateMessage);
        assertEquals("demo-room", listener.presenceUpdateMessage.getSessionId());
        assertEquals(new UserId("omar 1"), listener.presenceUpdateMessage.getUserId());
        assertEquals("omar 1", listener.presenceUpdateMessage.getDisplayName());
        assertEquals("JOINED", listener.presenceUpdateMessage.getPresence());
        assertEquals("JOINED", listener.presenceUpdateMessage.getEventType().name());
    }

    @Test
    void dispatchesInsertCharOperationMessagesThroughTheActualReceivePath() throws Exception {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","senderId":{"value":"omar1"},"operationKind":"INSERT_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"omar1"},"clock":1},"parentId":null,"value":"a"}},"messageType":"OPERATION"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.operationMessage);
        assertEquals("demo-room", listener.operationMessage.getSessionId());
        assertEquals(new UserId("omar1"), listener.operationMessage.getSenderId());
        assertEquals("INSERT_CHAR", listener.operationMessage.getOperationKind());

        BlockOpPayload<InsertCharOp> payload = mapper.readValue(
                mapper.treeAsTokens(listener.operationMessage.getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, InsertCharOp.class)
        );
        assertEquals(new BlockId(new UserId("SYSTEM"), 0), payload.getBlockId());
        assertEquals(new CharId(new UserId("omar1"), 1), payload.getOp().id());
        assertEquals('a', payload.getOp().value());
    }

    @Test
    void dispatchesDeleteCharOperationMessagesThroughTheActualReceivePath() throws Exception {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","senderId":{"value":"omar1"},"operationKind":"DELETE_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"omar1"},"clock":2}}},"messageType":"OPERATION"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.operationMessage);
        BlockOpPayload<DeleteCharOp> payload = mapper.readValue(
                mapper.treeAsTokens(listener.operationMessage.getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, DeleteCharOp.class)
        );
        assertEquals(new BlockId(new UserId("SYSTEM"), 0), payload.getBlockId());
        assertEquals(new CharId(new UserId("omar1"), 2), payload.getOp().id());
    }

    @Test
    void dispatchesRestoreCharOperationMessagesThroughTheActualReceivePath() throws Exception {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","senderId":{"value":"SERVER"},"operationKind":"RESTORE_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"omar1"},"clock":2}}},"messageType":"OPERATION"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.operationMessage);
        BlockOpPayload<RestoreCharOp> payload = mapper.readValue(
                mapper.treeAsTokens(listener.operationMessage.getPayload()),
                mapper.getTypeFactory().constructParametricType(BlockOpPayload.class, RestoreCharOp.class)
        );
        assertEquals(new BlockId(new UserId("SYSTEM"), 0), payload.getBlockId());
        assertEquals(new CharId(new UserId("omar1"), 2), payload.getOp().id());
    }

    @Test
    void dispatchesInsertBlockOperationMessagesThroughTheActualReceivePath() throws Exception {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","senderId":{"value":"omar1"},"operationKind":"INSERT_BLOCK","payload":{"id":{"userId":{"value":"omar1"},"clock":3},"parentId":{"userId":{"value":"SYSTEM"},"clock":0}},"messageType":"OPERATION"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.operationMessage);
        InsertBlockOp payload = mapper.treeToValue(listener.operationMessage.getPayload(), InsertBlockOp.class);
        assertEquals(new BlockId(new UserId("omar1"), 3), payload.id());
        assertEquals(new BlockId(new UserId("SYSTEM"), 0), payload.parentId());
    }

    @Test
    void parsesExactCursorUpdateJsonThroughTheActualReceivePath() {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","userId":{"value":"user638"},"blockId":null,"characterOffset":0,"messageType":"CURSOR_UPDATE"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.cursorUpdateMessage);
        assertEquals("demo-room", listener.cursorUpdateMessage.getSessionId());
        assertEquals(new UserId("user638"), listener.cursorUpdateMessage.getUserId());
        assertNull(listener.cursorUpdateMessage.getBlockId());
        assertEquals(0, listener.cursorUpdateMessage.getCharacterOffset());
    }

    @Test
    void parsesCursorUpdatesWithUnknownFieldsAndNestedExtraProperties() {
        ClientWebSocketHandler handler = new ClientWebSocketHandler();
        CapturingListener listener = new CapturingListener();
        handler.setListener(listener);

        String json = """
                {"sessionId":"demo-room","userId":{"value":"user638","ignored":"yes"},"blockId":null,"characterOffset":3,"messageType":"CURSOR_UPDATE","extra":"ignored"}
                """;

        handler.handleIncoming(json);

        assertNotNull(listener.cursorUpdateMessage);
        assertEquals("demo-room", listener.cursorUpdateMessage.getSessionId());
        assertEquals(new UserId("user638"), listener.cursorUpdateMessage.getUserId());
        assertNull(listener.cursorUpdateMessage.getBlockId());
        assertEquals(3, listener.cursorUpdateMessage.getCharacterOffset());
    }

    @Test
    void serializesUndoRequestMessageWithFlatMessageType() throws Exception {
        String json = mapper.writeValueAsString(new UndoRequestMessage());

        assertEquals(
                MessageType.UNDO_REQUEST.name(),
                mapper.readTree(json).get("messageType").asText()
        );
    }

    @Test
    void serializesRedoRequestMessageWithFlatMessageType() throws Exception {
        String json = mapper.writeValueAsString(new RedoRequestMessage());

        assertEquals(
                MessageType.REDO_REQUEST.name(),
                mapper.readTree(json).get("messageType").asText()
        );
    }

    private static class CapturingListener implements ClientWebSocketHandler.Listener {
        private JoinSessionResponse joinSessionResponse;
        private PresenceUpdateMessage presenceUpdateMessage;
        private OperationMessage operationMessage;
        private CursorUpdateMessage cursorUpdateMessage;

        @Override
        public void onJoinSessionResponse(JoinSessionResponse response) {
            this.joinSessionResponse = response;
        }

        @Override
        public void onError(ErrorMessage errorMessage) {
        }

        @Override
        public void onPresenceUpdate(PresenceUpdateMessage message) {
            this.presenceUpdateMessage = message;
        }

        @Override
        public void onCursorUpdate(CursorUpdateMessage message) {
            this.cursorUpdateMessage = message;
        }

        @Override
        public void onOperation(OperationMessage message) {
            this.operationMessage = message;
        }
    }
}
