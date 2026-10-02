package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import com.forgerag.client.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evaluation Node — LLM-as-Judge grounding check.
 *
 * Prompts Gemini (temperature=0) to explicitly check the draft answer
 * against the retrieved context. The model must return its verdict as
 * either "VERDICT: VALID" or "VERDICT: HALLUCINATED" on a dedicated line,
 * followed by its reasoning. This makes the verdict reliably parseable.
 *
 * Updates AgentState.verdict and AgentState.evaluationReasoning.
 */
@Component
public class EvaluationNode {

    private static final Logger log = LoggerFactory.getLogger(EvaluationNode.class);
    private final GeminiClient geminiClient;

    public EvaluationNode(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public void execute(AgentState state) {
        log.debug("EvaluationNode executing (retry={})", state.getRetryCount());

        String prompt = buildEvaluationPrompt(
                state.getQuery(),
                state.getRetrievedContext(),
                state.getDraftAnswer()
        );

        String response = geminiClient.evaluate(prompt);
        parseVerdict(response, state);
        log.info("EvaluationNode verdict={} (retry={})", state.getVerdict(), state.getRetryCount());
    }

    private String buildEvaluationPrompt(String query, List<String> context, String draft) {
        String contextBlock = formatContext(context);
        return """
                You are a strict grounding evaluator. Your job is to check whether \
                every factual claim in the DRAFT ANSWER is supported by the CONTEXT CHUNKS. \
                A claim is hallucinated if it cannot be verified from the context.
                
                CONTEXT CHUNKS:
                %s
                
                ORIGINAL QUESTION: %s
                
                DRAFT ANSWER: %s
                
                Instructions:
                1. List any claims in the draft that are NOT supported by the context chunks.
                2. On its own line, write EXACTLY one of:
                   VERDICT: VALID
                   VERDICT: HALLUCINATED
                3. After the verdict line, explain your reasoning briefly.
                
                Your evaluation:""".formatted(contextBlock, query, draft);
    }

    private String formatContext(List<String> chunks) {
        if (chunks.isEmpty()) return "[No context retrieved]";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("[Chunk ").append(i + 1).append("]\n").append(chunks.get(i)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private void parseVerdict(String response, AgentState state) {
        String upper = response.toUpperCase();
        String verdict;
        if (upper.contains("VERDICT: VALID")) {
            verdict = "VALID";
        } else if (upper.contains("VERDICT: HALLUCINATED")) {
            verdict = "HALLUCINATED";
        } else {
            // If the model doesn't follow the format, treat as HALLUCINATED (fail-safe)
            log.warn("EvaluationNode: could not parse verdict from response — defaulting to HALLUCINATED");
            verdict = "HALLUCINATED";
        }
        state.setVerdict(verdict);
        state.setEvaluationReasoning(response.strip());
    }
}
