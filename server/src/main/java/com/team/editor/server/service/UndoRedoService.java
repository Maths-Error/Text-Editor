package com.team.editor.server.service;

import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.message.OperationKind;
import com.team.editor.server.session.CollaborationSession;
import com.team.editor.server.session.ConnectedUser;
import com.team.editor.server.session.UndoableOperation;
import org.springframework.stereotype.Service;

/**
 * Owns server-side collaborative undo/redo behavior for one live session.
 */
@Service
public class UndoRedoService {
    private static final UserId SERVER_USER_ID = new UserId("SERVER");

    private final PermissionService permissionService;
    private final OperationRelayService operationRelayService;
    private final ObjectMapper objectMapper;

    public UndoRedoService(
            PermissionService permissionService,
            OperationRelayService operationRelayService,
            ObjectMapper objectMapper
    ) {
        this.permissionService = permissionService;
        this.operationRelayService = operationRelayService;
        this.objectMapper = objectMapper;
    }

    public UndoRedoResult undo(CollaborationSession collaborationSession, ConnectedUser requester) {
        ErrorMessage contextError = validateContext(collaborationSession, requester);
        if (contextError != null) {
            return UndoRedoResult.failure(contextError);
        }

        Optional<UndoableOperation> latestUndo = collaborationSession.peekUndo();
        if (latestUndo.isEmpty()) {
            return UndoRedoResult.noOp();
        }

        UndoableOperation undoableOperation = latestUndo.get();
        if (!undoableOperation.hasInverseOperation()) {
            return UndoRedoResult.failure(new ErrorMessage(
                    "UNDO_NOT_SUPPORTED_FOR_OPERATION",
                    "Undo is not supported for operation '" + undoableOperation.getOriginalOperation().getOperationKind() + "'."
            ));
        }

        undoableOperation = collaborationSession.popUndo().orElseThrow();
        OperationMessage undoOperation = copyGeneratedOperation(
                undoableOperation.getInverseOperation(),
                collaborationSession.getSessionId(),
                "undo"
        );
        addUndoRedoMetadata(undoOperation, requester, "UNDO");
        collaborationSession.recordOperation(undoOperation);
        collaborationSession.pushRedo(undoableOperation);

        int recipients = operationRelayService.broadcastOperationToAll(collaborationSession, undoOperation);
        return UndoRedoResult.success(undoOperation, recipients);
    }

    public UndoRedoResult redo(CollaborationSession collaborationSession, ConnectedUser requester) {
        ErrorMessage contextError = validateContext(collaborationSession, requester);
        if (contextError != null) {
            return UndoRedoResult.failure(contextError);
        }

        Optional<UndoableOperation> latestRedo = collaborationSession.popRedo();
        if (latestRedo.isEmpty()) {
            return UndoRedoResult.noOp();
        }

        UndoableOperation redoableOperation = latestRedo.get();
        if (!redoableOperation.hasRedoOperation()) {
            return UndoRedoResult.failure(new ErrorMessage(
                    "UNDO_NOT_SUPPORTED_FOR_OPERATION",
                    "Redo is not supported for operation '" + redoableOperation.getOriginalOperation().getOperationKind() + "'."
            ));
        }

        OperationMessage redoOperation = copyGeneratedOperation(
                redoableOperation.getRedoOperation(),
                collaborationSession.getSessionId(),
                "redo"
        );
        addUndoRedoMetadata(redoOperation, requester, "REDO");
        OperationMessage inverseForNextUndo = createInverseOperation(redoOperation);
        if (inverseForNextUndo == null) {
            return UndoRedoResult.failure(new ErrorMessage(
                    "UNDO_NOT_SUPPORTED_FOR_OPERATION",
                    "Redo is not supported because the original operation cannot be inverted safely."
            ));
        }

        collaborationSession.recordOperation(redoOperation);
        collaborationSession.recordUndoableOperation(redoOperation, inverseForNextUndo, createRedoOperation(redoOperation));

        int recipients = operationRelayService.broadcastOperationToAll(collaborationSession, redoOperation);
        return UndoRedoResult.success(redoOperation, recipients);
    }

