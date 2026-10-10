package com.team.editor.client.renderer;

import com.team.editor.client.state.EditorState;
import com.team.editor.common.crdt.block.BlockId;
import com.team.editor.common.crdt.character.CharId;
import com.team.editor.common.model.UserId;
import com.team.editor.common.ops.character.CharacterFormatState;
import org.junit.jupiter.api.Test;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyleConstants;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingFormattingRendererTest {
    @Test
    void applyFormattingSetsBoldAttributeAfterPlainTextRefresh() throws Exception {
        DefaultStyledDocument document = new DefaultStyledDocument();
        document.insertString(0, "ab", null);

        SwingFormattingRenderer.applyFormatting(
                document,
                List.of(new EditorState.FormatRun(0, 1, true, false, false))
        );

        assertTrue(StyleConstants.isBold(document.getCharacterElement(0).getAttributes()));
        assertFalse(StyleConstants.isBold(document.getCharacterElement(1).getAttributes()));
    }

    @Test
    void applyFormattingSetsItalicAttributeAfterPlainTextRefresh() throws Exception {
        DefaultStyledDocument document = new DefaultStyledDocument();
        document.insertString(0, "ab", null);

        SwingFormattingRenderer.applyFormatting(
                document,
                List.of(new EditorState.FormatRun(1, 2, false, true, false))
        );

        assertFalse(StyleConstants.isItalic(document.getCharacterElement(0).getAttributes()));
        assertTrue(StyleConstants.isItalic(document.getCharacterElement(1).getAttributes()));
    }

    @Test
    void restoredFormattedCharacterRendersBoldAfterPlainTextRefresh() throws Exception {
        EditorState state = new EditorState(new UserId("alice"), "doc-restore-format");
        BlockId blockId = state.getFirstBlockId();
        CharId charId = state.getEngine().insertChar(blockId, null, 'A');
        CharacterFormatState boldState = new CharacterFormatState(true, false, false);
        state.applyFormatStateToCharacter(blockId, charId, boldState);

        state.getEngine().deleteChar(blockId, charId);
        state.getEngine().applyRestoreChar(blockId, charId);
        state.applyFormatStateToCharacter(blockId, charId, boldState);

        DefaultStyledDocument document = new DefaultStyledDocument();
        document.insertString(0, state.getText(), null);
        SwingFormattingRenderer.applyFormatting(document, state.getFormatRuns());

        assertTrue(StyleConstants.isBold(document.getCharacterElement(0).getAttributes()));
    }
}
