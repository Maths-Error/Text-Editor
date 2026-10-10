package com.team.editor.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.model.UserId;

class BlockIdTest {

    @Test
    void shouldCreateBlockIdSuccessfully() {
        BlockId blockId = new BlockId(new UserId("user-1"), 5);

        assertEquals("user-1", blockId.getUserId().getValue());
        assertEquals(5, blockId.getClock());
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(IllegalArgumentException.class, () -> new BlockId(null, 1));
    }

    @Test
    void shouldTreatSameUserAndClockAsEqual() {
        BlockId first = new BlockId(new UserId("user-1"), 10);
        BlockId second = new BlockId(new UserId("user-1"), 10);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldTreatDifferentClockAsNotEqual() {
        BlockId first = new BlockId(new UserId("user-1"), 10);
        BlockId second = new BlockId(new UserId("user-1"), 11);

        assertNotEquals(first, second);
    }

    @Test
    void shouldCompareByClockDescendingFirst()
    {
        // According to Conflict Resolution rule: Primary Sort is Descending clock
        BlockId lowerClock = new BlockId(new UserId("user-1"), 2);
        BlockId higherClock = new BlockId(new UserId("user-1"), 5);
        assertTrue(higherClock.compareTo(lowerClock) < 0);
        assertTrue(lowerClock.compareTo(higherClock) > 0);
    }

    @Test
    void shouldCompareByUserIdDescendingWhenClockMatches()
    {
        // According to Conflict Resolution rule: Secondary Sort is Descending UserId
        BlockId alice = new BlockId(new UserId("alice"), 7);
        BlockId bob = new BlockId(new UserId("bob"), 7);

        assertTrue(bob.compareTo(alice) < 0);
        assertTrue(alice.compareTo(bob) > 0);
    }
}