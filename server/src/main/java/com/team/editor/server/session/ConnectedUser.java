package com.team.editor.server.session;

import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.message.SessionRole;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;

/**
 * Represents one connected participant in a collaboration session.
 */
public class ConnectedUser {
    private final UserId userId;
    private final String displayName;
    private final SessionRole role;
    private final WebSocketSession webSocketSession;

    /**
     * Creates a connected-user record and applies the default role behavior in one place.
     */
    public ConnectedUser(UserId userId, String displayName, WebSocketSession webSocketSession) {
        this(userId, displayName, SessionRole.EDITOR, webSocketSession);
    }

    /**
     * Creates the server-side record used to associate a user, role, and websocket connection.
     */
    public ConnectedUser(UserId userId, String displayName, SessionRole role, WebSocketSession webSocketSession) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be null or blank");
        }
        this.displayName = displayName.trim();
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.webSocketSession = Objects.requireNonNull(webSocketSession, "webSocketSession cannot be null");
    }

    public UserId getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SessionRole getRole() {
        return role;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    /**
     * Returns the Spring websocket session id for this user.
     * Other session-management classes use this value as the key for add/remove lookups.
     */
    public String getWebSocketSessionId() {
        return webSocketSession.getId();
    }
}
