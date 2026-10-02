package com.forgerag.dto;

/**
 * Evaluation verdict returned inside QueryResponse.
 * verdict: "VALID" or "HALLUCINATED"
 * reasoning: the LLM-judge's explanation.
 */
public class EvaluationResult {

    private String verdict;
    private String reasoning;

    public EvaluationResult() {}

    public EvaluationResult(String verdict, String reasoning) {
        this.verdict = verdict;
        this.reasoning = reasoning;
    }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
}
