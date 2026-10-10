package com.team.editor.client.controller;

import com.team.editor.client.config.ClientConfig;
import com.team.editor.client.session.ClientSession;
import com.team.editor.client.session.SessionRole;
import com.team.editor.client.state.ConnectionState;
import com.team.editor.client.websocket.ClientWebSocketHandler;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionRequest;
import com.team.editor.common.protocol.dto.JoinSessionResponse;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.dto.PresenceUpdateMessage;
import com.team.editor.common.protocol.dto.RedoRequestMessage;
import com.team.editor.common.protocol.dto.UndoRequestMessage;

import java.util.ArrayList;
import java.util.List;

public class SessionController implements ClientWebSocketHandler.Listener {

    private final ClientWebSocketHandler webSocketHandler;
    private final ClientConfig config;
    private ClientSession session;
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private final List<SessionListener> listeners = new ArrayList<>();

    public interface SessionListener {
        void onConnectionStateChanged(ConnectionState state);
        void onJoinResponse(JoinSessionResponse response);
        void onPresenceUpdate(PresenceUpdateMessage message);
        void onCursorUpdate(CursorUpdateMessage message);
        void onOperation(OperationMessage message);
        void onError(ErrorMessage errorMessage);
    }

    public SessionController() {
        this(new ClientConfig());
    }

    public SessionController(ClientConfig config) {
        this.config = config == null ? new ClientConfig() : config;
        this.webSocketHandler = new ClientWebSocketHandler(this.config.getWebSocketUrl());
        this.webSocketHandler.setListener(this);
    }

    public void addListener(SessionListener listener) {
        listeners.add(listener);
    }

    private void updateState(ConnectionState state) {
        this.connectionState = state;
        for (SessionListener listener : listeners) {
            listener.onConnectionStateChanged(state);
        }
    }

    public boolean connect() {
        updateState(ConnectionState.CONNECTING);
        webSocketHandler.setWebSocketUrl(config.getWebSocketUrl());
        boolean connected = webSocketHandler.connect();
        if (!connected) {
            updateState(ConnectionState.ERROR);
            return false;
        }
        updateState(ConnectionState.CONNECTED);
        return true;
    }

    public void disconnect() {
        webSocketHandler.disconnect();
        updateState(ConnectionState.DISCONNECTED);
        session = null;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        config.setServerBaseUrl(serverBaseUrl);
        webSocketHandler.setWebSocketUrl(config.getWebSocketUrl());
    }

    public String getServerBaseUrl() {
        return config.getServerBaseUrl();
    }

    public void joinSession(String sessionId, String username) {
        UserId userId = new UserId(username);

        session = new ClientSession(
                sessionId,
                userId,
                username,
                SessionRole.EDITOR
        );

        JoinSessionRequest request = new JoinSessionRequest(sessionId, userId, username);
        webSocketHandler.sendJoinRequest(request);
    }

    public void joinDocumentSession(String documentId, String userIdValue, String displayName, String documentRole) {
        UserId userId = new UserId(userIdValue);
        SessionRole clientRole = mapDocumentRoleToClientRole(documentRole);

        session = new ClientSession(
                documentId,
                userId,
                displayName,
                clientRole
        );

        JoinSessionRequest request = new JoinSessionRequest(
                documentId,
                userId,
                displayName,
                mapDocumentRoleToProtocolRole(documentRole)
        );
        webSocketHandler.sendJoinRequest(request);
    }

    public static SessionRole mapDocumentRoleToClientRole(String documentRole) {
        if ("VIEWER".equalsIgnoreCase(documentRole)) {
            return SessionRole.VIEWER;
        }
        return SessionRole.EDITOR;
    }

    public static com.team.editor.common.protocol.message.SessionRole mapDocumentRoleToProtocolRole(String documentRole) {
        if ("VIEWER".equalsIgnoreCase(documentRole)) {
            return com.team.editor.common.protocol.message.SessionRole.VIEWER;
        }
        return com.team.editor.common.protocol.message.SessionRole.EDITOR;
    }

    public void sendOperation(OperationMessage message) {
        if (session == null || session.getRole() != SessionRole.EDITOR) {
            return;
        }
        webSocketHandler.sendOperation(message);
    }

    public void requestUndo() {
        if (session == null || session.getRole() != SessionRole.EDITOR) {
            return;
        }
        webSocketHandler.sendUndoRequest(new UndoRequestMessage());
    }

    public void requestRedo() {
        if (session == null || session.getRole() != SessionRole.EDITOR) {
            return;
        }
        webSocketHandler.sendRedoRequest(new RedoRequestMessage());
    }
    
    public void sendCursorUpdate(CursorUpdateMessage message) {
        webSocketHandler.sendCursorUpdate(message);
    }

    public void simulateIncomingMessage(String json) {
        webSocketHandler.handleIncoming(json);
    }

    public ClientSession getSession() {
        return session;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    @Override
    public void onJoinSessionResponse(JoinSessionResponse response) {
        if (response.isAccepted()) {
            if (session != null) {
                if (response.getSessionId() != null && !response.getSessionId().isBlank()) {
                    session.setSessionId(response.getSessionId());
                }
                if (response.getAssignedUserId() != null) {
                    session.setUserId(response.getAssignedUserId());
                }
            }
            updateState(ConnectionState.JOINED);
        } else {
            updateState(ConnectionState.ERROR);
        }
        for (SessionListener listener : listeners) {
            listener.onJoinResponse(response);
        }
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        updateState(ConnectionState.ERROR);
        for (SessionListener listener : listeners) {
            listener.onError(errorMessage);
        }
    }

    @Override
    public void onPresenceUpdate(PresenceUpdateMessage message) {
        for (SessionListener listener : listeners) {
            listener.onPresenceUpdate(message);
        }
    }

    @Override
    public void onCursorUpdate(CursorUpdateMessage message) {
        for (SessionListener listener : listeners) {
            listener.onCursorUpdate(message);
        }
    }

    @Override
    public void onOperation(OperationMessage message) {
        for (SessionListener listener : listeners) {
            listener.onOperation(message);
        }
    }
}
