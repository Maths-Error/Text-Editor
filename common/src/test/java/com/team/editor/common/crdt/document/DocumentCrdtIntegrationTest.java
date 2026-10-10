package com.team.editor.common.crdt.document;

import com.team.editor.common.crdt.block.BlockCrdtEngine;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.block.BlockNode;
import com.team.editor.common.crdt.block.BlockTree;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.model.DocumentId;
import com.team.editor.common.model.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentCrdtIntegrationTest {

    // Verifies that a brand-new document with no blocks renders as empty text.
    @Test
    void emptyDocumentReturnsEmptyString() {
        BlockCrdtEngine engine = new BlockCrdtEngine(new UserId("u1"));
        DocumentCrdt document = new DocumentCrdt(
                new DocumentId("doc-1"),
                engine
        );

        assertEquals("", document.getVisibleText());
    }

    // Confirms that the document delegates rendering to the single visible block.
    @Test
    void singleBlockRendersItsVisibleText() {
        UserId user = new UserId("u1");
        BlockCrdtEngine engine = new BlockCrdtEngine(user);
        BlockTree blockTree = engine.getTree();

        BlockId blockId = new BlockId(user, 1);
        blockTree.insert(blockId, null);

        BlockNode block = blockTree.getNode(blockId);
        insertText(block, user, 1, "Hello");

        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-2"), engine);

        assertEquals("Hello", document.getVisibleText());
    }

    // Ensures visible blocks are rendered in order and separated by newline characters.
    @Test
    void multipleVisibleBlocksAreJoinedWithNewLine() {
        UserId user = new UserId("u1");
        BlockCrdtEngine engine = new BlockCrdtEngine(user);
        BlockTree blockTree = engine.getTree();

        BlockId block1 = new BlockId(user, 1);
        BlockId block2 = new BlockId(user, 2);

        blockTree.insert(block1, null);
        blockTree.insert(block2, block1);

        insertText(blockTree.getNode(block1), user, 1, "Hello");
        insertText(blockTree.getNode(block2), user, 10, "World");

        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-3"), engine);

        assertEquals("Hello\nWorld", document.getVisibleText());
    }

    // Checks that tombstoned characters stay hidden when the document text is generated.
    @Test
    void deletedCharactersDoNotAppearInRenderedDocument() {
        UserId user = new UserId("u1");
        BlockCrdtEngine engine = new BlockCrdtEngine(user);
        BlockTree blockTree = engine.getTree();

        BlockId blockId = new BlockId(user, 1);
        blockTree.insert(blockId, null);

        BlockNode block = blockTree.getNode(blockId);

        CharId a = new CharId(user, 1);
        CharId b = new CharId(user, 2);
        CharId c = new CharId(user, 3);

        block.getInnerTree().insert(a, null, 'a');
        block.getInnerTree().insert(b, a, 'b');
        block.getInnerTree().insert(c, b, 'c');

        block.getInnerTree().delete(b);

        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-4"), engine);

        assertEquals("ac", document.getVisibleText());
    }

    // Verifies that deleted blocks are excluded even if they still contain visible characters.
    @Test
    void deletedBlocksDoNotAppearInRenderedDocument() {
        UserId user = new UserId("u1");
        BlockCrdtEngine engine = new BlockCrdtEngine(user);
        BlockTree blockTree = engine.getTree();

        BlockId hiddenBlock = new BlockId(user, 2);
        BlockId visibleBlock = new BlockId(user, 1);

        blockTree.insert(hiddenBlock, null);
        blockTree.insert(visibleBlock, null);

        insertText(blockTree.getNode(hiddenBlock), user, 1, "Hidden");
        insertText(blockTree.getNode(visibleBlock), user, 10, "Shown");

        blockTree.delete(hiddenBlock);

        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-5"), engine);

        assertEquals("Shown", document.getVisibleText());
    }

    // Confirms overflow handling creates a following block and inserts the overflow text into it.
    @Test
    void handleBlockOverflowCreatesNewBlockAndPastesOverflowText() {
        UserId user = new UserId("u1");
        BlockCrdtEngine engine = new BlockCrdtEngine(user);
        BlockTree blockTree = engine.getTree();

        BlockId originalBlock = engine.insert(null);
        insertText(blockTree.getNode(originalBlock), user, 100, "Hello");

        DocumentCrdt document = new DocumentCrdt(new DocumentId("doc-6"), engine);

        BlockId newBlock = document.handleBlockOverflow(originalBlock, "World");

        assertEquals("Hello\nWorld", document.getVisibleText());
        assertEquals("World", blockTree.getNode(newBlock).getInnerTree().generateVisibleText());
    }

    private void insertText(BlockNode block, UserId userId, long startingClock, String text) {
        CharId parentId = null;
        long clock = startingClock;

        for (char ch : text.toCharArray()) {
            CharId currentId = new CharId(userId, clock++);
            block.getInnerTree().insert(currentId, parentId, ch);
            parentId = currentId;
        }
    }
}
