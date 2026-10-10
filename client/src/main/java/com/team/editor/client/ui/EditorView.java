package com.team.editor.client.ui;

import com.team.editor.client.controller.EditorController;
import com.team.editor.client.cursor.CursorManager;
import com.team.editor.client.renderer.SwingFormattingRenderer;
import com.team.editor.client.state.ConnectionState;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.StyledDocument;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class EditorView extends JFrame {
    private static final Color PAGE_BACKGROUND = new Color(244, 247, 251);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(219, 226, 235);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color NEUTRAL_BUTTON = new Color(232, 238, 246);
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Color READ_ONLY_ORANGE = new Color(180, 83, 9);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font EDITOR_FONT = new Font("Consolas", Font.PLAIN, 16);

    private final EditorController controller;
    private final JTextPane textPane;
    private final ConnectionStatusBar statusBar;
    private final ActiveUsersPanel activeUsersPanel;
    private final CursorManager cursorManager;
    private final JLabel documentTitleLabel;
    private final JLabel documentIdLabel;
    private final JLabel roleLabel;
    private final JLabel connectionLabel;
    private final JLabel snapshotLabel;
    private final JLabel readOnlyLabel;
    private final JButton saveSnapshotButton;
    private final JButton exportTextButton;
    private final JButton undoButton;
    private final JButton redoButton;
    private final JToggleButton boldButton;
    private final JToggleButton italicButton;
    private final JToggleButton underlineButton;
    private final JPanel shareCodesPanel;
    private final JTextField editorCodeField;
    private final JTextField viewerCodeField;
    private boolean isUpdatingFromNetwork = false;
    private boolean updatingFormatButtons = false;

    public EditorView(EditorController controller, String username) {
        this.controller = controller;
        setTitle("Collab Editor - " + username);
        setSize(1080, 720);
        setMinimumSize(new Dimension(820, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        documentTitleLabel = new JLabel("Document: (not selected)");
        documentIdLabel = new JLabel("ID: (not selected)");
        roleLabel = new JLabel("Role: (unknown)");
        connectionLabel = new JLabel("Status: Disconnected");
        snapshotLabel = new JLabel("Snapshot: v0");
        readOnlyLabel = new JLabel("Read-only viewer mode");
        readOnlyLabel.setForeground(READ_ONLY_ORANGE);
        readOnlyLabel.setFont(UI_FONT_BOLD);
        readOnlyLabel.setVisible(false);
        styleHeaderLabel(documentTitleLabel, true);
        styleHeaderLabel(documentIdLabel, false);
        styleHeaderLabel(roleLabel, false);
        styleHeaderLabel(connectionLabel, false);
        styleHeaderLabel(snapshotLabel, false);

        saveSnapshotButton = new JButton("Save Snapshot");
        saveSnapshotButton.addActionListener(e -> controller.saveSnapshot());
        saveSnapshotButton.setVisible(false);
        stylePrimaryButton(saveSnapshotButton);

        exportTextButton = new JButton("Export Text");
        exportTextButton.addActionListener(e -> controller.exportTextToFile());
        exportTextButton.setVisible(false);
        styleNeutralButton(exportTextButton);

        undoButton = new JButton("Undo");
        undoButton.setToolTipText("Undo latest document operation");
        undoButton.addActionListener(e -> controller.requestUndo());
        undoButton.setVisible(false);
        styleSmallButton(undoButton);

        redoButton = new JButton("Redo");
        redoButton.setToolTipText("Redo latest undone document operation");
        redoButton.addActionListener(e -> controller.requestRedo());
        redoButton.setVisible(false);
        styleSmallButton(redoButton);

        boldButton = new JToggleButton("B");
        boldButton.setFont(boldButton.getFont().deriveFont(Font.BOLD));
        boldButton.addActionListener(e -> toggleFormatFromButton("BOLD"));

        italicButton = new JToggleButton("I");
        italicButton.setFont(italicButton.getFont().deriveFont(Font.ITALIC));
        italicButton.addActionListener(e -> toggleFormatFromButton("ITALIC"));

        underlineButton = new JToggleButton("U");
        underlineButton.addActionListener(e -> toggleFormatFromButton("UNDERLINE"));

        textPane = new JTextPane() {
            @Override
            public void paste() {
                java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                try {
                    String text = (String) clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    if (text != null && !text.isEmpty()) {
                        replaceSelection(text);
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            }

            @Override
            public void cut() {
                String selected = getSelectedText();
                if (selected != null && !selected.isEmpty()) {
                    int start = getSelectionStart();
                    int end = getSelectionEnd();
                    
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(selected);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

                    replaceSelection("");
                }
            }
        };
        textPane.setFont(EDITOR_FONT);
        textPane.setForeground(TEXT_DARK);
        textPane.setCaretColor(PRIMARY_BLUE);
        textPane.setMargin(new Insets(18, 18, 18, 18));
        textPane.setEditable(true);
        
        setupKeyListener();

        cursorManager = new CursorManager(textPane);

        statusBar = new ConnectionStatusBar();
        activeUsersPanel = new ActiveUsersPanel();
        shareCodesPanel = new JPanel(new GridBagLayout());
        editorCodeField = new JTextField(16);
        viewerCodeField = new JTextField(16);
        setupShareCodesPanel();

        setLayout(new BorderLayout());
        getContentPane().setBackground(PAGE_BACKGROUND);
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createEditorScrollPane(), BorderLayout.CENTER);
        add(createSidebarPanel(), BorderLayout.EAST);
        add(statusBar, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                boolean focused = textPane.requestFocusInWindow();
                System.out.println(
                        "EDITOR UI windowOpened -> requested textPane focus, success=" + focused
                                + ", editable=" + textPane.isEditable()
                );
            }
        });
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        panel.setBackground(CARD_BACKGROUND);

        addHeaderLabel(panel, 0, 0, documentTitleLabel);
        addHeaderLabel(panel, 1, 0, documentIdLabel);
        addHeaderLabel(panel, 0, 1, roleLabel);
        addHeaderLabel(panel, 1, 1, connectionLabel);
        addHeaderLabel(panel, 0, 2, snapshotLabel);
        addHeaderLabel(panel, 1, 2, readOnlyLabel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(2, 22, 3, 4);
        panel.add(saveSnapshotButton, constraints);

        GridBagConstraints exportConstraints = new GridBagConstraints();
        exportConstraints.gridx = 2;
        exportConstraints.gridy = 1;
        exportConstraints.anchor = GridBagConstraints.EAST;
        exportConstraints.insets = new Insets(3, 22, 3, 4);
        panel.add(exportTextButton, exportConstraints);

        GridBagConstraints formatConstraints = new GridBagConstraints();
        formatConstraints.gridx = 2;
        formatConstraints.gridy = 2;
        formatConstraints.anchor = GridBagConstraints.EAST;
        formatConstraints.insets = new Insets(3, 22, 2, 4);
        panel.add(createEditorControlsPanel(), formatConstraints);

        return panel;
    }

    private JPanel createEditorControlsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panel.setBackground(CARD_BACKGROUND);
        styleFormatButton(boldButton);
        styleFormatButton(italicButton);
        styleFormatButton(underlineButton);
        panel.add(undoButton);
        panel.add(redoButton);
        panel.add(boldButton);
        panel.add(italicButton);
        panel.add(underlineButton);
        return panel;
    }

    private void styleFormatButton(JToggleButton button) {
        button.setFont(UI_FONT_BOLD);
        button.setForeground(TEXT_DARK);
        button.setBackground(NEUTRAL_BUTTON);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        button.setPreferredSize(new Dimension(42, 30));
        button.setFocusPainted(false);
        button.setVisible(false);
    }

    private JScrollPane createEditorScrollPane() {
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(18, 18, 18, 10),
                BorderFactory.createLineBorder(BORDER_COLOR)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public void updateSnapshotVersion(long version) {
        snapshotLabel.setText("Snapshot: v" + version);
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void addHeaderLabel(JPanel panel, int column, int row, JLabel label) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(2, 8, 2, 16);
        panel.add(label, constraints);
    }

    private JPanel createSidebarPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(PAGE_BACKGROUND);
        panel.setPreferredSize(new Dimension(260, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 8, 18, 18));
        panel.add(activeUsersPanel, BorderLayout.CENTER);
        panel.add(shareCodesPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void setupShareCodesPanel() {
        shareCodesPanel.setBackground(CARD_BACKGROUND);
        shareCodesPanel.setBorder(createCardBorder("Share Codes"));
        editorCodeField.setEditable(false);
        viewerCodeField.setEditable(false);
        styleReadOnlyField(editorCodeField);
        styleReadOnlyField(viewerCodeField);
        addShareCodeRow(0, "Editor:", editorCodeField);
        addShareCodeRow(1, "Viewer:", viewerCodeField);
        shareCodesPanel.setVisible(false);
    }

    private void addShareCodeRow(int row, String label, JTextField field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 4, 4, 8);
        JLabel rowLabel = new JLabel(label);
        rowLabel.setFont(UI_FONT_BOLD);
        rowLabel.setForeground(TEXT_DARK);
        shareCodesPanel.add(rowLabel, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(4, 4, 4, 4);
        shareCodesPanel.add(field, fieldConstraints);

        JButton copyButton = new JButton("Copy");
        styleSmallButton(copyButton);
        copyButton.addActionListener(e -> copyToClipboard(field.getText()));
        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 2;
        buttonConstraints.gridy = row;
        buttonConstraints.insets = new Insets(4, 4, 4, 4);
        shareCodesPanel.add(copyButton, buttonConstraints);
    }

    private void styleHeaderLabel(JLabel label, boolean important) {
        label.setFont(important ? new Font("Segoe UI", Font.BOLD, 15) : UI_FONT);
        label.setForeground(important ? TEXT_DARK : TEXT_MUTED);
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(UI_FONT_BOLD);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker()),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));
        button.setPreferredSize(new Dimension(150, 34));
    }

    private void stylePrimaryButton(JButton button) {
        styleButton(button, PRIMARY_BLUE, Color.WHITE);
    }

    private void styleNeutralButton(JButton button) {
        styleButton(button, NEUTRAL_BUTTON, TEXT_DARK);
    }

    private void styleSmallButton(JButton button) {
        button.setFont(UI_FONT_BOLD);
        button.setBackground(NEUTRAL_BUTTON);
        button.setForeground(TEXT_DARK);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)
        ));
    }

    private void styleReadOnlyField(JTextField field) {
        field.setFont(UI_FONT);
        field.setForeground(TEXT_DARK);
        field.setBackground(new Color(249, 250, 252));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)
        ));
    }

    private javax.swing.border.Border createCardBorder(String title) {
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                title
        );
        titledBorder.setTitleFont(UI_FONT_BOLD);
        titledBorder.setTitleColor(TEXT_DARK);
        return BorderFactory.createCompoundBorder(
                titledBorder,
                BorderFactory.createEmptyBorder(8, 10, 10, 10)
        );
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    private void setupKeyListener() {
        ((AbstractDocument) textPane.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (allowProgrammaticDocumentChange("insert", fb, offset, 0, string, attr)) {
                    return;
                }
                fb.insertString(offset, string, attr);
                handleLocalDocumentChange(offset, 0, string);
                queueCursorUpdateFromCurrentCaret();
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (allowProgrammaticDocumentChange("remove", fb, offset, length, null, null)) {
                    return;
                }
                fb.remove(offset, length);
                handleLocalDocumentChange(offset, length, null);
                queueCursorUpdateFromCurrentCaret();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (allowProgrammaticDocumentChange("replace", fb, offset, length, text, attrs)) {
                    return;
                }
                fb.replace(offset, length, text, attrs);
                handleLocalDocumentChange(offset, length, text);
                queueCursorUpdateFromCurrentCaret();
            }
        });

        textPane.addCaretListener(e -> {
            if (isUpdatingFromNetwork) {
                return;
            }
            updateFormatButtonsFromSelection();
            sendCursorUpdate(e.getDot());
        });

        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (isUpdatingFromNetwork) {
                    return;
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (isUpdatingFromNetwork) {
                    return;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (isUpdatingFromNetwork) {
                    return;
                }
                queueCursorUpdateFromCurrentCaret();
            }
        });

        bindUndoRedoShortcuts(textPane);
    }

    private void bindUndoRedoShortcuts(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "serverUndo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK), "serverRedo");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "serverRedo");

        actionMap.put("serverUndo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                controller.requestUndo();
            }
        });
        actionMap.put("serverRedo", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                controller.requestRedo();
            }
        });
    }

    public void refreshText(Integer newCaretPos) {
        updateTextPreservingCaret(controller.getState().getText(), newCaretPos);
    }

    public String getText() {
        return textPane.getText();
    }

    public void updateTextPreservingCaret(String newText) {
        updateTextPreservingCaret(newText, null);
    }

    public void updateTextPreservingCaret(String newText, Integer newCaretPos) {
        if (newText == null) {
            newText = "";
        }

        isUpdatingFromNetwork = true;
        try {
            int currentCaret = textPane.getCaretPosition();
            int selectionStart = textPane.getSelectionStart();
            int selectionEnd = textPane.getSelectionEnd();
            int targetCaret = newCaretPos == null ? currentCaret : newCaretPos;

            if (!newText.equals(textPane.getText())) {
                textPane.setText(newText);
            }
            applyFormattingToDocument();
            textPane.revalidate();
            textPane.repaint();

            try {
                int newLen = textPane.getDocument().getLength();
                if (newCaretPos == null && selectionStart != selectionEnd) {
                    textPane.setCaretPosition(Math.min(selectionStart, newLen));
                    textPane.moveCaretPosition(Math.min(selectionEnd, newLen));
                } else {
                    textPane.setCaretPosition(Math.min(targetCaret, newLen));
                }
            } catch (IllegalArgumentException e) {
                // Ignore bounds issue
            }

            cursorManager.refreshCursors();
        } finally {
            isUpdatingFromNetwork = false;
            updateFormatButtonsFromSelection();
        }
    }

    private void applyFormattingToDocument() {
        StyledDocument document = textPane.getStyledDocument();
        SwingFormattingRenderer.applyFormatting(document, controller.getState().getFormatRuns());
    }

    private boolean allowProgrammaticDocumentChange(
            String changeType,
            DocumentFilter.FilterBypass filterBypass,
            int offset,
            int length,
            String text,
            AttributeSet attributes
    ) throws BadLocationException {
        if (!isUpdatingFromNetwork) {
            return false;
        }
        if ("insert".equals(changeType)) {
            filterBypass.insertString(offset, text, attributes);
        } else if ("remove".equals(changeType)) {
            filterBypass.remove(offset, length);
        } else {
            filterBypass.replace(offset, length, text, attributes);
        }
        return true;
    }

    private void queueLocalDocumentChange(int offset, int replacedLength, String text) {
        SwingUtilities.invokeLater(() -> handleLocalDocumentChange(offset, replacedLength, text));
    }

    private void queueCursorUpdateFromCurrentCaret() {
        SwingUtilities.invokeLater(() -> {
            if (isUpdatingFromNetwork) {
                return;
            }
            sendCursorUpdate(textPane.getCaretPosition());
        });
    }

    private void sendCursorUpdate(int offset) {
        controller.sendCursorUpdate(offset);
    }

    private void handleLocalDocumentChange(int offset, int replacedLength, String text) {
        if (replacedLength > 0) {
            controller.deleteTextLocal(offset, offset + replacedLength);
        }

        if (text == null || text.isEmpty()) {
            return;
        }

        int currentOffset = offset;
        for (char c : text.toCharArray()) {
            if (c == '\r') {
                continue;
            }

            if (c == '\n') {
                controller.insertNewlineLocal(currentOffset);
                currentOffset++;
                continue;
            }

            if (c < 32 || c == 127) {
                continue;
            }

            controller.insertCharLocal(currentOffset, c);
            currentOffset++;
        }
    }

    private void toggleFormatFromButton(String format) {
        if (updatingFormatButtons) {
            return;
        }

        controller.toggleFormat(format, textPane.getSelectionStart(), textPane.getSelectionEnd());
        updateFormatButtonsFromSelection();
    }

    private void updateFormatButtonsFromSelection() {
        updatingFormatButtons = true;
        try {
            int start = textPane.getSelectionStart();
            int end = textPane.getSelectionEnd();
            boldButton.setSelected(controller.isFormatButtonSelected("BOLD", start, end));
            italicButton.setSelected(controller.isFormatButtonSelected("ITALIC", start, end));
            underlineButton.setSelected(controller.isFormatButtonSelected("UNDERLINE", start, end));
        } finally {
            updatingFormatButtons = false;
        }
    }

    private String printableText(String text) {
        return text == null ? "null" : '"' + text.replace("\r", "\\r").replace("\n", "\\n") + '"';
    }

    private String printableChar(char c) {
        return switch (c) {
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case KeyEvent.CHAR_UNDEFINED -> "<undefined>";
            default -> Character.isISOControl(c) ? "\\u" + String.format("%04x", (int) c) : "'" + c + "'";
        };
    }

    public void updateConnectionState(ConnectionState state) {
        connectionLabel.setText("Status: " + state.name());
        connectionLabel.setForeground(state == ConnectionState.JOINED ? new Color(22, 163, 74) : TEXT_MUTED);
        statusBar.updateStatus(state);
    }

    public void setDocumentInfo(
            String title,
            String documentId,
            String role,
            long snapshotVersion,
            boolean canEdit
    ) {
        documentTitleLabel.setText("Document: " + safe(title));
        documentIdLabel.setText("ID: " + safe(documentId));
        roleLabel.setText("Role: " + safe(role));
        snapshotLabel.setText("Snapshot: v" + snapshotVersion);
        readOnlyLabel.setVisible(!canEdit);
        saveSnapshotButton.setVisible(canEdit);
        undoButton.setVisible(canEdit);
        redoButton.setVisible(canEdit);
        undoButton.setEnabled(canEdit);
        redoButton.setEnabled(canEdit);
        boldButton.setVisible(canEdit);
        italicButton.setVisible(canEdit);
        underlineButton.setVisible(canEdit);
        exportTextButton.setVisible(true);
        textPane.setEditable(canEdit);
        textPane.setBackground(canEdit ? Color.WHITE : new Color(248, 250, 252));
        textPane.setCursor(Cursor.getPredefinedCursor(canEdit ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public void showOwnerShareCodes(String editorCode, String viewerCode) {
        editorCodeField.setText(editorCode == null ? "" : editorCode);
        viewerCodeField.setText(viewerCode == null ? "" : viewerCode);
        shareCodesPanel.setVisible(true);
    }

    public void hideShareCodes() {
        editorCodeField.setText("");
        viewerCodeField.setText("");
        shareCodesPanel.setVisible(false);
    }

    public void addActiveUser(String username) {
        activeUsersPanel.addUser(username);
    }

    public void removeActiveUser(String username) {
        activeUsersPanel.removeUser(username);
    }

    public void updateRemoteCursor(String username, int offset) {
        cursorManager.updateCursor(username, offset);
    }

    public void removeRemoteCursor(String username) {
        cursorManager.removeCursor(username);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(not available)" : value;
    }
}
