package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Router Edge — conditional branching after EvaluationNode.
 *
 * Returns a routing decision based on AgentState:
 *  - "valid"      → proceed to final output
 *  - "retry"      → send to QueryRewriterNode for another attempt
 *  - "exhausted"  → retry limit reached; return ungrounded answer with 422
 */
public class RouterEdge {

    private static final Logger log = LoggerFactory.getLogger(RouterEdge.class);

    public enum Decision { VALID, RETRY, EXHAUSTED }

    public static Decision route(AgentState state) {
        if (state.isGrounded()) {
            log.info("Router: VALID — answer is grounded");
            return Decision.VALID;
        }
        if (state.isRetryLimitReached()) {
            log.warn("Router: EXHAUSTED — retry limit {} reached without grounded answer", state.getMaxRetries());
            return Decision.EXHAUSTED;
        }
        log.info("Router: RETRY — hallucination detected, retry {}/{}", 
                 state.getRetryCount() + 1, state.getMaxRetries());
        return Decision.RETRY;
    }
}
