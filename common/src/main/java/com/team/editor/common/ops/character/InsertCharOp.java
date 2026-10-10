package com.team.editor.common.ops.character;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.team.editor.common.crdt.character.CharId;

public record InsertCharOp(CharId id, CharId parentId, char value, CharacterFormatState formatState)
{
    @JsonCreator
    public InsertCharOp(
            @JsonProperty("id") CharId id, 
            @JsonProperty("parentId") CharId parentId, 
            @JsonProperty("value") char value,
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

    public InsertCharOp(CharId id, CharId parentId, char value)
    {
        this(id, parentId, value, null);
    }
}
