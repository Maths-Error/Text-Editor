package com.team.editor.server.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionRequest;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;
import com.team.editor.server.service.OperationRelayService;
import com.team.editor.server.service.PermissionService;
import com.team.editor.server.service.PresenceService;
import com.team.editor.server.service.SessionJoinService;
import com.team.editor.server.service.UndoRedoService;
import com.team.editor.server.service.UndoRedoService.UndoRedoResult;
import com.team.editor.server.validation.MessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * Handles websocket lifecycle events and routes collaboration messages to the appropriate services.
 */
@Component
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CollaborationWebSocketHandler.class);
    private static final String CLEANUP_MARKER = CollaborationWebSocketHandler.class.getName() + ".cleanupCompleted";
    private static final int MAX_LOGGED_PAYLOAD_LENGTH = 500;
    private final ObjectMapper objectMapper;
    private final MessageValidator messageValidator;
    private final SessionJoinService sessionJoinService;
    private final OperationRelayService operationRelayService;
    private final UndoRedoService undoRedoService;

    
    private final PresenceService presenceService;
    private final PermissionService permissionService;

    public CollaborationWebSocketHandler(
            ObjectMapper objectMapper,
            MessageValidator messageValidator,
            SessionJoinService sessionJoinService,
            OperationRelayService operationRelayService,
            UndoRedoService undoRedoService,
            PresenceService presenceService,
            PermissionService permissionService
    ) {
        this.objectMapper = objectMapper;
        this.messageValidator = messageValidator;
        this.sessionJoinService = sessionJoinService;
        this.operationRelayService = operationRelayService;
        this.undoRedoService = undoRedoService;
        this.presenceService = presenceService;
        this.permissionService = permissionService;
    }

    /**
     * Called automatically by Spring when a new websocket connection is opened on /ws/collab.
     * Connection setup is transport-level only; session membership is established after a join request succeeds.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connected: sessionId={}, remoteAddress={}", session.getId(), session.getRemoteAddress());
    }

    /**
     * Called automatically by Spring whenever a websocket client sends a text frame.
     * This is the main entry point for live collaboration messages: it parses JSON, checks messageType,
     * routes join and operation messages, and rejects malformed or unsupported payloads.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info(
                "WebSocket message received: sessionId={}, payloadLength={}, payload={}",
                session.getId(),
                payload.length(),
                abbreviatePayload(payload)
        );

        MessageValidator.ValidationResult<JsonNode> payloadValidation = messageValidator.parseJson(payload, objectMapper);
        if (!payloadValidation.isValid()) {
            sendErrorResponse(session, payloadValidation.errorMessage(), "Rejected malformed JSON payload");
            return;
        }

        JsonNode payloadNode = payloadValidation.value();
        MessageValidator.ValidationResult<MessageType> messageTypeValidation =
                messageValidator.validateMessageType(payloadNode);
        if (!messageTypeValidation.isValid()) {
            sendErrorResponse(session, messageTypeValidation.errorMessage(), "Rejected payload with invalid message type");
            return;
        }

        switch (messageTypeValidation.value()) {
            case JOIN_SESSION_REQUEST -> handleJoinSessionRequest(session, payloadNode);
            case OPERATION -> handleOperationMessage(session, payloadNode);
            case UNDO_REQUEST -> handleUndoRequest(session, payloadNode);
            case REDO_REQUEST -> handleRedoRequest(session, payloadNode);
            case CURSOR_UPDATE -> handleCursorUpdate(session, payloadNode);
            default -> sendErrorResponse(
                    session,
                    new ErrorMessage(
                            "UNSUPPORTED_MESSAGE_TYPE",
                            "Message type '" + messageTypeValidation.value() + "' is not supported yet."
                    ),
                    "Rejected unsupported message type"
            );
        }
    }

    /**
     * Called by Spring when the websocket transport itself fails.
     * The handler closes the socket if needed and then runs the same cleanup path used for normal disconnects.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn(
                "WebSocket transport error: sessionId={}, remoteAddress={}, message={}",
                session.getId(),
                session.getRemoteAddress(),
                exception.getMessage(),
                exception
        );

        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException closeException) {
            log.warn("Failed to close errored WebSocket session: sessionId={}", session.getId(), closeException);
        } finally {
            cleanupDisconnectedSession(session, "transport error");
        }
    }

    /**
     * Called automatically when the websocket closes normally.
     * After logging, it removes the joined user from the in-memory room and broadcasts a leave update when needed.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info(
                "WebSocket disconnected: sessionId={}, remoteAddress={}, code={}, reason={}",
                session.getId(),
                session.getRemoteAddress(),
                status.getCode(),
                status.getReason()
        );
        cleanupDisconnectedSession(session, "connection closed");
    }

    /**
     * Runs the shared disconnect cleanup path.
     * It removes the websocket from the session state once, broadcasts a leave presence update to the remaining users,
     * and lets the registry prune empty sessions.
     */
    private void cleanupDisconnectedSession(WebSocketSession session, String trigger) {
        Object existingMarker = session.getAttributes().putIfAbsent(CLEANUP_MARKER, Boolean.TRUE);
        if (existingMarker != null) {
            return;
        }

        SessionJoinService.DisconnectResult disconnectResult = sessionJoinService.cleanupDisconnectedSession(session);
        if (disconnectResult.hasDisconnectedUser()
                && disconnectResult.collaborationSession() != null
                && !disconnectResult.collaborationSession().isEmpty()) {
            presenceService.broadcastUserLeft(
                    disconnectResult.collaborationSession(),
                    disconnectResult.disconnectedUser()
            );
        }

        log.debug(
                "Disconnect cleanup completed: sessionId={}, trigger={}, roomRemoved={}",
                session.getId(),
                trigger,
                disconnectResult.sessionRemoved()
        );
    }

    /**
     * Handles join requests.
     * It validates the join JSON, deserializes it into JoinSessionRequest, calls the join service,
     * sends the join response to the same socket, and then notifies the other users in the room that someone joined.
     */
    private void handleJoinSessionRequest(WebSocketSession session, JsonNode payloadNode) {
        MessageValidator.ValidationResult<Void> joinPayloadValidation =
                messageValidator.validateJoinRequestPayload(payloadNode);
        if (!joinPayloadValidation.isValid()) {
            sendErrorResponse(session, joinPayloadValidation.errorMessage(), "Rejected invalid join request");
            return;
        }

        try {
            JoinSessionRequest joinSessionRequest = objectMapper.treeToValue(payloadNode, JoinSessionRequest.class);
            SessionJoinService.JoinResult joinResult = sessionJoinService.joinSession(joinSessionRequest, session);

            if (!joinResult.isAccepted()) {
                sendErrorResponse(session, joinResult.errorMessage(), "Rejected join request");
                return;
            }

            sendJson(session, joinResult.response());
            if (joinResult.collaborationSession() != null && joinResult.connectedUser() != null) {
                int replayedOperationCount = operationRelayService.replayOperationHistoryToUser(
                        joinResult.collaborationSession(),
                        joinResult.connectedUser()
                );
                presenceService.broadcastUserJoined(joinResult.collaborationSession(), joinResult.connectedUser());
                presenceService.sendExistingUsersToNewUser(joinResult.collaborationSession(), joinResult.connectedUser());

                log.info(
                        "Join sync completed: webSocketSessionId={}, collaborationSessionId={}, userId={}, historyOperationsSent={}, connectedUsers={}",
                        session.getId(),
                        joinResult.collaborationSession().getSessionId(),
                        joinResult.connectedUser().getUserId(),
                        replayedOperationCount,
                        joinResult.collaborationSession().getConnectedUsers().size()
                );
            }

            log.info(
                    "Join request accepted: webSocketSessionId={}, collaborationSessionId={}, userId={}",
                    session.getId(),
                    joinResult.response().getSessionId(),
                    joinResult.response().getAssignedUserId()
            );
        } catch (JsonProcessingException exception) {
            sendErrorResponse(
                    session,
                    new ErrorMessage("INVALID_JOIN_REQUEST", "Join request payload could not be parsed."),
                    "Rejected unparsable join request"
            );
        }
    }

    /**
     * Handles live operation messages.
     * It validates the raw and typed operation payloads, resolves which joined user sent the message,
     * checks edit permission, and relays the operation to the other users in the same room.
     */
    private void handleOperationMessage(WebSocketSession session, JsonNode payloadNode) {
        MessageValidator.ValidationResult<Void> operationPayloadValidation =
                messageValidator.validateOperationPayload(payloadNode);
        if (!operationPayloadValidation.isValid()) {
            sendErrorResponse(session, operationPayloadValidation.errorMessage(), "Rejected invalid operation payload");
            return;
        }

        try {
            OperationMessage operationMessage = objectMapper.treeToValue(payloadNode, OperationMessage.class);
            operationMessage.setPayload(normalizeOperationPayload(operationMessage.getPayload()));
            SessionJoinService.JoinedSessionContext joinedSessionContext = sessionJoinService.getJoinedSessionContext(session);
            if (joinedSessionContext == null || joinedSessionContext.collaborationSession() == null || joinedSessionContext.connectedUser() == null) {
                sendErrorResponse(
                        session,
                        new ErrorMessage(
                                "SESSION_NOT_JOINED",
                                "WebSocket client must join a collaboration session before sending operations."
                        ),
                        "Rejected operation from non-joined websocket"
                );
                return;
            }

            operationMessage.setSessionId(joinedSessionContext.collaborationSession().getSessionId());
            operationMessage.setSenderId(joinedSessionContext.connectedUser().getUserId());
            undoRedoService.ensureOperationId(operationMessage);

            log.info(
                    "RECEIVED OPERATION from {} in room {}: webSocketSessionId={}, rawPayload={}, normalizedPayload={}",
                    joinedSessionContext.connectedUser().getUserId(),
                    joinedSessionContext.collaborationSession().getSessionId(),
                    session.getId(),
                    abbreviatePayload(payloadNode.toString()),
                    abbreviatePayload(operationMessage.getPayload().toString())
            );

            MessageValidator.ValidationResult<OperationMessage> operationValidation =
                    messageValidator.validateOperationRequest(
                            operationMessage,
                            joinedSessionContext.collaborationSession(),
                            joinedSessionContext.connectedUser()
                    );
            if (!operationValidation.isValid()) {
                sendErrorResponse(session, operationValidation.errorMessage(), "Rejected operation message");
                return;
            }

            if (!permissionService.canEdit(joinedSessionContext.connectedUser())) {
                sendErrorResponse(
                        session,
                        new ErrorMessage(
                                "PERMISSION_DENIED",
                                "User role '" + joinedSessionContext.connectedUser().getRole() + "' cannot send edit operations."
                        ),
                        "Rejected operation because user cannot edit"
                );
                return;
            }

            OperationMessage inverseOperation = undoRedoService.createInverseOperation(operationMessage);
            OperationMessage redoOperation = undoRedoService.createRedoOperation(operationMessage);
            joinedSessionContext.collaborationSession().recordOperation(operationMessage);
            joinedSessionContext.collaborationSession().recordUndoableOperation(operationMessage, inverseOperation, redoOperation);
            joinedSessionContext.collaborationSession().clearRedoStack();
            int relayedCount = operationRelayService.relayOperation(
                    joinedSessionContext.collaborationSession(),
                    joinedSessionContext.connectedUser(),
                    operationMessage
            );
            log.info(
                    "Operation relayed: sessionId={}, senderId={}, recipients={}, operationKind={}",
                    joinedSessionContext.collaborationSession().getSessionId(),
                    joinedSessionContext.connectedUser().getUserId(),
                    relayedCount,
                    operationMessage.getOperationKind()
            );
        } catch (JsonProcessingException exception) {
            sendErrorResponse(
                    session,
                    new ErrorMessage("INVALID_OPERATION_MESSAGE", "Operation payload could not be parsed."),
                    "Rejected unparsable operation payload"
            );
        }
    }

    private void handleUndoRequest(WebSocketSession session, JsonNode payloadNode) {
        handleUndoRedoRequest(session, payloadNode, true);
    }

    private void handleRedoRequest(WebSocketSession session, JsonNode payloadNode) {
        handleUndoRedoRequest(session, payloadNode, false);
    }

    private void handleUndoRedoRequest(WebSocketSession session, JsonNode payloadNode, boolean undo) {
        MessageValidator.ValidationResult<Void> requestValidation =
                messageValidator.validateUndoRedoRequestPayload(payloadNode);
        if (!requestValidation.isValid()) {
            sendErrorResponse(session, requestValidation.errorMessage(), "Rejected invalid undo/redo request");
            return;
        }

        SessionJoinService.JoinedSessionContext joinedSessionContext = sessionJoinService.getJoinedSessionContext(session);
        if (joinedSessionContext == null || joinedSessionContext.collaborationSession() == null || joinedSessionContext.connectedUser() == null) {
            sendErrorResponse(
                    session,
                    new ErrorMessage(
                            "SESSION_NOT_JOINED",
                            "WebSocket client must join a collaboration session before requesting undo or redo."
                    ),
                    "Rejected undo/redo request from non-joined websocket"
            );
            return;
        }

        if (!permissionService.canEdit(joinedSessionContext.connectedUser())) {
            sendErrorResponse(
                    session,
                    new ErrorMessage(
                            "PERMISSION_DENIED",
                            "User role '" + joinedSessionContext.connectedUser().getRole() + "' cannot request undo or redo."
                    ),
                    "Rejected undo/redo request because user cannot edit"
            );
            return;
        }

        UndoRedoResult result = undo
                ? undoRedoService.undo(joinedSessionContext.collaborationSession(), joinedSessionContext.connectedUser())
                : undoRedoService.redo(joinedSessionContext.collaborationSession(), joinedSessionContext.connectedUser());
        if (!result.isSuccess()) {
            sendErrorResponse(session, result.errorMessage(), "Rejected undo/redo request");
            return;
        }

        if (result.isNoOp()) {
            log.info(
                    "{} request ignored because the stack is empty: sessionId={}, requesterId={}",
                    undo ? "Undo" : "Redo",
                    joinedSessionContext.collaborationSession().getSessionId(),
                    joinedSessionContext.connectedUser().getUserId()
            );
            return;
        }

        log.info(
                "{} completed: sessionId={}, requesterId={}, recipients={}, generatedOperationKind={}",
                undo ? "Undo" : "Redo",
                joinedSessionContext.collaborationSession().getSessionId(),
                joinedSessionContext.connectedUser().getUserId(),
                result.recipientCount(),
                result.operationMessage().getOperationKind()
        );
    }

    /**
     * Handles live cursor update messages.
     */
    private void handleCursorUpdate(WebSocketSession session, JsonNode payloadNode) {
        MessageValidator.ValidationResult<Void> cursorPayloadValidation =
                messageValidator.validateCursorUpdatePayload(payloadNode);
        if (!cursorPayloadValidation.isValid()) {
            sendErrorResponse(session, cursorPayloadValidation.errorMessage(), "Rejected invalid cursor payload");
            return;
        }

        SessionJoinService.JoinedSessionContext joinedSessionContext = sessionJoinService.getJoinedSessionContext(session);
        if (joinedSessionContext == null || joinedSessionContext.collaborationSession() == null || joinedSessionContext.connectedUser() == null) {
            return; // Ignore if not joined
        }

        try {
            CursorUpdateMessage cursorMessage = deserializeCursorUpdate(payloadNode);
            cursorMessage.setSessionId(joinedSessionContext.collaborationSession().getSessionId());
            cursorMessage.setUserId(joinedSessionContext.connectedUser().getUserId());

            operationRelayService.relayCursorUpdate(
                    joinedSessionContext.collaborationSession(),
                    joinedSessionContext.connectedUser(),
                    cursorMessage
            );
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Failed to parse cursor payload: sessionId={}, payload={}",
                    session.getId(),
                    abbreviatePayload(payloadNode.toString()),
                    exception
            );
            sendErrorResponse(
                    session,
                    new ErrorMessage("INVALID_CURSOR_MESSAGE", "Cursor payload could not be parsed."),
                    "Rejected unparsable cursor payload"
            );
        }
    }

    private CursorUpdateMessage deserializeCursorUpdate(JsonNode payloadNode)
            throws JsonProcessingException {
        try {
            return objectMapper.treeToValue(payloadNode, CursorUpdateMessage.class);
        } catch (JsonProcessingException exception) {
            ObjectNode cursorOnlyNode = objectMapper.createObjectNode();
            JsonNode blockIdNode = payloadNode.get("blockId");
            if (blockIdNode != null) {
                cursorOnlyNode.set("blockId", blockIdNode);
            }
            JsonNode characterOffsetNode = payloadNode.get("characterOffset");
            if (characterOffsetNode != null) {
                cursorOnlyNode.set("characterOffset", characterOffsetNode);
            }
            return objectMapper.treeToValue(cursorOnlyNode, CursorUpdateMessage.class);
        }
    }

    private JsonNode normalizeOperationPayload(JsonNode payloadNode) throws JsonProcessingException {
        if (payloadNode != null && payloadNode.isTextual()) {
            return objectMapper.readTree(payloadNode.asText());
        }
        return payloadNode;
    }

    /**
     * Sends an error DTO as JSON text to the current websocket client and writes a readable server log entry.
     * The handler uses this helper for malformed JSON, invalid data, unsupported message types, and permission failures.
     */
    private void sendErrorResponse(WebSocketSession session, ErrorMessage errorMessage, String logPrefix) {
        log.warn(
                "{}: sessionId={}, errorCode={}, message={}",
                logPrefix,
                session.getId(),
                errorMessage.getErrorCode(),
                errorMessage.getMessage()
        );
        sendJson(session, errorMessage);
    }

    /**
     * Serializes a shared protocol message into JSON and sends it through the websocket.
     * This is the final step after the handler has chosen a success or error response path.
     */
    private void sendJson(WebSocketSession session, CollaborationMessage message) {
        if (!session.isOpen()) {
            log.debug("Skipping outbound WebSocket message because the session is closed: sessionId={}", session.getId());
            return;
        }

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException exception) {
            log.warn(
                    "Failed to send WebSocket message: sessionId={}, messageType={}",
                    session.getId(),
                    message.getMessageType(),
                    exception
            );
        }
    }

    /**
     * Shortens very large payloads before they are written to logs.
     * This keeps debug output readable while still showing what kind of message arrived.
     */
    private String abbreviatePayload(String payload) {
        if (payload.length() <= MAX_LOGGED_PAYLOAD_LENGTH) {
            return payload;
        }
        return payload.substring(0, MAX_LOGGED_PAYLOAD_LENGTH) + "...";
    }
}
