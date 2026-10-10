package com.team.editor.common.ops.block;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.editor.common.crdt.block.BlockId;

public record DeleteBlockOp(BlockId id)
{
    @JsonCreator
    public DeleteBlockOp(@JsonProperty("id") BlockId id)
    {
        this.id = id;
        if (id == null)
        {
            throw new IllegalArgumentException("BlockId cannot be null");
        }
    }
}