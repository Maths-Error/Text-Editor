package com.team.editor.client.cursor;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Coordinates remote cursor state for the client editor surface.
 */
public class CursorManager {
    private final JTextPane textPane;
    private final Map<String, RemoteCursor> cursors = new HashMap<>();
    private final Map<String, Object> highlights = new HashMap<>();

    private final Color[] COLORS = {
            new Color(231, 76, 60),  // Red
            new Color(52, 152, 219), // Blue
            new Color(46, 204, 113), // Green
            new Color(155, 89, 182), // Purple
            new Color(241, 196, 15), // Yellow
            new Color(230, 126, 34)  // Orange
    };
    private int colorIndex = 0;

    public CursorManager(JTextPane textPane) {
        this.textPane = textPane;
    }

    public void updateCursor(String username, int offset) {
        SwingUtilities.invokeLater(() -> {
            RemoteCursor cursor = cursors.computeIfAbsent(username, k -> {
                Color color = COLORS[colorIndex % COLORS.length];
                colorIndex++;
                return new RemoteCursor(username, color, offset);
            });

            cursor.setOffset(offset);
            paintCursor(cursor);
        });
    }

    public void refreshCursors() {
        SwingUtilities.invokeLater(() -> {
            for (RemoteCursor cursor : new ArrayList<>(cursors.values())) {
                paintCursor(cursor);
            }
        });
    }

    public void removeCursor(String username) {
        SwingUtilities.invokeLater(() -> {
            cursors.remove(username);
            Object existingHighlight = highlights.remove(username);
            if (existingHighlight != null) {
                textPane.getHighlighter().removeHighlight(existingHighlight);
            }
        });
    }

    private void paintCursor(RemoteCursor cursor) {
        if (cursor == null) {
            return;
        }

        try {
            Highlighter highlighter = textPane.getHighlighter();
            String username = cursor.getUsername();
            Object existingHighlight = highlights.remove(username);
            if (existingHighlight != null) {
                highlighter.removeHighlight(existingHighlight);
            }

            int documentLength = textPane.getDocument().getLength();
            int safeOffset = Math.max(0, Math.min(cursor.getOffset(), documentLength));
            int highlightStart = safeOffset;
            int highlightEnd = safeOffset;

            if (documentLength > 0) {
                if (safeOffset < documentLength) {
                    highlightEnd = safeOffset + 1;
                } else {
                    highlightStart = safeOffset - 1;
                }
            }

            Object highlight = highlighter.addHighlight(
                    highlightStart,
                    highlightEnd,
                    new CaretPainter(cursor.getColor(), username, safeOffset)
            );
            highlights.put(username, highlight);
        } catch (BadLocationException e) {
            // Ignore temporary invalid positions
        }
    }

    private static class CaretPainter implements Highlighter.HighlightPainter {
        private final Color color;
        private final String name;
        private final int offset;

        public CaretPainter(Color color, String name, int offset) {
            this.color = color;
            this.name = name;
            this.offset = offset;
        }

        @Override
        public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {
            try {
                Rectangle r = findCaretRectangle(c);
                if (r == null) {
                    return;
                }

                Graphics2D g2 = (Graphics2D) g.create();

                int lineHeight = Math.max(r.height, c.getFontMetrics(c.getFont()).getHeight());
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(r.x, r.y, r.x, r.y + lineHeight);

                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(name);
                int textHeight = fm.getHeight();
                int labelY = r.y - textHeight;
                if (labelY < 0) {
                    labelY = r.y + lineHeight;
                }

                g2.fillRect(r.x, labelY, textWidth + 4, textHeight);
                g2.setColor(Color.WHITE);
                g2.drawString(name, r.x + 2, labelY + textHeight - 3);

                g2.dispose();
            } catch (BadLocationException e) {
                // Ignore
            }
        }

        private Rectangle findCaretRectangle(JTextComponent component) throws BadLocationException {
            int documentLength = component.getDocument().getLength();
            int safeOffset = Math.max(0, Math.min(offset, documentLength));
            Shape caretShape = component.modelToView2D(safeOffset);
            if (caretShape != null) {
                return caretShape.getBounds();
            }

            if (safeOffset > 0) {
                Shape previousShape = component.modelToView2D(safeOffset - 1);
                if (previousShape != null) {
                    Rectangle previous = previousShape.getBounds();
                    if (safeOffset == documentLength) {
                        previous.x = previous.x + Math.max(previous.width, 1);
                    }
                    return previous;
                }
            }

            return null;
        }
    }
}
