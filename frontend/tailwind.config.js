/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      colors: {
        // Light-theme base
        'primary-bg':   '#e9f1ec',
        'primary-text': '#111827',
        // Semantic text + surface tokens (used in Query, Ingest, SequenceTrace)
        'ink':     '#111827',   // primary text on cards
        'muted':   '#6b7280',   // secondary / dimmed text
        'surface': '#f9fafb',   // card background
        'line':    '#e5e7eb',   // border / divider
        // Accent
        'brand':       '#0ea5e9',  // teal/cyan CTA
        'brand-light': '#38bdf8',  // lighter brand hover
        // Verdict colors
        'verify': '#22c55e',  // grounded / success green
        'alert':  '#f59e0b',  // hallucinated / warning amber
        // Dark theme (interactive demo section on Home)
        'dark-bg':      '#0f172a',
        'dark-surface': '#1e293b',
        'dark-text':    '#f8fafc',
        'dark-muted':   '#94a3b8',
        // Legacy line tokens
        'line-light': '#e5e7eb',
        'line-dark':  '#334155',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      letterSpacing: {
        tightest: '-.075em',
        tighter:  '-.05em',
        tight:    '-.025em',
        normal:   '0',
        wide:     '.025em',
        wider:    '.05em',
        widest:   '.1em',
        superwide:'.15em',
      },
      keyframes: {
        'fade-in': {
          '0%':   { opacity: '0' },
          '100%': { opacity: '1' },
        },
        'slide-up': {
          '0%':   { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'page-enter': {
          '0%':   { opacity: '0', transform: 'translateY(4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'fade-in':  'fade-in 0.2s ease-out',
        'slide-up': 'slide-up 0.25s ease-out',
        'page-enter': 'page-enter 0.3s ease-out',
      },
    },
  },
  plugins: [],
}
