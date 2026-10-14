package com.forgerag.agent;

import com.forgerag.agent.node.*;
import com.forgerag.client.GeminiClient;
import com.forgerag.ingestion.EmbeddingService;
import com.forgerag.repository.VectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for the AgentGraphConfig state machine loop.
 *
 * Strategy:
 *  - Mock GeminiClient  (no live Gemini API needed)
 *  - Mock VectorRepository (no DB needed)
 *  - Use real EmbeddingService, RetrievalNode, GenerationNode,
 *    EvaluationNode, QueryRewriterNode, RouterEdge, AgentGraphConfig
 *
 * This exercises the full cyclic state machine — retrieval → generation →
 * evaluation → routing → [retry: query rewrite → retrieval loop] → output —
 * with controlled mock responses so each Decision branch is verifiable.
 */
@ExtendWith(MockitoExtension.class)
class AgentGraphIntegrationTest {

    @Mock private GeminiClient geminiClient;
    @Mock private VectorRepository vectorRepository;

    private EmbeddingService embeddingService;
    private RetrievalNode retrievalNode;
    private GenerationNode generationNode;
    private EvaluationNode evaluationNode;
    private QueryRewriterNode queryRewriterNode;
    private AgentGraphConfig agentGraph;

    // Fake embedding: unit vector [1.0, 0.0]
    private static final float[] FAKE_EMBEDDING = {1.0f, 0.0f};

    @BeforeEach
    void setUp() {
        embeddingService    = new EmbeddingService(geminiClient);
        retrievalNode       = new RetrievalNode(embeddingService, vectorRepository);
        generationNode      = new GenerationNode(geminiClient);
        evaluationNode      = new EvaluationNode(geminiClient);
        queryRewriterNode   = new QueryRewriterNode(geminiClient);

        agentGraph = new AgentGraphConfig(
                retrievalNode, generationNode, evaluationNode, queryRewriterNode);
        // Set maxRetries = 2 for deterministic tests
        ReflectionTestUtils.setField(agentGraph, "maxRetries", 2);
    }

    // ── Happy path: grounded on first attempt ───────────────────────────────

    @Test
    void groundedOnFirstAttempt_returnsValidState() {
        // Embedding
        when(geminiClient.embed(any())).thenReturn(FAKE_EMBEDDING);
        // Retrieval returns one context chunk
        when(vectorRepository.findSimilar(any(float[].class), anyInt()))
                .thenReturn(List.of("Google was founded in 1998 by Larry Page and Sergey Brin."));
        // Generator drafts a correct answer
        when(geminiClient.generate(any()))
                .thenReturn("Google was founded in 1998 by Larry Page and Sergey Brin.");
        // Evaluator returns VALID verdict on first call
        when(geminiClient.evaluate(any()))
                .thenReturn("VERDICT: VALID\nAll claims are supported by the context.");

        AgentState result = agentGraph.run("When was Google founded?");

        assertTrue(result.isGrounded(), "Should be grounded on first attempt");
        assertEquals("VALID", result.getVerdict());
        assertEquals(0, result.getRetryCount(), "No retries expected");
        assertNotNull(result.getDraftAnswer());
        assertNotNull(result.getEvaluationReasoning());

        // Verify Gemini was called once each for generate and evaluate
        verify(geminiClient, times(1)).generate(any());
        verify(geminiClient, times(1)).evaluate(any());
    }

    // ── Retry path: hallucinated first, valid on second ─────────────────────

    @Test
    void hallucinatedFirstAttempt_validOnRetry_returnsGrounded() {
        when(geminiClient.embed(any())).thenReturn(FAKE_EMBEDDING);
        when(vectorRepository.findSimilar(any(float[].class), anyInt()))
                .thenReturn(List.of("Google was founded in 1998."));

        // Generate: first returns a bad draft, second is the rewritten query,
        // third is the answer generated using the rewritten query
        when(geminiClient.generate(any()))
                .thenReturn("Google was founded in 1995.")   // hallucinated draft
                .thenReturn("founding year of Google search") // rewritten query (QueryRewriterNode)
                .thenReturn("Google was founded in 1998.");   // correct answer on retry

        // Evaluate: first HALLUCINATED, second VALID
        when(geminiClient.evaluate(any()))
                .thenReturn("VERDICT: HALLUCINATED\n\"1995\" is not supported by the context.")
                .thenReturn("VERDICT: VALID\nAll claims are supported.");

        AgentState result = agentGraph.run("When was Google founded?");

        assertTrue(result.isGrounded(), "Should be grounded after 1 retry");
        assertEquals("VALID", result.getVerdict());
        assertEquals(1, result.getRetryCount(), "Should have retried exactly once");

        // generate() called 3 times: initial draft + query rewrite + second draft
        // evaluate() called 2 times: initial HALLUCINATED + retry VALID
        verify(geminiClient, times(3)).generate(any());
        verify(geminiClient, times(2)).evaluate(any());
    }

