package com.team.editor.client.controller;

import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionResponse;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.dto.PresenceUpdateMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionControllerTest {

    @Test
    void joinResponseWithMessageTypeAdvancesSessionState() {
        SessionController controller = new SessionController();

        controller.joinSession("demo-room", "user77");
        controller.simulateIncomingMessage("""
                {"sessionId":"demo-room","accepted":true,"assignedUserId":{"value":"user77"},"message":"Joined session successfully.","messageType":"JOIN_SESSION_RESPONSE"}
                """);

        assertEquals("JOINED", controller.getConnectionState().name());
        assertNotNull(controller.getSession());
        assertEquals("demo-room", controller.getSession().getSessionId());
        assertEquals(new UserId("user77"), controller.getSession().getUserId());
    }

    @Test
    void operationMessagesAreForwardedToSessionListeners() {
        SessionController controller = new SessionController();
        CapturingSessionListener listener = new CapturingSessionListener();
        controller.addListener(listener);

        controller.simulateIncomingMessage("""
                {"sessionId":"demo-room","senderId":{"value":"omar1"},"operationKind":"INSERT_CHAR","payload":{"blockId":{"userId":{"value":"SYSTEM"},"clock":0},"op":{"id":{"userId":{"value":"omar1"},"clock":1},"parentId":null,"value":"a"}},"messageType":"OPERATION"}
                """);

        assertNotNull(listener.operationMessage);
        assertEquals("demo-room", listener.operationMessage.getSessionId());
        assertEquals(new UserId("omar1"), listener.operationMessage.getSenderId());
        assertEquals("INSERT_CHAR", listener.operationMessage.getOperationKind());
    }

    @Test
    void mapsDocumentRolesToWebSocketRoles() {
        assertEquals(
                com.team.editor.common.protocol.message.SessionRole.EDITOR,
                SessionController.mapDocumentRoleToProtocolRole("OWNER")
        );
        assertEquals(
                com.team.editor.common.protocol.message.SessionRole.EDITOR,
                SessionController.mapDocumentRoleToProtocolRole("EDITOR")
        );
        assertEquals(
                com.team.editor.common.protocol.message.SessionRole.VIEWER,
                SessionController.mapDocumentRoleToProtocolRole("VIEWER")
        );
    }

    private static final class CapturingSessionListener implements SessionController.SessionListener {
        private OperationMessage operationMessage;

        @Override
        public void onConnectionStateChanged(com.team.editor.client.state.ConnectionState state) {
        }

        @Override
        public void onJoinResponse(JoinSessionResponse response) {
        }

        @Override
        public void onPresenceUpdate(PresenceUpdateMessage message) {
        }

        @Override
        public void onCursorUpdate(CursorUpdateMessage message) {
        }

        @Override
        public void onOperation(OperationMessage message) {
            this.operationMessage = message;
        }

        @Override
        public void onError(ErrorMessage errorMessage) {
        }
    }
}
