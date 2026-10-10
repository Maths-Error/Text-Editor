package com.team.editor.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.team.editor.common.crdt.block.BlockCrdtEngine;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.block.BlockNode;
import com.team.editor.common.crdt.document.DocumentCrdt;
import com.team.editor.common.model.DocumentId;
import com.team.editor.common.model.UserId;
import java.util.List;

class BlockCrdtEngineTest {

    @Test
    void shouldInitializeWithCorrectUserIdAndZeroClock() {
        UserId user = new UserId("user-1");
        BlockCrdtEngine engine = new BlockCrdtEngine(user);

        assertEquals(user, engine.getUserId());
        assertEquals(0, engine.getBlockClock());
    }

    @Test
    void shouldIncrementClockOnLocalInsert() {
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("user-1"));
        
        BlockId id1 = engine.insert(null);
        assertEquals(1, engine.getBlockClock());
        assertEquals(1, id1.getClock());

        BlockId id2 = engine.insert(id1);
        assertEquals(2, engine.getBlockClock());
        assertEquals(2, id2.getClock());
    }

    @Test
    void shouldSyncClockOnApplyInsert() {
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("user-1"));
        BlockId externalId = new BlockId(new UserId("user-2"), 50);

        engine.applyInsert(externalId, null);

        assertEquals(50, engine.getBlockClock());
    }

    @Test
    void shouldReturnVisibleBlocksInOrder() {
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("user-1"));
        
        BlockId b1 = engine.insert(null);
        BlockId b2 = engine.insert(null);

        List<BlockNode> visible = engine.getVisibleBlocks();
        assertEquals(2, visible.size());
        assertEquals(b2, visible.get(0).getId());
        assertEquals(b1, visible.get(1).getId());
    }

    @Test
    void shouldHandleDeletion() {
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("user-1"));
        BlockId b1 = engine.insert(null);
        
        engine.delete(b1);

        assertTrue(engine.getVisibleBlocks().isEmpty());
    }

    @Test
    void splitBlockShouldRenderTheNewBlockImmediatelyAfterTheTarget() {
        UserId user = new UserId("user-1");
        BlockCrdtEngine engine = new BlockCrdtEngine(user);
        BlockId rootBlock = engine.insert(null);
        BlockId followingBlock = engine.insert(rootBlock);
        insertText(engine, rootBlock, "abc");
        insertText(engine, followingBlock, "xyz");

        BlockId insertedBlock = engine.splitBlock(rootBlock);
        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-1"), engine);

        assertEquals("abc\n\nxyz", document.getVisibleText());
        List<BlockNode> visible = engine.getVisibleBlocks();
        assertEquals(rootBlock, visible.get(0).getId());
        assertEquals(insertedBlock, visible.get(1).getId());
        assertEquals(followingBlock, visible.get(2).getId());
    }

    @Test
    void applySplitShouldMatchLocalSplitRenderingOrder() {
        UserId alice = new UserId("alice");
        BlockId targetBlockId = new BlockId(new UserId("SYSTEM"), 0);
        BlockId existingFollowingBlockId = new BlockId(alice, 1);
        BlockId splitBlockId = new BlockId(new UserId("bob"), 2);

        BlockCrdtEngine engine = new BlockCrdtEngine(alice);
        engine.applyInsert(targetBlockId, null);
        engine.applyInsert(existingFollowingBlockId, targetBlockId);
        insertText(engine, targetBlockId, "abc");
        insertText(engine, existingFollowingBlockId, "xyz");

        engine.applySplit(targetBlockId, splitBlockId);
        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-2"), engine);

        assertEquals("abc\n\nxyz", document.getVisibleText());
        List<BlockNode> visible = engine.getVisibleBlocks();
        assertEquals(targetBlockId, visible.get(0).getId());
        assertEquals(splitBlockId, visible.get(1).getId());
        assertEquals(existingFollowingBlockId, visible.get(2).getId());
    }

    @Test
    void applyInsertShouldRenderNewChildImmediatelyAfterTheTargetBlock() {
        UserId alice = new UserId("alice");
        BlockId targetBlockId = new BlockId(new UserId("SYSTEM"), 0);
        BlockId existingFollowingBlockId = new BlockId(alice, 1);
        BlockId insertedBlockId = new BlockId(alice, 2);

        BlockCrdtEngine engine = new BlockCrdtEngine(alice);
        engine.applyInsert(targetBlockId, null);
        engine.applyInsert(existingFollowingBlockId, targetBlockId);
        insertText(engine, targetBlockId, "abc");
        insertText(engine, existingFollowingBlockId, "xyz");

        engine.applyInsert(insertedBlockId, targetBlockId);
        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-3"), engine);

        assertEquals("abc\n\nxyz", document.getVisibleText());
        List<BlockNode> visible = engine.getVisibleBlocks();
        assertEquals(targetBlockId, visible.get(0).getId());
        assertEquals(insertedBlockId, visible.get(1).getId());
        assertEquals(existingFollowingBlockId, visible.get(2).getId());
    }

    private void insertText(BlockCrdtEngine engine, BlockId blockId, String text) {
        com.team.editor.common.crdt.character.CharId parentId = null;
        for (char ch : text.toCharArray()) {
            parentId = engine.insertChar(blockId, parentId, ch);
        }
    }
}
