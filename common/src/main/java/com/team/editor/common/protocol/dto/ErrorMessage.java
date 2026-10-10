package com.team.editor.common.protocol.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.team.editor.common.protocol.message.CollaborationMessage;
import com.team.editor.common.protocol.message.MessageType;

/**
 * Shared error payload used for readable transport and validation failures.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorMessage implements CollaborationMessage {
    private String errorCode;
    private String message;

    // --------------------
    // Constructors
    // --------------------

    public ErrorMessage() {
    }

    public ErrorMessage(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    // --------------------
    // Getters
    // --------------------

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    // --------------------
    // Setters
    // --------------------

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // --------------------
    // Message Type
    // --------------------

    @Override
    public MessageType messageType() {
        return MessageType.ERROR;
    }
}
