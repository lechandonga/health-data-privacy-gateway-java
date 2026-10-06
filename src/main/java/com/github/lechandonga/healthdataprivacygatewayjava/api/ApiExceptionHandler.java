package com.github.lechandonga.healthdataprivacygatewayjava.api;

import com.github.lechandonga.healthdataprivacygatewayjava.deid.RecordValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "reason", e.getMessage()));
    }

    @ExceptionHandler(RecordValidationException.class)
    public ResponseEntity<Map<String, Object>> invalid(RecordValidationException e) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("error", "INVALID_RECORD", "reason", e.getReason().name(),
                        "field", e.getField() == null ? "" : e.getField(),
                        "message", e.getMessage()));
    }
}
