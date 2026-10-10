package com.team.editor.common.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.protocol.dto.OperationMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OperationMessageTest {
    @Test
    void deserializesOldOperationJsonWithoutOperationId() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String rawJson = """
                {"sessionId":"room","senderId":{"value":"alice"},"operationKind":"INSERT_CHAR","payload":{"char":"a"},"messageType":"OPERATION"}
                """;

        OperationMessage operationMessage = objectMapper.readValue(rawJson, OperationMessage.class);

        assertNull(operationMessage.getOperationId());
        assertEquals("room", operationMessage.getSessionId());
        assertEquals("INSERT_CHAR", operationMessage.getOperationKind());
    }
}
