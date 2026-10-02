package com.forgerag.controller;

import com.forgerag.agent.AgentGraphConfig;
import com.forgerag.agent.AgentState;
import com.forgerag.dto.EvaluationResult;
import com.forgerag.dto.QueryRequest;
import com.forgerag.dto.QueryResponse;
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

    public QueryController(AgentGraphConfig agentGraph) {
        this.agentGraph = agentGraph;
    }

    @PostMapping("/query")
    public ResponseEntity<QueryResponse> query(@Valid @RequestBody QueryRequest request) {
        log.info("Query received: '{}'", request.getQuery());

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
