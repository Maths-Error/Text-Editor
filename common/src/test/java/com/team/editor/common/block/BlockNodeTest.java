package com.team.editor.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.block.BlockNode;
import com.team.editor.common.model.UserId;

class BlockNodeTest {

    @Test
    void shouldCreateBlockNodeSuccessfully() {
        BlockId id = new BlockId(new UserId("user-1"), 1);
        BlockId parentId = new BlockId(new UserId("user-1"), 0);

        BlockNode node = new BlockNode(id, parentId);

        assertEquals(id, node.getId());
        assertEquals(parentId, node.getParentId());
        assertFalse(node.isDeleted());
        assertNotNull(node.getInnerTree());
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    void shouldAllowNullParentForRootLevelBlock() {
        BlockId id = new BlockId(new UserId("user-1"), 1);

        BlockNode node = new BlockNode(id, null);

        assertNull(node.getParentId());
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(IllegalArgumentException.class, () -> new BlockNode(null, null));
    }

    @Test
    void shouldMarkNodeAsDeleted() {
        BlockId id = new BlockId(new UserId("user-1"), 3);
        BlockNode node = new BlockNode(id, null);

        node.setDeleted(true);

        assertTrue(node.isDeleted());
    }

    @Test
    void shouldAddChildrenSuccessfully() {
        BlockNode parent = new BlockNode(new BlockId(new UserId("user-1"), 1), null);
        BlockNode child = new BlockNode(new BlockId(new UserId("user-1"), 2), parent.getId());

        parent.getChildren().add(child);

        assertEquals(1, parent.getChildren().size());
        assertEquals(child, parent.getChildren().get(0));
    }
}