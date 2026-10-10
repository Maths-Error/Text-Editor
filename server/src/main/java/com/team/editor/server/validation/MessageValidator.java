package com.team.editor.server.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.editor.common.protocol.dto.ErrorMessage;
import com.team.editor.common.protocol.dto.JoinSessionRequest;
import com.team.editor.common.protocol.dto.OperationMessage;
import com.team.editor.common.protocol.message.MessageType;
import com.team.editor.common.protocol.message.SessionRole;
import com.team.editor.server.session.CollaborationSession;
import com.team.editor.server.session.ConnectedUser;
import org.springframework.stereotype.Component;

@Component
public class MessageValidator {
    /**
     * Tries to parse the raw websocket text into a JSON tree.
     * The websocket handler calls this first whenever a client sends text.
     * On success it returns the parsed JSON node, and on failure it returns an error message object for the handler to send back.
     */
    public ValidationResult<JsonNode> parseJson(String payload, ObjectMapper objectMapper) {
        if (payload == null || payload.isBlank()) {
            return ValidationResult.failure("INVALID_JSON", "Payload must be a non-empty JSON string.");
        }

        try {
            return ValidationResult.success(objectMapper.readTree(payload));
        } catch (JsonProcessingException exception) {
            return ValidationResult.failure("INVALID_JSON", "Malformed JSON payload.");
        }
    }

    /**
     * Checks whether the incoming JSON contains a readable messageType field
     * that matches one of the shared protocol enum values.
     * The handler uses this to decide which branch of the websocket flow should run next.
     */
    public ValidationResult<MessageType> validateMessageType(JsonNode payloadNode) {
        JsonNode messageTypeNode = payloadNode.get("messageType");
        if (messageTypeNode == null || !messageTypeNode.isTextual()) {
            return ValidationResult.failure("INVALID_MESSAGE_TYPE", "Field 'messageType' is required.");
        }

        String rawMessageType = messageTypeNode.asText().trim();
        if (rawMessageType.isBlank()) {
            return ValidationResult.failure("INVALID_MESSAGE_TYPE", "Field 'messageType' cannot be blank.");
        }

        try {
            return ValidationResult.success(MessageType.valueOf(rawMessageType));
        } catch (IllegalArgumentException exception) {
            return ValidationResult.failure(
                    "INVALID_MESSAGE_TYPE",
                    "Unknown message type '" + rawMessageType + "'."
            );
        }
    }

    /**
     * Validates the raw JSON shape of a join request before deserialization.
     * This is called after the handler sees JOIN_SESSION_REQUEST.
     * It makes sure the required join fields exist so the server can reject bad requests cleanly instead of crashing during object mapping.
     */
    public ValidationResult<Void> validateJoinRequestPayload(JsonNode payloadNode) {
        ValidationResult<Void> sessionIdValidation = requireTextField(payloadNode, "sessionId");
        if (!sessionIdValidation.isValid()) {
            return sessionIdValidation;
        }

        JsonNode userIdNode = payloadNode.get("userId");
        if (userIdNode == null || !userIdNode.isObject()) {
            return ValidationResult.failure(
                    "INVALID_JOIN_REQUEST",
                    "Field 'userId' is required and must be an object."
            );
        }

        ValidationResult<Void> userIdValueValidation = requireTextField(userIdNode, "value", "userId.value");
        if (!userIdValueValidation.isValid()) {
            return userIdValueValidation;
        }

        ValidationResult<Void> displayNameValidation = requireTextField(payloadNode, "displayName");
        if (!displayNameValidation.isValid()) {
            return displayNameValidation;
        }

        JsonNode roleNode = payloadNode.get("role");
        if (roleNode == null || roleNode.isNull()) {
            return ValidationResult.success(null);
        }

        return validateEnumField(roleNode, "role", SessionRole.class, "INVALID_JOIN_REQUEST");
    }

    /**
     * Validates the already-deserialized join request object.
     * The join service calls this as a second safety check before the user is added to an in-memory session.
     */
    public ValidationResult<JoinSessionRequest> validateJoinRequest(JoinSessionRequest request) {
        if (request == null) {
            return ValidationResult.failure("INVALID_JOIN_REQUEST", "Join request cannot be null.");
        }

        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            return ValidationResult.failure("INVALID_JOIN_REQUEST", "Field 'sessionId' is required.");
        }

