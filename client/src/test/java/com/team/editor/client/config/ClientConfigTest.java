package com.team.editor.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientConfigTest {

    @Test
    void defaultServerBaseUrlUsesLocalhost() {
        ClientConfig config = new ClientConfig();

        assertEquals("http://localhost:8082", config.getServerBaseUrl());
        assertEquals("ws://localhost:8082/ws/collab", config.getWebSocketUrl());
    }

    @Test
    void httpServerUrlConvertsToWebSocketUrl() {
        ClientConfig config = new ClientConfig("http://192.168.1.12:8080/");

        assertEquals("http://192.168.1.12:8080", config.getServerBaseUrl());
        assertEquals("ws://192.168.1.12:8080/ws/collab", config.getWebSocketUrl());
    }

    @Test
    void httpsServerUrlConvertsToSecureWebSocketUrl() {
        ClientConfig config = new ClientConfig("https://example.com");

        assertEquals("wss://example.com/ws/collab", config.getWebSocketUrl());
    }

    @Test
    void blankServerUrlFallsBackToDefault() {
        ClientConfig config = new ClientConfig("   ");

        assertEquals("http://localhost:8082", config.getServerBaseUrl());
    }
}
