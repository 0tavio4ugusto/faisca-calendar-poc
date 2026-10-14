import { useState, useEffect } from 'react'

const API = 'http://localhost:8080/api/calendar'

function App() {
  const [connected, setConnected] = useState(false)
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  // Verifica se ja ta conectado (e se voltou do redirect do Google)
  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.get('connected') === 'true') {
      setConnected(true)
      setMessage('Conectado com sucesso!')
      window.history.replaceState({}, '', '/')
    }

    fetch(`${API}/status`)
      .then(res => res.json())
      .then(data => setConnected(data.connected))
      .catch(() => {})
  }, [])

  // Carrega eventos
  const loadEvents = () => {
    setLoading(true)
    setMessage('')
    fetch(`${API}/events`)
      .then(res => res.json())
      .then(data => {
        if (data.error) {
          setMessage('Erro: ' + data.error)
        } else {
          setEvents(data)
        }
        setLoading(false)
      })
      .catch(err => {
        setMessage('Erro ao buscar eventos')
        setLoading(false)
      })
  }

  // Cria evento de teste
  const createTestEvent = () => {
    setLoading(true)
    setMessage('')
    fetch(`${API}/events/test`, { method: 'POST' })
      .then(res => res.json())
      .then(data => {
        if (data.error) {
          setMessage('Erro: ' + data.error)
        } else {
          setMessage('Evento criado! ID: ' + data.eventId)
          loadEvents() // Recarrega a lista
        }
        setLoading(false)
      })
      .catch(err => {
        setMessage('Erro ao criar evento')
        setLoading(false)
      })
  }

  return (
    <div className="container">
      <h1>Faisca Arquitetura</h1>
      <h2>POC - Google Calendar API</h2>

      {!connected ? (
        <div className="section">
          <p>Conecte sua conta Google para sincronizar eventos.</p>
          <a href={`${API}/connect`} className="btn btn-primary">
            Conectar Google Calendar
          </a>
        </div>
      ) : (
        <div className="section">
          <p className="status connected">Conectado ao Google Calendar</p>

          <div className="actions">
            <button onClick={createTestEvent} disabled={loading} className="btn btn-green">
              Criar Evento de Teste
            </button>
            <button onClick={loadEvents} disabled={loading} className="btn btn-blue">
              Listar Eventos
            </button>
          </div>

          {message && <p className="message">{message}</p>}

          {events.length > 0 && (
            <div className="events">
              <h3>Proximos Eventos ({events.length})</h3>
              {events.map(ev => (
                <div key={ev.id} className="event-card">
                  <strong>{ev.summary}</strong>
                  <span>{ev.start}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default App
