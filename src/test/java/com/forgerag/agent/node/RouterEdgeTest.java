package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RouterEdge.
 *
 * No Spring context needed — RouterEdge.route() is a pure static method.
 * Tests cover all three Decision outcomes: VALID, RETRY, and EXHAUSTED.
 */
class RouterEdgeTest {

    // ── VALID path ──────────────────────────────────────────────────────────

    @Test
    void validVerdict_returnsValid() {
        AgentState state = stateWith("VALID", 0, 3);
        assertEquals(RouterEdge.Decision.VALID, RouterEdge.route(state));
    }

    @Test
    void validVerdict_caseInsensitive() {
        AgentState state = stateWith("valid", 0, 3);
        assertEquals(RouterEdge.Decision.VALID, RouterEdge.route(state),
                "isGrounded() should be case-insensitive");
    }

    @Test
    void validVerdict_evenAtRetryLimit_returnsValid() {
        // VALID always wins — even if retryCount equals maxRetries
        AgentState state = stateWith("VALID", 3, 3);
        assertEquals(RouterEdge.Decision.VALID, RouterEdge.route(state),
                "VALID verdict should take priority over retry-limit check");
    }

    // ── RETRY path ──────────────────────────────────────────────────────────

    @Test
    void hallucinatedVerdict_belowLimit_returnsRetry() {
        AgentState state = stateWith("HALLUCINATED", 0, 3);
        assertEquals(RouterEdge.Decision.RETRY, RouterEdge.route(state));
    }

    @Test
    void hallucinatedVerdict_oneBeforeLimit_returnsRetry() {
        AgentState state = stateWith("HALLUCINATED", 2, 3);
        assertEquals(RouterEdge.Decision.RETRY, RouterEdge.route(state),
                "retryCount=2 with maxRetries=3 should still be RETRY");
    }

    @Test
    void unknownVerdict_treatedAsHallucinated_belowLimit() {
        // Unrecognised verdict is treated as HALLUCINATED by EvaluationNode,
        // so the router sees a non-grounded state → RETRY
        AgentState state = stateWith("UNKNOWN_FORMAT", 0, 3);
        assertEquals(RouterEdge.Decision.RETRY, RouterEdge.route(state));
    }

    // ── EXHAUSTED path ──────────────────────────────────────────────────────

    @Test
    void hallucinatedVerdict_atLimit_returnsExhausted() {
        AgentState state = stateWith("HALLUCINATED", 3, 3);
        assertEquals(RouterEdge.Decision.EXHAUSTED, RouterEdge.route(state));
    }

    @Test
    void hallucinatedVerdict_beyondLimit_returnsExhausted() {
        // Safety check: should never happen in practice, but router must be safe
        AgentState state = stateWith("HALLUCINATED", 5, 3);
        assertEquals(RouterEdge.Decision.EXHAUSTED, RouterEdge.route(state));
    }

    @Test
    void nullVerdict_atLimit_returnsExhausted() {
        // Null verdict → not grounded, retry limit reached → EXHAUSTED
        AgentState state = stateWith(null, 3, 3);
        assertEquals(RouterEdge.Decision.EXHAUSTED, RouterEdge.route(state));
    }

    @Test
    void maxRetriesOne_firstAttemptFails_returnsExhausted() {
        // maxRetries=1, retryCount=1 → exhausted on second attempt
        AgentState state = stateWith("HALLUCINATED", 1, 1);
        assertEquals(RouterEdge.Decision.EXHAUSTED, RouterEdge.route(state));
    }

    // ── Boundary: maxRetries=0 (fail fast) ─────────────────────────────────

    @Test
    void maxRetriesZero_hallucinatedVerdict_returnsExhausted() {
        // If max-retries is configured to 0, any hallucination exhausts immediately
        AgentState state = stateWith("HALLUCINATED", 0, 0);
        assertEquals(RouterEdge.Decision.EXHAUSTED, RouterEdge.route(state),
                "With maxRetries=0, retryCount=0 is already at the limit");
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /**
     * Builds an AgentState with the given verdict, retryCount, and maxRetries.
     * Uses incrementRetryCount() to set retryCount to avoid exposing a setter.
     */
    private AgentState stateWith(String verdict, int retryCount, int maxRetries) {
        AgentState state = new AgentState("test query", maxRetries);
        state.setVerdict(verdict);
        for (int i = 0; i < retryCount; i++) {
            state.incrementRetryCount();
        }
        return state;
    }
}
