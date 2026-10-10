package com.team.editor.client.renderer;

import com.team.editor.client.state.EditorState;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.util.List;

/**
 * Applies CRDT formatting state to Swing's styled document after text refreshes.
 */
public final class SwingFormattingRenderer {
    private SwingFormattingRenderer() {
    }

    public static void applyFormatting(StyledDocument document, List<EditorState.FormatRun> formatRuns) {
        if (document == null) {
            return;
        }

        SimpleAttributeSet plain = new SimpleAttributeSet();
        StyleConstants.setBold(plain, false);
        StyleConstants.setItalic(plain, false);
        StyleConstants.setUnderline(plain, false);
        document.setCharacterAttributes(0, document.getLength(), plain, false);

        if (formatRuns == null) {
            return;
        }

        for (EditorState.FormatRun run : formatRuns) {
            int start = Math.max(0, Math.min(run.getStartOffset(), document.getLength()));
            int end = Math.max(start, Math.min(run.getEndOffset(), document.getLength()));
            if (start >= end) {
                continue;
            }

            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setBold(attributes, run.isBold());
            StyleConstants.setItalic(attributes, run.isItalic());
            StyleConstants.setUnderline(attributes, run.isUnderline());
            document.setCharacterAttributes(start, end - start, attributes, false);

            for (int offset = start; offset < end; offset++) {
                AttributeSet storedAttributes = document.getCharacterElement(offset).getAttributes();
                System.out.println(
                        "SWING FORMAT APPLIED -> offset="
                                + offset
                                + ", expectedBold="
                                + run.isBold()
                                + ", actualBold="
                                + StyleConstants.isBold(storedAttributes)
                                + ", expectedItalic="
                                + run.isItalic()
                                + ", actualItalic="
                                + StyleConstants.isItalic(storedAttributes)
                );
            }
        }
    }
}
