package com.team.editor.common.protocol.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OperationKind {
    CHARACTER_INSERT("INSERT_CHAR"),
    CHARACTER_DELETE("DELETE_CHAR"),
    CHARACTER_RESTORE("RESTORE_CHAR"),
    CHARACTER_REPLACE("REPLACE_CHAR"),
    BLOCK_INSERT("INSERT_BLOCK"),
    BLOCK_DELETE("DELETE_BLOCK"),
    BLOCK_MOVE("MOVE_BLOCK"),
    BLOCK_SPLIT("SPLIT_BLOCK"),
    FORMAT_APPLY("APPLY_FORMAT"),
    FORMAT_REMOVE("REMOVE_FORMAT"),
    INSERT_TEXT("INSERT_TEXT"),
    DELETE_TEXT("DELETE_TEXT");

    private final String wireName;

    OperationKind(String wireName) {
        this.wireName = wireName;
    }

    @JsonCreator
    public static OperationKind fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("OperationKind cannot be null");
        }

        for (OperationKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value) || kind.wireName.equalsIgnoreCase(value)) {
                return kind;
            }
        }

        throw new IllegalArgumentException("Unknown OperationKind: " + value);
    }

    @JsonValue
    public String getWireName() {
        return wireName;
    }
}
