package com.team.editor.server.session;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks active collaboration sessions by session id.
 */
@Component
public class SessionRegistry {
    private final ConcurrentMap<String, CollaborationSession> sessionsById = new ConcurrentHashMap<>();

    /**
     * Tries to find an existing collaboration room by session id.
     * Join and cleanup flows call this when they need to read a room without forcing creation.
     */
    public Optional<CollaborationSession> getSession(String sessionId) {
        return Optional.ofNullable(sessionsById.get(normalizeSessionId(sessionId)));
    }
    /**
     * Creates a room only if it does not already exist.
     * The first successful join for a new session triggers this.
     * If another user already created the room, the existing room is returned instead.
     */
    public CollaborationSession createSessionIfMissing(String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        return sessionsById.computeIfAbsent(normalizedSessionId, CollaborationSession::new);
    }

    /**
     * Gets the room for a session or creates it when this is the first join.
     * The join service uses this as the main entry point before storing the connected user.
     */
    public CollaborationSession getOrCreateSession(String sessionId) {
        return createSessionIfMissing(sessionId);
    }

    /**
     * Removes a room only when it has become empty.
     * Disconnect cleanup calls this so old in-memory sessions do not stay around after the last user leaves.
     */
    public void removeSessionIfEmpty(String sessionId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        sessionsById.computeIfPresent(
                normalizedSessionId,
                (ignored, existingSession) -> existingSession.isEmpty() ? null : existingSession
        );
    }

    /**
     * Trims and validates the session id before it is used as a map key.
     * This keeps the registry from treating blank or space-only ids as valid rooms.
     */
    private String normalizeSessionId(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        String normalizedSessionId = sessionId.trim();
        if (normalizedSessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be blank");
        }
        return normalizedSessionId;
    }
}
