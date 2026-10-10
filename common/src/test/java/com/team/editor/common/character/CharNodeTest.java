package com.team.editor.common.character;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.crdt.character.CharNode;
import com.team.editor.common.model.UserId;

class CharNodeTest {

    @Test
    void shouldCreateCharNodeSuccessfully() {
        CharId id = new CharId(new UserId("user-1"), 1);
        CharId parentId = new CharId(new UserId("user-1"), 0);

        CharNode node = new CharNode(id, parentId, 'A');

        assertEquals(id, node.getId());
        assertEquals(parentId, node.getParentId());
        assertEquals('A', node.getValue());
        assertFalse(node.isTombstone());
        assertTrue(node.isVisible());
    }

    @Test
    void shouldAllowNullParentForRootLevelCharacter() {
        CharId id = new CharId(new UserId("user-1"), 1);

        CharNode node = new CharNode(id, null, 'R');

        assertNull(node.getParentId());
        assertEquals('R', node.getValue());
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(IllegalArgumentException.class, () -> new CharNode(null, null, 'X'));
    }

    @Test
    void shouldMarkNodeAsDeletedWithoutRemovingIt() {
        CharId id = new CharId(new UserId("user-1"), 3);
        CharNode node = new CharNode(id, null, 'Z');

        node.markDeleted();

        assertTrue(node.isTombstone());
        assertFalse(node.isVisible());
        assertEquals('Z', node.getValue());
        assertEquals(id, node.getId());
    }
}