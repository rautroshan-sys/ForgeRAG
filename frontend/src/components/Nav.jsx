import { Link, NavLink } from 'react-router-dom'

export default function Nav() {
  const linkClass = ({ isActive }) =>
    `transition-colors duration-200 ${
      isActive
        ? 'text-primary-text font-semibold'
        : 'text-primary-text/50 hover:text-primary-text'
    }`

  return (
    <header className="w-full bg-primary-bg border-b border-primary-text/10">
      <div className="max-w-[1400px] mx-auto px-8 lg:px-16 py-6 flex items-center justify-between">

        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 text-primary-text font-bold text-xl tracking-tight">
          Forge<span className="opacity-50">RAG</span>
          <span className="ml-2 px-2 py-0.5 text-[10px] font-mono border border-primary-text/20 rounded-full tracking-widest uppercase">Verifier</span>
        </Link>

        {/* Navigation */}
        <nav className="flex items-center gap-6 text-sm font-medium">
          <NavLink to="/" end className={linkClass}>
            Home
          </NavLink>
          <NavLink to="/query" className={linkClass}>
            Query
          </NavLink>
          <NavLink to="/ingest" className={linkClass}>
            Knowledge Base
          </NavLink>
          <NavLink to="/history" className={linkClass}>
            History
          </NavLink>
          <a
            href="https://github.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-primary-text/50 hover:text-primary-text transition-colors duration-200 hidden sm:block"
          >
            GitHub ↗
          </a>
        </nav>
      </div>
    </header>
  )
}
