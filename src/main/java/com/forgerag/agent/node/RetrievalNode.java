package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import com.forgerag.ingestion.EmbeddingService;
import com.forgerag.repository.VectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retrieval Node — executes Inner Product search over document_chunks.
 *
 * Also measures the top similarity score and sets AgentState.retrievalConfident
 * so the RouterEdge can decide whether to fall back to web search (CRAG).
 *
 * Threshold config: forgerag.retrieval.confidence-threshold (default -0.40)
 * pgvector negates IP for ordering, so scores are negative:
 *   -0.05  =  very high similarity  (confident)
 *   -0.40  =  moderate / borderline  (threshold)
 *   -0.80  =  low similarity         (fallback to web search)
 */
@Component
public class RetrievalNode {

    private static final Logger log = LoggerFactory.getLogger(RetrievalNode.class);
    private static final int TOP_K = 5;

    @Value("${forgerag.retrieval.confidence-threshold:-0.40}")
    private float confidenceThreshold;

    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;

    public RetrievalNode(EmbeddingService embeddingService, VectorRepository vectorRepository) {
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
    }

    public void execute(AgentState state) {
        log.debug("RetrievalNode executing for query='{}' (retry={})", state.getQuery(), state.getRetryCount());
        float[] queryEmbedding = embeddingService.embed(state.getQuery());
        List<String> chunks = vectorRepository.findSimilar(queryEmbedding, TOP_K);
        state.setRetrievedContext(chunks);

        // Measure confidence: is the best matching chunk close enough?
        float bestScore = vectorRepository.findBestScore(queryEmbedding);
        boolean confident = !chunks.isEmpty() && bestScore > confidenceThreshold;
        state.setRetrievalConfident(confident);

        log.info("RetrievalNode retrieved {} chunks, bestScore={}, confident={}",
                chunks.size(), bestScore, confident);
    }
}
