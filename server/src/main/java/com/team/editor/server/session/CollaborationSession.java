package com.team.editor.server.session;

import java.util.Collection;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.OperationMessage;

/**
 * Stores the connected users that currently belong to one collaboration session.
 */
public class CollaborationSession {
    private static final int MAX_UNDO_HISTORY = 10;

    private final String sessionId;
    private final ConcurrentMap<String, ConnectedUser> usersByWebSocketSessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> webSocketSessionIdByUserId = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<OperationMessage> operationHistory = new CopyOnWriteArrayList<>();
    private final Deque<UndoableOperation> undoStack = new ArrayDeque<>();
    private final Deque<UndoableOperation> redoStack = new ArrayDeque<>();
    private final Set<String> seenOperationIds = ConcurrentHashMap.newKeySet();

    /**
     * Creates one in-memory collaboration room for a specific session id.
     * The registry calls this the first time a user joins a session that does not exist yet.
     */
    public CollaborationSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }
        this.sessionId = sessionId.trim();
    }

    public String getSessionId() {
        return sessionId;
    }

    /**
     * Adds a connected user to this room after the join request has been validated.
     * It returns false when a different websocket in the same room is already using the same user id.
     * If the same websocket joins again with a different user id, the old mapping is replaced.
     */
    public synchronized boolean addUser(ConnectedUser user) {
        Objects.requireNonNull(user, "user cannot be null");

        String userKey = user.getUserId().getValue();
        String existingWebSocketSessionId = webSocketSessionIdByUserId.get(userKey);
        if (existingWebSocketSessionId != null && !existingWebSocketSessionId.equals(user.getWebSocketSessionId())) {
            return false;
        }

        ConnectedUser previousUser = usersByWebSocketSessionId.put(user.getWebSocketSessionId(), user);
        if (previousUser != null && !previousUser.getUserId().equals(user.getUserId())) {
            webSocketSessionIdByUserId.remove(previousUser.getUserId().getValue());
        }

        webSocketSessionIdByUserId.put(userKey, user.getWebSocketSessionId());
        return true;
    }

    /**
     * Removes a user from this room by websocket session id.
     * Disconnect cleanup calls this when the socket closes or fails.
     * The removed user is returned so the caller can know whether anything was actually stored here.
     */
    public synchronized Optional<ConnectedUser> removeUserByWebSocketSessionId(String webSocketSessionId) {
        if (webSocketSessionId == null || webSocketSessionId.isBlank()) {
            return Optional.empty();
        }

        ConnectedUser removedUser = usersByWebSocketSessionId.remove(webSocketSessionId);
        if (removedUser != null) {
            webSocketSessionIdByUserId.remove(removedUser.getUserId().getValue());
        }
        return Optional.ofNullable(removedUser);
    }

    /**
     * Looks up which live user belongs to a websocket session id.
     * This is used when incoming websocket messages need to be tied back to the joined user.
     */
    public Optional<ConnectedUser> findUserByWebSocketSessionId(String webSocketSessionId) {
        if (webSocketSessionId == null || webSocketSessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(usersByWebSocketSessionId.get(webSocketSessionId));
    }

    /**
     * Finds a user in this room by their logical user id instead of by socket id.
     * The room uses this to detect duplicate joins from different sockets with the same user id.
     */
    public Optional<ConnectedUser> findUserByUserId(UserId userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String webSocketSessionId = webSocketSessionIdByUserId.get(userId.getValue());
        if (webSocketSessionId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(usersByWebSocketSessionId.get(webSocketSessionId));
    }

    /**
     * Returns a safe snapshot of the users currently stored in this room.
     * The returned collection is a copy so callers do not modify the internal maps directly.
     */
    public Collection<ConnectedUser> getConnectedUsers() {
        return List.copyOf(usersByWebSocketSessionId.values());
    }

    /**
     * Stores one operation in the room history so late joiners can replay the shared document state.
     */
    public void recordOperation(OperationMessage operationMessage) {
        Objects.requireNonNull(operationMessage, "operationMessage cannot be null");
        operationHistory.add(copyOperation(operationMessage));
        rememberOperationId(operationMessage.getOperationId());
    }

    public boolean rememberOperationId(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            return false;
        }
        return seenOperationIds.add(operationId);
    }

    public boolean hasSeenOperationId(String operationId) {
        return operationId != null && seenOperationIds.contains(operationId);
    }

    public synchronized void recordUndoableOperation(OperationMessage original, OperationMessage inverse) {
        recordUndoableOperation(original, inverse, original);
    }

    public synchronized void recordUndoableOperation(
            OperationMessage original,
            OperationMessage inverse,
            OperationMessage redo
    ) {
        Objects.requireNonNull(original, "original cannot be null");
        if (original.getOperationId() == null || original.getOperationId().isBlank()) {
            throw new IllegalArgumentException("original operationId is required");
        }

        undoStack.push(new UndoableOperation(original.getOperationId(), original, inverse, redo));
        while (undoStack.size() > MAX_UNDO_HISTORY) {
            undoStack.removeLast();
        }
    }

    public synchronized Optional<UndoableOperation> popUndo() {
        return undoStack.isEmpty() ? Optional.empty() : Optional.of(undoStack.pop());
    }

    public synchronized Optional<UndoableOperation> peekUndo() {
        return undoStack.isEmpty() ? Optional.empty() : Optional.of(undoStack.peek());
    }

    public synchronized void pushRedo(UndoableOperation operation) {
        Objects.requireNonNull(operation, "operation cannot be null");
        redoStack.push(operation);
    }

    public synchronized Optional<UndoableOperation> popRedo() {
        return redoStack.isEmpty() ? Optional.empty() : Optional.of(redoStack.pop());
    }

    public synchronized void clearRedoStack() {
        redoStack.clear();
    }

    public synchronized int getUndoStackSize() {
        return undoStack.size();
    }

    public synchronized int getRedoStackSize() {
        return redoStack.size();
    }

    /**
     * Returns a stable snapshot of the operations recorded for this room.
     */
    public List<OperationMessage> getOperationHistorySnapshot() {
        return List.copyOf(operationHistory);
    }

    private OperationMessage copyOperation(OperationMessage operationMessage) {
        return new OperationMessage(
                operationMessage.getSessionId(),
                operationMessage.getSenderId(),
                operationMessage.getOperationId(),
                operationMessage.getOperationKindEnum(),
                operationMessage.getPayload() == null ? null : operationMessage.getPayload().deepCopy()
        );
    }

    /**
     * Returns all currently connected users except the socket identified by {@code excludedWebSocketSessionId}.
     * The room uses this when relaying operations or join-presence updates so the sender does not receive a duplicate echo.
     */
    public Collection<ConnectedUser> getOtherConnectedUsers(String excludedWebSocketSessionId) {
        return usersByWebSocketSessionId.values().stream()
                .filter(user -> !user.getWebSocketSessionId().equals(excludedWebSocketSessionId))
                .toList();
    }

    /**
     * Tells the registry whether this room has any live users left.
     * After a disconnect, the registry uses this to decide if the room should be removed.
     */
    public boolean isEmpty() {
        return usersByWebSocketSessionId.isEmpty();
    }
}
