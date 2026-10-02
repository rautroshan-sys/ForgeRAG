import { Link, NavLink } from 'react-router-dom'

export default function Nav() {
  return (
    <header className="w-full bg-primary-bg border-b border-primary-text/10">
      <div className="max-w-[1400px] mx-auto px-8 lg:px-16 py-6 flex items-center justify-between">
        
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2 text-primary-text font-bold text-xl tracking-tight">
          Forge<span className="opacity-50">RAG</span>
          <span className="ml-2 px-2 py-0.5 text-[10px] font-mono border border-primary-text/20 rounded-full tracking-widest uppercase">Verifier</span>
        </Link>

        {/* Navigation - Minimal */}
        <nav className="flex items-center gap-8 text-sm font-medium">
          <NavLink
            to="/"
            className={({ isActive }) =>
              `transition-colors duration-200 ${isActive ? 'text-primary-text' : 'text-primary-text/50 hover:text-primary-text'}`
            }
          >
            Homepage
          </NavLink>
          <a href="#" className="text-primary-text/50 hover:text-primary-text transition-colors duration-200 hidden sm:block">
            GitHub ↗
          </a>
        </nav>
      </div>
    </header>
  )
}
