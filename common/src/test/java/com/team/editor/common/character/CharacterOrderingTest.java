package com.team.editor.common.character;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.crdt.character.CharNode;
import com.team.editor.common.crdt.character.CharacterOrdering;
import com.team.editor.common.model.UserId;

class CharacterOrderingTest {

    @Test
    void shouldPlaceHigherClockBeforeLowerClock() {
        CharNode lower = new CharNode(new CharId(new UserId("user-1"), 1), null, 'A');
        CharNode higher = new CharNode(new CharId(new UserId("user-2"), 5), null, 'B');

        List<CharNode> nodes = new ArrayList<>(List.of(lower, higher));
        nodes.sort(CharacterOrdering.SIBLING_ORDER);

        assertEquals(higher, nodes.get(0));
        assertEquals(lower, nodes.get(1));
    }

 @Test
    void shouldUseUserIdAsTieBreakerWhenClockMatches() {
        CharNode first = new CharNode(new CharId(new UserId("alice"), 3), null, 'A');
        CharNode second = new CharNode(new CharId(new UserId("bob"), 3), null, 'B');

        List<CharNode> nodes = new ArrayList<>(List.of(second, first));
        nodes.sort(CharacterOrdering.SIBLING_ORDER);

        assertEquals(second, nodes.get(0));
        assertEquals(first, nodes.get(1));
    }

    @Test
    void shouldReturnZeroWhenComparingSameNodeReference() {
        CharNode node = new CharNode(new CharId(new UserId("user-1"), 1), null, 'X');

        assertEquals(0, CharacterOrdering.compareSiblings(node, node));
    }

    @Test
    void shouldRejectNullNodes() {
        CharNode node = new CharNode(new CharId(new UserId("user-1"), 1), null, 'X');

        assertThrows(IllegalArgumentException.class, () -> CharacterOrdering.compareSiblings(node, null));
        assertThrows(IllegalArgumentException.class, () -> CharacterOrdering.compareSiblings(null, node));
    }
}