package com.team.editor.common.model;

import java.util.Objects;
import java.util.UUID;

public final class DocumentId {
    private final String value;

    public DocumentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DocumentId value cannot be null or blank");
        }
        this.value = value;
    }

    public static DocumentId newId() {
        return new DocumentId(UUID.randomUUID().toString());
    }

    public String value() {
        return value;
    }

    // override since java already has a method equals we want to override it
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentId)) return false;
        DocumentId that = (DocumentId) o;
        return value.equals(that.value);
    }

    // Hash code is based on the ID value so we can use it in hash collections, matching equals().
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    // for when Java wants a text representation of the whole object.
    @Override
    public String toString() {
        return value;
    }
}