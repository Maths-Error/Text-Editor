package com.team.editor.client.ui;

import com.team.editor.common.protocol.dto.document.ShareCodeResponse;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Font;

public class ShareCodesDialog {

    public static void showCodes(Component parent, ShareCodeResponse response) {
        if (response == null) {
            JOptionPane.showMessageDialog(parent, "No share codes were returned.");
            return;
        }

        String message = "Document ID: " + safe(response.getDocumentId()) + "\n"
                + "Editor Code: " + safe(response.getEditorCode()) + "\n"
                + "Viewer Code: " + safe(response.getViewerCode());

        JTextArea messageArea = new JTextArea(message);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JOptionPane.showMessageDialog(parent, messageArea, "Share Codes", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "(not available)" : value;
    }
}
