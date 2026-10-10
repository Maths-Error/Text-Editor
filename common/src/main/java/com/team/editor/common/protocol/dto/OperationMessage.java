package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.team.editor.common.model.UserId;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;
import com.team.editor.common.protocol.message.OperationKind;

/**
 * Carries one editor operation between collaboration clients.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperationMessage implements CollaborationMessage {
    private static final ObjectMapper PAYLOAD_OBJECT_MAPPER = new ObjectMapper();

    private String sessionId;
    private UserId senderId;
    private String operationId;
    private OperationKind operationKind;
    private JsonNode payload = NullNode.getInstance();


    // --------------------
    // Constructors
    // --------------------

    public OperationMessage() {
    }

    public OperationMessage(String sessionId, UserId senderId, OperationKind operationKind, JsonNode payload) {
        this.sessionId = sessionId;
        this.senderId = senderId;
        this.operationKind = operationKind;
        setPayload(payload);
    }

    public OperationMessage(String sessionId, UserId senderId, String operationId, OperationKind operationKind, JsonNode payload) {
        this(sessionId, senderId, operationKind, payload);
        this.operationId = operationId;
    }

    public OperationMessage(String sessionId, UserId senderId, String operationKind, String payloadJson) {
        this(
                sessionId,
                senderId,
                OperationKind.fromString(operationKind),
                parsePayloadJson(payloadJson)
        );
    }

    // --------------------
    // Getters
    // --------------------

    public String getSessionId() {
        return sessionId;
    }

    public UserId getSenderId() {
        return senderId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getOperationKind() {
        return operationKind == null ? null : operationKind.getWireName();
    }

    public OperationKind getOperationKindEnum() {
        return operationKind;
    }

    public JsonNode getPayload() {
        return payload;
    }

    // --------------------
    // Setters
    // --------------------

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setSenderId(UserId senderId) {
        this.senderId = senderId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public void setOperationKind(String operationKind) {
        this.operationKind = operationKind == null ? null : OperationKind.fromString(operationKind);
    }

    public void setOperationKind(OperationKind operationKind) {
        this.operationKind = operationKind;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload == null ? NullNode.getInstance() : payload.deepCopy();
    }

    // --------------------
    // Message Type
    // --------------------

    @Override
    public MessageType messageType() {
        return MessageType.OPERATION;
    }

    private static JsonNode parsePayloadJson(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return NullNode.getInstance();
        }

        try {
            return PAYLOAD_OBJECT_MAPPER.readTree(payloadJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Operation payload must be valid JSON.", exception);
        }
    }
}
