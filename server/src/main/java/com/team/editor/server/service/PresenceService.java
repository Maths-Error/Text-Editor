package com.team.editor.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.protocol.dto.PresenceUpdateMessage;
import com.team.editor.common.protocol.message.PresenceEventType;
import com.team.editor.server.session.CollaborationSession;
import com.team.editor.server.session.ConnectedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;

/**
 * Broadcasts simple JOINED and LEFT presence updates to users in the same live collaboration room.
 */
@Service
public class PresenceService {
    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final ObjectMapper objectMapper;

    public PresenceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Notifies the other users in the room that a new user has joined successfully.
     */
    public int broadcastUserJoined(CollaborationSession collaborationSession, ConnectedUser joinedUser) {
        int broadcastCount = broadcastToUsers(
                collaborationSession.getOtherConnectedUsers(joinedUser.getWebSocketSessionId()),
                new PresenceUpdateMessage(
                        collaborationSession.getSessionId(),
                        joinedUser.getUserId(),
                        joinedUser.getDisplayName(),
                        PresenceEventType.JOINED
                )
        );
        log.info(
                "Broadcasted join presence: sessionId={}, joinedUserId={}, recipients={}",
                collaborationSession.getSessionId(),
                joinedUser.getUserId(),
                broadcastCount
        );
        return broadcastCount;
    }

    /**
     * Sends presence updates for all existing users in the session to the newly joined user.
     */
    public void sendExistingUsersToNewUser(CollaborationSession collaborationSession, ConnectedUser newUser) {
        if (!newUser.getWebSocketSession().isOpen()) {
            return;
        }

        int sentCount = 0;
        for (ConnectedUser existingUser : collaborationSession.getOtherConnectedUsers(newUser.getWebSocketSessionId())) {
            PresenceUpdateMessage message = new PresenceUpdateMessage(
                    collaborationSession.getSessionId(),
                    existingUser.getUserId(),
                    existingUser.getDisplayName(),
                    PresenceEventType.JOINED
            );
            try {
                newUser.getWebSocketSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                sentCount++;
            } catch (IOException exception) {
                log.warn(
                        "Failed to send existing user presence to new user: sessionId={}, newUserId={}, existingUserId={}",
                        message.getSessionId(),
                        newUser.getUserId(),
                        existingUser.getUserId(),
                        exception
                );
            }
        }

        log.info(
                "Sent existing user presence snapshot: sessionId={}, newUserId={}, existingUsers={}",
                collaborationSession.getSessionId(),
                newUser.getUserId(),
                sentCount
        );
    }

    /**
     * Notifies the remaining users in the room that a user has left or disconnected.
     * The disconnect cleanup removes the departing user first, so the remaining session users are the recipients.
     */
    public int broadcastUserLeft(CollaborationSession collaborationSession, ConnectedUser disconnectedUser) {
        int broadcastCount = broadcastToUsers(
                collaborationSession.getConnectedUsers(),
                new PresenceUpdateMessage(
                        collaborationSession.getSessionId(),
                        disconnectedUser.getUserId(),
                        disconnectedUser.getDisplayName(),
                        PresenceEventType.LEFT
                )
        );
        log.info(
                "Broadcasted leave presence: sessionId={}, disconnectedUserId={}, recipients={}",
                collaborationSession.getSessionId(),
                disconnectedUser.getUserId(),
                broadcastCount
        );
        return broadcastCount;
    }

    /**
     * Shared helper used by join and leave broadcasts to send one presence message to a set of recipients.
     */
    private int broadcastToUsers(Iterable<ConnectedUser> users, PresenceUpdateMessage message) {
        int broadcastCount = 0;

        for (ConnectedUser user : users) {
            if (!user.getWebSocketSession().isOpen()) {
                continue;
            }

            try {
                user.getWebSocketSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
                broadcastCount++;
            } catch (IOException exception) {
                log.warn(
                        "Failed to broadcast presence update: sessionId={}, recipientId={}, eventType={}",
                        message.getSessionId(),
                        user.getUserId(),
                        message.getEventType(),
                        exception
                );
            }
        }

        return broadcastCount;
    }
}
