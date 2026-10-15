/**
 * SequenceTrace — visualises the 6-node CRAG agent pipeline.
 *
 * Props:
 *   stage:         'idle' | 'retrieving' | 'websearching' | 'generating' | 'evaluating' | 'routing' | 'rewriting' | 'done' | 'failed'
 *   verdict:       'VALID' | 'HALLUCINATED' | null
 *   retryCount:    number
 *   webSearchUsed: boolean
 */

const NODES = [
  { id: 'retrieval',  label: 'Retrieval',     icon: '🔍', desc: 'pgvector inner-product search' },
  { id: 'websearch',  label: 'Web Search',     icon: '🌐', desc: 'CRAG fallback: Gemini + Google Search' },
  { id: 'generation', label: 'Generation',    icon: '✍️',  desc: 'Gemini drafts an answer' },
  { id: 'evaluation', label: 'Evaluation',    icon: '⚖️',  desc: 'LLM-as-judge grounding check' },
  { id: 'router',     label: 'Router',        icon: '↔️',  desc: 'Conditional branch on verdict' },
  { id: 'rewriter',   label: 'Query Rewriter',icon: '🔄', desc: 'Rewrites query for next retry' },
]

// Which nodes are "active" (processing), "done", "failed", or "pending" for each stage
const STAGE_STATES = {
  idle:          { active: [],              done: [],                                                        failed: [] },
  retrieving:    { active: ['retrieval'],   done: [],                                                        failed: [] },
  websearching:  { active: ['websearch'],   done: ['retrieval'],                                             failed: [] },
  generating:    { active: ['generation'],  done: ['retrieval'],                                             failed: [] },
  evaluating:    { active: ['evaluation'],  done: ['retrieval', 'generation'],                               failed: [] },
  routing:       { active: ['router'],      done: ['retrieval', 'generation', 'evaluation'],                 failed: [] },
  rewriting:     { active: ['rewriter'],    done: ['retrieval', 'generation', 'evaluation', 'router'],      failed: [] },
  done:          { active: [],              done: ['retrieval', 'generation', 'evaluation', 'router'],       failed: [] },
  failed:        { active: [],              done: ['retrieval', 'generation', 'evaluation', 'router'],       failed: ['router'] },
}

function getNodeStatus(nodeId, stage, verdict, webSearchUsed) {
  const states = STAGE_STATES[stage] || STAGE_STATES.idle

  // Web Search node: only shown as active/done if CRAG was triggered
  if (nodeId === 'websearch') {
    if (stage === 'websearching') return 'active'
    if (webSearchUsed && ['generating', 'evaluating', 'routing', 'rewriting', 'done', 'failed'].includes(stage)) return 'done'
    return 'pending'
  }

  if (states.active.includes(nodeId)) return 'active'
  if (stage === 'done' && nodeId === 'router') return verdict === 'VALID' ? 'done' : 'failed'
  if (states.done.includes(nodeId)) return 'done'
  if (states.failed.includes(nodeId)) return 'failed'
  return 'pending'
}

const statusClasses = {
  pending: 'node-step node-step-pending',
  active:  'node-step node-step-active',
  done:    'node-step node-step-done',
  failed:  'node-step node-step-failed',
}

export default function SequenceTrace({ stage = 'idle', verdict = null, retryCount = 0, webSearchUsed = false }) {
  return (
    <div className="card space-y-2" aria-label="Agent pipeline trace">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-ink">Agent Pipeline</h3>
        <div className="flex items-center gap-2">
          {webSearchUsed && (
            <span className="text-xs bg-blue-500/20 text-blue-400 border border-blue-500/30 px-2 py-0.5 rounded-full">
              🌐 Web
            </span>
          )}
          {retryCount > 0 && (
            <span className="text-xs text-muted bg-surface px-2 py-0.5 rounded-full border border-line">
              {retryCount} retr{retryCount === 1 ? 'y' : 'ies'}
            </span>
          )}
        </div>
      </div>

      <div className="space-y-1.5">
        {NODES.map((node) => {
          const status = getNodeStatus(node.id, stage, verdict, webSearchUsed)
          return (
            <div key={node.id} className={statusClasses[status]} role="listitem">
              <div className="relative flex-shrink-0 w-5 h-5 flex items-center justify-center">
                {status === 'active' && (
                  <span className="absolute w-5 h-5 rounded-full border-2 border-brand/60 animate-ping" />
                )}
                {status === 'pending' && <span className="w-4 h-4 rounded-full border border-current opacity-30" />}
                {status === 'done'    && <span>✓</span>}
                {status === 'failed'  && <span>✗</span>}
                {status === 'active'  && <span className="relative w-2 h-2 rounded-full bg-current" />}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-1.5">
                  <span className="text-base leading-none">{node.icon}</span>
                  <span className="text-sm font-medium">{node.label}</span>
                </div>
                <p className="text-xs opacity-60 mt-0.5">{node.desc}</p>
              </div>
            </div>
          )
        })}
      </div>

      {/* Status hints */}
      {stage === 'websearching' && (
        <div className="mt-2 flex items-center gap-2 text-xs text-blue-400 border-t border-line pt-2">
          <span>🌐</span>
          <span>Low retrieval confidence — falling back to Google Search (CRAG)</span>
        </div>
      )}
      {webSearchUsed && stage === 'done' && (
        <div className="mt-2 flex items-center gap-2 text-xs text-blue-400 border-t border-line pt-2">
          <span>🌐</span>
          <span>Answer grounded in live web results</span>
        </div>
      )}
      {(stage === 'rewriting' || retryCount > 0) && !webSearchUsed && (
        <div className="mt-2 flex items-center gap-2 text-xs text-muted border-t border-line pt-2">
          <span className="text-alert">↩</span>
          <span>Hallucination detected — rewriting query and retrying retrieval</span>
        </div>
      )}
    </div>
  )
}
