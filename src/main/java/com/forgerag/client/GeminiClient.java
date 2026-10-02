package com.forgerag.client;

import com.forgerag.config.GeminiConfig;
import com.forgerag.exception.EmbeddingFailedException;
import com.forgerag.exception.ModelUnavailableException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around LangChain4j's Gemini integrations.
 * Exposes three operations:
 *  - generate(prompt) → String  (for Generation + QueryRewriter nodes)
 *  - evaluate(prompt) → String  (for EvaluationNode, temperature=0)
 *  - embed(text)      → float[] (for EmbeddingService)
 *
 * All network exceptions are caught and re-thrown as ForgeRAG domain exceptions
 * so GlobalExceptionHandler maps them to the correct HTTP status codes.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final ChatLanguageModel generatorModel;
    private final ChatLanguageModel evaluatorModel;
    private final EmbeddingModel embeddingModel;

    public GeminiClient(GeminiConfig config) {
        this.generatorModel = GoogleAiGeminiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getGeneratorModel())
                .temperature(0.2)
                .build();

        this.evaluatorModel = GoogleAiGeminiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getEvaluatorModel())
                .temperature(0.0)   // deterministic judge
                .build();

        this.embeddingModel = GoogleAiEmbeddingModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getEmbeddingModel())
                .build();
    }

    /**
     * Calls the generator model (temperature=0.2).
     * Used by GenerationNode and QueryRewriterNode.
     */
    public String generate(String prompt) {
        try {
            log.debug("Calling Gemini generator, prompt length={}", prompt.length());
            return generatorModel.chat(prompt);
        } catch (Exception e) {
            log.error("Gemini generator call failed", e);
            throw new ModelUnavailableException("Generation call to Gemini failed: " + e.getMessage(), e);
        }
    }

    /**
     * Calls the evaluator model (temperature=0.0 for determinism).
     * Used by EvaluationNode.
     */
    public String evaluate(String prompt) {
        try {
            log.debug("Calling Gemini evaluator");
            return evaluatorModel.chat(prompt);
        } catch (Exception e) {
            log.error("Gemini evaluator call failed", e);
            throw new ModelUnavailableException("Evaluation call to Gemini failed: " + e.getMessage(), e);
        }
    }

    /**
     * Embeds a single text string. Returns the raw float[] values.
     * Caller (EmbeddingService) is responsible for L2 normalization.
     */
    public float[] embed(String text) {
        try {
            log.debug("Calling Gemini embedding, text length={}", text.length());
            Response<Embedding> response = embeddingModel.embed(text);
            return response.content().vector();
        } catch (Exception e) {
            log.error("Gemini embedding call failed", e);
            throw new EmbeddingFailedException("Could not generate embeddings: " + e.getMessage(), e);
        }
    }
}
