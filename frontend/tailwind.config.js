/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      colors: {
        // The light mint/gray background from the screenshots
        'primary-bg': '#e9f1ec', 
        'primary-text': '#111827',
        // Dark theme colors for the interactive sections
        'dark-bg': '#0f172a',
        'dark-surface': '#1e293b',
        'dark-text': '#f8fafc',
        'dark-muted': '#94a3b8',
        brand: '#38bdf8', // accent color
        'line-light': '#cbd5e1',
        'line-dark': '#334155',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      letterSpacing: {
        tightest: '-.075em',
        tighter: '-.05em',
        tight: '-.025em',
        normal: '0',
        wide: '.025em',
        wider: '.05em',
        widest: '.1em',
        superwide: '.15em',
      }
    },
  },
  plugins: [],
}
