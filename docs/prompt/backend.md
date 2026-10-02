ROLE
You are implementing the backend for ForgeRAG, an academic mini-project (not a timeboxed hackathon — work for correctness and completeness, not speed). Work strictly from the planning docs already created — they are the single source of truth for scope, stack, and contract. Do not redesign or relitigate tech choices already made in ARCHITECTURE.md (Java 17, Spring Boot, LangChain4j, LangGraph4j, PostgreSQL + pgvector, Gemini API, Maven).

SOURCE OF TRUTH
- docs/PRD.md — scope, MVP boundaries
- docs/ARCHITECTURE.md — stack, components, data flow, key engineering decisions
- docs/API_CONTRACT.md — exact endpoint signatures (frozen — do not deviate)
- docs/TASKS.md — task list and priority order, with real status already marked
- docs/CURRENT_STATE.md — what's actually built and verified so far; read this before touching anything

SCOPE BOUNDARY
- Work ONLY inside the Java backend (`pom.xml`, `src/main/java/com/forgerag/`, `src/main/resources/`, `src/test/`) at the repo root.
- Do NOT modify, create, or delete anything inside `frontend/` — it already exists and is out of scope for this phase.
- Do NOT begin any frontend work under any circumstance, even if backend finishes early — stop and report instead.

STEP 0 — INSPECT BEFORE ACTING
1. List the current contents of `src/main/java/com/forgerag/` and `pom.xml` before writing anything.
2. Read `docs/CURRENT_STATE.md` — the skeleton and database layer are already built and verified (Steps 1–2 of the original build order). Do not re-scaffold or re-create what already exists; extend it.
3. Check whether the toolchain is actually stable right now (`java -version`, `mvn -version`, `echo $JAVA_HOME`) before writing code — this project has a documented history of JDK/Maven mismatches breaking builds silently. If the toolchain looks wrong, stop and report before writing anything.
4. If `ARCHITECTURE.md` and `API_CONTRACT.md` conflict on any point, stop and flag it — don't silently pick one.

GROUND RULES (error-proofing for autonomous execution)
1. Follow `API_CONTRACT.md` exactly — same routes, methods, request/response shapes, status codes, error formats. If the contract is ambiguous or incomplete for a case, make the smallest reasonable extension and log it in `CURRENT_STATE.md` as a documented deviation — never guess silently.
2. Never commit secrets — read all credentials (`GEMINI_API_KEY`, DB credentials) from environment variables; every var used must have a matching placeholder in `.env.example` (add any that are missing).
3. Fail loudly, not silently: if a required env var is missing at startup, the app should error clearly at boot, not crash mysteriously mid-request or fall back to a bad default.
4. Database connection: handle failure with a clear error message and a single retry, not an infinite reconnect loop.
5. No destructive commands (no `rm -rf`, no dropping/overwriting the existing `document_chunks` table or its data).
6. Dependencies: add whatever is genuinely needed to implement the documented stack (e.g. `spring-boot-starter-validation`, a logging config) even if `ARCHITECTURE.md` didn't name it explicitly — but don't add speculative libraries unrelated to a documented requirement.
7. Distinguish infrastructure failures from code bugs. For infrastructure/tooling failures (network unreachable, Maven Central unreachable, DNS broken) — don't loop-retry blindly more than once; report and move to the next task. For the backend failing to start, an endpoint misbehaving, or a test failing — keep debugging it until it passes or you hit a genuine external blocker, and only then report it as blocked.
8. Enforce `forgerag.agent.max-retries` in the agentic loop from the first version — an unbounded hallucination-retry loop is a correctness bug, not a later polish item.

IMPLEMENTATION ORDER
Follow `docs/TASKS.md`'s actual section order (Data Layer → Ingestion → Agent Core → Integration), not a generic phase list. Within each section, build and test each class in isolation before wiring it to the next — this project's established practice is: prove each node as a plain service call first, prove the LangGraph4j graph with a trivial always-valid router second, then add real judgment/retry logic last. Don't skip straight to the full agent loop.

STEP 4 — RUN AND VERIFY (mandatory, not optional)
1. Actually start the backend (`mvn spring-boot:run`).
2. Hit every endpoint in `API_CONTRACT.md` (curl or equivalent) with at least one valid and one invalid input each, including `/api/ingest`, `/api/query` (both a grounded case and a case designed to trigger at least one retry), `/api/chunks`, and `/api/health`.
3. Fix what's broken. Re-test after each fix.
4. Do not report an endpoint as working unless you actually executed a request against it and saw the expected response.

STEP 5 — UPDATE DOCS (reflect reality, not intent)
- `docs/CURRENT_STATE.md`: what's implemented, what's working (with evidence), what's incomplete, current blockers, key technical decisions/deviations made during implementation.
- `docs/TASKS.md`: update each task's status marker (✅/🔄/⬜) based on what was actually verified, not what was attempted.

FINAL REPORT (then stop)
In chat, give:
1. Exactly which endpoints are confirmed working (with what you tested).
2. Which are broken or incomplete, and why.
3. Any deviations you made from `API_CONTRACT.md` and why.
4. Any blockers preventing further backend work.

Then stop. Do not begin frontend work.