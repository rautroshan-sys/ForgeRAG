ROLE
You are integrating the already-built frontend and backend for ForgeRAG. Both sides exist independently — the backend implements the agent loop and endpoints, the frontend was built against the frozen contract without a live backend to test against. Your job is to make them actually work together and prove it, not assume the contract was followed correctly on both sides just because each was "done" in isolation. `docs/API_CONTRACT.md` is frozen and is the single source of truth for how frontend and backend talk to each other.

SOURCE OF TRUTH & PRECEDENCE
- `docs/API_CONTRACT.md` is frozen for this phase — do not edit it to match whatever the backend or frontend currently does.
- If frontend, backend, and the contract disagree: fix the backend and/or frontend to match the contract. Only propose changing the contract itself if matching it is genuinely impossible without breaking the MVP — and if so, stop and ask before editing it.
- "Redesign" (not allowed) means changing architecture, tech choices, or component structure. "Fix" (allowed, expected) means correcting a mismatch against the frozen contract — request/response shape, status code, error format, CORS config, env var. Don't treat a legitimate contract-conformance fix as an off-limits redesign.

GROUND RULES
1. Do not introduce changes unrelated to making frontend and backend conform to the contract — no unrelated refactors, no new features, no visual redesign.
2. Environment/config: the frontend's API base URL must come from `VITE_API_BASE_URL` (already wired) — not hardcoded into `api/client.js`. Confirm the backend's CORS config (per `API_CONTRACT.md`'s CORS section) actually permits `http://localhost:5173` in dev.
3. No destructive commands; don't touch anything outside `frontend/` and the Java backend source.
4. Keep API communication in `frontend/src/api/client.js` — no direct fetch calls from components.
5. Respect `forgerag.agent.max-retries` — don't let an integration test accidentally hammer the Gemini API with unbounded retries while debugging a mismatch.

STEP 1 — CONTRACT CONFORMANCE CHECK (before wiring anything)
For every endpoint in `API_CONTRACT.md` (`/api/health`, `/api/ingest`, `/api/query`, `/api/chunks`), verify on the actual running backend:
- Request format, response format, HTTP status codes (including the 422 "retry limit exhausted" case on `/api/query`, and the 400/502 error cases on `/api/ingest` and `/api/query`)
- Validation behavior, error response shape
- CORS configuration (frontend origin must be explicitly allowed, not `*`)
- Required env vars present in both the backend's config and `frontend/.env.example`
Fix any mismatch found here first — this prevents debugging integration failures that are actually just contract drift between what each side assumed.

STEP 2 — WIRE THE FRONTEND
- Point `frontend/.env` at the real running backend (`VITE_API_BASE_URL`).
- Confirm `src/api/client.js`'s functions (`runQuery`, `ingestDocument`, `listChunks`) actually match the real response shapes — the frontend was built against the documented contract, not a live server, so this is the first real test of that assumption.
- If a real response doesn't match what the frontend expects, fix whichever side is wrong relative to the frozen contract (see Precedence rule above) — don't patch around a mismatch with ad hoc field-checking in the component.

STEP 3 — END-TO-END VERIFICATION (mandatory)
1. Run both backend (`mvn spring-boot:run`) and frontend (`npm run dev`) simultaneously.
2. Walk the real user flow in the browser: ingest a document via the Ingest view, confirm it appears in the recent-chunks list, ask a question via the Query view that should retrieve from it, confirm the answer, verdict, and retry count render correctly.
3. Deliberately test a case likely to trigger at least one hallucination-and-retry cycle, and confirm the `SequenceTrace` component and retry messaging reflect it accurately — this is the project's core "wow moment," so it needs to actually work live, not just in isolated unit tests.
4. Test the failure paths too: an empty query, an empty document, and (if feasible) a simulated Gemini API failure — confirm the frontend shows a sensible error state rather than a blank page or an unhandled exception.

STEP 4 — UPDATE DOCS (reflect reality)
- `docs/CURRENT_STATE.md`: confirmed end-to-end working flows (with what was tested), any contract mismatches found and fixed, remaining gaps.
- `docs/TASKS.md`: mark Integration section tasks done based on what was actually verified end-to-end, not per-side completion.

FINAL REPORT (then stop)
1. Which full user flows are confirmed working end-to-end (ingest → query → grounded answer; ingest → query that triggers retry → eventual answer or 422).
2. Any contract mismatches found between frontend and backend, and how each was resolved.
3. Any remaining gaps or known-fragile points (e.g. Gemini rate limits, chunking quality on the demo document).
4. Blockers, if any.

Then stop.