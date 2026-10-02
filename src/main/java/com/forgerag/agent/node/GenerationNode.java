package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import com.forgerag.client.GeminiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generation Node — drafts an answer grounded in the retrieved context.
 *
 * Builds a prompt combining the query and top retrieved chunks,
 * calls Gemini to generate a draft, and stores it in AgentState.draftAnswer.
 */
@Component
public class GenerationNode {

    private static final Logger log = LoggerFactory.getLogger(GenerationNode.class);
    private final GeminiClient geminiClient;

    public GenerationNode(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public void execute(AgentState state) {
        log.debug("GenerationNode executing (retry={})", state.getRetryCount());

        List<String> context = state.getRetrievedContext();
        if (context.isEmpty()) {
            log.warn("GenerationNode: no retrieved context — generating with empty context");
        }

        String contextBlock = formatContext(context);
        String prompt = buildPrompt(state.getQuery(), contextBlock);

        String draft = geminiClient.generate(prompt);
        state.setDraftAnswer(draft);
        log.debug("GenerationNode produced draft of {} chars", draft.length());
    }

    private String formatContext(List<String> chunks) {
        if (chunks.isEmpty()) return "[No context retrieved]";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("[Chunk ").append(i + 1).append("]\n").append(chunks.get(i)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String buildPrompt(String query, String context) {
        return """
                You are a precise, factual assistant. Answer the question below \
                using ONLY the information provided in the context chunks. \
                If the context does not contain enough information to answer, say so explicitly. \
                Do not invent or infer facts not present in the context.
                
                Context:
                %s
                
                Question: %s
                
                Answer:""".formatted(context, query);
    }
}
