package com.team.editor.common.ops.block;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.editor.common.crdt.block.BlockId;

public record InsertBlockOp(BlockId id, BlockId parentId)
{
    @JsonCreator
    public InsertBlockOp(
            @JsonProperty("id") BlockId id,
            @JsonProperty("parentId") BlockId parentId)
    {
        this.id = id;
        this.parentId = parentId;
        if (id == null)
        {
            throw new IllegalArgumentException("BlockId cannot be null");
        }
    }
}