package com.team.editor.common.ops.character;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.editor.common.crdt.character.CharId;

public record RestoreCharOp(CharId id, CharId parentId, Character value, CharacterFormatState formatState)
{
    @JsonCreator
    public RestoreCharOp(
            @JsonProperty("id") CharId id,
            @JsonProperty("parentId") CharId parentId,
            @JsonProperty("value") Character value,
            @JsonProperty("formatState") CharacterFormatState formatState)
    {
        this.id = id;
        this.parentId = parentId;
        this.value = value;
        this.formatState = formatState;
        if (id == null)
        {
            throw new IllegalArgumentException("CharId cannot be null");
        }
    }

    public RestoreCharOp(CharId id)
    {
        this(id, null, null, null);
    }
}
