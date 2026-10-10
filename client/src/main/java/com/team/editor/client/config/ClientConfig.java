package com.team.editor.client.config;

public class ClientConfig {
    public static final String DEFAULT_SERVER_BASE_URL = "http://localhost:8082";
    private static final String WEBSOCKET_PATH = "/ws/collab";

    private String serverBaseUrl;

    public ClientConfig() {
        this.serverBaseUrl = DEFAULT_SERVER_BASE_URL;
    }

    public ClientConfig(String serverBaseUrl) {
        this.serverBaseUrl = normalizeBaseUrl(serverBaseUrl);
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public String getWebSocketUrl() {
        String baseUrl = normalizeBaseUrl(serverBaseUrl);

        if (baseUrl.startsWith("https://")) {
            return "wss://" + baseUrl.substring("https://".length()) + WEBSOCKET_PATH;
        }

        if (baseUrl.startsWith("http://")) {
            return "ws://" + baseUrl.substring("http://".length()) + WEBSOCKET_PATH;
        }

        return "ws://" + baseUrl + WEBSOCKET_PATH;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = normalizeBaseUrl(serverBaseUrl);
    }

    public static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_SERVER_BASE_URL;
        }

        String normalized = value.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.isBlank()) {
            return DEFAULT_SERVER_BASE_URL;
        }

        return normalized;
    }
}
