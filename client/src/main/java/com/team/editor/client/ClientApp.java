package com.team.editor.client;

import com.team.editor.client.controller.DocumentController;
import com.team.editor.client.ui.DocumentDashboardView;
import javax.swing.SwingUtilities;

public class ClientApp {

    public static void main(String[] args) {
        // Apply modern light UI theme
        try {
            javax.swing.UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception ex) {
            System.err.println("Failed to initialize FlatLaf");
        }

        SwingUtilities.invokeLater(() -> {
            DocumentController controller = new DocumentController();
            DocumentDashboardView view = new DocumentDashboardView(controller);
            view.start();
        });
    }
}
