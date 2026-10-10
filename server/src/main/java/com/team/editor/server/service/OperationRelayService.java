package com.team.editor.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.server.session.CollaborationSession;
import com.team.editor.server.session.ConnectedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.List;

/**
 * Handles the live relay of operation messages between users in the same collaboration session.
 */
@Service
public class OperationRelayService {
    private static final Logger log = LoggerFactory.getLogger(OperationRelayService.class);

    private final ObjectMapper objectMapper;

    public OperationRelayService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the operation message to every other connected user in the same room.
     * The sender is excluded so the server does not echo the same edit back to the originating socket.
     */
    public int relayOperation(CollaborationSession collaborationSession, ConnectedUser sender, OperationMessage operationMessage) {
        int relayedCount = 0;
        String serializedOperation;
        try {
            serializedOperation = objectMapper.writeValueAsString(operationMessage);
        } catch (IOException exception) {
            log.warn(
                    "Failed to serialize operation relay payload: sessionId={}, senderId={}, operationKind={}",
                    collaborationSession.getSessionId(),
                    sender.getUserId(),
                    operationMessage.getOperationKind(),
                    exception
            );
            return 0;
        }

        List<String> recipientSessionIds = collaborationSession.getOtherConnectedUsers(sender.getWebSocketSessionId()).stream()
                .map(recipient -> recipient.getWebSocketSession().getId())
                .toList();
        log.info(
                "BROADCASTING OPERATION to sessions: {} in room {} with payload={}",
                recipientSessionIds,
                collaborationSession.getSessionId(),
                serializedOperation
        );

        for (ConnectedUser recipient : collaborationSession.getOtherConnectedUsers(sender.getWebSocketSessionId())) {
            if (!recipient.getWebSocketSession().isOpen()) {
                continue;
            }

            try {
                recipient.getWebSocketSession().sendMessage(
                        new TextMessage(serializedOperation)
                );
                relayedCount++;
            } catch (IOException exception) {
                log.warn(
                        "Failed to relay operation: sessionId={}, senderId={}, recipientId={}",
                        collaborationSession.getSessionId(),
                        sender.getUserId(),
                        recipient.getUserId(),
                        exception
                );
            }
        }

        log.debug(
                "Relayed operation to room recipients: sessionId={}, senderId={}, operationKind={}, recipients={}",
                collaborationSession.getSessionId(),
                sender.getUserId(),
                operationMessage.getOperationKind(),
                relayedCount
        );
        return relayedCount;
    }

    /**
     * Replays the room's recorded operation history to a newly joined user so late joiners can reconstruct the current document.
     */
    public int replayOperationHistoryToUser(CollaborationSession collaborationSession, ConnectedUser recipient) {
        if (!recipient.getWebSocketSession().isOpen()) {
            return 0;
        }

        int replayedCount = 0;
        for (OperationMessage operationMessage : collaborationSession.getOperationHistorySnapshot()) {
            try {
                recipient.getWebSocketSession().sendMessage(
                        new TextMessage(objectMapper.writeValueAsString(operationMessage))
                );
                replayedCount++;
            } catch (IOException exception) {
                log.warn(
                        "Failed to replay operation history: sessionId={}, recipientId={}, operationKind={}",
                        collaborationSession.getSessionId(),
                        recipient.getUserId(),
                        operationMessage.getOperationKind(),
                        exception
                );
                break;
            }
        }

        log.info(
                "Replayed operation history to joined user: sessionId={}, recipientId={}, operationCount={}",
                collaborationSession.getSessionId(),
                recipient.getUserId(),
                replayedCount
        );
        return replayedCount;
    }

    /**
     * Sends a server-generated operation to every connected user in the room, including the requester.
     * Undo and redo use this path because every client must apply the generated correction once.
     */
    public int broadcastOperationToAll(CollaborationSession collaborationSession, OperationMessage operationMessage) {
        int relayedCount = 0;
        String serializedOperation;
        try {
            serializedOperation = objectMapper.writeValueAsString(operationMessage);
        } catch (IOException exception) {
            log.warn(
                    "Failed to serialize generated operation payload: sessionId={}, operationKind={}",
                    collaborationSession.getSessionId(),
                    operationMessage.getOperationKind(),
                    exception
            );
            return 0;
        }

        for (ConnectedUser recipient : collaborationSession.getConnectedUsers()) {
            if (!recipient.getWebSocketSession().isOpen()) {
                continue;
            }

            try {
                recipient.getWebSocketSession().sendMessage(new TextMessage(serializedOperation));
                relayedCount++;
            } catch (IOException exception) {
                log.warn(
                        "Failed to broadcast generated operation: sessionId={}, recipientId={}",
                        collaborationSession.getSessionId(),
                        recipient.getUserId(),
                        exception
                );
            }
        }

        return relayedCount;
    }

    /**
     * Sends the cursor update message to every other connected user in the same room.
     * The sender is excluded so the server does not echo the same update back.
     */
    public int relayCursorUpdate(CollaborationSession collaborationSession, ConnectedUser sender, CursorUpdateMessage cursorUpdateMessage) {
        int relayedCount = 0;

        for (ConnectedUser recipient : collaborationSession.getOtherConnectedUsers(sender.getWebSocketSessionId())) {
            if (!recipient.getWebSocketSession().isOpen()) {
                continue;
            }

            try {
                recipient.getWebSocketSession().sendMessage(
                        new TextMessage(objectMapper.writeValueAsString(cursorUpdateMessage))
                );
                relayedCount++;
            } catch (IOException exception) {
                log.warn(
                        "Failed to relay cursor update: sessionId={}, senderId={}, recipientId={}",
                        collaborationSession.getSessionId(),
                        sender.getUserId(),
                        recipient.getUserId(),
                        exception
                );
            }
        }

        return relayedCount;
    }
}
