package com.github.lechandonga.healthdataprivacygatewayjava.web;

import com.github.lechandonga.healthdataprivacygatewayjava.access.AccessDeniedException;
import com.github.lechandonga.healthdataprivacygatewayjava.access.RecordRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> denied(AccessDeniedException ex) {
        return Map.of("decision", "DENIED", "reason", ex.getReason(),
                "identifiable", false, "view", ex.getView());
    }

    @ExceptionHandler(RecordRejectedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> rejected(RecordRejectedException ex) {
        return Map.of("decision", "REJECTED", "reason", "MALFORMED_RECORD",
                "patientId", ex.getPatientId(), "errors", ex.getReasons());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> missingParam(MissingServletRequestParameterException ex) {
        return Map.of("decision", "REJECTED", "reason", "MISSING_PARAMETER",
                "parameter", ex.getParameterName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> notFound(IllegalArgumentException ex) {
        return Map.of("decision", "DENIED", "reason", ex.getMessage());
    }
}
