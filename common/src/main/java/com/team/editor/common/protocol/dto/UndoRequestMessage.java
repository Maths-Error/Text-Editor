package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;

/**
 * Flat websocket payload used by an editor to ask the server to undo the latest session operation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UndoRequestMessage implements CollaborationMessage {
    public UndoRequestMessage() {
    }

    @Override
    public MessageType messageType() {
        return MessageType.UNDO_REQUEST;
    }
}
