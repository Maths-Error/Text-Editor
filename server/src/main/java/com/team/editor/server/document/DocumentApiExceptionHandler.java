package com.team.editor.server.document;

import com.team.editor.common.protocol.dto.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DocumentApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(DocumentApiExceptionHandler.class);

    @ExceptionHandler(DocumentApiException.class)
    public ResponseEntity<ErrorMessage> handleDocumentApiException(DocumentApiException exception) {
        ErrorMessage errorMessage = new ErrorMessage(exception.getErrorCode(), exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus()).body(errorMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorMessage> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        ErrorMessage errorMessage = new ErrorMessage(
                "INVALID_REQUEST",
                "Request body is missing or invalid JSON."
        );
        return ResponseEntity.status(400).body(errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleUnexpectedException(Exception exception) {
        logger.error("Unexpected document API error.", exception);

        ErrorMessage errorMessage = new ErrorMessage("INTERNAL_ERROR", "Unexpected document API error.");
        return ResponseEntity.status(500).body(errorMessage);
    }
}
