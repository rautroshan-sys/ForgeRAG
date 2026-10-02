/**
 * SequenceTrace — visualises the 5-node agent pipeline.
 *
 * Props:
 *   stage:   'idle' | 'retrieving' | 'generating' | 'evaluating' | 'routing' | 'rewriting' | 'done' | 'failed'
 *   verdict: 'VALID' | 'HALLUCINATED' | null
 *   retryCount: number
 *
 * Each node shows its status visually based on the current stage.
 */

const NODES = [
  { id: 'retrieval',  label: 'Retrieval',     icon: '🔍', desc: 'pgvector inner-product search' },
  { id: 'generation', label: 'Generation',    icon: '✍️',  desc: 'Gemini drafts an answer' },
  { id: 'evaluation', label: 'Evaluation',    icon: '⚖️',  desc: 'LLM-as-judge grounding check' },
  { id: 'router',     label: 'Router',        icon: '↔️',  desc: 'Conditional branch on verdict' },
  { id: 'rewriter',   label: 'Query Rewriter',icon: '🔄', desc: 'Rewrites query for next retry' },
]

// Which nodes are "active" (processing), "done", "failed", or "pending" for each stage
const STAGE_STATES = {
  idle:        { active: [],            done: [],                                        failed: [] },
  retrieving:  { active: ['retrieval'], done: [],                                        failed: [] },
  generating:  { active: ['generation'],done: ['retrieval'],                             failed: [] },
  evaluating:  { active: ['evaluation'],done: ['retrieval', 'generation'],               failed: [] },
  routing:     { active: ['router'],    done: ['retrieval', 'generation', 'evaluation'], failed: [] },
  rewriting:   { active: ['rewriter'],  done: ['retrieval', 'generation', 'evaluation', 'router'], failed: [] },
  done:        { active: [],            done: ['retrieval', 'generation', 'evaluation', 'router'], failed: [] },
  failed:      { active: [],            done: ['retrieval', 'generation', 'evaluation', 'router'], failed: ['router'] },
}

function getNodeStatus(nodeId, stage, verdict) {
  const states = STAGE_STATES[stage] || STAGE_STATES.idle
  if (states.active.includes(nodeId)) return 'active'
  if (stage === 'done' && nodeId === 'router') return verdict === 'VALID' ? 'done' : 'failed'
  if (states.done.includes(nodeId)) return 'done'
  if (states.failed.includes(nodeId)) return 'failed'
  // Rewriter: only show as done if we went through a retry
  return 'pending'
}

const statusClasses = {
  pending: 'node-step node-step-pending',
  active:  'node-step node-step-active',
  done:    'node-step node-step-done',
  failed:  'node-step node-step-failed',
}

const statusIcon = {
  pending: <span className="w-5 h-5 rounded-full border border-current opacity-30 flex-shrink-0" />,
  active:  <span className="w-5 h-5 rounded-full border-2 border-current flex-shrink-0 animate-ping absolute" />,
  done:    <span className="w-5 h-5 flex-shrink-0 text-verify">✓</span>,
  failed:  <span className="w-5 h-5 flex-shrink-0 text-alert">✗</span>,
}

export default function SequenceTrace({ stage = 'idle', verdict = null, retryCount = 0 }) {
  return (
    <div className="card space-y-2" aria-label="Agent pipeline trace">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-ink">Agent Pipeline</h3>
        {retryCount > 0 && (
          <span className="text-xs text-muted bg-surface px-2 py-0.5 rounded-full border border-line">
            {retryCount} retr{retryCount === 1 ? 'y' : 'ies'}
          </span>
        )}
      </div>

      <div className="space-y-1.5">
        {NODES.map((node) => {
          const status = getNodeStatus(node.id, stage, verdict)
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

      {/* Cycle arrow — shown when we're in a retry */}
      {(stage === 'rewriting' || retryCount > 0) && (
        <div className="mt-2 flex items-center gap-2 text-xs text-muted border-t border-line pt-2">
          <span className="text-alert">↩</span>
          <span>Hallucination detected — rewriting query and retrying retrieval</span>
        </div>
      )}
    </div>
  )
}
