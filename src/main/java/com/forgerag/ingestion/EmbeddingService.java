package com.forgerag.ingestion;

import com.forgerag.client.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Calls Gemini for embeddings and L2-normalizes the output vector.
 *
 * Normalization is done here (at ingest time) so that Inner Product search
 * at query time is mathematically equivalent to cosine similarity — without
 * the per-comparison normalization cost. See ARCHITECTURE.md § Key Decisions.
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private final GeminiClient geminiClient;

    public EmbeddingService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Returns a unit-length (L2-normalized) embedding for the given text.
     */
    public float[] embed(String text) {
        float[] raw = geminiClient.embed(text);
        return normalize(raw);
    }

    private float[] normalize(float[] v) {
        double sumSq = 0.0;
        for (float x : v) sumSq += (double) x * x;
        double norm = Math.sqrt(sumSq);
        if (norm == 0.0) {
            log.warn("Zero-norm embedding returned from Gemini — returning as-is");
            return v;
        }
        float[] result = new float[v.length];
        for (int i = 0; i < v.length; i++) result[i] = (float) (v[i] / norm);
        return result;
    }
}
