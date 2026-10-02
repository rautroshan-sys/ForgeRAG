# API Contract — ForgeRAG

Frozen contract for the MVP. No auth in scope (see Architecture — Non-Goals).

---

## `POST /api/ingest`

Uploads and ingests a document: chunks it (structure-aware), embeds + normalizes each chunk, stores in pgvector.

**Request**
```
Content-Type: text/plain
Body: raw document text
```

**Response — 200 OK**
```json
{
  "status": "ingested",
  "chunksStored": 14,
  "sourceDoc": "manual-upload"
}
```

**Response — 400 Bad Request** (empty or unparseable document)
```json
{
  "error": "DOCUMENT_EMPTY",
  "message": "No content to chunk."
}
```

**Response — 502 Bad Gateway** (embedding call to Gemini failed)
```json
{
  "error": "EMBEDDING_FAILED",
  "message": "Could not generate embeddings for one or more chunks."
}
```

---

## `POST /api/query`

**Request**
```json
{
  "query": "What was the company's revenue growth last quarter?"
}
```

**Response — 200 OK** (grounded answer, possibly after retries)
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

**Response — 422 Unprocessable Entity** (retry limit exhausted, still ungrounded)
```json
{
  "answer": null,
  "grounded": false,
  "retryCount": 3,
  "evaluation": {
    "verdict": "HALLUCINATED",
    "reasoning": "Draft claims a figure not present in any retrieved chunk after 3 rewrite attempts."
  }
}
```

**Response — 400 Bad Request** (missing/empty query)
```json
{
  "error": "QUERY_REQUIRED",
  "message": "Request body must include a non-empty 'query' field."
}
```

**Response — 502 Bad Gateway** (Gemini API unreachable/erroring)
```json
{
  "error": "MODEL_UNAVAILABLE",
  "message": "Generation or evaluation call to Gemini failed."
}
```

---

## `GET /api/chunks`

Lists recently ingested chunks, for the admin view. Most recent first.

**Query params:** `?limit=20` (optional, default 20, max 100)

**Response — 200 OK**
```json
{
  "chunks": [
    {
      "id": 42,
      "content": "## Revenue\nRevenue grew 12% quarter-over-quarter...",
      "sourceDoc": "manual-upload",
      "createdAt": "2026-09-20T10:15:00Z"
    }
  ]
}
```

---

## CORS

The frontend runs on a separate origin in development (Vite dev server, typically `http://localhost:5173`). The backend must allow it — add a `CorsConfigurationSource` bean permitting `http://localhost:5173` (dev) and whatever origin it's deployed to later. Don't use `allowedOrigins("*")` once any auth is added in the future; explicit origins only.

---

## `GET /api/health`

Already implemented (Step 1). No request body.

**Response — 200 OK**
```
ok
```