package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.crdt.block.BlockId;

/**
 * Shared wrapper used by block-scoped character operations.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockOpPayload<T> {
    private BlockId blockId;
    private T op;

    public BlockOpPayload() {
    }

    public BlockOpPayload(BlockId blockId, T op) {
        this.blockId = blockId;
        this.op = op;
    }

    public BlockId getBlockId() {
        return blockId;
    }

    public void setBlockId(BlockId blockId) {
        this.blockId = blockId;
    }

    public T getOp() {
        return op;
    }

    public void setOp(T op) {
        this.op = op;
    }
}
