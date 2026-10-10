package com.team.editor.client.session;

import com.team.editor.common.model.UserId;

public class ClientSession {

    private String sessionId;
    private UserId userId;
    private String displayName;
    private SessionRole role;

    public ClientSession() {
    }

    public ClientSession(String sessionId, UserId userId, String displayName, SessionRole role) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
    }

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
}