    public void ensureOperationId(OperationMessage operationMessage) {
        if (operationMessage.getOperationId() == null || operationMessage.getOperationId().isBlank()) {
            operationMessage.setOperationId(generateOperationId("op"));
        }
    }

    public OperationMessage createInverseOperation(OperationMessage operationMessage) {
        if (operationMessage == null || operationMessage.getOperationKindEnum() == null) {
            return null;
        }

        if (operationMessage.getOperationKindEnum() == OperationKind.CHARACTER_INSERT) {
            return createDeleteForInsertedCharacter(operationMessage);
        }

        if (operationMessage.getOperationKindEnum() == OperationKind.CHARACTER_DELETE) {
            return createRestoreForDeletedCharacter(operationMessage);
        }

        if (operationMessage.getOperationKindEnum() == OperationKind.CHARACTER_RESTORE) {
            return createDeleteForRestoredCharacter(operationMessage);
        }

        if (operationMessage.getOperationKindEnum() == OperationKind.FORMAT_APPLY
                || operationMessage.getOperationKindEnum() == OperationKind.FORMAT_REMOVE) {
            return createInverseFormatOperation(operationMessage);
        }

        return null;
    }

    public OperationMessage createRedoOperation(OperationMessage operationMessage) {
        if (operationMessage == null || operationMessage.getOperationKindEnum() == null) {
            return null;
        }

        if (operationMessage.getOperationKindEnum() == OperationKind.CHARACTER_INSERT) {
            return createRestoreForInsertedCharacter(operationMessage);
        }

        if (operationMessage.getOperationKindEnum() == OperationKind.CHARACTER_DELETE
                || operationMessage.getOperationKindEnum() == OperationKind.FORMAT_APPLY
                || operationMessage.getOperationKindEnum() == OperationKind.FORMAT_REMOVE
                || operationMessage.getOperationKindEnum() == OperationKind.CHARACTER_RESTORE) {
            return copyGeneratedOperation(operationMessage, operationMessage.getSessionId(), "redo-template");
        }

        return null;
    }

    private OperationMessage createDeleteForInsertedCharacter(OperationMessage insertOperation) {
        JsonNode payload = insertOperation.getPayload();
        JsonNode blockIdNode = payload == null ? null : payload.get("blockId");
        JsonNode opNode = payload == null ? null : payload.get("op");
        JsonNode charIdNode = opNode == null ? null : opNode.get("id");
        if (blockIdNode == null || charIdNode == null || charIdNode.isNull()) {
            return null;
        }

        ObjectNode deletePayload = objectMapper.createObjectNode();
        deletePayload.set("blockId", blockIdNode.deepCopy());

        ObjectNode deleteOp = objectMapper.createObjectNode();
        deleteOp.set("id", charIdNode.deepCopy());
        copyFieldIfPresent(opNode, deleteOp, "parentId");
        copyFieldIfPresent(opNode, deleteOp, "value");
        copyFieldIfPresent(opNode, deleteOp, "formatState");
        deletePayload.set("op", deleteOp);

        return new OperationMessage(
                insertOperation.getSessionId(),
                SERVER_USER_ID,
                generateOperationId("undo"),
                OperationKind.CHARACTER_DELETE,
                deletePayload
        );
    }

    private OperationMessage createRestoreForDeletedCharacter(OperationMessage deleteOperation) {
        JsonNode payload = deleteOperation.getPayload();
        JsonNode blockIdNode = payload == null ? null : payload.get("blockId");
        JsonNode opNode = payload == null ? null : payload.get("op");
        JsonNode charIdNode = opNode == null ? null : opNode.get("id");

        if (blockIdNode == null || blockIdNode.isNull()
                || charIdNode == null || charIdNode.isNull()
                || !hasRequiredDeleteInverseMetadata(opNode)) {
            return null;
        }

        ObjectNode restorePayload = objectMapper.createObjectNode();
        restorePayload.set("blockId", blockIdNode.deepCopy());

        ObjectNode restoreOp = objectMapper.createObjectNode();
        restoreOp.set("id", charIdNode.deepCopy());
        copyFieldIfPresent(opNode, restoreOp, "parentId");
        copyFieldIfPresent(opNode, restoreOp, "value");
        copyFieldIfPresent(opNode, restoreOp, "formatState");
        restorePayload.set("op", restoreOp);

        return new OperationMessage(
                deleteOperation.getSessionId(),
                SERVER_USER_ID,
                generateOperationId("undo"),
                OperationKind.CHARACTER_RESTORE,
                restorePayload
        );
    }

