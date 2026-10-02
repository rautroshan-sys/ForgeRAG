/**
 * API client for ForgeRAG.
 *
 * All communication with the backend goes through this file.
 * No direct fetch() calls from components or pages — only through these functions.
 *
 * Base URL comes from VITE_API_BASE_URL env var.
 * In dev, Vite's proxy forwards /api/* to the backend, so we use relative paths.
 */

const BASE = import.meta.env.VITE_API_BASE_URL || ''

async function apiFetch(path, options = {}) {
  const url = `${BASE}${path}`
  const res = await fetch(url, options)
  return res
}

/**
 * POST /api/query
 * @param {string} query
 * @returns {Promise<{ok: boolean, status: number, data: object}>}
 */
export async function runQuery(query) {
  const res = await apiFetch('/api/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  })
  const data = await res.json()
  return { ok: res.ok, status: res.status, data }
}

/**
 * POST /api/ingest
 * @param {string} documentText raw document text
 * @returns {Promise<{ok: boolean, status: number, data: object}>}
 */
export async function ingestDocument(documentText) {
  const res = await apiFetch('/api/ingest', {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain' },
    body: documentText,
  })
  const data = await res.json()
  return { ok: res.ok, status: res.status, data }
}

/**
 * GET /api/chunks
 * @param {number} limit max chunks to return (default 20)
 * @returns {Promise<{ok: boolean, status: number, data: object}>}
 */
export async function listChunks(limit = 20) {
  const res = await apiFetch(`/api/chunks?limit=${limit}`)
  const data = await res.json()
  return { ok: res.ok, status: res.status, data }
}

/**
 * GET /api/health
 * @returns {Promise<boolean>} true if backend is reachable
 */
export async function checkHealth() {
  try {
    const res = await apiFetch('/api/health')
    return res.ok
  } catch {
    return false
  }
}
