package com.forgerag.dto;

import java.util.List;

/**
 * Response shape for POST /api/query — matches the frozen API contract exactly.
 *
 * 200 OK  → grounded=true,  answer=<text>, retryCount, evaluation.verdict="VALID"
 * 422     → grounded=false, answer=null,   retryCount, evaluation.verdict="HALLUCINATED"
 *
 * Extended for CRAG:
 *   webSearchUsed   → true if the CRAG web-search fallback was triggered
 *   webSearchSources→ citation URLs returned by Gemini Google Search grounding
 */
public class QueryResponse {

    private String answer;
    private boolean grounded;
    private int retryCount;
    private EvaluationResult evaluation;
    private boolean webSearchUsed;
    private List<String> webSearchSources;

    public QueryResponse() {}

    public QueryResponse(String answer, boolean grounded, int retryCount,
                         EvaluationResult evaluation,
                         boolean webSearchUsed, List<String> webSearchSources) {
        this.answer = answer;
        this.grounded = grounded;
        this.retryCount = retryCount;
        this.evaluation = evaluation;
        this.webSearchUsed = webSearchUsed;
        this.webSearchSources = webSearchSources;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public boolean isGrounded() { return grounded; }
    public void setGrounded(boolean grounded) { this.grounded = grounded; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public EvaluationResult getEvaluation() { return evaluation; }
    public void setEvaluation(EvaluationResult evaluation) { this.evaluation = evaluation; }

    public boolean isWebSearchUsed() { return webSearchUsed; }
    public void setWebSearchUsed(boolean webSearchUsed) { this.webSearchUsed = webSearchUsed; }

    public List<String> getWebSearchSources() { return webSearchSources; }
    public void setWebSearchSources(List<String> webSearchSources) { this.webSearchSources = webSearchSources; }
}
