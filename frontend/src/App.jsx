import { useState, useEffect } from 'react'

const API = 'http://localhost:8080/api/calendar'

function App() {
  const [connected, setConnected] = useState(false)
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ summary: '', description: '', startDate: '', endDate: '' })

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.get('connected') === 'true') {
      setConnected(true)
      setMessage('Conectado com sucesso!')
      window.history.replaceState({}, '', '/')
    }
    fetch(`${API}/status`).then(res => res.json()).then(data => setConnected(data.connected)).catch(() => {})
  }, [])

  const loadEvents = () => {
    setLoading(true)
    setMessage('')
    fetch(`${API}/events`)
      .then(res => res.json())
      .then(data => {
        if (data.error) { setMessage('Erro: ' + data.error) } else { setEvents(data) }
        setLoading(false)
      })
      .catch(() => { setMessage('Erro ao buscar eventos'); setLoading(false) })
  }

  const createEvent = () => {
    if (!form.summary || !form.startDate || !form.endDate) {
      setMessage('Preencha titulo, data inicio e data fim')
      return
    }
    setLoading(true)
    fetch(`${API}/events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })
      .then(res => res.json())
      .then(data => {
        if (data.error) { setMessage('Erro: ' + data.error) } else {
          setMessage('Evento criado!')
          setForm({ summary: '', description: '', startDate: '', endDate: '' })
          loadEvents()
        }
        setLoading(false)
      })
      .catch(() => { setMessage('Erro ao criar evento'); setLoading(false) })
  }

  const updateEvent = () => {
    if (!form.summary) { setMessage('Preencha o titulo'); return }
    setLoading(true)
    fetch(`${API}/events/${editing}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    })
      .then(res => res.json())
      .then(data => {
        if (data.error) { setMessage('Erro: ' + data.error) } else {
          setMessage('Evento atualizado!')
          setEditing(null)
          setForm({ summary: '', description: '', startDate: '', endDate: '' })
          loadEvents()
        }
        setLoading(false)
      })
      .catch(() => { setMessage('Erro ao atualizar evento'); setLoading(false) })
  }

  const deleteEvent = (eventId) => {
    if (!confirm('Deletar este evento?')) return
    setLoading(true)
    fetch(`${API}/events/${eventId}`, { method: 'DELETE' })
      .then(res => res.json())
      .then(data => {
        if (data.error) { setMessage('Erro: ' + data.error) } else {
          setMessage('Evento deletado!')
          loadEvents()
        }
        setLoading(false)
      })
      .catch(() => { setMessage('Erro ao deletar evento'); setLoading(false) })
  }

  const startEdit = (ev) => {
    setEditing(ev.id)
    setForm({
      summary: ev.summary || '',
      description: '',
      startDate: ev.start || '',
      endDate: ev.end || ''
    })
  }

  const handleSubmit = () => {
    if (editing) { updateEvent() } else { createEvent() }
  }

  return (
    <div className="container">
      <h1>Faisca Arquitetura</h1>
      <h2>POC - Google Calendar API</h2>

      {!connected ? (
        <div className="section">
          <p>Conecte sua conta Google para sincronizar eventos.</p>
          <a href={`${API}/connect`} className="btn btn-primary">Conectar Google Calendar</a>
        </div>
      ) : (
        <div className="section">
          <p className="status connected">Conectado ao Google Calendar</p>

          {/* Formulario de criar/editar */}
          <div className="form-card">
            <h3>{editing ? 'Editar Evento' : 'Criar Evento'}</h3>
            <input placeholder="Titulo" value={form.summary} onChange={e => setForm({...form, summary: e.target.value})} />
            <input placeholder="Descricao (opcional)" value={form.description} onChange={e => setForm({...form, description: e.target.value})} />
            <div className="form-row">
              <div>
                <label>Data inicio</label>
                <input type="date" value={form.startDate} onChange={e => setForm({...form, startDate: e.target.value})} />
              </div>
              <div>
                <label>Data fim</label>
                <input type="date" value={form.endDate} onChange={e => setForm({...form, endDate: e.target.value})} />
              </div>
            </div>
            <div className="form-actions">
              <button onClick={handleSubmit} disabled={loading} className="btn btn-green">
                {editing ? 'Salvar' : 'Criar'}
              </button>
              {editing && (
                <button onClick={() => { setEditing(null); setForm({ summary: '', description: '', startDate: '', endDate: '' }) }} className="btn btn-gray">
                  Cancelar
                </button>
              )}
            </div>
          </div>

          <div className="actions">
            <button onClick={loadEvents} disabled={loading} className="btn btn-blue">Listar Eventos</button>
          </div>

          {message && <p className="message">{message}</p>}

          {events.length > 0 && (
            <div className="events">
              <h3>Proximos Eventos ({events.length})</h3>
              {events.map(ev => (
                <div key={ev.id} className="event-card">
                  <div className="event-info">
                    <strong>{ev.summary}</strong>
                    <span>{ev.start}</span>
                  </div>
                  <div className="event-actions">
                    <button onClick={() => startEdit(ev)} className="btn-small btn-edit">Editar</button>
                    <button onClick={() => deleteEvent(ev.id)} className="btn-small btn-delete">Deletar</button>
                  </div>
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