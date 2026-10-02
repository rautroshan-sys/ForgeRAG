package com.forgerag.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps exceptions to the exact error response shapes in API_CONTRACT.md.
 * No silent swallowing — every path produces a structured error body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Validation failures: @NotBlank on QueryRequest.query → 400 QUERY_REQUIRED */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fe = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String field = fe != null ? fe.getField() : "unknown";
        log.warn("Validation failure on field '{}': {}", field, fe != null ? fe.getDefaultMessage() : "");
        return ResponseEntity.badRequest().body(Map.of(
                "error", "QUERY_REQUIRED",
                "message", "Request body must include a non-empty 'query' field."
        ));
    }

    /** Document ingestion: empty body / no content */
    @ExceptionHandler(DocumentEmptyException.class)
    public ResponseEntity<Map<String, String>> handleDocumentEmpty(DocumentEmptyException ex) {
        log.warn("Document empty: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "error", "DOCUMENT_EMPTY",
                "message", "No content to chunk."
        ));
    }

    /** Gemini API unreachable or errors out */
    @ExceptionHandler(ModelUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleModelUnavailable(ModelUnavailableException ex) {
        log.error("Gemini model unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "MODEL_UNAVAILABLE",
                "message", "Generation or evaluation call to Gemini failed."
        ));
    }

    /** Embedding call to Gemini failed during ingestion */
    @ExceptionHandler(EmbeddingFailedException.class)
    public ResponseEntity<Map<String, String>> handleEmbeddingFailed(EmbeddingFailedException ex) {
        log.error("Embedding failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "error", "EMBEDDING_FAILED",
                "message", "Could not generate embeddings for one or more chunks."
        ));
    }

    /** Catch-all — never silently swallow unknowns */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "INTERNAL_ERROR",
                "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred."
        ));
    }
}
