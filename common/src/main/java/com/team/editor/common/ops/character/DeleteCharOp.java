package com.team.editor.common.ops.character;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.editor.common.crdt.character.CharId;

public record DeleteCharOp(CharId id, CharId parentId, Character value, CharacterFormatState formatState)
{
    @JsonCreator
    public DeleteCharOp(
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

    public DeleteCharOp(CharId id, CharId parentId, Character value)
    {
        this(id, parentId, value, null);
    }

    public DeleteCharOp(CharId id)
    {
        this(id, null, null, null);
    }
}
