import React, { useMemo, useState } from 'react';
import {
  CalendarClock,
  Plus,
  Trash2,
  MapPin,
  User,
  Truck,
  CheckCircle2,
  AlertCircle,
  QrCode,
  Mail,
  Phone,
  Pencil,
  Repeat,
  X
} from 'lucide-react';
import { MUNICIPAL_WARDS } from '../data/mockData';
import {
  addScheduleToFirestore,
  deleteScheduleFromFirestore,
  updateScheduleSlot,
  addScheduleTemplate,
  updateScheduleTemplate,
  deleteScheduleTemplate
} from '../firebase/firestoreService';

const CAMPUS_LAT = parseFloat(import.meta.env.VITE_CAMPUS_LAT) || 12.823;
const CAMPUS_LNG = parseFloat(import.meta.env.VITE_CAMPUS_LNG) || 80.044;
const DEPARTMENTS = MUNICIPAL_WARDS.filter((w) => w.id !== 'all');

const todayStr = () => {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

const binLabel = (b) => b.name || b.location || b.code || b.id;
const binLat = (b) => b.coordinates?.lat ?? b.lat;
const binLng = (b) => b.coordinates?.lng ?? b.lng;

const statusColor = (status) => {
  if (status === 'Completed') return '#34D399';
  if (status === 'In Progress') return '#FBBF24';
  return '#38BDF8';
};

export default function SchedulesView({ workers, bins, schedules, templates = [], collectionLogs, onEnsureSlots }) {
  const [workerId, setWorkerId] = useState('');
  const [departmentId, setDepartmentId] = useState(DEPARTMENTS[0]?.id || '');
  const [binId, setBinId] = useState('custom');
  const [customName, setCustomName] = useState('');
  const [customLat, setCustomLat] = useState(String(CAMPUS_LAT));
  const [customLng, setCustomLng] = useState(String(CAMPUS_LNG));
  const [date, setDate] = useState(todayStr());
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('10:00');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [filterDate, setFilterDate] = useState(todayStr());
  const [repeat, setRepeat] = useState('daily'); // daily | once
  const [editing, setEditing] = useState(null); // { kind: 'template' | 'slot', id }

  const resetForm = () => {
    setEditing(null);
    setNotes('');
    setCustomName('');
    setBinId('custom');
    setRepeat('daily');
    setError(null);
  };

  const startEdit = (kind, item) => {
    setEditing({ kind, id: item.id });
    setRepeat(kind === 'template' ? 'daily' : 'once');
    setWorkerId(item.workerId);
    setDepartmentId(item.departmentId || DEPARTMENTS.find((d) => d.name === item.department)?.id || DEPARTMENTS[0]?.id || '');
    setBinId('custom');
    setCustomName(item.locationName || '');
    setCustomLat(String(item.lat ?? CAMPUS_LAT));
    setCustomLng(String(item.lng ?? CAMPUS_LNG));
    setDate(kind === 'template' ? (item.startDate || todayStr()) : item.date);
    setStartTime(item.startTime);
    setEndTime(item.endTime);
    setNotes(item.notes || '');
    setError(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const selectedWorker = workers.find((w) => w.id === workerId) || null;
  const department = DEPARTMENTS.find((d) => d.id === departmentId);
  const deptBins = useMemo(
    () => bins.filter((b) => !b.wardId || b.wardId === departmentId),
    [bins, departmentId]
  );

  const workerSlots = useMemo(
    () => schedules.filter((s) => s.workerId === workerId),
    [schedules, workerId]
  );
  const workerToday = workerSlots.filter((s) => s.date === todayStr());
  const workerDone = workerToday.filter((s) => s.status === 'Completed').length;

  const visibleSlots = schedules.filter((s) => (!filterDate || s.date === filterDate));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!selectedWorker) return setError('Choose a registered worker first.');
    if (!date || !startTime || !endTime) return setError('Date, start and end time are required.');
    if (endTime <= startTime) return setError('End time must be after the start time.');

    let locationName;
    let lat;
    let lng;
    if (binId === 'custom') {
      locationName = customName.trim();
      lat = parseFloat(customLat);
      lng = parseFloat(customLng);
      if (!locationName) return setError('Enter a location name.');
      if (!Number.isFinite(lat) || !Number.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
        return setError('Latitude / longitude are not valid.');
      }
    } else {
      const bin = bins.find((b) => b.id === binId);
      if (!bin) return setError('Selected bin no longer exists.');
      locationName = binLabel(bin);
      lat = Number(binLat(bin));
      lng = Number(binLng(bin));
      if (!Number.isFinite(lat) || !Number.isFinite(lng)) return setError('That bin has no coordinates.');
    }

    const overlaps = (x) => startTime < x.endTime && endTime > x.startTime;
    const editingId = editing?.id;
    const slotClash = repeat === 'once' && schedules.find(
      (x) => x.id !== editingId && x.workerId === workerId && x.date === date && overlaps(x)
    );
    const routineClash = templates.find(
      (x) => x.active !== false && x.id !== editingId && x.workerId === workerId && overlaps(x) &&
        (repeat === 'daily' || (x.startDate || '') <= date)
    );
    const clash = slotClash || routineClash;
    if (clash) {
      return setError(`${selectedWorker.name} already has ${clash.startTime}-${clash.endTime} at ${clash.locationName}${routineClash ? ' (daily routine)' : ' on that day'}.`);
    }

    const fields = {
      workerId: selectedWorker.id,
      workerName: selectedWorker.name,
      department: department?.name || '',
      departmentId,
      locationName,
      lat,
      lng,
      startTime,
      endTime,
      notes: notes.trim()
    };

    setBusy(true);
    try {
      if (editing?.kind === 'template') {
        await updateScheduleTemplate(editing.id, { ...fields, startDate: date });
      } else if (editing?.kind === 'slot') {
        await updateScheduleSlot(editing.id, { ...fields, date });
      } else if (repeat === 'daily') {
        await addScheduleTemplate({ ...fields, startDate: date });
        if (onEnsureSlots) onEnsureSlots();
      } else {
        await addScheduleToFirestore({ ...fields, date });
      }
      resetForm();
      setFilterDate(date);
    } catch (err) {
      setError(`Could not save: ${err.message}`);
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async (slot) => {
    if (!window.confirm(`Remove ${slot.startTime} ${slot.locationName} for ${slot.workerName}?`)) return;
    try {
      if (editing?.id === slot.id) resetForm();
      await deleteScheduleFromFirestore(slot.id);
    } catch (err) {
      setError(`Could not delete: ${err.message}`);
    }
  };

  const handleDeleteTemplate = async (t) => {
    if (!window.confirm(`Stop the daily routine "${t.startTime} ${t.locationName}" for ${t.workerName}? Upcoming slots are removed; completed history is kept.`)) return;
    try {
      if (editing?.id === t.id) resetForm();
      await deleteScheduleTemplate(t.id);
    } catch (err) {
      setError(`Could not delete: ${err.message}`);
    }
  };

  const handleTogglePause = async (t) => {
    try {
      await updateScheduleTemplate(t.id, { active: t.active === false });
    } catch (err) {
      setError(`Could not update: ${err.message}`);
    }
  };

  const labelStyle = { display: 'block', fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 800, color: '#FFF', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <CalendarClock size={22} color="var(--cyan-400)" /> Daily Garbage Collection Schedule
        </h2>
        <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)', marginTop: '6px' }}>
          Assign a registered worker to a department location and time slot. The worker sees it on their phone as a
          timetable + map, and the slot turns <strong>Completed</strong> when the resident scans the worker's QR.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px', alignItems: 'start' }}>
        {/* Assignment form */}
        <form onSubmit={handleSubmit} className="glass-panel" style={{ padding: '22px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ fontSize: '15px', fontWeight: 800, color: '#FFF' }}>
              {editing ? (editing.kind === 'template' ? 'Edit daily routine' : 'Edit this day slot') : 'Assign a collection slot'}
            </h3>
            {editing && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={resetForm} style={{ gap: 4 }}>
                <X size={14} /> Cancel edit
              </button>
            )}
          </div>

          {!editing && (
            <div>
              <label style={labelStyle}>Repeat</label>
              <select className="input-field" value={repeat} onChange={(e) => setRepeat(e.target.value)}>
                <option value="daily" style={{ background: '#0D1322' }}>Every day (automatic)</option>
                <option value="once" style={{ background: '#0D1322' }}>Only on the selected date</option>
              </select>
            </div>
          )}

          {error && (
            <div style={{ display: 'flex', gap: '8px', padding: '10px 12px', borderRadius: '10px', background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.4)', color: '#FCA5A5', fontSize: '12.5px' }}>
              <AlertCircle size={16} style={{ flexShrink: 0, marginTop: '1px' }} /> <div>{error}</div>
            </div>
          )}

          <div>
            <label style={labelStyle}>Worker</label>
            <select className="input-field" value={workerId} onChange={(e) => setWorkerId(e.target.value)}>
              <option value="" style={{ background: '#0D1322' }}>
                {workers.length ? 'Select a registered worker…' : 'No workers registered yet'}
              </option>
              {workers.map((w) => (
                <option key={w.id} value={w.id} style={{ background: '#0D1322' }}>
                  {w.name} ({w.id}) — {w.phone || w.email}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label style={labelStyle}>Department / Zone</label>
            <select className="input-field" value={departmentId} onChange={(e) => { setDepartmentId(e.target.value); setBinId('custom'); }}>
              {DEPARTMENTS.map((d) => (
                <option key={d.id} value={d.id} style={{ background: '#0D1322' }}>{d.name}</option>
              ))}
            </select>
          </div>

          <div>
            <label style={labelStyle}>Collection point</label>
            <select className="input-field" value={binId} onChange={(e) => setBinId(e.target.value)}>
              <option value="custom" style={{ background: '#0D1322' }}>Custom location…</option>
              {deptBins.map((b) => (
                <option key={b.id} value={b.id} style={{ background: '#0D1322' }}>{binLabel(b)}</option>
              ))}
            </select>
          </div>

          {binId === 'custom' && (
            <>
              <input className="input-field" placeholder="Location name (e.g. Paari Hostel Gate)" value={customName} onChange={(e) => setCustomName(e.target.value)} />
              <div style={{ display: 'flex', gap: '10px' }}>
                <input className="input-field" placeholder="Latitude" value={customLat} onChange={(e) => setCustomLat(e.target.value)} />
                <input className="input-field" placeholder="Longitude" value={customLng} onChange={(e) => setCustomLng(e.target.value)} />
              </div>
            </>
          )}

          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <div style={{ flex: '1 1 140px' }}>
              <label style={labelStyle}>{repeat === 'daily' ? 'Starting from' : 'Date'}</label>
              <input type="date" className="input-field" value={date} onChange={(e) => setDate(e.target.value)} />
            </div>
            <div style={{ flex: '1 1 100px' }}>
              <label style={labelStyle}>Start</label>
              <input type="time" className="input-field" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
            </div>
            <div style={{ flex: '1 1 100px' }}>
              <label style={labelStyle}>End</label>
              <input type="time" className="input-field" value={endTime} onChange={(e) => setEndTime(e.target.value)} />
            </div>
          </div>

          <input className="input-field" placeholder="Notes for the worker (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} />

          <button type="submit" className="btn btn-primary" disabled={busy} style={{ gap: '8px' }}>
            {editing ? <Pencil size={16} /> : <Plus size={16} />} {busy ? 'Saving…' : editing ? 'Save changes' : repeat === 'daily' ? 'Create daily routine' : 'Assign slot'}
          </button>
        </form>

        {/* Worker account profile */}
        <div className="glass-panel" style={{ padding: '22px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <h3 style={{ fontSize: '15px', fontWeight: 800, color: '#FFF' }}>Worker account profile</h3>
          {!selectedWorker ? (
            <p style={{ fontSize: '12.5px', color: 'var(--text-muted)' }}>Select a worker to see their profile and today's progress.</p>
          ) : (
            <>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div style={{ width: 46, height: 46, borderRadius: 12, background: 'rgba(56,189,248,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <User size={22} color="#38BDF8" />
                </div>
                <div>
                  <div style={{ fontWeight: 800, color: '#FFF', fontSize: 16 }}>{selectedWorker.name}</div>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{selectedWorker.id} · {selectedWorker.status || 'Available'}</div>
                </div>
              </div>
              <div style={{ display: 'grid', gap: 6, fontSize: 12.5, color: 'var(--text-secondary)' }}>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><Mail size={14} /> {selectedWorker.email}</div>
                {selectedWorker.phone && <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><Phone size={14} /> {selectedWorker.phone}</div>}
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><Truck size={14} /> {selectedWorker.vehicle || 'No vehicle'}</div>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><MapPin size={14} /> {selectedWorker.wardName || 'No zone'}</div>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><QrCode size={14} /> Shows own QR in the app · residents scan it to confirm pickup</div>
              </div>
              <div style={{ display: 'flex', gap: 10 }}>
                <div style={{ flex: 1, padding: 12, borderRadius: 10, background: 'rgba(0,0,0,0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: 22, fontWeight: 800, color: '#FFF' }}>{workerToday.length}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Slots today</div>
                </div>
                <div style={{ flex: 1, padding: 12, borderRadius: 10, background: 'rgba(0,0,0,0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: 22, fontWeight: 800, color: '#34D399' }}>{workerDone}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Collected</div>
                </div>
                <div style={{ flex: 1, padding: 12, borderRadius: 10, background: 'rgba(0,0,0,0.3)', textAlign: 'center' }}>
                  <div style={{ fontSize: 22, fontWeight: 800, color: '#FBBF24' }}>{workerToday.length - workerDone}</div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Pending</div>
                </div>
              </div>
            </>
          )}
        </div>
      </div>

      {/* Daily routines */}
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 800, color: '#FFF', display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
          <Repeat size={16} color="var(--cyan-400)" /> Daily routines ({templates.length})
        </h3>
        <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 12 }}>
          These repeat every day on their own. Editing a routine (new worker, new area, new time) updates all upcoming days; completed days stay as history.
        </p>
        {templates.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No daily routines yet. Choose "Every day (automatic)" in the form above.</p>
        ) : (
          <div style={{ display: 'grid', gap: 8 }}>
            {templates.slice().sort((a, b) => (a.startTime || '').localeCompare(b.startTime || '')).map((t) => (
              <div key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap', padding: '10px 12px', borderRadius: 10, background: 'rgba(0,0,0,0.25)', opacity: t.active === false ? 0.55 : 1 }}>
                <div style={{ minWidth: 110, color: '#FFF', fontWeight: 700 }}>{t.startTime}–{t.endTime}</div>
                <div style={{ flex: 1, minWidth: 200, fontSize: 13, color: 'var(--text-secondary)' }}>
                  <strong style={{ color: '#FFF' }}>{t.workerName}</strong> ({t.workerId}) · {t.locationName}
                  <div style={{ fontSize: 11.5, color: 'var(--text-muted)' }}>{t.department}{t.active === false ? ' · Paused' : ' · Every day'}</div>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={() => startEdit('template', t)} title="Edit routine"><Pencil size={14} /> Edit</button>
                <button className="btn btn-secondary btn-sm" onClick={() => handleTogglePause(t)}>{t.active === false ? 'Resume' : 'Pause'}</button>
                <button className="btn btn-secondary btn-sm" onClick={() => handleDeleteTemplate(t)} title="Stop routine"><Trash2 size={14} /></button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Timetable */}
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10, marginBottom: 14 }}>
          <h3 style={{ fontSize: '15px', fontWeight: 800, color: '#FFF' }}>Timetable ({visibleSlots.length})</h3>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <input type="date" className="input-field" style={{ width: 'auto' }} value={filterDate} onChange={(e) => setFilterDate(e.target.value)} />
            <button className="btn btn-secondary btn-sm" onClick={() => setFilterDate('')}>All dates</button>
          </div>
        </div>

        {visibleSlots.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No slots for this selection. Assign one using the form above.</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ textAlign: 'left', color: 'var(--text-muted)', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                  <th style={{ padding: '8px 10px' }}>Date</th>
                  <th style={{ padding: '8px 10px' }}>Time</th>
                  <th style={{ padding: '8px 10px' }}>Worker</th>
                  <th style={{ padding: '8px 10px' }}>Department</th>
                  <th style={{ padding: '8px 10px' }}>Location</th>
                  <th style={{ padding: '8px 10px' }}>Status</th>
                  <th style={{ padding: '8px 10px' }} />
                </tr>
              </thead>
              <tbody>
                {visibleSlots.map((s) => (
                  <tr key={s.id} style={{ borderTop: '1px solid var(--glass-border-subtle)', color: 'var(--text-secondary)' }}>
                    <td style={{ padding: '10px' }}>{s.date}{s.templateId && <Repeat size={11} style={{ marginLeft: 6, opacity: 0.6 }} />}</td>
                    <td style={{ padding: '10px', color: '#FFF', fontWeight: 700 }}>{s.startTime}–{s.endTime}</td>
                    <td style={{ padding: '10px' }}>{s.workerName} <span style={{ color: 'var(--text-muted)' }}>({s.workerId})</span></td>
                    <td style={{ padding: '10px' }}>{s.department}</td>
                    <td style={{ padding: '10px' }}>{s.locationName}</td>
                    <td style={{ padding: '10px' }}>
                      <span style={{ color: statusColor(s.status), fontWeight: 700, display: 'inline-flex', gap: 5, alignItems: 'center' }}>
                        {s.status === 'Completed' && <CheckCircle2 size={14} />} {s.status}
                      </span>
                    </td>
                    <td style={{ padding: '10px', textAlign: 'right' }}>
                      {s.status !== 'Completed' && (
                        <div style={{ display: 'inline-flex', gap: 6 }}>
                          <button className="btn btn-secondary btn-sm" onClick={() => startEdit('slot', s)} title="Edit this day slot">
                            <Pencil size={14} />
                          </button>
                          <button className="btn btn-secondary btn-sm" onClick={() => handleDelete(s)} title="Remove slot">
                            <Trash2 size={14} />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Citizen confirmations */}
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <h3 style={{ fontSize: '15px', fontWeight: 800, color: '#FFF', marginBottom: 12 }}>
          Resident confirmations (QR scans) ({collectionLogs.length})
        </h3>
        {collectionLogs.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No collection has been confirmed by a resident yet.</p>
        ) : (
          <div style={{ display: 'grid', gap: 8 }}>
            {collectionLogs.slice(0, 20).map((l) => (
              <div key={l.id} style={{ display: 'flex', justifyContent: 'space-between', gap: 10, flexWrap: 'wrap', padding: '10px 12px', borderRadius: 10, background: 'rgba(0,0,0,0.25)', fontSize: 12.5, color: 'var(--text-secondary)' }}>
                <span><strong style={{ color: '#FFF' }}>{l.citizenName || 'Resident'}</strong> confirmed <strong style={{ color: '#34D399' }}>{l.workerName}</strong> at {l.locationName}</span>
                <span style={{ color: 'var(--text-muted)' }}>
                  {l.confirmedAt?.seconds ? new Date(l.confirmedAt.seconds * 1000).toLocaleString() : l.date}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
