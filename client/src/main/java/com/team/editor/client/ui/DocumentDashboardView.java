package com.team.editor.client.ui;

import com.team.editor.client.controller.DocumentController;
import com.team.editor.client.service.DocumentSessionService;
import com.team.editor.common.protocol.dto.document.DocumentSummaryDto;
import com.team.editor.common.protocol.dto.document.ShareCodeResponse;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class DocumentDashboardView extends JFrame {
    private static final Color PAGE_BACKGROUND = new Color(244, 247, 251);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(219, 226, 235);
    private static final Color PRIMARY_BLUE = new Color(37, 99, 235);
    private static final Color WARM_ORANGE = new Color(245, 158, 11);
    private static final Color SOFT_GREEN = new Color(22, 163, 74);
    private static final Color DANGER_RED = new Color(220, 38, 38);
    private static final Color NEUTRAL_BUTTON = new Color(232, 238, 246);
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font UI_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 13);

    private final DocumentController controller;

    private JTextField serverUrlField;
    private JTextField displayNameField;
    private JTextField documentTitleField;
    private JTextField shareCodeField;
    private JPanel documentsSectionPanel;
    private JComboBox<DocumentListItem> documentsComboBox;
    private JButton openSelectedDocumentButton;
    private JButton deleteSelectedDocumentButton;
    private JTextArea currentDocumentArea;
    private JTextArea outputArea;
    private boolean documentsLoaded;

    public DocumentDashboardView(DocumentController controller) {
        this.controller = controller;
        this.controller.setView(this);

        setTitle("Collaborative Text Editor - Documents");
        setSize(980, 760);
        setMinimumSize(new Dimension(760, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    public void start() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private void initUI() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(CARD_BACKGROUND);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        contentPanel.setPreferredSize(new Dimension(880, 710));

        JLabel titleLabel = new JLabel("Collaborative Text Editor");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Create, import, and open shared documents");
        subtitleLabel.setFont(UI_FONT);
        subtitleLabel.setForeground(TEXT_MUTED);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(subtitleLabel);
        contentPanel.add(Box.createVerticalStrut(18));

        serverUrlField = createTextField(controller.getServerBaseUrl());
        displayNameField = createTextField("Omar");
        documentTitleField = createTextField("Untitled Document");
        shareCodeField = createTextField("");

        contentPanel.add(createUserServerSection());
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(createCreateSection());
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(createJoinSection());
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(createDocumentsSection());
        contentPanel.add(Box.createVerticalStrut(12));

        JPanel infoPanel = new JPanel(new BorderLayout(10, 10));
        infoPanel.setBackground(CARD_BACKGROUND);
        infoPanel.setAlignmentX(LEFT_ALIGNMENT);

        currentDocumentArea = new JTextArea(6, 50);
        currentDocumentArea.setEditable(false);
        styleTextArea(currentDocumentArea);
        currentDocumentArea.setText("No document opened yet.");
        JScrollPane currentDocumentScroll = new JScrollPane(currentDocumentArea);
        currentDocumentScroll.setBorder(createSectionBorder("Current Document"));
        infoPanel.add(currentDocumentScroll, BorderLayout.NORTH);

        outputArea = new JTextArea(5, 48);
        outputArea.setEditable(false);
        styleTextArea(outputArea);
        outputArea.setText("Ready.");

        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(createSectionBorder("Status"));
        infoPanel.add(outputScroll, BorderLayout.CENTER);
        contentPanel.add(infoPanel);

        JPanel pagePanel = new JPanel(new GridBagLayout());
        pagePanel.setBackground(PAGE_BACKGROUND);
        GridBagConstraints pageConstraints = new GridBagConstraints();
        pageConstraints.gridx = 0;
        pageConstraints.gridy = 0;
        pageConstraints.anchor = GridBagConstraints.NORTH;
        pageConstraints.insets = new Insets(24, 24, 24, 24);
        pagePanel.add(contentPanel, pageConstraints);

        JScrollPane scrollPane = new JScrollPane(pagePanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        setContentPane(scrollPane);
    }

    private JPanel createUserServerSection() {
        JPanel section = createSectionPanel("User and Server");
        addField(section, 0, "Server URL:", serverUrlField);
        addField(section, 1, "Display Name:", displayNameField);
        return section;
    }

    private JPanel createCreateSection() {
        JPanel section = createSectionPanel("Create");
        addField(section, 0, "Document Title:", documentTitleField);
        addButtonRow(section, 1, createButton(), importFileButton());
        return section;
    }

    private JPanel createJoinSection() {
        JPanel section = createSectionPanel("Join Shared Document");
        addField(section, 0, "Share Code:", shareCodeField);
        addButtonRow(section, 1, openByCodeButton());
        return section;
    }

    private JPanel createDocumentsSection() {
        JPanel section = createSectionPanel("My Documents");
        section.setPreferredSize(new Dimension(824, 112));
        section.setMaximumSize(new Dimension(860, 126));
        documentsSectionPanel = section;

        JButton listButton = new JButton("List My Documents");
        styleNeutralButton(listButton);
        listButton.addActionListener(e -> controller.listDocuments(getServerUrl(), getDisplayName()));
        GridBagConstraints listConstraints = new GridBagConstraints();
        listConstraints.gridx = 1;
        listConstraints.gridy = 0;
        listConstraints.anchor = GridBagConstraints.WEST;
        listConstraints.insets = new Insets(4, 6, 8, 6);
        section.add(listButton, listConstraints);

        documentsComboBox = new JComboBox<>();
        documentsComboBox.setPreferredSize(new Dimension(260, 34));
        documentsComboBox.setMinimumSize(new Dimension(220, 34));
        documentsComboBox.setFont(UI_FONT);
        documentsComboBox.setEnabled(false);

        openSelectedDocumentButton = new JButton("Open Selected Document");
        stylePrimaryButton(openSelectedDocumentButton);
        openSelectedDocumentButton.setEnabled(false);
        openSelectedDocumentButton.addActionListener(e -> openSelectedDocument());

        deleteSelectedDocumentButton = new JButton("Delete Selected Document");
        styleDangerButton(deleteSelectedDocumentButton);
        deleteSelectedDocumentButton.addActionListener(e -> deleteSelectedDocument());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 1;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 6, 6, 10);
        section.add(createLabel("Saved Documents:"), labelConstraints);

        GridBagConstraints comboConstraints = new GridBagConstraints();
        comboConstraints.gridx = 1;
        comboConstraints.gridy = 1;
        comboConstraints.fill = GridBagConstraints.HORIZONTAL;
        comboConstraints.anchor = GridBagConstraints.WEST;
        comboConstraints.weightx = 1.0;
        comboConstraints.insets = new Insets(4, 6, 6, 6);
        section.add(documentsComboBox, comboConstraints);

        GridBagConstraints openConstraints = new GridBagConstraints();
        openConstraints.gridx = 2;
        openConstraints.gridy = 1;
        openConstraints.anchor = GridBagConstraints.WEST;
        openConstraints.insets = new Insets(4, 6, 6, 6);
        section.add(openSelectedDocumentButton, openConstraints);

        GridBagConstraints deleteConstraints = new GridBagConstraints();
        deleteConstraints.gridx = 3;
        deleteConstraints.gridy = 1;
        deleteConstraints.anchor = GridBagConstraints.WEST;
        deleteConstraints.insets = new Insets(4, 6, 6, 6);
        section.add(deleteSelectedDocumentButton, deleteConstraints);
        return section;
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(createSectionBorder(title));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(860, 150));
        return panel;
    }

    private JTextField createTextField(String value) {
        JTextField field = new JTextField(value, 34);
        styleTextField(field);
        return field;
    }

    private JButton createButton() {
        JButton createButton = new JButton("Create Document");
        createButton.addActionListener(e -> controller.createDocumentAndOpenEditor(
                getServerUrl(),
                getDisplayName(),
                documentTitleField.getText()
        ));
        stylePrimaryButton(createButton);
        return createButton;
    }

    private JButton importFileButton() {
        JButton importFileButton = new JButton("Import File");
        importFileButton.addActionListener(e -> controller.importFileAndOpenEditor(
                getServerUrl(),
                getDisplayName()
        ));
        styleSuccessButton(importFileButton);
        return importFileButton;
    }

    private JButton openByCodeButton() {
        JButton openByCodeButton = new JButton("Open by Share Code");
        openByCodeButton.addActionListener(e -> controller.openDocumentByCodeAndOpenEditor(
                getServerUrl(),
                getDisplayName(),
                shareCodeField.getText()
        ));
        styleWarmButton(openByCodeButton);
        return openByCodeButton;
    }

    private void openSelectedDocument() {
        DocumentListItem selectedItem = (DocumentListItem) documentsComboBox.getSelectedItem();
        if (selectedItem == null) {
            showStatus("Please select a document first.");
            return;
        }

        controller.openDocumentFromList(getServerUrl(), selectedItem.getDocumentId(), getDisplayName());
    }

    private void deleteSelectedDocument() {
        if (!documentsLoaded) {
            showStatus("No documents loaded. Click List My Documents first.");
            return;
        }

        if (documentsComboBox.getItemCount() == 0) {
            showStatus("No document selected to delete.");
            return;
        }

        DocumentListItem selectedItem = (DocumentListItem) documentsComboBox.getSelectedItem();
        if (selectedItem == null) {
            showStatus("Please select a document first.");
            return;
        }

        if (controller.isDocumentOpenInEditor(selectedItem.getDocumentId())) {
            showWarning(
                    "Close Editor First",
                    "Close the editor window before deleting this document."
            );
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "Delete \"" + selectedItem.getTitleForMessage() + "\"?\nThis action cannot be undone.",
                "Delete Document",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        controller.deleteDocumentFromList(
                getServerUrl(),
                selectedItem.getDocumentId(),
                getDisplayName(),
                selectedItem.getTitleForMessage()
        );
    }

    private void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(UI_FONT_BOLD);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker()),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        button.setPreferredSize(new Dimension(190, 34));
    }

    private void stylePrimaryButton(JButton button) {
        styleButton(button, PRIMARY_BLUE, Color.WHITE);
    }

    private void styleWarmButton(JButton button) {
        styleButton(button, WARM_ORANGE, Color.WHITE);
    }

    private void styleSuccessButton(JButton button) {
        styleButton(button, SOFT_GREEN, Color.WHITE);
    }

    private void styleDangerButton(JButton button) {
        styleButton(button, DANGER_RED, Color.WHITE);
    }

    private void styleNeutralButton(JButton button) {
        styleButton(button, NEUTRAL_BUTTON, TEXT_DARK);
    }

    private void styleTextField(JTextField field) {
        field.setFont(UI_FONT);
        field.setForeground(TEXT_DARK);
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(430, 34));
        field.setMinimumSize(new Dimension(220, 34));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)
        ));
    }

    private void styleTextArea(JTextArea area) {
        area.setFont(MONO_FONT);
        area.setForeground(TEXT_DARK);
        area.setBackground(new Color(249, 250, 252));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UI_FONT_BOLD);
        label.setForeground(TEXT_DARK);
        return label;
    }

    private javax.swing.border.Border createSectionBorder(String title) {
        javax.swing.border.TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                title
        );
        titledBorder.setTitleFont(UI_FONT_BOLD);
        titledBorder.setTitleColor(TEXT_DARK);
        return BorderFactory.createCompoundBorder(
                titledBorder,
                BorderFactory.createEmptyBorder(10, 12, 12, 12)
        );
    }

    private void addField(JPanel panel, int row, String labelText, JTextField field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(6, 6, 6, 10);
        panel.add(createLabel(labelText), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.WEST;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.insets = new Insets(6, 6, 6, 6);
        panel.add(field, fieldConstraints);
    }

    private void addButtonRow(JPanel panel, int row, JButton... buttons) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(CARD_BACKGROUND);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) {
                buttonPanel.add(Box.createHorizontalStrut(10));
            }
            buttonPanel.add(buttons[i]);
        }

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = row;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 6, 8, 6);
        panel.add(buttonPanel, constraints);
    }

    public String getServerUrl() {
        return serverUrlField.getText();
    }

    public String getUserId() {
        return getDisplayName();
    }

    public String getDisplayName() {
        return displayNameField.getText();
    }

    public void showStatus(String message) {
        outputArea.setText(message == null ? "" : message);
    }

    public void showCurrentDocument(DocumentSessionService session) {
        if (session == null || !session.hasOpenDocument()) {
            currentDocumentArea.setText("No document opened yet.");
            setOpenEditorButtonReady(false);
            return;
        }

        setOpenEditorButtonReady(true);

        StringBuilder builder = new StringBuilder();
        builder.append("Current Document\n");
        builder.append("Title: ").append(safe(session.getTitle())).append('\n');
        builder.append("Role: ").append(safe(session.getRole())).append('\n');
        builder.append("Can Edit: ").append(session.isCanEdit()).append('\n');
        builder.append("Can Share: ").append(session.isCanShare()).append('\n');
        builder.append("Snapshot Version: ").append(session.getSnapshotVersion()).append('\n');

        if (session.getEditorCode() != null && !session.getEditorCode().isBlank()) {
            builder.append("Editor Code: ").append(session.getEditorCode()).append('\n');
        }
        if (session.getViewerCode() != null && !session.getViewerCode().isBlank()) {
            builder.append("Viewer Code: ").append(session.getViewerCode()).append('\n');
        }

        currentDocumentArea.setText(builder.toString());
    }

    public void showDocumentList(List<DocumentSummaryDto> documents) {
        documentsLoaded = true;
        DefaultComboBoxModel<DocumentListItem> model = new DefaultComboBoxModel<>();
        if (documents == null || documents.isEmpty()) {
            documentsComboBox.setModel(model);
            documentsComboBox.setEnabled(false);
            openSelectedDocumentButton.setEnabled(false);
            deleteSelectedDocumentButton.setEnabled(false);
            showStatus("No documents found.");
        } else {
            for (DocumentSummaryDto document : documents) {
                model.addElement(new DocumentListItem(document));
            }
            documentsComboBox.setModel(model);
            documentsComboBox.setSelectedIndex(0);
            documentsComboBox.setEnabled(true);
            openSelectedDocumentButton.setEnabled(true);
            deleteSelectedDocumentButton.setEnabled(true);
            showStatus("Loaded " + documents.size() + " document(s). Choose one from the dropdown.");
        }

        if (documentsSectionPanel != null) {
            documentsSectionPanel.revalidate();
            documentsSectionPanel.repaint();
        }
        revalidate();
        repaint();
    }

    public void removeDocumentFromList(String documentId) {
        DefaultComboBoxModel<DocumentListItem> model =
                (DefaultComboBoxModel<DocumentListItem>) documentsComboBox.getModel();

        for (int index = 0; index < model.getSize(); index++) {
            DocumentListItem item = model.getElementAt(index);
            if (item != null && item.hasDocumentId(documentId)) {
                model.removeElementAt(index);
                break;
            }
        }

        boolean hasDocuments = model.getSize() > 0;
        documentsComboBox.setEnabled(hasDocuments);
        openSelectedDocumentButton.setEnabled(hasDocuments);
        deleteSelectedDocumentButton.setEnabled(hasDocuments);
        if (hasDocuments) {
            documentsComboBox.setSelectedIndex(0);
        }

        if (documentsSectionPanel != null) {
            documentsSectionPanel.revalidate();
            documentsSectionPanel.repaint();
        }
    }

    public void showShareCodes(ShareCodeResponse response) {
        ShareCodesDialog.showCodes(this, response);
    }

    public void setJoiningLiveSession(boolean joining) {
    }

    public void setOpenEditorButtonReady(boolean ready) {
    }

    public void setEditorAlreadyOpen(boolean open) {
    }

    public void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void showWarning(String title, String message) {
        showStatus(message);
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(not available)" : value;
    }

    private static class DocumentListItem {
        private final String documentId;
        private final String title;
        private final String role;

        DocumentListItem(DocumentSummaryDto summary) {
            this.documentId = summary == null ? null : summary.getDocumentId();
            this.title = summary == null ? null : summary.getTitle();
            this.role = summary == null || summary.getRole() == null ? null : summary.getRole().name();
        }

        String getDocumentId() {
            return documentId;
        }

        String getTitleForMessage() {
            return title == null || title.isBlank() ? "Untitled Document" : title;
        }

        boolean hasDocumentId(String documentId) {
            if (this.documentId == null) {
                return documentId == null;
            }
            return this.documentId.equals(documentId);
        }

        @Override
        public String toString() {
            String displayTitle = getTitleForMessage();
            if (role == null || role.isBlank()) {
                return displayTitle;
            }
            return displayTitle + " (" + role + ")";
        }
    }
}
