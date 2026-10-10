package com.team.editor.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfig {
    /**
     * Creates the Jackson mapper used by the websocket handler to turn raw JSON
     * into request objects and to serialize response objects back into JSON text.
     * Spring calls this once while building the application context.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
