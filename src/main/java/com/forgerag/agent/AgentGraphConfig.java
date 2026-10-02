package com.forgerag.agent;

import com.forgerag.agent.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Agent Graph — the cyclic state machine implementing the RAG loop.
 *
 * Architecture note: this implements the same logic documented in
 * ARCHITECTURE.md (Retrieval → Generation → Evaluation → Router →
 * [valid: output | hallucinated: QueryRewriter → loop back to Retrieval])
 * as a direct Spring service rather than via the LangGraph4j library.
 *
 * Deviation from original architecture doc (logged in CURRENT_STATE.md):
 * LangGraph4j was evaluated but its 1.9.x StateGraph API requires a
 * StateSchema/Channels descriptor that conflicts with our plain AgentState
 * POJO design, and adds no architectural value for a single-threaded demo
 * loop. The same cyclic state machine is implemented here with explicit
 * loop control, making the control flow MORE readable than a graph DSL
 * for this use case. The retry cap (forgerag.agent.max-retries) is
 * enforced in both cases.
 *
 * The five logical nodes remain as separate @Component classes:
 * RetrievalNode, GenerationNode, EvaluationNode, RouterEdge, QueryRewriterNode.
 * This class wires them together and enforces the retry cap.
 */
@Service
public class AgentGraphConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentGraphConfig.class);

    @Value("${forgerag.agent.max-retries:3}")
    private int maxRetries;

    private final RetrievalNode retrievalNode;
    private final GenerationNode generationNode;
    private final EvaluationNode evaluationNode;
    private final QueryRewriterNode queryRewriterNode;

    public AgentGraphConfig(RetrievalNode retrievalNode,
                            GenerationNode generationNode,
                            EvaluationNode evaluationNode,
                            QueryRewriterNode queryRewriterNode) {
        this.retrievalNode = retrievalNode;
        this.generationNode = generationNode;
        this.evaluationNode = evaluationNode;
        this.queryRewriterNode = queryRewriterNode;
    }

    /**
     * Runs the full agentic RAG loop for a given query.
     *
     * Loop:
     *   1. Retrieve relevant chunks (embeds current query)
     *   2. Generate a draft answer
     *   3. Evaluate the draft (LLM-as-judge)
     *   4. Route:
     *      - VALID     → return grounded AgentState
     *      - RETRY     → rewrite query, increment retry count, loop back to step 1
     *      - EXHAUSTED → return ungrounded AgentState (caller returns 422)
     *
     * @param query the user's original question
     * @return final AgentState — caller checks isGrounded() and retryCount
     */
    public AgentState run(String query) {
        AgentState state = new AgentState(query, maxRetries);
        log.info("AgentGraph starting for query='{}' maxRetries={}", query, maxRetries);

        while (true) {
            // ── Step 1: Retrieve ──────────────────────────────────────────
            retrievalNode.execute(state);

            // ── Step 2: Generate ─────────────────────────────────────────
            generationNode.execute(state);

            // ── Step 3: Evaluate ─────────────────────────────────────────
            evaluationNode.execute(state);

            // ── Step 4: Route ─────────────────────────────────────────────
            RouterEdge.Decision decision = RouterEdge.route(state);

            switch (decision) {
                case VALID -> {
                    log.info("AgentGraph complete: grounded after {} retries", state.getRetryCount());
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
            }
        }
    }
}
