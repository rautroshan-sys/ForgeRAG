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
| Framework | Spring Boot (REST APIs, dependency injection) |
| AI orchestration | LangChain4j (API plumbing) + LangGraph4j (state machine / cyclic graphs) |
| Database | PostgreSQL + `pgvector` extension |
| Foundation model | Gemini API — used for both the Generator and Evaluator nodes |
| Ingestion | Structure-aware / semantic chunking (preserves headers & paragraph hierarchy) |
| Build tool | Maven |

Key dependency coordinates (see `pom.xml`):

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-google-ai-gemini</artifactId>
</dependency>
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-core</artifactId>
</dependency>
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-langchain4j</artifactId>
</dependency>
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
</dependency>
```

---

## Project Structure

```
forgerag/
├── pom.xml
├── README.md
├── docs/
│   ├── industrial_rag_flowchart.pdf
│   └── industrial_rag_infographic.pdf
├── src/
│   ├── main/
│   │   ├── java/com/forgerag/
│   │   │   ├── ForgeRagApplication.java
│   │   │   ├── controller/
│   │   │   │   └── QueryController.java          # API Controller
│   │   │   ├── agent/
│   │   │   │   ├── AgentState.java                 # shared state object
│   │   │   │   ├── AgentGraphConfig.java            # builds the LangGraph4j StateGraph
│   │   │   │   └── node/
│   │   │   │       ├── RetrievalNode.java
│   │   │   │       ├── GenerationNode.java
│   │   │   │       ├── EvaluationNode.java
│   │   │   │       ├── RouterEdge.java
│   │   │   │       └── QueryRewriterNode.java
│   │   │   ├── ingestion/
│   │   │   │   ├── ChunkingService.java             # structure-aware / semantic chunking
│   │   │   │   ├── EmbeddingService.java
│   │   │   │   └── DocumentIngestController.java
│   │   │   ├── repository/
│   │   │   │   └── VectorRepository.java             # raw JDBC pgvector queries
│   │   │   ├── client/
│   │   │   │   └── GeminiClient.java
│   │   │   ├── dto/
│   │   │   │   ├── QueryRequest.java
│   │   │   │   ├── QueryResponse.java
│   │   │   │   └── EvaluationResult.java
│   │   │   ├── config/
│   │   │   │   ├── DataSourceConfig.java
│   │   │   │   └── GeminiConfig.java
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/
│   │           └── V1__init_schema.sql               # table + vector_ip_ops index DDL
│   └── test/
│       └── java/com/forgerag/
│           ├── agent/node/
│           │   ├── RetrievalNodeTest.java
│           │   ├── EvaluationNodeTest.java
│           │   └── AgentGraphIntegrationTest.java     # tests the full retry loop
│           └── ingestion/ChunkingServiceTest.java
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 15+ with the `pgvector` extension enabled
- A Gemini API key ([get one here](https://ai.google.dev/gemini-api/docs/api-key))

### 1. Clone & configure

```bash
git clone <your-repo-url>
cd forgerag
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

### 2. Enable pgvector and create the schema

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

Apply the Flyway migration in `src/main/resources/db/migration/V1__init_schema.sql`, which creates the vector column and the `vector_ip_ops` Inner Product index.

### 3. Set your API key

```bash
export GEMINI_API_KEY=your_key_here
```

### 4. Build and run

```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

---

## Configuration

Key properties in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/forgerag
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}

gemini.api.key=${GEMINI_API_KEY}
gemini.model.generator=gemini-2.5-flash
gemini.model.evaluator=gemini-2.5-flash

forgerag.agent.max-retries=3
```

---

## API Reference

### `POST /api/query`

**Request:**
```json
{
  "query": "What was the company's revenue growth last quarter?"
}
```

**Response:**
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

If the retry limit is exhausted without producing a grounded answer, the API returns a `422` with the last evaluation verdict instead of silently returning an unverified answer.

### `POST /api/ingest`

Uploads and ingests a document: chunks it using structure-aware/semantic chunking, embeds and normalizes the chunks, and stores them in pgvector.

---

## Key Engineering Decisions

- **Inner Product over Cosine Distance** — embeddings are normalized at ingestion time, so Inner Product (`<#>`) search is mathematically equivalent to cosine similarity but avoids the extra normalization computation per comparison at query time, indexed via `vector_ip_ops`.
- **Structure-aware chunking, not fixed-size splitting** — preserves headers, sections, and paragraph hierarchy so retrieved context isn't semantically decapitated mid-sentence or mid-clause.
- **Single `AgentState` object** — every node reads from and writes to one shared state object, keeping the graph's data flow explicit and easy to reason about (and test) as it grows.
- **LLM-as-Judge for evaluation** — rather than a separate fine-tuned classifier, the Evaluator uses the same model family (Gemini) with a strict grounding-check prompt, keeping the system simpler to maintain for a mini-project scope while still providing an explicit verdict + reasoning trail.

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
