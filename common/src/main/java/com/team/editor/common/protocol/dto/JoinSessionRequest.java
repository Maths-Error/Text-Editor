package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;
import com.team.editor.common.protocol.message.SessionRole;

/**
 * Join request sent by a client when opening a collaboration session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JoinSessionRequest implements CollaborationMessage {
    private String sessionId;
    private UserId userId;
    private String displayName;
    private SessionRole role;

    // --------------------
    // Constructors
    // --------------------

    public JoinSessionRequest() {
    }

    public JoinSessionRequest(String sessionId, UserId userId, String displayName) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.displayName = displayName;
    }

    public JoinSessionRequest(String sessionId, UserId userId, String displayName, SessionRole role) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
    }

    // --------------------
    // Getters
    // --------------------

    public String getSessionId() {
        return sessionId;
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

    // --------------------
    // Setters
    // --------------------

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setRole(SessionRole role) {
        this.role = role;
    }

    // --------------------
    // Message Type
    // --------------------

    @Override
    public MessageType messageType() {
        return MessageType.JOIN_SESSION_REQUEST;
    }
}
