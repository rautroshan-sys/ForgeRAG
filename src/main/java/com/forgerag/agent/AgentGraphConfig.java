package com.forgerag.agent;

import com.forgerag.agent.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Agent Graph — the cyclic state machine implementing the CRAG loop.
 *
 * Loop:
 *   1. Retrieve relevant chunks (embeds current query)
 *   1a. Route: if retrieval confidence is low → WebSearchNode (CRAG fallback)
 *   2. Generate a draft answer (from vector-DB context OR web-search context)
 *   3. Evaluate the draft (LLM-as-judge)
 *   4. Route:
 *      - VALID     → return grounded AgentState
 *      - RETRY     → rewrite query, increment retry count, loop back to step 1
 *      - EXHAUSTED → return ungrounded AgentState (caller returns 422)
 *
 * @param query the user's original question
 * @return final AgentState — caller checks isGrounded(), isWebSearchUsed(), getWebSearchSources()
 */
@Service
public class AgentGraphConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentGraphConfig.class);

    @Value("${forgerag.agent.max-retries:3}")
    private int maxRetries;

    private final RetrievalNode retrievalNode;
    private final WebSearchNode webSearchNode;
    private final GenerationNode generationNode;
    private final EvaluationNode evaluationNode;
    private final QueryRewriterNode queryRewriterNode;

    public AgentGraphConfig(RetrievalNode retrievalNode,
                            WebSearchNode webSearchNode,
                            GenerationNode generationNode,
                            EvaluationNode evaluationNode,
                            QueryRewriterNode queryRewriterNode) {
        this.retrievalNode = retrievalNode;
        this.webSearchNode = webSearchNode;
        this.generationNode = generationNode;
        this.evaluationNode = evaluationNode;
        this.queryRewriterNode = queryRewriterNode;
    }

    /**
     * Runs the full agentic CRAG loop for a given query.
     *
     * Loop:
     *   1.  Retrieve relevant chunks (embeds current query, measures confidence score)
     *   1a. CRAG route: if retrieval confidence is low → WebSearchNode (web fallback)
     *   2.  Generate a draft answer (from vector-DB OR web-search context)
     *   3.  Evaluate the draft (LLM-as-judge)
     *   4.  Route:
     *       - VALID     → return grounded AgentState
     *       - RETRY     → rewrite query, increment retry count, loop back to step 1
     *       - EXHAUSTED → return ungrounded AgentState (caller returns 422)
     *
     * @param query the user's original question
     * @return final AgentState — caller checks isGrounded(), isWebSearchUsed(), getWebSearchSources()
     */
    public AgentState run(String query) {
        AgentState state = new AgentState(query, maxRetries);
        log.info("AgentGraph starting for query='{}' maxRetries={}", query, maxRetries);

        while (true) {
            // ── Step 1: Retrieve ──────────────────────────────────────────
            retrievalNode.execute(state);

            // ── Step 1a: CRAG pre-generation confidence check ─────────────
            if (RouterEdge.routeAfterRetrieval(state) == RouterEdge.Decision.WEB_SEARCH) {
                webSearchNode.execute(state);
            }

            // ── Step 2: Generate ─────────────────────────────────────────
            generationNode.execute(state);

            // ── Step 3: Evaluate ─────────────────────────────────────────
            evaluationNode.execute(state);

            // ── Step 4: Route ─────────────────────────────────────────────
            RouterEdge.Decision decision = RouterEdge.route(state);

            switch (decision) {
                case VALID -> {
                    log.info("AgentGraph complete: grounded after {} retries, webSearch={}",
                             state.getRetryCount(), state.isWebSearchUsed());
                    return state;
                }
                case EXHAUSTED -> {
                    log.warn("AgentGraph exhausted: {} retries without grounded answer", state.getRetryCount());
                    return state;
                }
                case RETRY -> {
                    // ── Step 5: Rewrite + loop ────────────────────────────
                    queryRewriterNode.execute(state);
                    // retryCount was incremented inside QueryRewriterNode
                }
                default -> {
                    // WEB_SEARCH handled pre-generation above; should not reach here
                    log.warn("Unexpected router decision {} — treating as EXHAUSTED", decision);
                    return state;
                }
            }
        }
    }
}
