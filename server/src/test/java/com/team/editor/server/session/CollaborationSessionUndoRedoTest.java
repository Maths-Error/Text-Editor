package com.team.editor.server.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.message.OperationKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollaborationSessionUndoRedoTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void undoStackKeepsLatestTenOperations() {
        CollaborationSession collaborationSession = new CollaborationSession("room");

        for (int i = 0; i < 12; i++) {
            OperationMessage original = operation("op-" + i, OperationKind.CHARACTER_INSERT, i);
            OperationMessage inverse = operation("undo-" + i, OperationKind.CHARACTER_DELETE, i);
            collaborationSession.recordUndoableOperation(original, inverse);
        }

        assertEquals(10, collaborationSession.getUndoStackSize());
        assertEquals("op-11", collaborationSession.popUndo().orElseThrow().getOperationId());
        assertEquals("op-10", collaborationSession.popUndo().orElseThrow().getOperationId());
    }

    private OperationMessage operation(String operationId, OperationKind operationKind, int clock) {
        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode idNode = payload.putObject("op").putObject("id");
        idNode.putObject("userId").put("value", "alice");
        idNode.put("clock", clock);

        return new OperationMessage(
                "room",
                new UserId("alice"),
                operationId,
                operationKind,
                payload
        );
    }
}
