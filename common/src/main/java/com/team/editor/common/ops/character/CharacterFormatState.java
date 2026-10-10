package com.team.editor.common.ops.character;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Optional formatting metadata carried with character operations.
 */
public record CharacterFormatState(boolean bold, boolean italic, boolean underline) {
    @JsonCreator
    public CharacterFormatState(
            @JsonProperty("bold") boolean bold,
            @JsonProperty("italic") boolean italic,
            @JsonProperty("underline") boolean underline
    ) {
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
    }

    public boolean hasAnyFormatting() {
        return bold || italic || underline;
    }
}
