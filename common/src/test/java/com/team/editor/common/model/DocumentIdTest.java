package com.team.editor.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentIdTest {

    @Test
    void shouldCreateNewUniqueIds() {
        DocumentId id1 = DocumentId.newId();
        DocumentId id2 = DocumentId.newId();

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }

    @Test
    void shouldStoreGivenValue() {
        DocumentId id = new DocumentId("doc-123");

        assertEquals("doc-123", id.value());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentId(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentId(" "));
    }

    @Test
    void shouldCompareIdsByValue() {
        DocumentId id1 = new DocumentId("doc-123");
        DocumentId id2 = new DocumentId("doc-123");
        DocumentId id3 = new DocumentId("doc-999");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }

    @Test
    void shouldHaveSameHashCodeForEqualIds() {
        DocumentId id1 = new DocumentId("doc-123");
        DocumentId id2 = new DocumentId("doc-123");

        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void shouldReturnValueAsStringRepresentation() {
        DocumentId id = new DocumentId("doc-123");

        assertEquals("doc-123", id.toString());
    }
}