        if (request.getUserId() == null) {
            return ValidationResult.failure("INVALID_JOIN_REQUEST", "Field 'userId' is required.");
        }

        if (request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            return ValidationResult.failure("INVALID_JOIN_REQUEST", "Field 'displayName' is required.");
        }

        return ValidationResult.success(request);
    }

    /**
     * Validates the raw JSON shape of an operation message before deserialization.
     * The websocket handler calls this after it sees the OPERATION message type and before it starts the live relay flow.
     * Session and sender identity are resolved from the joined websocket session rather than trusted from the client payload.
     */
    public ValidationResult<Void> validateOperationPayload(JsonNode payloadNode) {
        ValidationResult<Void> operationKindValidation = requireTextField(
                payloadNode,
                "operationKind",
                "operationKind",
                "INVALID_OPERATION_MESSAGE"
        );
        if (!operationKindValidation.isValid()) {
            return operationKindValidation;
        }

        JsonNode operationPayloadNode = payloadNode.get("payload");
        if (operationPayloadNode == null || operationPayloadNode.isNull()) {
            return ValidationResult.failure("INVALID_OPERATION_MESSAGE", "Field 'payload' is required.");
        }

        if (operationPayloadNode.isTextual()) {
            if (operationPayloadNode.asText().trim().isEmpty()) {
                return ValidationResult.failure("INVALID_OPERATION_MESSAGE", "Field 'payload' cannot be blank.");
            }
            return ValidationResult.success(null);
        }

        if (!operationPayloadNode.isObject()) {
            return ValidationResult.failure(
                    "INVALID_OPERATION_MESSAGE",
                    "Field 'payload' must be a JSON object or a JSON string."
            );
        }

        return ValidationResult.success(null);
    }

    /**
     * Undo/redo request messages are intentionally flat and carry no client-owned identity fields.
     * The websocket session already tells the server which live user and document room made the request.
     */
    public ValidationResult<Void> validateUndoRedoRequestPayload(JsonNode payloadNode) {
        return validateMessageType(payloadNode).isValid()
                ? ValidationResult.success(null)
                : ValidationResult.failure("INVALID_MESSAGE_TYPE", "Field 'messageType' is required.");
    }

    /**
     * Validates the already-deserialized operation message together with the current websocket session context.
     * This makes sure the sender really joined a session and that the operation includes the editable operation data.
     * The server uses the joined websocket context as the trusted source for session and sender identity.
     */
    public ValidationResult<OperationMessage> validateOperationRequest(
            OperationMessage operationMessage,
            CollaborationSession collaborationSession,
            ConnectedUser sender
    ) {
        if (operationMessage == null) {
            return ValidationResult.failure("INVALID_OPERATION_MESSAGE", "Operation message cannot be null.");
        }

        if (collaborationSession == null || sender == null) {
            return ValidationResult.failure(
                    "SESSION_NOT_JOINED",
                    "WebSocket client must join a collaboration session before sending operations."
            );
        }

        if (operationMessage.getOperationKind() == null || operationMessage.getOperationKind().isBlank()) {
            return ValidationResult.failure("INVALID_OPERATION_MESSAGE", "Field 'operationKind' is required.");
        }

        JsonNode payloadNode = operationMessage.getPayload();
        if (payloadNode == null || payloadNode.isNull()) {
            return ValidationResult.failure("INVALID_OPERATION_MESSAGE", "Field 'payload' is required.");
        }

        if (payloadNode.isTextual() && payloadNode.asText().trim().isEmpty()) {
            return ValidationResult.failure("INVALID_OPERATION_MESSAGE", "Field 'payload' cannot be blank.");
        }

        return ValidationResult.success(operationMessage);
    }

    /**
     * Validates the raw JSON shape of a cursor update before deserialization.
     * Cursor updates only require a numeric character offset; blockId is optional and may be null.
     * Session and user identity are resolved from the joined websocket session rather than trusted from the client.
     */
    public ValidationResult<Void> validateCursorUpdatePayload(JsonNode payloadNode) {
        JsonNode characterOffsetNode = payloadNode.get("characterOffset");
        if (characterOffsetNode == null || !characterOffsetNode.canConvertToInt()) {
            return ValidationResult.failure(
                    "INVALID_CURSOR_MESSAGE",
                    "Field 'characterOffset' is required and must be an integer."
            );
        }

        if (characterOffsetNode.asInt() < 0) {
            return ValidationResult.failure(
                    "INVALID_CURSOR_MESSAGE",
                    "Field 'characterOffset' cannot be negative."
            );
        }

        JsonNode blockIdNode = payloadNode.get("blockId");
        if (blockIdNode != null && !blockIdNode.isNull() && !blockIdNode.isObject()) {
            return ValidationResult.failure(
                    "INVALID_CURSOR_MESSAGE",
                    "Field 'blockId' must be an object or null."
            );
        }

        return ValidationResult.success(null);
    }

    /**
     * Convenience helper for required text fields that use the same JSON name in code and in error messages.
     */
    private ValidationResult<Void> requireTextField(JsonNode node, String fieldName) {
        return requireTextField(node, fieldName, fieldName, "INVALID_JOIN_REQUEST");
    }

    /**
     * Convenience helper for required text fields with a custom message-field name.
     */
    private ValidationResult<Void> requireTextField(JsonNode node, String fieldName, String messageFieldName) {
        return requireTextField(node, fieldName, messageFieldName, "INVALID_JOIN_REQUEST");
    }

    /**
     * Shared helper used by the validator methods above.
     * It checks one text field, reports a readable error when the field is missing or blank,
     * and keeps the higher-level validation methods short and focused on request flow.
     */
    private ValidationResult<Void> requireTextField(
            JsonNode node,
            String fieldName,
            String messageFieldName,
            String errorCode
    ) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || !fieldNode.isTextual()) {
            return ValidationResult.failure(
                    errorCode,
                    "Field '" + messageFieldName + "' is required."
            );
        }

        if (fieldNode.asText().trim().isEmpty()) {
            return ValidationResult.failure(
                    errorCode,
                    "Field '" + messageFieldName + "' cannot be blank."
            );
        }

        return ValidationResult.success(null);
    }

    /**
     * Validates a string enum field such as the optional role on join requests.
     */
    private <E extends Enum<E>> ValidationResult<Void> validateEnumField(
            JsonNode node,
            String messageFieldName,
            Class<E> enumType,
            String errorCode
    ) {
        if (!node.isTextual()) {
            return ValidationResult.failure(errorCode, "Field '" + messageFieldName + "' must be a string.");
        }

        String rawValue = node.asText().trim();
        if (rawValue.isEmpty()) {
            return ValidationResult.failure(errorCode, "Field '" + messageFieldName + "' cannot be blank.");
        }

        try {
            Enum.valueOf(enumType, rawValue);
            return ValidationResult.success(null);
        } catch (IllegalArgumentException exception) {
            return ValidationResult.failure(errorCode, "Field '" + messageFieldName + "' has an invalid value.");
        }
    }

    /**
     * Small wrapper that lets validation methods return either a parsed/validated value
     * or an ErrorMessage that the websocket handler can serialize back to the client.
     */
    public record ValidationResult<T>(T value, ErrorMessage errorMessage) {
        /**
         * Builds a successful validation result and carries the parsed value forward to the next step.
         */
        public static <T> ValidationResult<T> success(T value) {
            return new ValidationResult<>(value, null);
        }

        /**
         * Builds a failed validation result with an error code and human-readable message.
         * The handler uses this to send clean error JSON instead of throwing low-level exceptions to the client.
         */
        public static <T> ValidationResult<T> failure(String errorCode, String message) {
            return new ValidationResult<>(null, new ErrorMessage(errorCode, message));
        }

        /**
         * Tells the caller whether validation passed and the value can safely be used.
         */
        public boolean isValid() {
            return errorMessage == null;
        }
    }
}
