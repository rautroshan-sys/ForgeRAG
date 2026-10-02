package com.forgerag.exception;

public class EmbeddingFailedException extends RuntimeException {
    public EmbeddingFailedException(String message) { super(message); }
    public EmbeddingFailedException(String message, Throwable cause) { super(message, cause); }
}