    private boolean hasRequiredDeleteInverseMetadata(JsonNode opNode) {
        if (opNode == null || !opNode.isObject()) {
            return false;
        }

        JsonNode valueNode = opNode.get("value");
        if (valueNode == null || !valueNode.isTextual()) {
            return false;
        }

        return true;
    }

    private OperationMessage createRestoreForInsertedCharacter(OperationMessage insertOperation) {
        JsonNode payload = insertOperation.getPayload();
        JsonNode blockIdNode = payload == null ? null : payload.get("blockId");
        JsonNode opNode = payload == null ? null : payload.get("op");
        JsonNode charIdNode = opNode == null ? null : opNode.get("id");
        if (blockIdNode == null || blockIdNode.isNull()
                || charIdNode == null || charIdNode.isNull()) {
            return null;
        }

        ObjectNode restorePayload = objectMapper.createObjectNode();
        restorePayload.set("blockId", blockIdNode.deepCopy());

        ObjectNode restoreOp = objectMapper.createObjectNode();
        restoreOp.set("id", charIdNode.deepCopy());
        copyFieldIfPresent(opNode, restoreOp, "parentId");
        copyFieldIfPresent(opNode, restoreOp, "value");
        copyFieldIfPresent(opNode, restoreOp, "formatState");
        restorePayload.set("op", restoreOp);

        return new OperationMessage(
                insertOperation.getSessionId(),
                SERVER_USER_ID,
                generateOperationId("redo"),
                OperationKind.CHARACTER_RESTORE,
                restorePayload
        );
    }

    private OperationMessage createDeleteForRestoredCharacter(OperationMessage restoreOperation) {
        JsonNode payload = restoreOperation.getPayload();
        JsonNode blockIdNode = payload == null ? null : payload.get("blockId");
        JsonNode opNode = payload == null ? null : payload.get("op");
        JsonNode charIdNode = opNode == null ? null : opNode.get("id");
        if (blockIdNode == null || blockIdNode.isNull()
                || charIdNode == null || charIdNode.isNull()) {
            return null;
        }

        ObjectNode deletePayload = objectMapper.createObjectNode();
        deletePayload.set("blockId", blockIdNode.deepCopy());

        ObjectNode deleteOp = objectMapper.createObjectNode();
        deleteOp.set("id", charIdNode.deepCopy());
        copyFieldIfPresent(opNode, deleteOp, "parentId");
        copyFieldIfPresent(opNode, deleteOp, "value");
        copyFieldIfPresent(opNode, deleteOp, "formatState");
        deletePayload.set("op", deleteOp);

        return new OperationMessage(
                restoreOperation.getSessionId(),
                SERVER_USER_ID,
                generateOperationId("undo"),
                OperationKind.CHARACTER_DELETE,
                deletePayload
        );
    }

    private OperationMessage createInverseFormatOperation(OperationMessage formatOperation) {
        JsonNode payload = formatOperation.getPayload();
        if (!hasRequiredFormatInverseMetadata(payload)) {
            return null;
        }

        ArrayNode inverseTargets = objectMapper.createArrayNode();
        for (JsonNode targetNode : payload.get("affectedChars")) {
            ObjectNode inverseTarget = objectMapper.createObjectNode();
            inverseTarget.set("blockId", targetNode.get("blockId").deepCopy());
            inverseTarget.set("charId", targetNode.get("charId").deepCopy());
            inverseTarget.put("previousValue", targetNode.get("newValue").asBoolean());
            inverseTarget.put("newValue", targetNode.get("previousValue").asBoolean());
            inverseTargets.add(inverseTarget);
        }

        ObjectNode inversePayload = objectMapper.createObjectNode();
        copyFieldIfPresent(payload, inversePayload, "startOffset");
        copyFieldIfPresent(payload, inversePayload, "endOffset");
        inversePayload.set("format", payload.get("format").deepCopy());
        inversePayload.put("newValue", inverseTargets.get(0).get("newValue").asBoolean());
        inversePayload.set("affectedChars", inverseTargets);

        // A mixed-format restore can contain both true and false target values.
        // Clients treat each target's newValue as authoritative; this kind is only the broad label.
        OperationKind inverseKind = inverseTargets.get(0).get("newValue").asBoolean()
                ? OperationKind.FORMAT_APPLY
                : OperationKind.FORMAT_REMOVE;

        return new OperationMessage(
                formatOperation.getSessionId(),
                SERVER_USER_ID,
                generateOperationId("undo"),
                inverseKind,
                inversePayload
        );
    }

