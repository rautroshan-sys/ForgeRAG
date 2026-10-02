package com.forgerag.controller;

import com.forgerag.dto.ChunkDto;
import com.forgerag.repository.VectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * GET /api/chunks — lists recently ingested chunks for the admin/ingest view.
 *
 * Query params: ?limit=N (optional, default 20, max 100)
 * Response shape matches API_CONTRACT.md exactly.
 */
@RestController
@RequestMapping("/api")
public class ChunksController {

    private static final Logger log = LoggerFactory.getLogger(ChunksController.class);
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final VectorRepository vectorRepository;

    public ChunksController(VectorRepository vectorRepository) {
        this.vectorRepository = vectorRepository;
    }

    @GetMapping("/chunks")
    public ResponseEntity<Map<String, Object>> listChunks(
            @RequestParam(defaultValue = "20") int limit) {

        int effectiveLimit = Math.min(Math.max(1, limit), MAX_LIMIT);
        log.debug("GET /api/chunks limit={} (effective={})", limit, effectiveLimit);

        List<ChunkDto> chunks = vectorRepository.findRecent(effectiveLimit);
        return ResponseEntity.ok(Map.of("chunks", chunks));
    }
}
