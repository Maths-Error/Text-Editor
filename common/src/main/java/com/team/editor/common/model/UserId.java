package com.team.editor.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class UserId implements Comparable<UserId> {
    private final String value;

    /**
     * Creates a typed user id from the JSON field named {@code value}.
     * Jackson uses this constructor when a join request is deserialized from websocket JSON.
     * If the value is missing or blank, the request should be rejected instead of creating an invalid user id.
     */
    @JsonCreator
    public UserId(@JsonProperty("value") String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId value cannot be null or blank");
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public int compareTo(UserId other) {
        if (other == null) {
            throw new IllegalArgumentException("Other UserId cannot be null");
        }
        return this.value.compareTo(other.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId userId)) return false;
        return Objects.equals(value, userId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
