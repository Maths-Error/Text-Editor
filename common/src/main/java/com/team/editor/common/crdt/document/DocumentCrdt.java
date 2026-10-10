package com.team.editor.common.crdt.document;

import com.team.editor.common.crdt.block.BlockCrdtEngine;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.block.BlockNode;
import com.team.editor.common.crdt.block.BlockTree;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.model.DocumentId;

import java.util.List;
import java.util.Objects;

public class DocumentCrdt {
    private final DocumentId documentId;
    private final BlockCrdtEngine engine;
    private final BlockTree blockTree;

    // add a name for document later if needed
    private String name;

    public DocumentCrdt(DocumentId documentId, BlockCrdtEngine engine) {
        this.documentId = Objects.requireNonNull(documentId, "DocumentId cannot be null");
        this.engine = Objects.requireNonNull(engine, "BlockCrdtEngine cannot be null");
        this.blockTree = engine.getTree();
    }

    public DocumentId getDocumentId() {
        return documentId;
    }

    public BlockCrdtEngine getEngine() {
        return engine;
    }

    public BlockTree getBlockTree() {
        return blockTree;
    }

    public String getVisibleText() {
        StringBuilder result = new StringBuilder();
        boolean firstBlock = true;

        for (BlockNode block : blockTree.generateVisibleBlocks()) {
            String blockText = block.getInnerTree().generateVisibleText();

            if (!firstBlock) {
                result.append('\n');
            }

            result.append(blockText);
            firstBlock = false;
        }

        return result.toString();
    }

    public BlockId handleBlockOverflow(BlockId overstuffedBlockId, String overflowText) {
        if (overflowText == null || overflowText.isEmpty()) {
            throw new IllegalArgumentException("Overflow text cannot be null or empty");
        }

        if (blockTree.getNode(overstuffedBlockId) == null) {
            throw new IllegalArgumentException("Source block does not exist");
        }

        BlockId newBlockId = engine.insert(overstuffedBlockId);
        engine.pasteTextIntoBlock(newBlockId, null, overflowText);
        return newBlockId;
    }

    public void removeOverflowCharacters(BlockId sourceBlockId, List<CharId> charIdsToDelete) {
        if (charIdsToDelete == null || charIdsToDelete.isEmpty()) {
            return;
        }

        for (CharId charId : charIdsToDelete) {
            engine.deleteChar(sourceBlockId, charId);
        }
    }
}