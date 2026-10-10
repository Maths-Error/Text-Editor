package com.team.editor.client.ui;

import com.team.editor.client.state.ConnectionState;

import javax.swing.*;
import java.awt.*;

public class ConnectionStatusBar extends JPanel {
    private static final Color BORDER_COLOR = new Color(219, 226, 235);
    private static final Color BACKGROUND = new Color(249, 250, 252);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);

    private final JLabel statusLabel;

    public ConnectionStatusBar() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        add(statusLabel, BorderLayout.WEST);
        updateStatus(ConnectionState.DISCONNECTED);
    }

    public void updateStatus(ConnectionState state) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + state.name());
            switch (state) {
                case CONNECTING, CONNECTED -> statusLabel.setForeground(new Color(255, 165, 0)); // Amber
                case JOINED -> statusLabel.setForeground(new Color(46, 204, 113)); // Emerald Green
                case ERROR -> statusLabel.setForeground(new Color(231, 76, 60)); // Alizarin Red
                default -> statusLabel.setForeground(TEXT_MUTED);
            }
        });
    }
}
