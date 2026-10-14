# ForgeRAG — Project Tracker

**Single source of truth for project progress. Read before every session. Update after every ticket.**

---

## 1. Project Overview

| Field | Value |
|---|---|
| **Project** | ForgeRAG — Stateful, self-correcting Agentic RAG |
| **Phase** | Implementation → Polish |
| **Last Updated** | 2026-10-14 (session 2) |
| **Overall Status** | In Progress — P0/P1/P2 complete; FRG-005 (E2E) and FRG-009 (integration test) remain |

### What is working
- Backend compiles cleanly (mvn compile exits 0)
- Full agent graph: Retrieval → Generation → Evaluation → Router → QueryRewriter (direct Spring Service loop; LangGraph4j DSL evaluated and deliberately skipped — documented in AgentGraphConfig.java)
- All five node classes implemented and wired
- VectorRepository: Inner Product search, save, findSimilar, findRecent, countAll
- EmbeddingService: L2 normalisation on GeminiClient.embed()
- ChunkingService: structure-aware (heading/paragraph) split, MAX_CHARS=1500
- DocumentIngestController: POST /api/ingest (text/plain)
- QueryController: POST /api/query with fast-fail NO_CONTEXT 422 on empty KB
- ChunksController: GET /api/chunks?limit=N
- HealthController: GET /api/health
- GlobalExceptionHandler: all domain exceptions mapped to API contract codes
- CorsConfig: permits CORS_ALLOWED_ORIGIN (default http://localhost:5173)
- GeminiConfig: binds generator/evaluator/embedding model names from properties
- Flyway V1 (768-dim HNSW) + V2 (3072-dim for gemini-embedding-001, sequential scan)
- Frontend build passes (vite build exits 0, 39 modules)
- Frontend routes: / (Home), /query (Query), /ingest (Ingest)
- Frontend API client: runQuery, ingestDocument, listChunks, checkHealth
- SequenceTrace component: stage-driven pipeline visualisation
- VerdictBadge component: VALID/HALLUCINATED badge
- Query page: form, loading state, result card, verdict, retry count, evaluation reasoning
- Ingest page: form, status feedback, chunk list with refresh

### What is incomplete / broken
- CRITICAL: Tailwind design tokens missing — 13+ custom classes used in JSX not defined in tailwind.config.js or index.css (card, btn-primary, btn-secondary, input-base, card-elevated, section-title, section-sub, node-step-*, text-ink, text-muted, text-verify, text-alert, bg-surface, border-line). UI renders completely unstyled for Query/Ingest/SequenceTrace.
- CRITICAL: Ingest.jsx line 157 reads chunk.text but ChunkDto field is "content" — chunks load but show blank text.
- MISSING: Nav.jsx only links to / and a # placeholder. /query and /ingest unreachable from nav.
- MISSING: No Evaluation History page or /history route.
- PARTIAL: Model names in application.properties (gemini-3.8-flash, gemini-embedding-001) differ from README (gemini-2.5-flash, text-embedding-004) — unconfirmed which is correct for LangChain4j 1.0.0-beta3.
- MISSING: Zero test coverage — src/test directory does not exist.
- PARTIAL: Ingest UI says chunking merges "up to 1000 chars" but code uses MAX_CHARS=1500.
- MISSING: animate-fade-in, animate-slide-up, page-enter CSS animations not defined.

---

## 2. Ticket Backlog

| ID | Ticket | Epic | Priority | Status | Depends On | Notes |
|---|---|---|---|---|---|---|
| FRG-001 | Repository audit | EPIC 1 | P0 | Done | — | Complete. Evidence in this tracker. |
| FRG-002 | Add missing Tailwind color tokens and component CSS classes | EPIC 1 | P0 | Done | FRG-001 | Without this Query/Ingest/SequenceTrace are completely unstyled |
| FRG-003 | Fix chunk.text → chunk.content field name in Ingest.jsx | EPIC 5 | P0 | Done | FRG-001 | Single-line bug; chunks load but show blank body |
| FRG-004 | Add /query and /ingest nav links to Nav.jsx | EPIC 3 | P1 | Done | FRG-002 | Pages exist but unreachable from UI |
| FRG-005 | End-to-end verification: ingest → query → grounded answer | EPIC 1 | P1 | Not Started | FRG-007 | Requires live backend + Gemini API key |
| FRG-006 | Add Evaluation History page (session-scoped, browser only) | EPIC 6 | P2 | Done | FRG-002, FRG-004 | No backend persistence; label scope accurately |
| FRG-007 | Confirm model IDs (gemini-3.8-flash, gemini-embedding-001) vs LangChain4j beta3 | EPIC 1 | P1 | Done | FRG-001 | Wrong ID → 502 on every API call |
| FRG-008 | Add unit tests: ChunkingService and RouterEdge | EPIC 7 | P2 | Done | FRG-001 | Pure-logic tests; no API mocking needed |
| FRG-009 | Add integration smoke test for agent loop (mocked GeminiClient) | EPIC 7 | P2 | Not Started | FRG-008 | Needs mock for GeminiClient |
| FRG-010 | Sync README model names with application.properties | EPIC 7 | P3 | Done | FRG-007 | Update after model IDs confirmed |
| FRG-011 | Fix Ingest UI chunking description (1000 → 1500 chars) | EPIC 5 | P3 | Done | — | Minor doc inaccuracy |
| FRG-012 | Add animate-fade-in, animate-slide-up, page-enter CSS animations | EPIC 3 | P2 | Done | FRG-002 | Purely additive after tokens exist |

---

## 3. Dependency Map



**First implementation batch (P0):** FRG-003, FRG-002, FRG-004
**Rationale:** FRG-003 is a one-line fix with zero risk. FRG-002 is the biggest unblocking item — without it the entire Query/Ingest UI is invisible. FRG-004 makes the app navigable.

---

## 4. Already Done — Do Not Re-implement

| Feature | Evidence |
|---|---|
| Backend agent loop (5 nodes) | src/main/java/com/forgerag/agent/ |
| All REST controllers + exception handler | src/main/java/com/forgerag/controller/ + exception/ |
| VectorRepository JDBC + pgvector | VectorRepository.java |
| EmbeddingService (L2 norm) | EmbeddingService.java |
| ChunkingService (structure-aware) | ChunkingService.java |
| Flyway migrations V1 + V2 | src/main/resources/db/migration/ |
| Frontend scaffold + routing | frontend/src/ |
| API client (3 endpoints + health) | frontend/src/api/client.js |
| SequenceTrace, VerdictBadge components | frontend/src/components/ |
| Query page logic | Query.jsx |
| Ingest page logic | Ingest.jsx |

---

## 5. Progress Log

| Date | Ticket | Work Completed | Verification | Next Step |
|---|---|---|---|---|
| 2026-10-14 | FRG-001 | Full repository audit: all source files read, mvn compile run, vite build run, all gaps identified | mvn compile OK, vite build OK, grep audits complete | Implement FRG-003, FRG-002, FRG-004 |
| 2026-10-14 | FRG-003 | Fixed chunk.text → chunk.content in Ingest.jsx:157 | vite build OK | — |
| 2026-10-14 | FRG-002 | Added 7 color tokens to tailwind.config.js; added 13 component classes to index.css | vite build OK (CSS 22.9 kB) | — |
| 2026-10-14 | FRG-012 | Added keyframes + animation tokens to tailwind.config.js | vite build OK | — |
| 2026-10-14 | FRG-004 | Added /query and /ingest NavLinks to Nav.jsx; refactored linkClass helper | vite build OK | — |
| 2026-10-14 | FRG-011 | Fixed Ingest UI chunking description: 1000→1500 chars; accurate model info | vite build OK | — |

---

## 6. Audit Table

| ID | Feature | Status | Gap | Evidence |
|---|---|---|---|---|
| — | Backend compile | DONE | — | mvn compile exits 0 |
| — | Agent graph loop | DONE | — | AgentGraphConfig.java |
| — | POST /api/query | DONE | — | QueryController.java |
| — | POST /api/ingest | DONE | — | DocumentIngestController.java |
| — | GET /api/chunks | DONE | — | ChunksController.java |
| — | GET /api/health | DONE | — | HealthController.java |
| — | CORS config | DONE | — | CorsConfig.java |
| — | Frontend build | DONE | — | vite build exits 0 |
| FRG-002 | Tailwind design tokens | MISSING | 13+ custom classes undefined | 43 JSX uses, 0 definitions in index.css/tailwind.config.js |
| FRG-003 | Chunk content display | BROKEN | chunk.text vs chunk.content | Ingest.jsx:157 |
| FRG-004 | Nav links | MISSING | /query /ingest unreachable | Nav.jsx has only / and # |
| FRG-005 | E2E verification | NOT RUN | Never run against live API | TASKS.md |
| FRG-006 | History page | MISSING | No route, component, or nav | — |
| FRG-007 | Model ID confirmation | PARTIAL | gemini-3.8-flash vs gemini-2.5-flash discrepancy | application.properties vs README |
| FRG-008 | Unit tests | MISSING | src/test does not exist | — |
| FRG-011 | Ingest chunking description | PARTIAL | UI says 1000 chars, code uses 1500 | Ingest.jsx:115, ChunkingService.java:35 |
| FRG-012 | CSS animations | MISSING | animate-fade-in, animate-slide-up not defined | Query.jsx:122,130; App.jsx:13 |

| 2026-10-14 | FRG-007 | Queried live Gemini API; confirmed gemini-3.8-flash + gemini-embedding-001 valid; synced GeminiConfig.java defaults | curl API check OK | — |
| 2026-10-14 | FRG-006 | Created History.jsx (localStorage, 50-entry cap, session-scope label); /history route; Nav link; appendHistory() in Query.jsx | vite build OK (40 modules) | — |
| 2026-10-14 | FRG-008 | Created ChunkingServiceTest (11 tests) + RouterEdgeTest (11 tests) | mvn test: 22/22 PASS | — |
| 2026-10-14 | FRG-010 | Synced README model names to gemini-3.8-flash / gemini-embedding-001 | README diff verified | — |
| 2026-10-14 | — | Committed all changes: git commit 4dabd25 (12 files, 1154 insertions) | git log OK | FRG-005 E2E when DB+API available |
