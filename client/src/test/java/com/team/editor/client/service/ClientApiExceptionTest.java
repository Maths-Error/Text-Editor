package com.team.editor.client.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientApiExceptionTest {

    @Test
    void storesStatusCodeAndResponseBody() {
        ClientApiException exception = new ClientApiException(403, "{\"errorCode\":\"PERMISSION_DENIED\"}");

        assertEquals(403, exception.getStatusCode());
        assertEquals("{\"errorCode\":\"PERMISSION_DENIED\"}", exception.getResponseBody());
        assertTrue(exception.getMessage().contains("403"));
    }
}
