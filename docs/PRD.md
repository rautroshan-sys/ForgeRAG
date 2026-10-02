# PRD — ForgeRAG (Industrial RAG Architecture)

## Problem

Naive RAG is a single shot: embed the query, fetch top-k chunks, hand them to an LLM, trust the output. That's acceptable for a chatbot demo. It breaks down in domains where a confident-but-wrong answer costs more than a slow one — legal, medical, compliance, and finance workflows, and anywhere multiple fragmented sources need to be reconciled before an answer is trustworthy.

## Users

- Primary (for this mini-project): the evaluator/panel assessing technical depth and engineering judgment, not an end consumer.
- Represented persona: an analyst or case-reviewer in a regulated domain who needs a grounded, auditable answer, not just a fast one.

## Pain Points

- Unsupported claims (hallucinations) in generated answers, with no mechanism to catch them before the user sees them.
- No audit trail — a wrong answer can't be traced back to *why* it was trusted.
- Single-shot retrieval can't reconcile facts that live across multiple documents or sources.
- Standard cosine-similarity vector search is a floating-point bottleneck at scale.

## Goals

- Demonstrate a self-correcting agentic RAG loop: draft → verify → rewrite-and-retry → grounded output.
- Demonstrate sound retrieval engineering: normalized embeddings, Inner Product search, structure-aware chunking.
- Produce a system whose control flow is explicit and inspectable (a real state machine), not a black-box chain.

## Non-Goals (Out of Scope for this mini-project)

- No multi-tenant auth/user accounts.
- No fine-tuned or custom-trained models — Gemini API only, used via prompting.
- No production-grade horizontal scaling, caching layer, or distributed deployment.
- No multi-source retrieval (SQL + documents) in the MVP — single pgvector document store only; listed as roadmap, not MVP.

## Functional Requirements

1. Ingest a document: chunk it (structure-aware), embed each chunk (normalized), store in pgvector.
2. Accept a query via `POST /api/query`.
3. Retrieve relevant chunks via Inner Product search.
4. Generate a draft answer grounded in retrieved context (Gemini).
5. Evaluate the draft against retrieved context for unsupported claims (Gemini as LLM-judge).
6. If hallucinated: rewrite the query, update agent state, retry retrieval — up to a capped retry limit.
7. If valid, or retry limit exhausted: return a final response with a grounded/ungrounded verdict and reasoning.

## Non-Functional Requirements

- Faithfulness over latency — a slower, correct answer is preferred over a fast, unverified one.
- Explainability — every response carries its evaluation verdict and reasoning, not just the answer text.
- Fail-safe behavior — if the retry limit is exhausted without a grounded answer, the system must say so explicitly (HTTP 422), never silently return an unverified draft as if it were trusted.

## Core User Flow

```
Ingest a document (via UI or API) → ask a question in the UI
→ system retrieves + drafts + self-checks → (if needed) rewrites and retries
→ UI shows the grounded answer plus the system's own verdict and reasoning
```

## Frontend Requirements

- **Landing page** — explains the project (what it does, why the self-correction loop matters), links to Query and Ingest views.
- **Query view** — input a question, see the final answer, a grounded/hallucinated badge, retry count, and the evaluation reasoning. Should visually reflect the agent's sequence (retrieval → generation → evaluation → router → output) rather than just a spinner.
- **Ingest/admin view** — paste or upload document text, see the ingestion result (chunks stored), and browse recently stored chunks.
- Stack: React + Vite + Tailwind. Talks to the backend only via the endpoints in `API_CONTRACT.md` — no direct DB access from the frontend.

## MVP Scope (ruthless cut)

**In:** single-document-store ingestion, the 5-node agent loop (Retrieval, Generation, Evaluation, Router, Query Rewriter), one retry cap, `/api/query` and `/api/ingest` endpoints, and a React frontend covering the three views above.

**Out:** everything in Non-Goals above. If time runs short, the loop with a **hardcoded single retry** (no configurable backoff) is the minimum viable demonstration of the core idea — cut configurability before cutting the loop itself.

## Why This Is a Strong Project

The differentiator isn't "a RAG app" — plenty of student projects are that. It's that the control flow itself is the engineering contribution: a cyclic, stateful agent (not a linear chain) that catches its own errors before they reach the user. That demonstrates state machine design, retrieval-system tuning (Inner Product vs. cosine, structure-aware chunking), and prompt-engineering discipline (LLM-as-judge) in one coherent system — each piece independently explainable to an evaluator, not just "it works, trust me."