package com.team.editor.common.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UserIdTest {

    @Test
    void shouldCreateUserIdSuccessfully() {
        UserId userId = new UserId("user-1");

        assertEquals("user-1", userId.getValue());
    }

    @Test
    void shouldTrimUserIdValue() {
        UserId userId = new UserId("  user-2  ");

        assertEquals("user-2", userId.getValue());
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new UserId("   "));
    }

    @Test
    void shouldCompareEqualWhenValuesMatch() {
        UserId first = new UserId("user-1");
        UserId second = new UserId("user-1");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldCompareByLexicographicOrder() {
        UserId first = new UserId("alice");
        UserId second = new UserId("bob");

        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(first) > 0);
    }
}