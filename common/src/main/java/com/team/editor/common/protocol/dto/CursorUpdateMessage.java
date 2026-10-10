package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;

/**
 * Carries a remote cursor update for a collaboration session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorUpdateMessage implements CollaborationMessage {
    private String sessionId;
    private UserId userId;
    private BlockId blockId;
    private int characterOffset;

    // --------------------
    // Constructors
    // --------------------

    public CursorUpdateMessage() {
    }

    public CursorUpdateMessage(String sessionId, UserId userId, BlockId blockId, int characterOffset) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.blockId = blockId;
        this.characterOffset = characterOffset;
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

    public BlockId getBlockId() {
        return blockId;
    }

    public int getCharacterOffset() {
        return characterOffset;
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

    public void setBlockId(BlockId blockId) {
        this.blockId = blockId;
    }

    public void setCharacterOffset(int characterOffset) {
        this.characterOffset = characterOffset;
    }

    // --------------------
    // Message Type
    // --------------------

    @Override
    public MessageType messageType() {
        return MessageType.CURSOR_UPDATE;
    }
}
