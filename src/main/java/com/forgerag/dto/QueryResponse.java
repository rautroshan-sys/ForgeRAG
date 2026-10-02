package com.forgerag.dto;

/**
 * Response shape for POST /api/query — matches the frozen API contract exactly.
 *
 * 200 OK  → grounded=true,  answer=<text>, retryCount, evaluation.verdict="VALID"
 * 422     → grounded=false, answer=null,   retryCount, evaluation.verdict="HALLUCINATED"
 */
public class QueryResponse {

    private String answer;
    private boolean grounded;
    private int retryCount;
    private EvaluationResult evaluation;

    public QueryResponse() {}

    public QueryResponse(String answer, boolean grounded, int retryCount, EvaluationResult evaluation) {
        this.answer = answer;
        this.grounded = grounded;
        this.retryCount = retryCount;
        this.evaluation = evaluation;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public boolean isGrounded() { return grounded; }
    public void setGrounded(boolean grounded) { this.grounded = grounded; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public EvaluationResult getEvaluation() { return evaluation; }
    public void setEvaluation(EvaluationResult evaluation) { this.evaluation = evaluation; }
}
