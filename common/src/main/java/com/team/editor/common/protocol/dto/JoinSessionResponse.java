package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;

/**
 * Response returned after a join attempt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JoinSessionResponse implements CollaborationMessage {
    private String sessionId;
    private boolean accepted;
    private UserId assignedUserId;
    private String message;

    // --------------------
    // Constructors
    // --------------------

    public JoinSessionResponse() {
    }

    public JoinSessionResponse(String sessionId, boolean accepted, UserId assignedUserId, String message) {
        this.sessionId = sessionId;
        this.accepted = accepted;
        this.assignedUserId = assignedUserId;
        this.message = message;
    }

    // --------------------
    // Getters
    // --------------------

    public String getSessionId() {
        return sessionId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public UserId getAssignedUserId() {
        return assignedUserId;
    }

    public String getMessage() {
        return message;
    }

    // --------------------
    // Setters
    // --------------------

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public void setAssignedUserId(UserId assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // --------------------
    // Message Type
    // --------------------

    @Override
    public MessageType messageType() {
        return MessageType.JOIN_SESSION_RESPONSE;
    }
}
