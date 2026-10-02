/**
 * VerdictBadge — shows VALID (green) or HALLUCINATED (orange) badge.
 *
 * @param {{ verdict: 'VALID'|'HALLUCINATED'|string, size?: 'sm'|'md' }} props
 */
export default function VerdictBadge({ verdict, size = 'md' }) {
  if (!verdict) return null

  const isValid = verdict.toUpperCase() === 'VALID'
  const sizeClasses = size === 'sm' ? 'text-xs px-2 py-0.5' : 'text-sm px-3 py-1'

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full font-semibold border ${sizeClasses} ${
        isValid
          ? 'bg-verify/15 text-verify border-verify/30'
          : 'bg-alert/15 text-alert border-alert/30'
      }`}
    >
      {/* Dot indicator */}
      <span className={`w-1.5 h-1.5 rounded-full ${isValid ? 'bg-verify' : 'bg-alert'}`} />
      {isValid ? 'Grounded' : 'Hallucinated'}
    </span>
  )
}
