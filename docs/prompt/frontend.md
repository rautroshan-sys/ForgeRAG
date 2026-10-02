ROLE
You are continuing the frontend for ForgeRAG, an academic mini-project. A working scaffold already exists — do not rebuild it from scratch, and do not redesign the visual direction or introduce a different stack. Your job is to extend what's there to cover anything missing, and to keep it in sync with the backend as it's built.

SOURCE OF TRUTH (in priority order if they ever conflict)
1. docs/PRD.md — MVP scope (what gets built, full stop: landing page, query view, ingest/admin view)
2. docs/API_CONTRACT.md — exact data shapes the UI must consume (includes `/api/query`, `/api/ingest`, `/api/chunks`, `/api/health`, and the CORS requirement)
3. The already-built components in `frontend/src/` — visual/UX truth for whatever IS in scope. Match the existing design tokens (Tailwind config: `paper`/`surface`/`ink`/`muted`/`line`/`verify`/`alert`/`brand` colors, `display`/`body`/`mono` font roles) rather than introducing new ones.
4. docs/ARCHITECTURE.md, docs/TASKS.md — stack and task order

NOTE ON STACK: the existing scaffold is plain JavaScript (JSX), not TypeScript. Stay in JS unless explicitly asked to migrate — introducing TypeScript mid-project is a stack change, not a continuation, and needs a deliberate decision, not a default.

SCOPE BOUNDARY
- Work ONLY inside `frontend/`.
- Do NOT modify, create, or delete anything inside the Java backend (`pom.xml`, `src/main/java/`, `src/main/resources/`).
- Do NOT create backend endpoints.
- The existing `src/api/client.js` already makes real `fetch` calls (no mock-data layer was built, since the contract was frozen before the backend existed) — don't introduce a `mock.ts`/`data/mock.js` layer now unless specifically asked; it would duplicate what the real client already does, and the Query/Ingest pages already handle loading/error states against the real contract shape.

STEP 0 — INSPECT BEFORE ACTING
1. List the current contents of `frontend/src/` before writing anything.
2. Read `docs/CURRENT_STATE.md` and the Frontend section of `docs/TASKS.md` for what's already built and verified vs. still outstanding.
3. If extending an existing component or page, read it first — extend/fix in place rather than overwriting blindly.

EXISTING STRUCTURE (already built — extend, don't replace)
```
frontend/
├── index.html
├── package.json / vite.config.js / tailwind.config.js / postcss.config.js
├── .env.example          (VITE_API_BASE_URL)
└── src/
    ├── main.jsx / App.jsx / index.css
    ├── api/client.js      (runQuery, ingestDocument, listChunks)
    ├── components/        (Nav, Footer, SequenceTrace, VerdictBadge)
    └── pages/              (Home, Query, Ingest)
```

GROUND RULES (error-proofing)
1. No giant components — split by responsibility, matching the pattern already established (one component = one visual/data concern).
2. Keep all API communication in `src/api/client.js` — no direct `fetch` calls from components or pages.
3. The frontend's API base URL comes from `VITE_API_BASE_URL` (already wired via `.env.example`) — never hardcode a backend URL into a component.
4. Implement loading, empty, and error states for every data-driven view — this is already done for Query and Ingest; keep the pattern for anything new.
5. Responsive behavior required for any new screen, not just desktop.
6. No destructive commands, no files touched outside `frontend/`.

RUN AND VERIFY (mandatory)
1. Actually run the frontend (`npm run dev`).
2. Fix build/runtime errors — don't report something as working from source review alone.
3. Since the real backend may not be fully built yet, verify against whatever endpoints are actually live; for endpoints not yet implemented, confirm the error state renders sensibly rather than crashing the page.
4. Confirm loading/empty/error states actually render, not just exist in code.

UPDATE DOCS (reflect reality)
- `docs/CURRENT_STATE.md`: what's implemented, what's verified working (and against what — real backend vs. a stubbed/partial one), blockers.
- `docs/TASKS.md`: update status per Frontend task based on what was actually verified.

FINAL REPORT (then stop)
1. Which pages/components are built and verified working, and against what backend state.
2. Any deviations from the existing design tokens/structure and why.
3. Blockers, if any.

Then stop. Do not integrate deeper with the backend beyond what `docs/API_CONTRACT.md` already defines, unless explicitly asked.