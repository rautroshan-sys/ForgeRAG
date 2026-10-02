package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import com.forgerag.ingestion.EmbeddingService;
import com.forgerag.repository.VectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Retrieval Node — executes Inner Product search over document_chunks.
 *
 * Embeds the current AgentState.query (which may be rewritten on retries)
 * and retrieves the top-5 most similar chunks. Updates AgentState.retrievedContext.
 */
@Component
public class RetrievalNode {

    private static final Logger log = LoggerFactory.getLogger(RetrievalNode.class);
    private static final int TOP_K = 5;

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
        log.debug("RetrievalNode retrieved {} chunks", chunks.size());
    }
}
