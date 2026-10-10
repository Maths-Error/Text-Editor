package com.team.editor.common.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.model.UserId;

class CharIdTest {

    @Test
    void shouldCreateCharIdSuccessfully() {
        CharId charId = new CharId(new UserId("user-1"), 5);

        assertEquals("user-1", charId.getUserId().getValue());
        assertEquals(5, charId.getClock());
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> new CharId(null, 1));
    }

    @Test
    void shouldRejectNegativeClock() {
        assertThrows(IllegalArgumentException.class, () -> new CharId(new UserId("user-1"), -1));
    }

    @Test
    void shouldTreatSameUserAndClockAsEqual() {
        CharId first = new CharId(new UserId("user-1"), 10);
        CharId second = new CharId(new UserId("user-1"), 10);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldTreatDifferentClockAsNotEqual() {
        CharId first = new CharId(new UserId("user-1"), 10);
        CharId second = new CharId(new UserId("user-1"), 11);

        assertNotEquals(first, second);
    }

    @Test
    void shouldCompareByClockFirst() {
        CharId lowerClock = new CharId(new UserId("user-1"), 2);
        CharId higherClock = new CharId(new UserId("user-1"), 5);

        assertTrue(lowerClock.compareTo(higherClock) < 0);
        assertTrue(higherClock.compareTo(lowerClock) > 0);
    }

    @Test
    void shouldCompareByUserIdWhenClockMatches() {
        CharId first = new CharId(new UserId("alice"), 7);
        CharId second = new CharId(new UserId("bob"), 7);

        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(first) > 0);
    }
}