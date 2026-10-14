import { useState, useEffect } from 'react'
import VerdictBadge from '../components/VerdictBadge'

const STORAGE_KEY = 'forgerag_history'

/** Load history from localStorage (array, most-recent first). */
function loadHistory() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

/** Append one entry and persist. Returns the updated list. */
export function appendHistory(entry) {
  const list = loadHistory()
  const updated = [entry, ...list].slice(0, 50) // cap at 50 entries
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated))
  } catch {
    // localStorage quota exceeded — silently skip
  }
  return updated
}

function formatTime(isoStr) {
  try {
    return new Date(isoStr).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch {
    return isoStr
  }
}

function formatDate(isoStr) {
  try {
    return new Date(isoStr).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return ''
  }
}

export default function History() {
  const [history, setHistory] = useState([])
  const [expanded, setExpanded] = useState(null) // index of expanded card

  useEffect(() => {
    setHistory(loadHistory())
  }, [])

  function handleClear() {
    localStorage.removeItem(STORAGE_KEY)
    setHistory([])
    setExpanded(null)
  }

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <div className="mb-8 space-y-1">
        <h1 className="section-title">Evaluation History</h1>
        <p className="section-sub">
          A log of queries run in this browser session.{' '}
          <span className="text-alert font-medium">
            Session-scoped only — cleared on page refresh and not persisted server-side.
          </span>
        </p>
      </div>

      {history.length > 0 && (
        <div className="flex items-center justify-between mb-5">
          <span className="text-xs text-muted font-mono">{history.length} entr{history.length === 1 ? 'y' : 'ies'}</span>
          <button
            onClick={handleClear}
            className="text-xs text-alert hover:text-alert/80 font-medium transition-colors"
            id="clear-history-btn"
          >
            Clear history
          </button>
        </div>
      )}

      {history.length === 0 ? (
        <div className="card border-dashed py-16 text-center space-y-2">
          <p className="text-muted text-sm">No history yet.</p>
          <p className="text-muted/70 text-xs">
            Run a query from the{' '}
            <a href="/query" className="text-brand hover:text-brand-light underline">Query page</a>
            {' '}— results will appear here automatically.
          </p>
        </div>
      ) : (
        <div className="space-y-3" id="history-list">
          {history.map((entry, idx) => {
            const isOpen = expanded === idx
            const verdict = entry.evaluation?.verdict ?? null
            return (
              <div
                key={idx}
                className={`card transition-colors duration-150 ${isOpen ? 'border-brand/30' : 'hover:border-line'}`}
              >
                {/* Summary row */}
                <button
                  className="w-full text-left flex items-start justify-between gap-4 group"
                  onClick={() => setExpanded(isOpen ? null : idx)}
                  aria-expanded={isOpen}
                >
                  <div className="flex-1 min-w-0 space-y-1">
                    <p className="text-sm font-medium text-ink truncate">{entry.query}</p>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="text-xs text-muted font-mono">
                        {formatDate(entry.timestamp)} · {formatTime(entry.timestamp)}
                      </span>
                      {entry.retryCount > 0 && (
                        <span className="text-xs text-muted bg-surface px-2 py-0.5 rounded-full border border-line">
                          {entry.retryCount} retr{entry.retryCount === 1 ? 'y' : 'ies'}
                        </span>
                      )}
                      {entry.status === 422 && (
                        <span className="text-xs text-alert font-mono">422 exhausted</span>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    <VerdictBadge verdict={verdict} size="sm" />
                    <span className={`text-muted text-xs transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`}>▾</span>
                  </div>
                </button>

                {/* Expanded detail */}
                {isOpen && (
                  <div className="mt-4 pt-4 border-t border-line space-y-3 animate-fade-in">
                    {entry.answer ? (
                      <div>
                        <p className="text-xs font-medium text-muted mb-1 uppercase tracking-wide">Answer</p>
                        <p className="text-sm text-ink leading-relaxed whitespace-pre-wrap">{entry.answer}</p>
                      </div>
                    ) : (
                      <p className="text-sm text-alert">No grounded answer — retry limit exhausted.</p>
                    )}

                    {entry.evaluation?.reasoning && (
                      <div>
                        <p className="text-xs font-medium text-muted mb-1 uppercase tracking-wide">Evaluation Reasoning</p>
                        <pre className="text-xs font-mono text-muted leading-relaxed whitespace-pre-wrap overflow-x-auto bg-surface border border-line rounded p-3">
                          {entry.evaluation.reasoning}
                        </pre>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
