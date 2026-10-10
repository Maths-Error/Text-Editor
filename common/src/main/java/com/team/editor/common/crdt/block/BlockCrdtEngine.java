package com.team.editor.common.crdt.block;

import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.model.UserId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class BlockCrdtEngine
{
    private final UserId userId;
    private final BlockTree tree;
    private long blockClock;
    private long charClock;
    private final Map<BlockId, List<BlockId>> orphanBuffer;
    private final Map<BlockId, List<BlockId>> pendingSplits;

    // setup the engine and start clocks at 0

    public BlockCrdtEngine(UserId userId) {
        if (userId == null)
        { throw new IllegalArgumentException("UserId cannot be null"); }
        this.userId = userId;
        this.tree = new BlockTree();
        this.blockClock = 0;
        this.charClock = 0;
        this.orphanBuffer = new HashMap<>();
        this.pendingSplits = new HashMap<>();
    }

    // Incremendt the clock and throw a new block into the tree under a parent
    public BlockId insert(BlockId parentId) {
        blockClock++;
        BlockId newId = new BlockId(userId, blockClock);
        tree.insert(newId, parentId);
        return newId;
    }

    public void ensureBlockClockAtLeast(long value) {
        if (value > this.blockClock) {
            this.blockClock = value;
        }
    }

    public void ensureCharacterClockAtLeast(long value) {
        if (value > this.charClock) {
            this.charClock = value;
        }
    }

    // handle an incoming block insert from someone else
    //  if no parent yet -> put it orphanBuffer (7arfeyan)

    public void applyInsert(BlockId id, BlockId parentId)
    {
        try {
            tree.insert(id, parentId);
            this.blockClock = Math.max(this.blockClock, id.getClock());
            
            processOrphans(id);
            processPendingSplits(id);
        } catch (IllegalStateException e)
        {
            orphanBuffer.computeIfAbsent(parentId, k -> new ArrayList<>()).add(id);
        }
    }

    // Creates a new block as a child of the target block so DFS renders it immediately after the target.
    public BlockId splitBlock(BlockId targetBlockId)
    {
        BlockNode targetNode = tree.getNode(targetBlockId);
        if (targetNode == null) {
            throw new IllegalArgumentException("Cannot split a block that does not exist.");
        }
        
        blockClock++;
        BlockId newSiblingId = new BlockId(userId, blockClock);
        tree.insert(newSiblingId, targetBlockId);
        
        return newSiblingId;
    }

    // sync a split from the network. save it for later if the target block hasn't arrived yet

    public void applySplit(BlockId targetBlockId, BlockId newSiblingId)
    {
        BlockNode targetNode = tree.getNode(targetBlockId);
        if (targetNode != null)
        {
            applyInsert(newSiblingId, targetBlockId);
        } else {
            pendingSplits.computeIfAbsent(targetBlockId, k -> new ArrayList<>()).add(newSiblingId);
        }
    }

    // Delete A block
    public void delete(BlockId id) {
        tree.delete(id);
    }
    public void applyDelete(BlockId id) {
        tree.delete(id);
    }

    // type a single letter into a specific block
    public CharId insertChar(BlockId blockId, CharId parentCharId, char value) {
        BlockNode block = tree.getNode(blockId);
        if (block == null) throw new IllegalArgumentException("Target Block not found.");
        
        charClock++;
        CharId newCharId = new CharId(userId, charClock);
        block.getInnerTree().insert(newCharId, parentCharId, value);
        return newCharId;
    }

    // hit backspace on a specific letter
    public void deleteChar(BlockId blockId, CharId charId) {
        BlockNode block = tree.getNode(blockId);
        if (block != null) {
            block.getInnerTree().delete(charId);
        }
    }

    public void restoreChar(BlockId blockId, CharId charId) {
        BlockNode block = tree.getNode(blockId);
        if (block != null) {
            block.getInnerTree().restore(charId);
        }
    }

    // swap out one character for another by just doing a delete then insert

    public CharId replaceChar(BlockId blockId, CharId targetCharId, char newValue) {
        deleteChar(blockId, targetCharId);
        return insertChar(blockId, targetCharId, newValue);
    }

    // drop a chunk of text in all at once
    // we just loops through and inserts each letter
    
    public CharId pasteTextIntoBlock(BlockId blockId, CharId startParentId, String text) {
        CharId currentParent = startParentId;
        for (char c : text.toCharArray()) {
            currentParent = insertChar(blockId, currentParent, c);
        }
        return currentParent;
    }

    // Process a character typed by another user over the network

    public void applyInsertChar(BlockId blockId, CharId charId, CharId parentCharId, char value)
    {
        BlockNode block = tree.getNode(blockId);
        if (block != null) {
            if (charId.getUserId().equals(userId))
            {
                this.charClock = Math.max(this.charClock, charId.getClock());
            }
            try{
                block.getInnerTree().insert(charId, parentCharId, value);
            } catch (IllegalStateException e)
            { }
        }
    }

    // Process a character deletion from someone else

    public void applyDeleteChar(BlockId blockId, CharId charId)
    {
        BlockNode block = tree.getNode(blockId);
        if (block != null) { block.getInnerTree().delete(charId); }
    }

    public void applyRestoreChar(BlockId blockId, CharId charId)
    {
        BlockNode block = tree.getNode(blockId);
        if (block != null) { block.getInnerTree().restore(charId); }
    }

    // duplicate a whole block structure and grab all the inner text too

    public BlockId copyBlock(BlockId sourceBlockId, BlockId targetParentId)
    {
        BlockNode sourceNode = tree.getNode(sourceBlockId);
        if (sourceNode == null) throw new IllegalArgumentException("Cannot copy a block that does not exist.");
        
        BlockId cloneId = insert(targetParentId);
        
        String textToCopy = sourceNode.getInnerTree().generateVisibleText();
        if (!textToCopy.isEmpty()) {
            pasteTextIntoBlock(cloneId, null, textToCopy);
        }
        
        return cloneId;
    }

    // Move block [Changing its order in the document] is a 4 steps fuction:
    // grab the text -> kill the old block -> make a new one -> paste the text back in

    public BlockId moveBlock(BlockId sourceBlockId, BlockId targetParentId)
    {
        BlockNode sourceNode = tree.getNode(sourceBlockId);
        if (sourceNode == null) throw new IllegalArgumentException("Cannot move a block that does not exist.");
        
        String textToMove = sourceNode.getInnerTree().generateVisibleText();
        delete(sourceBlockId);
        BlockId newLocationId = insert(targetParentId);
        
        if (!textToMove.isEmpty())
        {
            pasteTextIntoBlock(newLocationId, null, textToMove);
        }
        return newLocationId;
    }

    // take text from one block and put it directly into another

    public CharId copyContentBetweenBlocks(BlockId sourceBlockId, BlockId targetBlockId, CharId targetStartParentId)
    {
        BlockNode sourceNode = tree.getNode(sourceBlockId);
        if (sourceNode == null) throw new IllegalArgumentException("Source block not found.");
        
        String textToCopy = sourceNode.getInnerTree().generateVisibleText();
        return pasteTextIntoBlock(targetBlockId, targetStartParentId, textToCopy);
    }

    // check if any blocks were waiting in the buffer for this new parent to show up

    private void processOrphans(BlockId newlyInsertedParentId)
    {
        if (orphanBuffer.containsKey(newlyInsertedParentId))
        {
            List<BlockId> orphans = orphanBuffer.remove(newlyInsertedParentId);
            for (BlockId orphanId : orphans)
            {
                applyInsert(orphanId, newlyInsertedParentId);
            }
        }
    }

    // check if any network splits were waiting for this block to arrive

    private void processPendingSplits(BlockId newlyInsertedTargetId)
    {
        if (pendingSplits.containsKey(newlyInsertedTargetId))
        {
            List<BlockId> siblingIds = pendingSplits.remove(newlyInsertedTargetId);
            for (BlockId siblingId : siblingIds)
            {
                applyInsert(siblingId, newlyInsertedTargetId);
            }
        }
    }

    // Getters functions
    public List<BlockNode> getVisibleBlocks() { return tree.generateVisibleBlocks(); }
    public BlockId getRootId()  { return tree.getVirtualRoot().getId(); }
    public UserId getUserId()   { return userId; }
    public long getBlockClock() { return blockClock; }
    public BlockTree getTree() { return tree; }
}
