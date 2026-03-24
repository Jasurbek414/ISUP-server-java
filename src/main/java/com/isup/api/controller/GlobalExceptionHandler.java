package com.isup.api.controller;

import com.isup.isapi.IsapiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Global exception handler for REST API controllers.
 * Converts IsapiException and general exceptions to structured JSON responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IsapiException.class)
    public ResponseEntity<Map<String, Object>> handleIsapiException(IsapiException ex) {
        int status = ex.getStatusCode() > 0 ? ex.getStatusCode() : 400;
        if (status == 404) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
        log.warn("ISAPI error: {}", ex.getMessage());
        return ResponseEntity.status(status > 0 && status < 600 ? status : 500)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
