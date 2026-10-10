package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;

/**
 * Flat websocket payload used by an editor to ask the server to redo the latest undone session operation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedoRequestMessage implements CollaborationMessage {
    public RedoRequestMessage() {
    }

    @Override
    public MessageType messageType() {
        return MessageType.REDO_REQUEST;
    }
}
