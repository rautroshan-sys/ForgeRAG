import { useState } from 'react'
import { runQuery } from '../api/client'
import VerdictBadge from '../components/VerdictBadge'
import SequenceTrace from '../components/SequenceTrace'

// Stage sequence during a live query (drives SequenceTrace animation)
const STAGE_SEQUENCE = ['retrieving', 'generating', 'evaluating', 'routing']
const STAGE_DELAY_MS = 900 // approximate per-stage advance

export default function Query() {
  const [queryText, setQueryText]     = useState('')
  const [stage, setStage]             = useState('idle')
  const [result, setResult]           = useState(null)   // null | { data, status }
  const [error, setError]             = useState(null)   // null | string
  const [isLoading, setIsLoading]     = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!queryText.trim() || isLoading) return

    setResult(null)
    setError(null)
    setIsLoading(true)

    // Animate through stages while the real request is in flight
    let stageIdx = 0
    setStage(STAGE_SEQUENCE[0])
    const stageTimer = setInterval(() => {
      stageIdx++
      if (stageIdx < STAGE_SEQUENCE.length) {
        setStage(STAGE_SEQUENCE[stageIdx])
      }
    }, STAGE_DELAY_MS)

    try {
      const { ok, status, data } = await runQuery(queryText.trim())

      clearInterval(stageTimer)

      if (ok || status === 422) {
        // 200 = grounded, 422 = retry-exhausted — both have structured response
        const finalStage = data.grounded ? 'done' : 'failed'
        // If retries happened, briefly show 'rewriting' before final
        if (data.retryCount > 0) {
          setStage('rewriting')
          await sleep(600)
        }
        setStage(finalStage)
        setResult({ data, status })
      } else {
        setStage('idle')
        setError(data?.message || 'An unexpected error occurred.')
      }
    } catch (err) {
      clearInterval(stageTimer)
      setStage('idle')
      setError('Could not reach the backend. Is the server running?')
    } finally {
      setIsLoading(false)
    }
  }

  function handleReset() {
    setQueryText('')
    setStage('idle')
    setResult(null)
    setError(null)
  }

  const verdict = result?.data?.evaluation?.verdict || null
  const retryCount = result?.data?.retryCount ?? 0

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10">
      <div className="mb-8 space-y-1">
        <h1 className="section-title">Query Interface</h1>
        <p className="section-sub">Ask a question. ForgeRAG retrieves, drafts, evaluates, and corrects before answering.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* ── Left: Query form + result ─────────────────────────── */}
        <div className="lg:col-span-2 space-y-5">

          {/* Query form */}
          <form onSubmit={handleSubmit} className="card space-y-4" id="query-form">
            <label htmlFor="query-input" className="block text-sm font-medium text-ink">
              Your Question
            </label>
            <textarea
              id="query-input"
              className="input-base h-28"
              placeholder="What was the revenue growth last quarter?"
              value={queryText}
              onChange={(e) => setQueryText(e.target.value)}
              disabled={isLoading}
            />
            <div className="flex items-center gap-3">
              <button
                type="submit"
                id="submit-query-btn"
                className="btn-primary"
                disabled={isLoading || !queryText.trim()}
              >
                {isLoading ? (
                  <>
                    <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
                    Processing…
                  </>
                ) : 'Run Query'}
              </button>
              {(result || error) && (
                <button type="button" onClick={handleReset} className="btn-secondary">
                  Clear
                </button>
              )}
            </div>
          </form>

          {/* Error state */}
          {error && (
            <div className="card border-alert/30 bg-alert/5 animate-fade-in space-y-1">
              <p className="text-sm font-medium text-alert">Error</p>
              <p className="text-sm text-muted">{error}</p>
            </div>
          )}

          {/* Result */}
          {result && (
            <div className="space-y-4 animate-slide-up">

              {/* Answer card */}
              <div className={`card border-2 ${result.data.grounded ? 'border-verify/30' : 'border-alert/30'}`}>
                <div className="flex items-start justify-between gap-4 mb-4">
                  <h2 className="text-sm font-semibold text-ink">Answer</h2>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <VerdictBadge verdict={verdict} />
                    {retryCount > 0 && (
                      <span className="text-xs text-muted bg-surface px-2 py-0.5 rounded-full border border-line">
                        {retryCount} retr{retryCount === 1 ? 'y' : 'ies'}
                      </span>
                    )}
                  </div>
                </div>

                {result.status === 422 ? (
                  <div className="space-y-2">
                    <p className="text-sm text-alert font-medium">Retry limit exhausted</p>
                    <p className="text-sm text-muted">
                      The system could not produce a grounded answer after {retryCount} attempt{retryCount !== 1 ? 's' : ''}.
                      The last evaluation found the draft to be unverified.
                    </p>
                  </div>
                ) : (
                  <p className="text-ink text-sm leading-relaxed whitespace-pre-wrap">{result.data.answer}</p>
                )}
              </div>

              {/* Evaluation reasoning */}
              {result.data.evaluation?.reasoning && (
                <details className="card group" id="evaluation-details">
                  <summary className="text-sm font-medium text-ink cursor-pointer hover:text-brand-light transition-colors list-none flex items-center justify-between">
                    <span>Evaluation Reasoning</span>
                    <span className="text-muted group-open:rotate-180 transition-transform duration-200">▾</span>
                  </summary>
                  <pre className="mt-3 text-xs font-mono text-muted leading-relaxed whitespace-pre-wrap overflow-x-auto">
                    {result.data.evaluation.reasoning}
                  </pre>
                </details>
              )}
            </div>
          )}
        </div>

        {/* ── Right: Pipeline trace ──────────────────────────────── */}
        <div className="space-y-4">
          <SequenceTrace stage={stage} verdict={verdict} retryCount={retryCount} />

          {/* Explainer */}
          {stage === 'idle' && (
            <div className="card-elevated text-xs text-muted space-y-2 animate-fade-in">
              <p className="font-medium text-ink text-sm">How this works</p>
              <p>ForgeRAG runs a cyclic state machine, not a linear chain. Submit a query to see each node execute in sequence.</p>
              <p className="text-muted/70">The system will rewrite and retry if hallucination is detected.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}
