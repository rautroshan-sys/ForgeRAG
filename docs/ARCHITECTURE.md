# Architecture — ForgeRAG

## Overview

Full-stack system: a Spring Boot backend (the agentic RAG engine) plus a React frontend (landing page, query view, ingest/admin view). The frontend is a thin client — all retrieval/generation/evaluation logic stays server-side; the UI only calls the contract in `API_CONTRACT.md`.

## Components

| Component | Choice | Notes |
|---|---|---|
| Language (backend) | Java 17+ | Confirmed working; toolchain stability has already caused two build failures this project — see Risk Register. |
| Framework (backend) | Spring Boot | REST controllers, dependency injection. |
| Frontend | React + Vite + Tailwind | Three views: landing page, query interface, ingest/admin view. No server-side rendering needed for MVP. |
| AI orchestration | LangChain4j + LangGraph4j | LangChain4j for the Gemini client; LangGraph4j for the cyclic agent state machine. |
| Database | PostgreSQL + pgvector | Single store for chunk content + embeddings. No separate cache/queue layer — out of scope for MVP. |
| Foundation model | Gemini API | Used for both Generation and Evaluation nodes (same model family, different prompts). |
| Auth | None (MVP) | Not a requirement for a mini-project demo; flagged as a non-goal, not an oversight. |
| Real-time | None | Request/response only, no streaming or websockets in MVP. The query view simulates the node sequence visually from the single final response, not from live server push. |
| Storage | pgvector `document_chunks` table | See schema below. No object storage needed — documents are chunked and embedded at ingest time, source text isn't retained separately. |

## Data Flow

```
Client
  │  POST /api/ingest (raw document)
  ▼
ChunkingService → EmbeddingService → VectorRepository → Postgres/pgvector

Client
  │  POST /api/query { "query": "..." }
  ▼
API Controller → initializes AgentState
  ▼
Retrieval Node (pgvector, Inner Product <#>, vector_ip_ops index)
  ▼
Generation Node (Gemini drafts answer from query + retrieved context)
  ▼
Evaluation Node (Gemini as LLM-judge: checks draft against context)
  ▼
Router (conditional edge on AgentState)
  ├─ valid        → Final Output Node → response to client
  └─ hallucinated → Query Rewriter Node → updates AgentState → loops back to Retrieval
                     (capped at forgerag.agent.max-retries)
```

## Database Schema (confirmed, live)

```sql
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    embedding VECTOR(768),      -- placeholder dimension, pending confirmation against Gemini's actual embedding output — see Risk Register
    source_doc TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX document_chunks_embedding_idx
    ON document_chunks USING hnsw (embedding vector_ip_ops);
```

Verified via `\d document_chunks` — index confirmed present and using `vector_ip_ops`.

## Key Engineering Decisions

- **Inner Product over Cosine Distance.** Embeddings are normalized at ingestion time (`EmbeddingService`), so `<#>` is mathematically equivalent to cosine similarity without the per-comparison normalization cost.
- **Structure-aware chunking, not fixed-size splitting.** Preserves header/paragraph hierarchy so retrieved context isn't semantically decapitated mid-sentence.
- **Single `AgentState` object.** Every node reads/writes one shared state object — keeps the graph's data flow explicit and testable.
- **LLM-as-Judge, not a separate classifier.** Keeps the system simpler for this scope while still producing an explicit verdict + reasoning trail.
- **Raw JDBC over `langchain4j-pgvector`'s store abstraction.** The built-in store defaults to cosine distance and doesn't expose `vector_ip_ops` index configuration — raw JDBC via `VectorRepository` was chosen specifically to keep control over the Inner Product indexing strategy.

## High-Risk / Fragile Points (flag, don't ignore)

- **Gemini API dependency.** Free-tier rate limits could throttle the self-correction loop mid-demo if multiple retries fire in quick succession. See Risk Register.
- **LangGraph4j is a smaller, less mature library** than its Python counterpart — sparser docs/examples, and dependency resolution has already required non-default Maven repos. Budget extra time here, don't assume it behaves like mainstream Spring libraries.
- **Toolchain fragility.** Two build failures already traced to JDK/`JAVA_HOME` mismatches, one caused by an unreviewed AI coding-agent action mid-session. Treat the local dev environment as something to verify, not assume, before every work session.