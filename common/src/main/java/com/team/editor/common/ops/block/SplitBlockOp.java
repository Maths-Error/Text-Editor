package com.team.editor.common.ops.block;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.editor.common.crdt.block.BlockId;

public record SplitBlockOp(BlockId targetBlockId, BlockId newSiblingId)
{
    @JsonCreator
    public SplitBlockOp(
            @JsonProperty("targetBlockId") BlockId targetBlockId,
            @JsonProperty("newSiblingId") BlockId newSiblingId)
    {
        this.targetBlockId = targetBlockId;
        this.newSiblingId = newSiblingId;
        if (targetBlockId == null || newSiblingId == null)
        {
            throw new IllegalArgumentException("BlockIds cannot be null");
        }
    }
}