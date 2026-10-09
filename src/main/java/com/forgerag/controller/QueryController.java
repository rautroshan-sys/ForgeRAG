package com.forgerag.controller;

import com.forgerag.agent.AgentGraphConfig;
import com.forgerag.agent.AgentState;
import com.forgerag.dto.EvaluationResult;
import com.forgerag.dto.QueryRequest;
import com.forgerag.dto.QueryResponse;
import com.forgerag.repository.VectorRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/query — the main entry point for the agentic RAG loop.
 *
 * Response shapes per API_CONTRACT.md:
 *  200  → grounded answer (grounded=true, verdict=VALID)
 *  422  → retry limit exhausted (grounded=false, verdict=HALLUCINATED, answer=null)
 *  400  → missing/empty query (handled by GlobalExceptionHandler)
 *  502  → Gemini API failure (handled by GlobalExceptionHandler)
 */
@RestController
@RequestMapping("/api")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);
    private final AgentGraphConfig agentGraph;
    private final VectorRepository vectorRepository;

    public QueryController(AgentGraphConfig agentGraph, VectorRepository vectorRepository) {
        this.agentGraph = agentGraph;
        this.vectorRepository = vectorRepository;
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        log.info("Query received: '{}'", request.getQuery());

        // Guard: if the vector store is empty, fail fast with a clear message
        // rather than running the full loop against empty context
        if (vectorRepository.countAll() == 0) {
            EvaluationResult emptyEval = new EvaluationResult(
                "NO_CONTEXT",
                "The knowledge base is empty. Please ingest documents before querying."
            );
            QueryResponse response = new QueryResponse(null, false, 0, emptyEval);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }

        AgentState finalState = agentGraph.run(request.getQuery());

        EvaluationResult evaluation = new EvaluationResult(
                finalState.getVerdict(),
                finalState.getEvaluationReasoning()
        );

        if (finalState.isGrounded()) {
            QueryResponse response = new QueryResponse(
                    finalState.getDraftAnswer(),
                    true,
                    finalState.getRetryCount(),
                    evaluation
            );
            return ResponseEntity.ok(response);
        } else {
            // Retry limit exhausted — return 422, never silently return unverified answer
            QueryResponse response = new QueryResponse(
                    null,   // answer=null per API contract for ungrounded 422
                    false,
                    finalState.getRetryCount(),
                    evaluation
            );
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
    }
}
