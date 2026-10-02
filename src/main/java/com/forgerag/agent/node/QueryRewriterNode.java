package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import com.forgerag.client.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Query Rewriter Node — rewrites the search query when evaluation detects hallucination.
 *
 * Uses Gemini to generate a better query based on:
 *  - The original question
 *  - The evaluation reasoning (what went wrong with the previous draft)
 *  - The retry count (to nudge the rewriter to broaden/narrow scope)
 *
 * Updates AgentState.query (the rewritten version) and increments retryCount.
 */
@Component
public class QueryRewriterNode {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriterNode.class);
    private final GeminiClient geminiClient;

    public QueryRewriterNode(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public void execute(AgentState state) {
        log.debug("QueryRewriterNode executing (retry={})", state.getRetryCount());

        String rewrittenQuery = rewrite(state);
        log.info("QueryRewriterNode: '{}' -> '{}'", state.getQuery(), rewrittenQuery);

        state.setQuery(rewrittenQuery);
        state.incrementRetryCount();
    }

    private String rewrite(AgentState state) {
        String prompt = """
                You are a search query optimizer. The previous retrieval attempt failed \
                to find supporting evidence for an answer, and the evaluator flagged the draft as hallucinated.
                
                Original user question: %s
                Current search query: %s
                Evaluator feedback: %s
                Retry attempt: %d
                
                Rewrite the search query to retrieve more relevant document chunks. \
                Make it more specific, use different keywords, or broaden/narrow scope as needed. \
                Return ONLY the rewritten query — no explanation, no prefix, just the query text.
                """.formatted(
                state.getOriginalQuery(),
                state.getQuery(),
                state.getEvaluationReasoning() != null ? state.getEvaluationReasoning() : "No reasoning available",
                state.getRetryCount() + 1
        );

        try {
            String rewritten = geminiClient.generate(prompt).strip();
            // Sanity check: if the rewrite is empty or too short, fall back to original
            if (rewritten.isBlank() || rewritten.length() < 5) {
                log.warn("QueryRewriterNode returned empty/short rewrite — keeping original query");
                return state.getQuery();
            }
            return rewritten;
        } catch (Exception e) {
            log.error("QueryRewriterNode: Gemini call failed — keeping original query", e);
            return state.getQuery();
        }
    }
}
