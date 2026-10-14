# ForgeRAG — Industrial RAG Architecture

**A stateful, self-correcting Agentic RAG backend that verifies its own answers before returning them.**

ForgeRAG is not a naive "retrieve-then-generate" script. It's a cyclic agentic state machine — the system drafts an answer, mathematically checks that draft against the retrieved evidence, and automatically rewrites the query and retries if it detects a hallucination. It's built to mimic how an enterprise compliance engine behaves: verify before you speak.

---

## Table of Contents

- [Why This Exists](#why-this-exists)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Key Engineering Decisions](#key-engineering-decisions)
- [Roadmap](#roadmap)
- [License](#license)

---

## Why This Exists

Naive RAG is a single shot: embed the query, fetch the top-k chunks, hand them to an LLM, hope for the best. That's fine for a chatbot demo. It falls apart in domains where being wrong has consequences.

| Problem | What breaks with naive RAG | What ForgeRAG does instead |
|---|---|---|
| **Compliance, legal & financial risk** | A confident but ungrounded answer is worse than a slow, correct one. No way to audit *why* an answer was trusted. | An **Evaluation Node** checks every draft against retrieved context before it's returned. Failed checks produce a visible reject-and-retry trail — an audit log, not a black box. |
| **Multi-hop reasoning across fragmented systems** | Single-shot retrieval can't connect facts that live across separate documents, tables, or sources. | ForgeRAG is a **cyclic state machine** (LangGraph4j), not a linear pipeline — it can re-query and revise before finalizing an answer. |
| **Retrieval accuracy & speed at scale** | Cosine-similarity search is a floating-point bottleneck under load. | Embeddings are **normalized** and indexed with **Inner Product (`<#>`) / `vector_ip_ops`** in pgvector — faster comparisons, no precision loss. |
| **Trust requires explainability** | "The model said so" isn't an answer regulators or auditors accept. | The self-correction loop *is* the confidence signal: every output carries a grounded/rejected verdict and the reasoning behind it. |

**One-line pitch:** *Most RAG systems trust their first answer. ForgeRAG doesn't — it verifies itself against the evidence before responding, the way a compliance review would.*

---

## How It Works

```
API Controller
      │  initializes AgentState
      ▼
Retrieval Node ◄────────────────────┐
      │  pgvector Inner Product        │
      │  search over semantic chunks   │
      ▼                                │
Generation Node                        │
      │  Gemini drafts an answer       │
      │  from query + context          │
      ▼                                │
Evaluation Node (LLM-as-Judge)         │
      │  Gemini checks the draft       │
      │  against retrieved context     │
      ▼                                │
   Router ──── hallucinated ──► Query Rewriter Node
      │                          (updates AgentState,
      │ valid                     loops back up)
      ▼
Final Output
  (grounded answer + confidence signal)
```

1. **API Controller** (Spring Boot) receives the query and initializes a shared `AgentState` object.
2. **Retrieval Node** queries PostgreSQL/pgvector using Inner Product search over normalized, semantically-chunked embeddings.
3. **Generation Node** passes the query + retrieved context to Gemini to draft an answer.
4. **Evaluation Node** prompts Gemini (as an LLM-judge) to explicitly check the draft for claims not supported by the retrieved context.
5. **Router** (a conditional edge) reads the evaluation verdict from `AgentState`:
   - **Valid** → routes to the Final Output Node.
   - **Hallucinated** → routes to the **Query Rewriter Node**, which updates the search parameters in `AgentState` and loops back to Retrieval.

This loop can run multiple times per query until the draft is grounded or a retry limit is hit — at which point the system fails safely rather than returning an unverified answer.

---

## Architecture

Full system diagrams (state machine flowchart + tech-stack/pipeline infographic) are in [`/docs`](./docs) — generated as `industrial_rag_flowchart.pdf` and `industrial_rag_infographic.pdf`.

```
Client → API Controller → Retrieval Node → Generation Node → Evaluation Node → Router
                               ▲                                                 │
                               └──────────────── Query Rewriter Node ◄───────────┘
                                                 (on hallucination)
```

---

## Tech Stack

| Layer | Choice |
|---|---|
| Core language | Java 17+ |
| Framework | Spring Boot 3.4 (REST APIs, dependency injection, validation) |
| AI orchestration | LangChain4j `1.0.0-beta3` (API plumbing) + LangGraph4j `1.9.2` (state machine / cyclic graphs) |
| Database | PostgreSQL 15+ + `pgvector` extension |
| Foundation model | Gemini API — `gemini-3.8-flash` for Generator & Evaluator nodes; `gemini-embedding-001` for embeddings (3072 dims) |
| Ingestion | Structure-aware / semantic chunking (preserves headers & paragraph hierarchy) |
| Build tool | Maven |
| Frontend | React 18 + Vite + Tailwind CSS (served separately on port 5173) |

Key dependency coordinates (see `pom.xml`):

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-google-ai-gemini</artifactId>
</dependency>
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-core</artifactId>
</dependency>
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

---

## Project Structure

```
forgerag/
├── pom.xml
├── README.md
├── .env.example                              # environment variable template
├── docs/
│   ├── industrial_rag_flowchart.pdf
│   └── industrial_rag_infographic.pdf
├── frontend/                                 # React + Vite + Tailwind CSS UI
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   ├── index.html
│   └── src/
│       ├── App.jsx                           # routing (Home / Query / Ingest)
│       ├── main.jsx
│       ├── index.css
│       ├── api/                              # fetch wrappers for backend API
│       ├── components/
│       │   ├── Nav.jsx
│       │   ├── Footer.jsx
│       │   ├── SequenceTrace.jsx             # renders the retry loop visually
│       │   └── VerdictBadge.jsx              # VALID / HALLUCINATED badge
│       └── pages/
│           ├── Home.jsx
│           ├── Query.jsx                     # query form + response display
│           └── Ingest.jsx                    # document ingestion form
└── src/
    ├── main/
    │   ├── java/com/forgerag/
    │   │   ├── ForgeRagApplication.java
    │   │   ├── controller/
    │   │   │   ├── QueryController.java      # POST /api/query
    │   │   │   ├── ChunksController.java     # GET  /api/chunks
    │   │   │   └── HealthController.java     # GET  /api/health
    │   │   ├── agent/
    │   │   │   ├── AgentState.java           # shared state object
    │   │   │   ├── AgentGraphConfig.java     # builds the LangGraph4j StateGraph
    │   │   │   └── node/
    │   │   │       ├── RetrievalNode.java
    │   │   │       ├── GenerationNode.java
    │   │   │       ├── EvaluationNode.java
    │   │   │       ├── RouterEdge.java
    │   │   │       └── QueryRewriterNode.java
    │   │   ├── ingestion/
    │   │   │   ├── ChunkingService.java      # structure-aware / semantic chunking
    │   │   │   ├── EmbeddingService.java     # embeds + L2-normalizes chunks
    │   │   │   └── DocumentIngestController.java  # POST /api/ingest
    │   │   ├── repository/
    │   │   │   └── VectorRepository.java    # raw JDBC pgvector queries
    │   │   ├── client/
    │   │   │   └── GeminiClient.java        # generate / evaluate / embed
    │   │   ├── dto/
    │   │   │   ├── QueryRequest.java
    │   │   │   ├── QueryResponse.java
    │   │   │   ├── EvaluationResult.java
    │   │   │   └── ChunkDto.java
    │   │   ├── config/
    │   │   │   ├── DataSourceConfig.java
    │   │   │   └── GeminiConfig.java
    │   │   └── exception/
    │   │       ├── GlobalExceptionHandler.java
    │   │       ├── DocumentEmptyException.java
    │   │       ├── EmbeddingFailedException.java
    │   │       └── ModelUnavailableException.java
    │   └── resources/
    │       ├── application.properties
    │       └── db/migration/
    │           └── V1__init_schema.sql       # table + vector_ip_ops index DDL
    └── test/
        └── java/com/forgerag/
            ├── agent/node/
            │   ├── RetrievalNodeTest.java
            │   ├── EvaluationNodeTest.java
            │   └── AgentGraphIntegrationTest.java  # tests the full retry loop
            └── ingestion/ChunkingServiceTest.java
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 15+ with the `pgvector` extension enabled
- Node.js 18+ and npm (for the frontend)
- A Gemini API key ([get one here](https://ai.google.dev/gemini-api/docs/api-key))

### 1. Clone & configure

```bash
git clone <your-repo-url>
cd forgerag
cp .env.example .env
# Edit .env and fill in GEMINI_API_KEY, DB_USER, DB_PASSWORD
```

### 2. Enable pgvector and create the schema

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Apply the Flyway migration in `src/main/resources/db/migration/V1__init_schema.sql`, which creates the vector column and the `vector_ip_ops` Inner Product index. Flyway runs this automatically on startup.

### 3. Set your environment variables

Edit the `.env` file (or export variables directly):

```bash
export GEMINI_API_KEY=your_key_here
export DB_URL=jdbc:postgresql://localhost:5432/forgerag
export DB_USER=forgerag
export DB_PASSWORD=forgerag
export CORS_ALLOWED_ORIGIN=http://localhost:5173
```

### 4. Build and run the backend

```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The UI will be available at `http://localhost:5173`.

---

## Configuration

Key properties in `application.properties` (values resolved from environment variables):

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

gemini.api.key=${GEMINI_API_KEY}
gemini.model.generator=gemini-3.8-flash
gemini.model.evaluator=gemini-3.8-flash
gemini.model.embedding=gemini-embedding-001

forgerag.agent.max-retries=3

cors.allowed-origin=${CORS_ALLOWED_ORIGIN:http://localhost:5173}
```

See [`.env.example`](./.env.example) for the full list of required environment variables.

---

## API Reference

### `POST /api/query`

**Request:**
```json
{
  "query": "What was the company's revenue growth last quarter?"
}
```

**Response — 200 OK (grounded answer):**
```json
{
  "answer": "Revenue grew 12% quarter-over-quarter, driven primarily by...",
  "grounded": true,
  "retryCount": 1,
  "evaluation": {
    "verdict": "VALID",
    "reasoning": "All claims in the draft are supported by retrieved chunks [doc_3, doc_7]."
  }
}
```

**Response — 422 Unprocessable Entity (retry limit exhausted or empty knowledge base):**
```json
{
  "answer": null,
  "grounded": false,
  "retryCount": 3,
  "evaluation": {
    "verdict": "HALLUCINATED",
    "reasoning": "Claim about Q3 margins is not supported by any retrieved chunk."
  }
}
```

If the retry limit is exhausted without producing a grounded answer, the API returns `422` with the last evaluation verdict — it never silently returns an unverified answer. If the knowledge base is empty, a `422` with verdict `NO_CONTEXT` is returned immediately.

**Error responses:**

| Status | `error` key | Trigger |
|---|---|---|
| `400` | `QUERY_REQUIRED` | Missing or blank `query` field |
| `422` | `NO_CONTEXT` | Knowledge base is empty |
| `502` | `MODEL_UNAVAILABLE` | Gemini API unreachable or errored |
| `502` | `EMBEDDING_FAILED` | Embedding call failed during ingestion |
| `500` | `INTERNAL_ERROR` | Unexpected server error |

---

### `POST /api/ingest`

Accepts raw document text (`Content-Type: text/plain`), chunks it using structure-aware/semantic chunking, embeds and L2-normalizes each chunk, and stores it in pgvector.

**Request:** plain text body (the document content)

**Response — 200 OK:**
```json
{
  "status": "ingested",
  "chunksCreated": 14,
  "chunksStored": 14,
  "chunksFailed": 0,
  "sourceDoc": "manual-upload"
}
```

**Error responses:** `400 DOCUMENT_EMPTY` if body is blank; `502 EMBEDDING_FAILED` if all chunks fail to embed.

---

### `GET /api/chunks`

Lists recently ingested chunks. Used by the frontend Ingest view.

**Query params:** `?limit=N` (optional, default `20`, max `100`)

**Response — 200 OK:**
```json
{
  "chunks": [
    {
      "id": 1,
      "content": "...",
      "sourceDoc": "manual-upload"
    }
  ]
}
```

---

### `GET /api/health`

Simple liveness check.

**Response — 200 OK:** `OK`

---

## Key Engineering Decisions

- **Inner Product over Cosine Distance** — embeddings are normalized at ingestion time, so Inner Product (`<#>`) search is mathematically equivalent to cosine similarity but avoids the extra normalization computation per comparison at query time, indexed via `vector_ip_ops`.
- **Structure-aware chunking, not fixed-size splitting** — preserves headers, sections, and paragraph hierarchy so retrieved context isn't semantically decapitated mid-sentence or mid-clause.
- **Single `AgentState` object** — every node reads from and writes to one shared state object, keeping the graph's data flow explicit and easy to reason about (and test) as it grows.
- **LLM-as-Judge for evaluation** — rather than a separate fine-tuned classifier, the Evaluator uses the same model family (Gemini) with a strict grounding-check prompt, keeping the system simpler to maintain for a mini-project scope while still providing an explicit verdict + reasoning trail.
- **Separate generator and evaluator model instances** — the generator runs at `temperature=0.2` for fluent output; the evaluator runs at `temperature=0.0` for deterministic grounding verdicts.
- **Fast-fail on empty knowledge base** — `QueryController` checks `VectorRepository.countAll()` before running the agent loop and returns a `422 NO_CONTEXT` immediately, rather than running the full LangGraph4j pipeline against empty context.

---

## Roadmap

- [ ] Configurable retry limit with exponential backoff between rewrite attempts
- [ ] Structured confidence scores (not just valid/hallucinated) surfaced in the API response
- [ ] Multi-source retrieval (SQL + document store) for true multi-hop reconciliation
- [ ] Tool-calling nodes (calculator, SQL engine) for the dynamic orchestration use case
- [ ] Admin dashboard for reviewing rejected drafts and rewrite chains

---

## License

Academic mini-project — license terms to be added per institution requirements.
