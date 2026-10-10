package com.team.editor.common.protocol.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a transferable collaboration payload shared across modules.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public interface CollaborationMessage {
    MessageType messageType();

    default MessageType getMessageType() {
        return messageType();
    }
}
