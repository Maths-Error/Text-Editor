package com.team.editor.client.ui;

import com.team.editor.client.controller.EditorController;
import com.team.editor.client.controller.SessionController;
import com.team.editor.client.state.ConnectionState;
import com.team.editor.client.state.EditorState;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.CursorUpdateMessage;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionResponse;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.dto.PresenceUpdateMessage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JoinSessionView extends JFrame implements SessionController.SessionListener {

    private final SessionController controller;
    private JTextField serverUrlField;
    private JTextField sessionIdField;
    private JTextField usernameField;
    private JButton joinButton;
    private JLabel statusLabel;
    
    private EditorView editorView;
    private EditorController editorController;
    private String currentUsername;
    private final List<PresenceUpdateMessage> pendingPresenceUpdates = new ArrayList<>();
    private final List<CursorUpdateMessage> pendingCursorUpdates = new ArrayList<>();
    private final List<OperationMessage> pendingOperations = new ArrayList<>();

    public JoinSessionView(SessionController controller) {
        this.controller = controller;
        this.controller.addListener(this);
        
        setTitle("Join Collab Session");
        setSize(420, 260);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel serverLabel = new JLabel("Server URL:");
        serverLabel.setFont(serverLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(serverLabel);

        serverUrlField = new JTextField(controller.getServerBaseUrl());
        serverUrlField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(serverUrlField);

        JLabel sessionLabel = new JLabel("Session ID:");
        sessionLabel.setFont(sessionLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(sessionLabel);
        
        sessionIdField = new JTextField("demo-room");
        sessionIdField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(sessionIdField);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(usernameLabel);
        
        usernameField = new JTextField("user" + (int)(Math.random()*1000));
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        panel.add(usernameField);

        joinButton = new JButton("Join Session");
        joinButton.setFont(joinButton.getFont().deriveFont(Font.BOLD, 14f));
        joinButton.setFocusPainted(false);
        joinButton.addActionListener(e -> {
            String serverUrl = serverUrlField.getText().trim();
            String sessionId = sessionIdField.getText().trim();
            currentUsername = usernameField.getText().trim();
            if (!sessionId.isEmpty() && !currentUsername.isEmpty()) {
                joinButton.setEnabled(false);
                statusLabel.setText("Connecting...");
                controller.setServerBaseUrl(serverUrl);
                if (controller.connect()) {
                    controller.joinSession(sessionId, currentUsername);
                }
            }
        });
        panel.add(new JLabel()); // Spacer
        panel.add(joinButton);
        
        statusLabel = new JLabel("Ready", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void start() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(state.name());
            if (state == ConnectionState.ERROR) {
                joinButton.setEnabled(true);
            }
            if (editorView != null) {
                editorView.updateConnectionState(state);
            }
        });
    }

    @Override
    public void onJoinResponse(JoinSessionResponse response) {
        if (response.isAccepted()) {
            SwingUtilities.invokeLater(() -> {
                this.setVisible(false);
                
                EditorState state = new EditorState(new UserId(currentUsername), sessionIdField.getText().trim());
                editorController = new EditorController(controller, state);
                
                editorView = new EditorView(editorController, currentUsername);
                editorController.setView(editorView);
                
                editorView.updateConnectionState(controller.getConnectionState());
                editorView.addActiveUser(currentUsername);
                flushPendingMessages();
                editorView.setVisible(true);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Join rejected: " + response.getMessage());
                joinButton.setEnabled(true);
            });
        }
    }

    @Override
    public void onPresenceUpdate(PresenceUpdateMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (editorView == null || editorController == null) {
                pendingPresenceUpdates.add(message);
                return;
            }

            String user = message.getUserId().getValue();
            String eventName = message.getEventType() != null ? message.getEventType().name() : message.getPresence();
            
            if ("JOINED".equals(eventName) || "ONLINE".equals(eventName)) {
                editorView.addActiveUser(user);
            } else if ("LEFT".equals(eventName) || "OFFLINE".equals(eventName)) {
                editorView.removeActiveUser(user);
                editorController.removeRemoteCursor(user);
            }
        });
    }

    @Override
    public void onCursorUpdate(CursorUpdateMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (editorController == null) {
                pendingCursorUpdates.add(message);
                return;
            }
            editorController.handleRemoteCursor(message);
        });
    }

    @Override
    public void onOperation(OperationMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (editorController == null) {
                pendingOperations.add(message);
                return;
            }
            editorController.handleRemoteOperation(message);
        });
    }

    @Override
    public void onError(ErrorMessage errorMessage) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Error: " + errorMessage.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            joinButton.setEnabled(true);
        });
    }

    private void flushPendingMessages() {
        for (PresenceUpdateMessage pendingPresenceUpdate : List.copyOf(pendingPresenceUpdates)) {
            String user = pendingPresenceUpdate.getUserId().getValue();
            String eventName = pendingPresenceUpdate.getEventType() != null
                    ? pendingPresenceUpdate.getEventType().name()
                    : pendingPresenceUpdate.getPresence();

            if ("JOINED".equals(eventName) || "ONLINE".equals(eventName)) {
                editorView.addActiveUser(user);
            } else if ("LEFT".equals(eventName) || "OFFLINE".equals(eventName)) {
                editorView.removeActiveUser(user);
                editorController.removeRemoteCursor(user);
            }
        }
        pendingPresenceUpdates.clear();

        for (OperationMessage pendingOperation : List.copyOf(pendingOperations)) {
            editorController.handleRemoteOperation(pendingOperation);
        }
        pendingOperations.clear();

        for (CursorUpdateMessage pendingCursorUpdate : List.copyOf(pendingCursorUpdates)) {
            editorController.handleRemoteCursor(pendingCursorUpdate);
        }
        pendingCursorUpdates.clear();
    }
}
