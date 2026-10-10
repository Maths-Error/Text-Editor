package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;
import com.team.editor.common.protocol.message.PresenceEventType;

/**
 * Announces that a user's presence state changed inside a collaboration session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PresenceUpdateMessage implements CollaborationMessage {
    private String sessionId;
    private UserId userId;
    private String displayName;
    private String presence;
    private PresenceEventType eventType;

    // --------------------
    // Constructors
    // --------------------

    public PresenceUpdateMessage() {
    }

    public PresenceUpdateMessage(String sessionId, UserId userId, String presence) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.presence = presence;
        this.eventType = parseEventType(presence);
    }

    public PresenceUpdateMessage(String sessionId, UserId userId, String displayName, PresenceEventType eventType) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.displayName = displayName;
        this.eventType = eventType;
        this.presence = eventType == null ? null : eventType.name();
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

    public String getPresence() {
        return presence;
    }

    public PresenceEventType getEventType() {
        return eventType;
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

    public void setPresence(String presence) {
        this.presence = presence;
        this.eventType = parseEventType(presence);
    }

    public void setEventType(PresenceEventType eventType) {
        this.eventType = eventType;
        this.presence = eventType == null ? null : eventType.name();
    }

    // --------------------
    // Message Type
    // --------------------

    @Override
    public MessageType messageType() {
        return MessageType.PRESENCE_UPDATE;
    }

    private PresenceEventType parseEventType(String presence) {
        if (presence == null || presence.isBlank()) {
            return null;
        }

        try {
            return PresenceEventType.valueOf(presence.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
