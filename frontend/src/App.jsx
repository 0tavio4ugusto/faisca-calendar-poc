import { useState, useEffect } from 'react'

const API = 'http://localhost/api/calendar'

const EMPTY_FORM = { summary: '', description: '', startDate: '', endDate: '' }
const MONTHS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

function parseDate(raw) {
  if (!raw) return null
  const [y, m, d] = raw.slice(0, 10).split('-').map(Number)
  if (!y || !m || !d) return null
  return {
    day: String(d).padStart(2, '0'),
    month: MONTHS[m - 1],
    monthUp: MONTHS[m - 1].toUpperCase(),
    year: y,
    time: raw.includes('T') ? raw.slice(11, 16) : ''
  }
}

function formatLong(raw) {
  const p = parseDate(raw)
  if (!p) return raw || ''
  return `${p.day} ${p.month} ${p.year}${p.time ? ` · ${p.time}` : ''}`
}

function App() {
  const [connected, setConnected] = useState(false)
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [editing, setEditing] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [newForm, setNewForm] = useState(EMPTY_FORM)

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (params.get('connected') === 'true') {
      setConnected(true)
      setMessage({ text: 'Conectado com sucesso!', type: 'info' })
      window.history.replaceState({}, '', '/')
    }
    fetch(`${API}/status`).then(res => res.json()).then(data => setConnected(data.connected)).catch(() => {})
  }, [])

  useEffect(() => {
    if (connected) loadEvents()
  }, [connected])

  const loadEvents = (keepMessage = false) => {
    setLoading(true)
    fetch(`${API}/events`)
      .then(res => res.json())
      .then(data => {
        if (data.error) {
          setMessage({ text: data.error, type: 'error' })
        } else {
          setEvents(data)
          if (!keepMessage) setMessage(null)
        }
        setLoading(false)
      })
      .catch(() => {
        setMessage({ text: 'Erro ao buscar eventos', type: 'error' })
        setLoading(false)
      })
  }

  const createEvent = () => {
    if (!newForm.summary || !newForm.startDate || !newForm.endDate) {
      setMessage({ text: 'Preencha título, data de início e data de fim', type: 'error' })
      return
    }
    setLoading(true)
    fetch(`${API}/events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newForm)
    })
      .then(res => res.json())
      .then(data => {
        if (data.error) {
          setMessage({ text: data.error, type: 'error' })
        } else {
          setMessage({ text: 'Evento criado!', type: 'info' })
          setNewForm(EMPTY_FORM)
          loadEvents(true)
        }
        setLoading(false)
      })
      .catch(() => {
        setMessage({ text: 'Erro ao criar evento', type: 'error' })
        setLoading(false)
      })
  }

  const startEdit = (ev) => {
    setEditing(ev.id)
    setEditForm({
      summary: ev.summary || '',
      description: '',
      startDate: (ev.start || '').slice(0, 10),
      endDate: (ev.end || '').slice(0, 10)
    })
  }

  const cancelEdit = () => {
    setEditing(null)
    setEditForm(EMPTY_FORM)
  }

  const saveEdit = () => {
    if (!editForm.summary) {
      setMessage({ text: 'Preencha o título', type: 'error' })
      return
    }
    // descrição só é enviada se preenchida, para não apagar a existente
    const body = {
      summary: editForm.summary,
      startDate: editForm.startDate,
      endDate: editForm.endDate
    }
    if (editForm.description.trim()) body.description = editForm.description
    setLoading(true)
    fetch(`${API}/events/${editing}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
      .then(res => res.json())
      .then(data => {
        if (data.error) {
          setMessage({ text: data.error, type: 'error' })
        } else {
          setMessage({ text: 'Evento atualizado!', type: 'info' })
          cancelEdit()
          loadEvents(true)
        }
        setLoading(false)
      })
      .catch(() => {
        setMessage({ text: 'Erro ao atualizar evento', type: 'error' })
        setLoading(false)
      })
  }

  const deleteEvent = (eventId) => {
    if (!confirm('Excluir este evento?')) return
    setLoading(true)
    fetch(`${API}/events/${eventId}`, { method: 'DELETE' })
      .then(res => res.json())
      .then(data => {
        if (data.error) {
          setMessage({ text: data.error, type: 'error' })
        } else {
          setMessage({ text: 'Evento excluído!', type: 'info' })
          loadEvents(true)
        }
        setLoading(false)
      })
      .catch(() => {
        setMessage({ text: 'Erro ao excluir evento', type: 'error' })
        setLoading(false)
      })
  }

  return (
    <div className="page">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true"></span>
          <span className="brand-name">faisca</span>
          <span className="brand-tag">calendar poc</span>
        </div>
        {connected && <span className="status-pill">conectado</span>}
      </header>

      {!connected ? (
        <section className="connect-panel">
          <h1>Sua agenda no Google Calendar</h1>
          <p>Conecte sua conta para ver, criar e editar eventos direto daqui.</p>
          <a href={`${API}/connect`} className="btn btn-primary">Conectar Google Calendar</a>
          <p className="fine-print">A POC pede acesso de leitura e escrita ao seu calendário principal.</p>
        </section>
      ) : (
        <>
          <div className="toolbar">
            <h1>Próximos eventos</h1>
            <button onClick={loadEvents} disabled={loading} className="btn btn-ghost">
              {loading ? 'Carregando…' : 'Atualizar'}
            </button>
          </div>

          {message && (
            <div className={`message ${message.type === 'error' ? 'message-error' : ''}`}>{message.text}</div>
          )}

          <div className="grid">
            <article className="card card-new">
              <h2 className="card-title">Novo evento</h2>
              <div className="field">
                <label htmlFor="new-summary">Título</label>
                <input
                  id="new-summary"
                  placeholder="Revisão de projeto"
                  value={newForm.summary}
                  onChange={e => setNewForm({ ...newForm, summary: e.target.value })}
                />
              </div>
              <div className="field">
                <label htmlFor="new-desc">Descrição</label>
                <input
                  id="new-desc"
                  placeholder="Opcional"
                  value={newForm.description}
                  onChange={e => setNewForm({ ...newForm, description: e.target.value })}
                />
              </div>
              <div className="date-row">
                <div className="field">
                  <label htmlFor="new-start">Início</label>
                  <input
                    id="new-start"
                    type="date"
                    value={newForm.startDate}
                    onChange={e => setNewForm({ ...newForm, startDate: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label htmlFor="new-end">Fim</label>
                  <input
                    id="new-end"
                    type="date"
                    value={newForm.endDate}
                    onChange={e => setNewForm({ ...newForm, endDate: e.target.value })}
                  />
                </div>
              </div>
              <button onClick={createEvent} disabled={loading} className="btn btn-primary btn-block">
                Criar evento
              </button>
            </article>

            {events.map(ev => {
              const p = parseDate(ev.start)
              return (
                <article key={ev.id} className={`card${editing === ev.id ? ' card-editing' : ''}`}>
                  {editing === ev.id ? (
                    <div className="edit-form">
                      <h2 className="card-title">Editar evento</h2>
                      <div className="field">
                        <label htmlFor={`edit-${ev.id}-summary`}>Título</label>
                        <input
                          id={`edit-${ev.id}-summary`}
                          value={editForm.summary}
                          onChange={e => setEditForm({ ...editForm, summary: e.target.value })}
                        />
                      </div>
                      <div className="field">
                        <label htmlFor={`edit-${ev.id}-desc`}>Descrição</label>
                        <input
                          id={`edit-${ev.id}-desc`}
                          placeholder="Deixe em branco para manter"
                          value={editForm.description}
                          onChange={e => setEditForm({ ...editForm, description: e.target.value })}
                        />
                      </div>
                      <div className="date-row">
                        <div className="field">
                          <label htmlFor={`edit-${ev.id}-start`}>Início</label>
                          <input
                            id={`edit-${ev.id}-start`}
                            type="date"
                            value={editForm.startDate}
                            onChange={e => setEditForm({ ...editForm, startDate: e.target.value })}
                          />
                        </div>
                        <div className="field">
                          <label htmlFor={`edit-${ev.id}-end`}>Fim</label>
                          <input
                            id={`edit-${ev.id}-end`}
                            type="date"
                            value={editForm.endDate}
                            onChange={e => setEditForm({ ...editForm, endDate: e.target.value })}
                          />
                        </div>
                      </div>
                      <div className="card-actions">
                        <button onClick={saveEdit} disabled={loading} className="btn btn-primary btn-small">
                          Salvar
                        </button>
                        <button onClick={cancelEdit} className="btn btn-ghost btn-small">
                          Cancelar
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="card-top">
                        {p ? (
                          <div className="date-badge">
                            <span className="day">{p.day}</span>
                            <span className="month">{p.monthUp}</span>
                          </div>
                        ) : (
                          <div className="date-badge"><span className="day">–</span></div>
                        )}
                        <div className="card-actions">
                          <button onClick={() => startEdit(ev)} className="link-btn">Editar</button>
                          <button onClick={() => deleteEvent(ev.id)} className="link-btn link-danger">Excluir</button>
                        </div>
                      </div>
                      <h2 className="card-title">{ev.summary}</h2>
                      {ev.description && <p className="card-desc">{ev.description}</p>}
                      <div className="card-foot">
                        {formatLong(ev.start)}
                        {ev.end ? ` – ${formatLong(ev.end)}` : ''}
                      </div>
                    </>
                  )}
                </article>
              )
            })}

            {events.length === 0 && (
              <div className="empty-note">
                Nenhum evento próximo. Crie o primeiro no card ao lado.
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}

export default App