    // ── Exhausted path: all attempts hallucinate ────────────────────────────

    @Test
    void allAttemptsHallucinated_returnsExhaustedState() {
        when(geminiClient.embed(any())).thenReturn(FAKE_EMBEDDING);
        when(vectorRepository.findSimilar(any(float[].class), anyInt()))
                .thenReturn(List.of("Context with no relevant information."));

        // Generator always returns a bad draft
        when(geminiClient.generate(any())).thenReturn("Invented answer with no support.");
        // Evaluator always returns HALLUCINATED
        when(geminiClient.evaluate(any()))
                .thenReturn("VERDICT: HALLUCINATED\nNo claims are supported by the context.");

        AgentState result = agentGraph.run("What is the revenue growth?");

        assertFalse(result.isGrounded(), "Should not be grounded when all retries exhausted");
        assertEquals("HALLUCINATED", result.getVerdict());
        assertEquals(2, result.getRetryCount(), "maxRetries=2, so retryCount should be 2");

        // evaluate() called: initial + 2 retries = 3 total
        verify(geminiClient, times(3)).evaluate(any());
    }

    // ── Empty context: retrieval returns nothing ────────────────────────────

    @Test
    void emptyRetrieval_generatesWithNoContext_evaluatorDecides() {
        when(geminiClient.embed(any())).thenReturn(FAKE_EMBEDDING);
        when(vectorRepository.findSimilar(any(float[].class), anyInt()))
                .thenReturn(List.of()); // empty retrieval

        when(geminiClient.generate(any()))
                .thenReturn("I cannot answer based on the available context.");
        when(geminiClient.evaluate(any()))
                .thenReturn("VERDICT: VALID\nThe response correctly acknowledges the lack of context.");

        AgentState result = agentGraph.run("What is the answer?");

        // Evaluator accepted the "I cannot answer" reply — still VALID
        assertTrue(result.isGrounded());
        assertEquals(0, result.getRetryCount());
        assertTrue(result.getRetrievedContext().isEmpty(),
                "Retrieved context should be empty");
    }

    // ── Query rewriter updates the query ────────────────────────────────────

    @Test
    void queryRewriter_updatesQueryOnRetry() {
        when(geminiClient.embed(any())).thenReturn(FAKE_EMBEDDING);
        when(vectorRepository.findSimilar(any(float[].class), anyInt()))
                .thenReturn(List.of("Some context."));

        // First generate: hallucinated. Second generate (called with rewritten query): valid.
        when(geminiClient.generate(any()))
                .thenReturn("Hallucinated answer.")  // initial draft
                .thenReturn("Rewritten query answer.") // second draft (after rewrite)
                .thenReturn("Rewritten query, never actually a third generation.");

        // Evaluator: HALLUCINATED, then the rewriter call goes through generate(),
        // then second evaluate is VALID
        when(geminiClient.evaluate(any()))
                .thenReturn("VERDICT: HALLUCINATED\nAnswer is not supported.")
                .thenReturn("VERDICT: VALID\nAll claims supported.");

        AgentState result = agentGraph.run("Original question?");

        assertTrue(result.isGrounded());
        assertEquals(1, result.getRetryCount());
        // The query in AgentState should have been rewritten (not the original)
        // QueryRewriterNode calls geminiClient.generate() to produce the rewritten query
        assertNotEquals("Original question?", result.getQuery(),
                "Query should have been rewritten after hallucination");
    }

    // ── Unrecognised evaluator response → treated as HALLUCINATED ───────────

    @Test
    void malformedEvaluatorResponse_treatedAsHallucinated_exhausts() {
        when(geminiClient.embed(any())).thenReturn(FAKE_EMBEDDING);
        when(vectorRepository.findSimilar(any(float[].class), anyInt()))
                .thenReturn(List.of("Some context."));
        when(geminiClient.generate(any())).thenReturn("Some answer.");
        // Evaluator never uses the expected VERDICT: format
        when(geminiClient.evaluate(any()))
                .thenReturn("I think this looks fine but I won't say the magic word.");

        AgentState result = agentGraph.run("What is X?");

        // Malformed responses default to HALLUCINATED → eventually exhausted
        assertFalse(result.isGrounded());
        assertEquals(2, result.getRetryCount(), "Should exhaust all retries");
    }
}
