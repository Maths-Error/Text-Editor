package com.team.editor.common.protocol.message;

/**
 * Identifies the collaboration payload categories supported by the shared protocol.
 */
public enum MessageType {
    JOIN_SESSION_REQUEST,
    JOIN_SESSION_RESPONSE,
    OPERATION,
    UNDO_REQUEST,
    REDO_REQUEST,
    CURSOR_UPDATE,
    PRESENCE_UPDATE,
    ERROR
}
