package com.team.editor.server.service;

import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionRequest;
import com.team.editor.common.protocol.dto.JoinSessionResponse;
import com.team.editor.common.protocol.message.SessionRole;
import com.team.editor.server.document.DocumentAccessService;
import com.team.editor.server.persistence.MongoDocumentRecord;
import com.team.editor.server.repository.DocumentRepository;
import com.team.editor.server.session.CollaborationSession;
import com.team.editor.server.session.ConnectedUser;
import com.team.editor.server.session.SessionRegistry;
import com.team.editor.server.validation.MessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

/**
 * Handles the server-side workflow for validating and admitting live session join requests.
 */
@Service
public class SessionJoinService {
    public static final String SESSION_ID_ATTRIBUTE = SessionJoinService.class.getName() + ".sessionId";
    public static final String USER_ID_ATTRIBUTE = SessionJoinService.class.getName() + ".userId";
    private static final Logger log = LoggerFactory.getLogger(SessionJoinService.class);

    private final SessionRegistry sessionRegistry;
    private final MessageValidator messageValidator;
    private final PermissionService permissionService;
    private final DocumentRepository documentRepository;
    private final DocumentAccessService documentAccessService;

    public SessionJoinService(
            SessionRegistry sessionRegistry,
            MessageValidator messageValidator,
            PermissionService permissionService,
            DocumentRepository documentRepository,
            DocumentAccessService documentAccessService
    ) {
        this.sessionRegistry = sessionRegistry;
        this.messageValidator = messageValidator;
        this.permissionService = permissionService;
        this.documentRepository = documentRepository;
        this.documentAccessService = documentAccessService;
    }

    /**
     * Validates a join request, resolves the target session, stores the connected user,
     * and returns the data needed for the response and any follow-up notifications.
     */
    public JoinResult joinSession(JoinSessionRequest request, WebSocketSession webSocketSession) {
        MessageValidator.ValidationResult<JoinSessionRequest> validationResult =
                messageValidator.validateJoinRequest(request);
        if (!validationResult.isValid()) {
            return JoinResult.rejected(validationResult.errorMessage());
        }

        String sessionId = request.getSessionId().trim();
        String displayName = request.getDisplayName().trim();
        String userId = request.getUserId().getValue();

        MongoDocumentRecord document = documentRepository.findById(sessionId).orElse(null);
        if (document == null || document.isDeleted()) {
            return JoinResult.rejected(
                    new ErrorMessage(
                            "DOCUMENT_NOT_FOUND",
                            "Document was not found for this WebSocket session."
                    )
            );
        }

        SessionRole assignedRole = resolveWebSocketRole(document, userId);
        if (assignedRole == null) {
            return JoinResult.rejected(
                    new ErrorMessage(
                            "PERMISSION_DENIED",
                            "User does not have access to this document."
                    )
            );
        }

        Object existingSessionIdAttribute = webSocketSession.getAttributes().get(SESSION_ID_ATTRIBUTE);
        if (existingSessionIdAttribute instanceof String existingSessionId
                && !existingSessionId.isBlank()
                && !existingSessionId.equals(sessionId)) {
            cleanupDisconnectedSession(webSocketSession);
        }

        CollaborationSession collaborationSession = sessionRegistry.getOrCreateSession(sessionId);
        ConnectedUser connectedUser = new ConnectedUser(request.getUserId(), displayName, assignedRole, webSocketSession);

        if (!collaborationSession.addUser(connectedUser)) {
            return JoinResult.rejected(
                    new ErrorMessage(
                            "DUPLICATE_USER",
                            "User '" + request.getUserId().getValue() + "' is already connected to session '" + sessionId + "'."
                    )
            );
        }

        webSocketSession.getAttributes().put(SESSION_ID_ATTRIBUTE, sessionId);
        webSocketSession.getAttributes().put(USER_ID_ATTRIBUTE, request.getUserId().getValue());

        log.info(
                "User joined collaboration session: webSocketSessionId={}, requestedSessionId={}, storedSessionId={}, userId={}, connectedUsers={}",
                webSocketSession.getId(),
                request.getSessionId(),
                collaborationSession.getSessionId(),
                request.getUserId(),
                collaborationSession.getConnectedUsers().size()
        );

        return JoinResult.accepted(
                new JoinSessionResponse(
                        sessionId,
                        true,
                        request.getUserId(),
                        "Joined session successfully."
                ),
                collaborationSession,
                connectedUser
        );
    }

