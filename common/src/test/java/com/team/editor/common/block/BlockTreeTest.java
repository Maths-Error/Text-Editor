package com.team.editor.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.block.BlockNode;
import com.team.editor.common.crdt.block.BlockTree;
import com.team.editor.common.model.UserId;
import java.util.List;

class BlockTreeTest {

    @Test
    void shouldInsertBlocksCorrectily() {
        BlockTree tree = new BlockTree();
        UserId user = new UserId("user-1");
        BlockId b1 = new BlockId(user, 1);
        BlockId b2 = new BlockId(user, 2);

        tree.insert(b1, null); // Child of virtual root
        tree.insert(b2, b1);   // Child of b1

        assertEquals(b1, tree.getNode(b1).getId());
        assertEquals(b2, tree.getNode(b2).getId());
        assertEquals(1, tree.getNode(b1).getChildren().size());
    }

    @Test
    void shouldSortSiblingsByDescendingClock() {
        BlockTree tree = new BlockTree();
        UserId user = new UserId("user-1");
        BlockId earlier = new BlockId(user, 1);
        BlockId later = new BlockId(user, 10);

        tree.insert(earlier, null);
        tree.insert(later, null);

        List<BlockNode> visible = tree.generateVisibleBlocks();
        assertEquals(2, visible.size());
        assertEquals(later, visible.get(0).getId());
        assertEquals(earlier, visible.get(1).getId());
    }

    @Test
    void shouldSortSiblingsByDescendingUserIdWhenClockMatches()
    {
        BlockTree tree = new BlockTree();
        BlockId alice = new BlockId(new UserId("alice"), 5);
        BlockId bob = new BlockId(new UserId("bob"), 5);

        tree.insert(alice, null);
        tree.insert(bob, null);

        List<BlockNode> visible = tree.generateVisibleBlocks();
        assertEquals(bob, visible.get(0).getId());
        assertEquals(alice, visible.get(1).getId());
    }

    @Test
    void shouldMaintainDfsOrder() {
        BlockTree tree = new BlockTree();
        UserId user = new UserId("user-1");
        BlockId b1 = new BlockId(user, 1);
        BlockId b1_1 = new BlockId(user, 2);
        BlockId b2 = new BlockId(user, 3);

        tree.insert(b1, null);
        tree.insert(b2, null);
        tree.insert(b1_1, b1);

        List<BlockNode> visible = tree.generateVisibleBlocks();
        assertEquals(3, visible.size());
        assertEquals(b2, visible.get(0).getId());
        assertEquals(b1, visible.get(1).getId());
        assertEquals(b1_1, visible.get(2).getId());
    }

    @Test
    void shouldHandleTombstones()
    {
        BlockTree tree = new BlockTree();
        BlockId b1 = new BlockId(new UserId("user-1"), 1);
        
        tree.insert(b1, null);
        tree.delete(b1);

        List<BlockNode> visible = tree.generateVisibleBlocks();
        assertTrue(visible.isEmpty());
    }
}