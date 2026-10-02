package com.forgerag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Binds Gemini-related configuration from application.properties.
 * All values come from environment variables — see .env.example.
 * Fails loudly at startup if GEMINI_API_KEY is missing (no default).
 */
@Configuration
public class GeminiConfig {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model.generator:gemini-2.5-flash}")
    private String generatorModel;

    @Value("${gemini.model.evaluator:gemini-2.5-flash}")
    private String evaluatorModel;

    @Value("${gemini.model.embedding:text-embedding-004}")
    private String embeddingModel;

    public String getApiKey() { return apiKey; }
    public String getGeneratorModel() { return generatorModel; }
    public String getEvaluatorModel() { return evaluatorModel; }
    public String getEmbeddingModel() { return embeddingModel; }
}
