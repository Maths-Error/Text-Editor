package com.team.editor.server.session;

import java.time.Instant;
import java.util.Objects;

import com.team.editor.common.protocol.dto.OperationMessage;

/**
 * Stores the original edit and the server-generated edit that can undo it.
 */
public class UndoableOperation {
    private final String operationId;
    private final OperationMessage originalOperation;
    private final OperationMessage inverseOperation;
    private final OperationMessage redoOperation;
    private final Instant timestamp;

    public UndoableOperation(String operationId, OperationMessage originalOperation, OperationMessage inverseOperation) {
        this(operationId, originalOperation, inverseOperation, originalOperation);
    }

    public UndoableOperation(
            String operationId,
            OperationMessage originalOperation,
            OperationMessage inverseOperation,
            OperationMessage redoOperation
    ) {
        this.operationId = Objects.requireNonNull(operationId, "operationId cannot be null");
        this.originalOperation = copyOperation(Objects.requireNonNull(originalOperation, "originalOperation cannot be null"));
        this.inverseOperation = inverseOperation == null ? null : copyOperation(inverseOperation);
        this.redoOperation = redoOperation == null ? null : copyOperation(redoOperation);
        this.timestamp = Instant.now();
    }

    public String getOperationId() {
        return operationId;
    }

    public OperationMessage getOriginalOperation() {
        return copyOperation(originalOperation);
    }

    public OperationMessage getInverseOperation() {
        return inverseOperation == null ? null : copyOperation(inverseOperation);
    }

    public OperationMessage getRedoOperation() {
        return redoOperation == null ? null : copyOperation(redoOperation);
    }

    public boolean hasInverseOperation() {
        return inverseOperation != null;
    }

    public boolean hasRedoOperation() {
        return redoOperation != null;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    private static OperationMessage copyOperation(OperationMessage operationMessage) {
        OperationMessage copy = new OperationMessage(
                operationMessage.getSessionId(),
                operationMessage.getSenderId(),
                operationMessage.getOperationId(),
                operationMessage.getOperationKindEnum(),
                operationMessage.getPayload() == null ? null : operationMessage.getPayload().deepCopy()
        );
        return copy;
    }
}
