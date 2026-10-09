-- Drop existing table
DROP TABLE IF EXISTS document_chunks CASCADE;

-- Recreate with 3072 dimensions for gemini-embedding-001 / gemini-embedding-2
CREATE TABLE document_chunks (
    id         BIGSERIAL PRIMARY KEY,
    content    TEXT        NOT NULL,
    embedding  VECTOR(3072),
    source_doc TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Note: We are not creating an HNSW index here.
-- pgvector's HNSW index supports a maximum of 2000 dimensions by default.
-- For 3072 dimensions, we rely on Exact Nearest Neighbor (sequential scan)
-- which is perfectly fast for standard RAG knowledge bases (< 100k chunks).
