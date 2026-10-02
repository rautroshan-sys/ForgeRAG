package com.forgerag.repository;

import com.forgerag.dto.ChunkDto;
import com.pgvector.PGvector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Raw JDBC repository for the document_chunks table.
 *
 * Uses raw JDBC (not langchain4j-pgvector) to retain full control over
 * the Inner Product index strategy (vector_ip_ops). See ARCHITECTURE.md.
 *
 * PGvector.addVectorType(conn) must be called on every connection before
 * using the VECTOR column — done via a DataSource wrapper registered at
 * startup and also called inline here for safety.
 */
@Repository
public class VectorRepository {

    private static final Logger log = LoggerFactory.getLogger(VectorRepository.class);
    private static final int DEFAULT_TOP_K = 5;

    private final JdbcTemplate jdbc;

    public VectorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Stores a chunk + its normalized embedding.
     */
    public void save(String content, float[] embedding, String sourceDoc) {
        jdbc.execute((Connection conn) -> {
            PGvector.addVectorType(conn);
            try (var ps = conn.prepareStatement(
                    "INSERT INTO document_chunks (content, embedding, source_doc) VALUES (?, ?, ?)")) {
                ps.setString(1, content);
                ps.setObject(2, new PGvector(embedding));
                ps.setString(3, sourceDoc);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /**
     * Retrieves the top-k most similar chunks using Inner Product (<#>).
     * Embeddings are normalized, so <#> == cosine similarity.
     * Ordering by <#> ASC returns highest similarity (pgvector negates IP for ordering).
     */
    public List<String> findSimilar(float[] queryEmbedding, int topK) {
        return jdbc.execute((Connection conn) -> {
            PGvector.addVectorType(conn);
            try (var ps = conn.prepareStatement(
                    "SELECT content FROM document_chunks ORDER BY embedding <#> ? LIMIT ?")) {
                ps.setObject(1, new PGvector(queryEmbedding));
                ps.setInt(2, topK);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> results = new java.util.ArrayList<>();
                    while (rs.next()) results.add(rs.getString("content"));
                    log.debug("findSimilar returned {} chunks", results.size());
                    return results;
                }
            }
        });
    }

    /** Convenience overload using default top-k. */
    public List<String> findSimilar(float[] queryEmbedding) {
        return findSimilar(queryEmbedding, DEFAULT_TOP_K);
    }

    /**
     * Returns the most recent chunks for the admin/ingest view (GET /api/chunks).
     */
    public List<ChunkDto> findRecent(int limit) {
        String sql = "SELECT id, content, source_doc, created_at FROM document_chunks " +
                     "ORDER BY created_at DESC LIMIT ?";
        return jdbc.query(sql, this::mapChunkRow, limit);
    }

    /** Returns total count — useful for the ingest response. */
    public int countAll() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM document_chunks", Integer.class);
        return count != null ? count : 0;
    }

    private ChunkDto mapChunkRow(ResultSet rs, int rowNum) throws SQLException {
        return new ChunkDto(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getString("source_doc"),
                rs.getTimestamp("created_at").toInstant().toString()
        );
    }
}