    private SessionRole resolveWebSocketRole(MongoDocumentRecord document, String userId) {
        // WebSocket edit permission comes from stored document access, not from the client request.
        if (documentAccessService.isOwner(document, userId)) {
            return SessionRole.EDITOR;
        }

        if (documentAccessService.isEditor(document, userId)) {
            return SessionRole.EDITOR;
        }

        if (documentAccessService.isViewer(document, userId)) {
            return SessionRole.VIEWER;
        }

        return null;
    }

    /**
     * Removes a websocket from the in-memory live session state when the socket closes or errors.
     * The returned record gives the handler enough context to broadcast a leave presence update and to know if the room became empty.
     */
    public DisconnectResult cleanupDisconnectedSession(WebSocketSession webSocketSession) {
        Object sessionIdAttribute = webSocketSession.getAttributes().remove(SESSION_ID_ATTRIBUTE);
        webSocketSession.getAttributes().remove(USER_ID_ATTRIBUTE);

        if (!(sessionIdAttribute instanceof String sessionId) || sessionId.isBlank()) {
            return DisconnectResult.empty();
        }

        CollaborationSession collaborationSession = sessionRegistry.getSession(sessionId).orElse(null);
        if (collaborationSession == null) {
            return DisconnectResult.empty();
        }

        ConnectedUser removedUser = collaborationSession.removeUserByWebSocketSessionId(webSocketSession.getId()).orElse(null);
        boolean sessionRemoved = collaborationSession.isEmpty();
        sessionRegistry.removeSessionIfEmpty(sessionId);

        return new DisconnectResult(collaborationSession, removedUser, sessionRemoved);
    }

    /**
     * Resolves the collaboration session and connected user associated with a websocket
     * so incoming messages can be validated, authorized, and routed correctly.
     */
    public JoinedSessionContext getJoinedSessionContext(WebSocketSession webSocketSession) {
        Object sessionIdAttribute = webSocketSession.getAttributes().get(SESSION_ID_ATTRIBUTE);
        if (!(sessionIdAttribute instanceof String sessionId) || sessionId.isBlank()) {
            return null;
        }

        CollaborationSession collaborationSession = sessionRegistry.getSession(sessionId).orElse(null);
        if (collaborationSession == null) {
            return null;
        }

        ConnectedUser connectedUser = collaborationSession.findUserByWebSocketSessionId(webSocketSession.getId()).orElse(null);
        if (connectedUser == null) {
            return null;
        }

        return new JoinedSessionContext(collaborationSession, connectedUser);
    }

    /**
     * Wraps the result of the join flow so the handler can decide whether to send
     * a JoinSessionResponse or an ErrorMessage, and optionally trigger a presence broadcast.
     */
    public record JoinResult(
            JoinSessionResponse response,
            ErrorMessage errorMessage,
            CollaborationSession collaborationSession,
            ConnectedUser connectedUser
    ) {
        public static JoinResult accepted(
                JoinSessionResponse response,
                CollaborationSession collaborationSession,
                ConnectedUser connectedUser
        ) {
            return new JoinResult(response, null, collaborationSession, connectedUser);
        }

        public static JoinResult rejected(ErrorMessage errorMessage) {
            return new JoinResult(null, errorMessage, null, null);
        }

        public boolean isAccepted() {
            return response != null;
        }
    }

    /**
     * Holds the live session and connected user that belong to one joined websocket.
     */
    public record JoinedSessionContext(CollaborationSession collaborationSession, ConnectedUser connectedUser) {
    }

    /**
     * Holds the result of disconnect cleanup so the caller can continue the leave flow.
     */
    public record DisconnectResult(
            CollaborationSession collaborationSession,
            ConnectedUser disconnectedUser,
            boolean sessionRemoved
    ) {
        public static DisconnectResult empty() {
            return new DisconnectResult(null, null, false);
        }

        public boolean hasDisconnectedUser() {
            return disconnectedUser != null;
        }
    }
}
