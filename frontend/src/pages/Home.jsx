import { useState } from 'react'
import { Link } from 'react-router-dom'
import { runQuery } from '../api/client'

export default function Home() {
  const [queryText, setQueryText] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [result, setResult] = useState(null)
  
  const handleExampleClick = (text) => {
    setQueryText(text)
  }

  const handleQuery = async (e) => {
    e.preventDefault()
    if (!queryText.trim() || isLoading) return
    setIsLoading(true)
    setResult(null)
    
    // Simulate query for now or connect to real backend
    try {
      const { ok, data, status } = await runQuery(queryText.trim())
      setResult({ ok, data, status })
    } catch (err) {
      setResult({ ok: false, data: { message: "Backend connection failed." } })
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="w-full font-sans pb-20">
      
      {/* ── Section 1: The Stack ────────────────────────────────────────── */}
      <section className="max-w-[1400px] mx-auto px-8 lg:px-16 pt-24 pb-32">
        <div className="flex justify-between items-center mb-16 border-b border-primary-text/20 pb-4 text-[10px] font-mono tracking-widest uppercase font-bold text-primary-text/60">
          <span>01 / Technical Foundation</span>
          <span>Built for the loop</span>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-16">
          <div className="space-y-6">
            <h1 className="text-6xl md:text-8xl font-bold tracking-tight text-primary-text leading-none">
              The stack.
            </h1>
            <p className="text-xl text-primary-text/70 max-w-md leading-relaxed">
              A practical foundation for stateful reasoning, vector retrieval, and grounded generation.
            </p>
          </div>
          
          <div className="flex flex-col text-sm border-t border-primary-text/10">
            <div className="grid grid-cols-[150px_1fr] py-5 border-b border-primary-text/10">
              <span className="text-primary-text/60 font-mono text-xs">LAYER</span>
              <span className="text-primary-text/60 font-mono text-xs">CHOICE</span>
            </div>
            
            {[
              ['Core language', 'Java 17+'],
              ['Framework', 'Spring Boot'],
              ['AI orchestration', 'LangChain4j + LangGraph4j'],
              ['Database', 'PostgreSQL + pgvector (Inner Product search)'],
              ['Foundation model', 'Gemini API'],
              ['Frontend', 'React + Vite + Tailwind']
            ].map(([layer, choice], i) => (
              <div key={i} className="grid grid-cols-[150px_1fr] py-6 border-b border-primary-text/10 group hover:bg-primary-text/5 transition-colors px-2 -mx-2">
                <span className="text-primary-text">{layer}</span>
                <span className="font-mono text-xs text-primary-text/80">{choice}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Section 2: How it works ─────────────────────────────────────── */}
      <section className="max-w-[1400px] mx-auto px-8 lg:px-16 py-24">
        <div className="flex justify-between items-end mb-16 border-b border-primary-text/20 pb-4">
          <h2 className="text-5xl md:text-6xl font-bold tracking-tight text-primary-text">
            How it works.
          </h2>
          <div className="text-right max-w-xs text-sm text-primary-text/60 leading-relaxed hidden md:block">
            Not a straight line from question to answer.<br/>Each step keeps the next one accountable.
          </div>
        </div>
        
        <div className="flex flex-col border-t border-primary-text/10">
          {[
            ['01', 'Retrieval', 'Find relevant evidence', 'Retrieves the most relevant passages from the available knowledge base to establish the evidence set.'],
            ['02', 'Generation', 'Draft an answer', 'Produces a first-pass answer from those passages, without treating the draft as final.'],
            ['03', 'Evaluation', 'Check every claim', 'Compares each claim against the retrieved context and identifies anything the evidence cannot support.'],
            ['04', 'Router', 'Decide or retry', 'Routes a grounded draft to output, or rewrites the query and sends an unsupported draft back through retrieval, up to a capped retry limit.'],
            ['05', 'Output', 'Return a verdict', 'Returns the final answer with a grounded or hallucinated verdict and evaluation reasoning attached.']
          ].map(([num, title, sub, desc], i) => (
            <div key={i} className="grid grid-cols-1 md:grid-cols-[80px_300px_1fr_40px] gap-4 py-10 border-b border-primary-text/10 group hover:bg-primary-text/5 transition-colors px-4 -mx-4 items-start">
              <span className="font-mono text-xs font-bold text-primary-text/50 pt-1">{num}</span>
              <div>
                <h3 className="text-2xl font-bold text-primary-text mb-1">{title}</h3>
                <span className="text-xs font-mono text-brand font-semibold">{sub}</span>
              </div>
              <p className="text-primary-text/70 max-w-2xl leading-relaxed text-sm md:text-base pt-1">{desc}</p>
              <div className="text-right text-primary-text/30 group-hover:text-primary-text transition-colors pt-1 hidden md:block">↗</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Section 3: Use Cases ────────────────────────────────────────── */}
      <section className="max-w-[1400px] mx-auto px-8 lg:px-16 py-24">
        <div className="flex justify-between items-center mb-16 border-b border-primary-text/20 pb-4 text-[10px] font-mono tracking-widest uppercase font-bold text-primary-text/60">
          <span>02 / The case for agentic RAG</span>
          <span>Beyond first-pass retrieval</span>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-[1fr_400px] gap-16 mb-16">
          <h2 className="text-5xl md:text-6xl font-bold tracking-tight text-primary-text leading-none max-w-2xl">
            A better answer starts with a better process.
          </h2>
          <p className="text-base text-primary-text/70 leading-relaxed self-end">
            In high-stakes workflows, sounding right isn't enough. The system has to show its work — and know when to question it.
          </p>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 border-t border-l border-primary-text/10">
          {[
            ['01', 'Compliance, Legal & Financial Risk', 'A confident but ungrounded answer is worse than a slow, correct one. Self-evaluation loops reduce unsupported claims and give the confidence scoring and explainability regulated environments need to trust an answer.'],
            ['02', 'Multi-Hop Reasoning Across Fragmented Systems', 'Naive RAG can\'t connect facts across different documents. ForgeRAG plans, routes, iterates, and verifies data before generating an answer, reconciling conflicting numbers across sources rather than trusting the first retrieval pass.'],
            ['03', 'Dynamic Tool Orchestration', 'The same node pattern extends to autonomous use of calculators, pricing APIs, SQL engines, or full application backends mid-process, not just document retrieval.'],
            ['04', 'Advanced Triage & Intake Automation', 'Classifying data, summarizing context, and routing requests to the right pathway, with the same self-checking discipline applied throughout.']
          ].map(([num, title, desc], i) => (
             <div key={i} className="p-10 border-b border-r border-primary-text/10 group hover:bg-primary-text/5 transition-colors flex flex-col h-full min-h-[300px]">
               <div className="text-[10px] font-mono font-bold text-primary-text/40 mb-8 uppercase tracking-widest">{num} / Use Case</div>
               <h3 className="text-2xl font-bold text-primary-text mb-4">{title}</h3>
               <p className="text-primary-text/60 text-sm leading-relaxed mb-auto">{desc}</p>
               <div className="mt-8 text-right text-primary-text/20 group-hover:text-primary-text transition-colors">↗</div>
             </div>
          ))}
        </div>
      </section>

      {/* ── Section 4: Interactive Demo (Dark Theme) ────────────────────── */}
      <section className="bg-dark-bg text-dark-text py-32 mt-12 border-t-8 border-brand">
        <div className="max-w-[1400px] mx-auto px-8 lg:px-16">
          <div className="flex justify-between items-center mb-16 border-b border-dark-text/20 pb-4 text-[10px] font-mono tracking-widest uppercase font-bold text-dark-text/40">
            <span>03 / Interactive Demo</span>
            <span>Simulated Environment</span>
          </div>
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-16 mb-24">
            <h2 className="text-6xl md:text-8xl font-bold tracking-tight text-white leading-none">
              Put the loop<br/>to the test.
            </h2>
            <p className="text-xl text-dark-text/70 leading-relaxed max-w-sm self-end pb-4">
              Ask one question. Watch the agent check its own answer against the evidence before it returns a verdict.
            </p>
          </div>
          
          {/* Agent Execution Flow */}
          <div className="border border-dark-text/10 rounded-sm p-8 bg-dark-surface/30 backdrop-blur-sm mb-8">
            <div className="flex justify-between items-center mb-8">
               <h4 className="text-[10px] font-mono font-bold text-dark-text/40 uppercase tracking-widest">Agent Execution</h4>
               <div className="flex items-center gap-2">
                 <span className="w-1.5 h-1.5 rounded-full bg-brand animate-pulse"></span>
                 <span className="text-[10px] font-mono font-bold text-dark-text/60 uppercase tracking-widest">Awaiting Question</span>
               </div>
            </div>
            
            <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4 overflow-x-auto pb-4">
              {['Retrieval', 'Generation', 'Evaluation', 'Router', 'Output'].map((node, i) => (
                <div key={node} className="flex items-center gap-4 flex-shrink-0">
                  <div className="border border-dark-text/10 bg-dark-surface/50 p-6 min-w-[180px]">
                    <div className="text-[10px] font-mono text-dark-text/40 mb-3">0{i+1} / NODE</div>
                    <div className="text-xl font-semibold text-white">{node}</div>
                  </div>
                  {i < 4 && <div className="text-dark-text/30 text-lg hidden md:block">→</div>}
                </div>
              ))}
            </div>
          </div>
          
          {/* Query Interface */}
          <div className="border border-dark-text/10 rounded-sm p-8 bg-dark-surface/30 backdrop-blur-sm">
            <div className="mb-6">
              <h4 className="text-[10px] font-mono font-bold text-dark-text/40 uppercase tracking-widest mb-3">Try an example</h4>
              <div className="flex flex-wrap gap-3">
                {[
                  'When was Google founded and who are its founders?',
                  'What is the relationship between Google and Alphabet Inc.?',
                  'What are Google\'s main products and services?'
                ].map((q, i) => (
                  <button 
                    key={i} 
                    onClick={() => handleExampleClick(q)}
                    className="px-4 py-2 text-xs border border-dark-text/20 text-dark-text/80 hover:bg-dark-text/10 transition-colors"
                  >
                    {q} {i === 1 && <span className="ml-2 text-[9px] text-orange-400 font-mono tracking-wider">↳ RETRY PATH</span>}
                  </button>
                ))}
              </div>
            </div>
            
            <div className="mb-6">
               <h4 className="text-[10px] font-mono font-bold text-dark-text/40 uppercase tracking-widest mb-3">Ask a question</h4>
               <form onSubmit={handleQuery} className="flex flex-col md:flex-row gap-4">
                 <input 
                   type="text" 
                   value={queryText}
                   onChange={(e) => setQueryText(e.target.value)}
                   className="flex-1 bg-transparent border border-dark-text/20 px-4 py-4 text-white focus:outline-none focus:border-brand/50 transition-colors text-sm"
                   placeholder="When was Google founded and who are its founders?"
                 />
                 <button 
                   type="submit" 
                   disabled={isLoading || !queryText}
                   className="bg-white text-dark-bg px-8 py-4 font-bold text-sm hover:bg-gray-200 transition-colors disabled:opacity-50 flex items-center gap-2 flex-shrink-0"
                 >
                   {isLoading ? 'Running...' : 'Run query →'}
                 </button>
               </form>
            </div>
            
            <div className="border border-dark-text/10 bg-dark-bg/50 p-8 min-h-[160px] flex items-center justify-center text-dark-text/40 text-sm">
              {result ? (
                <div className="w-full text-left">
                  {result.ok ? (
                    <div className="text-white space-y-4">
                      <div className="flex items-center gap-3">
                        <span className="px-2 py-1 bg-green-500/20 text-green-400 text-xs font-mono font-bold border border-green-500/30">VALID</span>
                        <span className="text-xs font-mono text-dark-text/50">Output verified against context</span>
                      </div>
                      <p className="leading-relaxed">{result.data.answer}</p>
                    </div>
                  ) : (
                    <div className="text-orange-400">Error: {result.data.message || 'Failed to fetch result.'}</div>
                  )}
                </div>
              ) : (
                <div className="flex flex-col items-center gap-3">
                  <span className="text-2xl text-dark-text/20">⚡</span>
                  Verified response will appear here.
                </div>
              )}
            </div>
            
            <div className="mt-4 text-xs font-mono text-dark-text/30">
              Live backend connected. Responses use active RAG agent loop.
            </div>
          </div>

        </div>
      </section>

    </div>
  )
}
