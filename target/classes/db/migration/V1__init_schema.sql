-- ForgeRAG V1 — initial schema
-- Requires pgvector extension (enable before running: CREATE EXTENSION IF NOT EXISTS vector;)

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_chunks (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT        NOT NULL,
    embedding  VECTOR(768),          -- text-embedding-004 outputs 768 dims
    source_doc TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- HNSW index using Inner Product (vector_ip_ops)
-- Embeddings are L2-normalised at ingest time, so <#> == cosine similarity
-- without the per-comparison normalisation cost.
CREATE INDEX IF NOT EXISTS document_chunks_embedding_idx
    ON document_chunks USING hnsw (embedding vector_ip_ops);
