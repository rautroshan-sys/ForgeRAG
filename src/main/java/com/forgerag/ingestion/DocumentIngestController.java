package com.forgerag.ingestion;

import com.forgerag.exception.DocumentEmptyException;
import com.forgerag.repository.VectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * POST /api/ingest
 * Accepts raw document text (Content-Type: text/plain),
 * chunks it, embeds + normalizes each chunk, and stores in pgvector.
 *
 * Response shape matches API_CONTRACT.md exactly.
 */
@RestController
@RequestMapping("/api")
public class DocumentIngestController {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestController.class);
    private static final String SOURCE_DOC = "manual-upload";

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepository;

    public DocumentIngestController(ChunkingService chunkingService,
                                    EmbeddingService embeddingService,
                                    VectorRepository vectorRepository) {
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
    }

    @PostMapping(value = "/ingest", consumes = "text/plain")
    public ResponseEntity<Map<String, Object>> ingest(@RequestBody String documentText) {
        log.info("Ingest request received, document length={}", documentText == null ? 0 : documentText.length());

        if (documentText == null || documentText.isBlank()) {
            throw new DocumentEmptyException("Empty document body received");
        }

        List<String> chunks = chunkingService.chunk(documentText);
        if (chunks.isEmpty()) {
            throw new DocumentEmptyException("Document produced no chunks after splitting");
        }

        log.info("Chunked document into {} chunks, embedding each...", chunks.size());
        for (String chunk : chunks) {
            float[] embedding = embeddingService.embed(chunk);
            vectorRepository.save(chunk, embedding, SOURCE_DOC);
        }

        log.info("Ingestion complete: {} chunks stored", chunks.size());
        return ResponseEntity.ok(Map.of(
                "status", "ingested",
                "chunksStored", chunks.size(),
                "sourceDoc", SOURCE_DOC
        ));
    }
}
