package com.team.editor.client.ui;

import javax.swing.*;
import java.awt.*;

public class ActiveUsersPanel extends JPanel {
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(219, 226, 235);
    private static final Color TEXT_DARK = new Color(31, 41, 55);
    private static final Color SELECTION_BLUE = new Color(219, 234, 254);
    private static final Font UI_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private final DefaultListModel<String> usersModel;

    public ActiveUsersPanel() {
        setLayout(new BorderLayout());
        setBackground(CARD_BACKGROUND);
        setBorder(createCardBorder("Active Users"));
        setPreferredSize(new Dimension(220, 0));

        usersModel = new DefaultListModel<>();
        JList<String> usersList = new JList<>(usersModel);
        usersList.setFont(UI_FONT);
        usersList.setForeground(TEXT_DARK);
        usersList.setBackground(CARD_BACKGROUND);
        usersList.setSelectionBackground(SELECTION_BLUE);
        usersList.setSelectionForeground(TEXT_DARK);
        usersList.setFixedCellHeight(28);

        JScrollPane scrollPane = new JScrollPane(usersList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
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

    public void addUser(String username) {
        SwingUtilities.invokeLater(() -> {
            if (!usersModel.contains(username)) {
                usersModel.addElement(username);
            }
        });
    }

    public void removeUser(String username) {
        SwingUtilities.invokeLater(() -> {
            usersModel.removeElement(username);
        });
    }
}
