import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Nav from './components/Nav'
import Footer from './components/Footer'
import Home from './pages/Home'
import Query from './pages/Query'
import Ingest from './pages/Ingest'

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen flex flex-col">
        <Nav />
        <main className="flex-1 page-enter">
          <Routes>
            <Route path="/"       element={<Home />} />
            <Route path="/query"  element={<Query />} />
            <Route path="/ingest" element={<Ingest />} />
          </Routes>
        </main>
        <Footer />
      </div>
    </BrowserRouter>
  )
}
