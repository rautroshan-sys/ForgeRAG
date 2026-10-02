# Tasks — ForgeRAG

Status legend: ✅ Done · 🔄 In Progress · ⬜ Not Started
Priority: **MVP** (demo-critical) · *Stretch* (nice-to-have, cut first if time is short)

## Foundation

- ✅ **MVP** — Bootable Spring Boot skeleton (`pom.xml`, `ForgeRagApplication`, `/api/health`), confirmed via `mvn spring-boot:run` + browser check.
- ✅ **MVP** — JDK 17 / Maven toolchain confirmed and committed as a known-good checkpoint.
- ⬜ **MVP** — Re-verify toolchain (`java -version`, `mvn -version`, `JAVA_HOME`) after the stopped autonomous-agent JDK-upgrade session — **open blocker, do this before Step 3 continues.**

## Data Layer

- ✅ **MVP** — PostgreSQL installed, `pgvector` extension enabled.
- ✅ **MVP** — `document_chunks` table created with `vector(768)` column.
- ✅ **MVP** — Inner Product HNSW index (`vector_ip_ops`) created and verified via `\d document_chunks`.
- ⬜ **MVP** — Confirm actual Gemini embedding output dimension matches `768`; alter column if not.
- ⬜ **MVP** — Save real schema as `V1__init_schema.sql` (currently only exists as ad hoc `psql` commands).

## Ingestion

- 🔄 **MVP** — Add remaining `pom.xml` dependencies (`postgresql`, `pgvector`, `spring-boot-starter-jdbc`, `langchain4j-google-ai-gemini`); confirm clean build.
- ⬜ **MVP** — `EmbeddingService` — calls Gemini, normalizes output. Test standalone before wiring further.
- ⬜ **MVP** — `ChunkingService` — structure-aware (header-boundary) split. Test standalone on a real doc, inspect chunk boundaries by eye.
- ⬜ **MVP** — `VectorRepository` — raw JDBC insert/query using `PGvector`; confirm `PGvector.addVectorType(...)` is registered correctly.
- ⬜ **MVP** — `DocumentIngestController` (`POST /api/ingest`) — wire the three services above only once each is individually verified.

## Agent Core (LangGraph4j)

- ⬜ **MVP** — `AgentState` class — fields for query, retrieved context, draft, evaluation verdict, retry count.
- ⬜ **MVP** — `RetrievalNode` — Inner Product query against `document_chunks`.
- ⬜ **MVP** — `GenerationNode` — Gemini draft generation from query + context.
- ⬜ *Stretch (build trivial first)* — `AgentGraphConfig` wiring with a router that always routes to Final Output — prove the graph mechanics before adding real judgment.
- ⬜ **MVP** — `EvaluationNode` — real LLM-as-judge grounding-check prompt.
- ⬜ **MVP** — `RouterEdge` — real conditional branch on evaluation verdict.
- ⬜ **MVP** — `QueryRewriterNode` — updates `AgentState`, loops back to Retrieval, respects `forgerag.agent.max-retries`.

## Integration

- ⬜ **MVP** — `QueryController` (`POST /api/query`) matching the frozen API contract, including the 422 "retry limit exhausted" path.
- ⬜ **MVP** — `GET /api/chunks` endpoint for the admin view.
- ⬜ **MVP** — CORS config permitting the Vite dev origin (`http://localhost:5173`).
- ⬜ **MVP** — End-to-end manual test: ingest a real doc, ask a question with a known-grounded answer, confirm `grounded: true` on first pass.
- ⬜ *Stretch* — End-to-end test deliberately designed to trigger at least one hallucination-and-retry cycle, to prove the loop actually works, not just the happy path.

## Frontend

- ✅ **MVP** — React + Vite + Tailwind scaffold: landing page, query view, ingest/admin view, API client.
- ⬜ **MVP** — Wire Query view to real `/api/query` once the backend endpoint exists (currently built against the contract, untested against a live server).
- ⬜ **MVP** — Wire Ingest view to real `/api/ingest` and `/api/chunks`.
- ⬜ *Stretch* — Polish the sequence-trace animation on the query view once the real response shape is confirmed end-to-end.

## Testing

- ⬜ *Stretch* — `RetrievalNodeTest`, `EvaluationNodeTest` (unit-level, mocked Gemini calls).
- ⬜ *Stretch* — `AgentGraphIntegrationTest` exercising the full retry loop.
- ⬜ *Stretch* — `ChunkingServiceTest` on a few representative document shapes.

## Demo Prep

- ⬜ **MVP** — Pick 1–2 documents + queries for the live demo where the "wow moment" (a caught-and-corrected hallucination) is reliably reproducible, not left to chance.
- ⬜ *Stretch* — Minimal CLI script or Postman collection so the demo doesn't depend on typing `curl` commands live.