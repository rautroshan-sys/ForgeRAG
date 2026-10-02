import { useState, useEffect, useCallback } from 'react'
import { ingestDocument, listChunks } from '../api/client'

export default function Ingest() {
  const [docText, setDocText]       = useState('')
  const [isIngesting, setIsIngesting] = useState(false)
  const [ingestStatus, setIngestStatus] = useState(null) // null | { ok: bool, message: string }
  
  const [chunks, setChunks]         = useState([])
  const [isLoadingChunks, setIsLoadingChunks] = useState(true)
  const [chunkError, setChunkError]   = useState(null)

  const fetchChunks = useCallback(async () => {
    setIsLoadingChunks(true)
    setChunkError(null)
    try {
      const { ok, data } = await listChunks(20)
      if (ok) {
        setChunks(data.chunks || [])
      } else {
        setChunkError(data.message || 'Failed to load chunks.')
      }
    } catch (err) {
      setChunkError('Network error while loading chunks.')
    } finally {
      setIsLoadingChunks(false)
    }
  }, [])

  // Load chunks on mount
  useEffect(() => {
    fetchChunks()
  }, [fetchChunks])

  async function handleIngest(e) {
    e.preventDefault()
    if (!docText.trim() || isIngesting) return

    setIsIngesting(true)
    setIngestStatus(null)

    try {
      const { ok, data, status } = await ingestDocument(docText.trim())
      if (ok) {
        setIngestStatus({ ok: true, message: `Successfully ingested document into ${data.chunksCreated} chunks.` })
        setDocText('')
        fetchChunks() // refresh the list
      } else {
        const msg = status === 400 ? 'Document too short or invalid.' : (data?.message || 'Ingestion failed.')
        setIngestStatus({ ok: false, message: msg })
      }
    } catch (err) {
      setIngestStatus({ ok: false, message: 'Network error. Backend down?' })
    } finally {
      setIsIngesting(false)
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <div className="mb-8 space-y-1">
        <h1 className="section-title">Document Ingestion</h1>
        <p className="section-sub">Paste text here. The system splits it by structure (paragraphs/headings) and embeds it via pgvector.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        
        {/* ── Left: Ingest Form ──────────────────────────────────── */}
        <div className="space-y-4">
          <form onSubmit={handleIngest} className="card space-y-4" id="ingest-form">
            <label htmlFor="doc-input" className="block text-sm font-medium text-ink">
              Document Text
            </label>
            <textarea
              id="doc-input"
              className="input-base h-64 font-mono text-xs leading-relaxed"
              placeholder="Paste your source document here..."
              value={docText}
              onChange={(e) => setDocText(e.target.value)}
              disabled={isIngesting}
            />
            
            <div className="flex items-center gap-4">
              <button
                type="submit"
                id="submit-ingest-btn"
                className="btn-primary"
                disabled={isIngesting || !docText.trim()}
              >
                {isIngesting ? (
                  <>
                    <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
                    Ingesting…
                  </>
                ) : 'Ingest Document'}
              </button>
            </div>

            {/* Status Message */}
            {ingestStatus && (
              <div className={`mt-4 p-3 rounded-lg text-sm border ${
                ingestStatus.ok 
                  ? 'bg-verify/10 border-verify/30 text-verify' 
                  : 'bg-alert/10 border-alert/30 text-alert'
              }`}>
                {ingestStatus.message}
              </div>
            )}
          </form>

          <div className="card-elevated">
            <h3 className="text-sm font-semibold text-ink mb-2">Chunking Rules</h3>
            <ul className="text-sm text-muted space-y-2 list-disc list-inside marker:text-brand/50">
              <li>Splits on double newlines (paragraphs).</li>
              <li>Merges short paragraphs up to 1000 chars.</li>
              <li>Prevents splitting mid-sentence.</li>
              <li>Calculates L2-normalized Gemini embeddings.</li>
            </ul>
          </div>
        </div>

        {/* ── Right: Chunk Viewer ────────────────────────────────── */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-ink">Recent Chunks</h2>
            <button 
              onClick={fetchChunks} 
              disabled={isLoadingChunks}
              className="text-xs text-brand hover:text-brand-light font-medium"
            >
              Refresh
            </button>
          </div>

          <div className="space-y-3 max-h-[600px] overflow-y-auto pr-2" id="chunks-list">
            {isLoadingChunks && chunks.length === 0 ? (
              <div className="card border-dashed py-12 text-center text-muted">
                <span className="inline-block w-6 h-6 border-2 border-brand/30 border-t-brand rounded-full animate-spin mb-2" />
                <p className="text-sm">Loading chunks...</p>
              </div>
            ) : chunkError ? (
              <div className="card border-alert/30 bg-alert/5 text-alert text-sm py-4 text-center">
                {chunkError}
              </div>
            ) : chunks.length === 0 ? (
              <div className="card border-dashed py-12 text-center text-muted">
                <p className="text-sm">No chunks found.</p>
                <p className="text-xs opacity-70 mt-1">Ingest a document to populate the vector store.</p>
              </div>
            ) : (
              chunks.map((chunk) => (
                <div key={chunk.id} className="card p-4 space-y-3 hover:border-brand/30 transition-colors">
                  <div className="flex items-center justify-between text-xs text-muted">
                    <span className="font-mono">ID: {chunk.id}</span>
                  </div>
                  <p className="text-sm text-ink leading-relaxed line-clamp-4">
                    {chunk.text}
                  </p>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