    private boolean hasRequiredFormatInverseMetadata(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            return false;
        }

        JsonNode formatNode = payload.get("format");
        if (formatNode == null || !formatNode.isTextual() || formatNode.asText().isBlank()) {
            return false;
        }

        JsonNode affectedCharsNode = payload.get("affectedChars");
        if (affectedCharsNode == null || !affectedCharsNode.isArray() || affectedCharsNode.isEmpty()) {
            return false;
        }

        for (JsonNode targetNode : affectedCharsNode) {
            if (!hasRequiredFormatTargetMetadata(targetNode)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasRequiredFormatTargetMetadata(JsonNode targetNode) {
        if (targetNode == null || !targetNode.isObject()) {
            return false;
        }

        JsonNode blockIdNode = targetNode.get("blockId");
        JsonNode charIdNode = targetNode.get("charId");
        JsonNode previousValueNode = targetNode.get("previousValue");
        JsonNode newValueNode = targetNode.get("newValue");

        return blockIdNode != null && blockIdNode.isObject()
                && charIdNode != null && charIdNode.isObject()
                && previousValueNode != null && previousValueNode.isBoolean()
                && newValueNode != null && newValueNode.isBoolean();
    }

    private void copyFieldIfPresent(JsonNode source, ObjectNode target, String fieldName) {
        JsonNode field = source.get(fieldName);
        if (field != null) {
            target.set(fieldName, field.deepCopy());
        }
    }

    private ErrorMessage validateContext(CollaborationSession collaborationSession, ConnectedUser requester) {
        if (collaborationSession == null || requester == null) {
            return new ErrorMessage(
                    "SESSION_NOT_JOINED",
                    "WebSocket client must join a collaboration session before requesting undo or redo."
            );
        }

        if (!permissionService.canEdit(requester)) {
            return new ErrorMessage(
                    "PERMISSION_DENIED",
                    "User role '" + requester.getRole() + "' cannot request undo or redo."
            );
        }

        return null;
    }

    private OperationMessage copyGeneratedOperation(OperationMessage operationMessage, String sessionId, String idPrefix) {
        return new OperationMessage(
                sessionId,
                SERVER_USER_ID,
                generateOperationId(idPrefix),
                operationMessage.getOperationKindEnum(),
                operationMessage.getPayload() == null ? null : operationMessage.getPayload().deepCopy()
        );
    }

    private void addUndoRedoMetadata(OperationMessage operationMessage, ConnectedUser requester, String action) {
        if (operationMessage == null || requester == null || operationMessage.getPayload() == null) {
            return;
        }
        if (!(operationMessage.getPayload() instanceof ObjectNode payloadObject)) {
            return;
        }

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("requestedByUserId", requester.getUserId().getValue());
        metadata.put("action", action);
        payloadObject.set("undoRedoMetadata", metadata);
        operationMessage.setPayload(payloadObject);
    }

    private String generateOperationId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    public record UndoRedoResult(OperationMessage operationMessage, int recipientCount, ErrorMessage errorMessage) {
        public static UndoRedoResult success(OperationMessage operationMessage, int recipientCount) {
            return new UndoRedoResult(operationMessage, recipientCount, null);
        }

        public static UndoRedoResult noOp() {
            return new UndoRedoResult(null, 0, null);
        }

        public static UndoRedoResult failure(ErrorMessage errorMessage) {
            return new UndoRedoResult(null, 0, errorMessage);
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }

        public boolean isNoOp() {
            return isSuccess() && operationMessage == null;
        }
    }
}
