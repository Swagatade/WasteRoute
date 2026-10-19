import React, { useMemo, useState } from 'react';
import { Route as RouteIcon, Plus, ArrowUp, ArrowDown, X, AlertCircle, CheckCircle2, Trash, Home, Users } from 'lucide-react';
import { MUNICIPAL_WARDS, DEFAULT_ZONES } from '../data/mockData';
import { createRoute, toDateString } from '../firebase/firestoreService';

const DEPARTMENTS = MUNICIPAL_WARDS.filter((w) => w.id !== 'all');

const binLabel = (b) => b.name || b.location || b.code || b.id;
const binLat = (b) => Number(b.coordinates?.lat ?? b.lat);
const binLng = (b) => Number(b.coordinates?.lng ?? b.lng);

const labelStyle = { display: 'block', fontSize: 12, fontWeight: 600, color: 'var(--text-muted)', marginBottom: 6 };

export default function RoutesView({ workers, bins, households, schedules, templates, onEnsureSlots }) {
  const [workerId, setWorkerId] = useState('');
  const [routeName, setRouteName] = useState('');
  const [departmentId, setDepartmentId] = useState(DEPARTMENTS[0]?.id || '');
  const [startDate, setStartDate] = useState(toDateString());
  const [startTime, setStartTime] = useState('08:30');
  const [minutesPerStop, setMinutesPerStop] = useState(30);
  const [stops, setStops] = useState([]); // ordered
  const [customName, setCustomName] = useState('');
  const [customLat, setCustomLat] = useState('');
  const [customLng, setCustomLng] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const today = toDateString();
  const worker = workers.find((w) => w.id === workerId) || null;
  const stopKey = (type, id) => `${type}:${id}`;
  const picked = new Set(stops.map((s) => s.key));

  // Only the selected zone's bins and hostels are offered (Men's hostels zone -> only men's hostels, etc.)
  const zoneBins = bins.filter((b) => (b.wardId || b.areaId) === departmentId);
  const zoneHomes = households.filter((h) => h.areaId === departmentId);
  const zoneDef = DEFAULT_ZONES.find((z) => z.id === departmentId);
  const showHostels = zoneDef?.kind === 'hostel' || zoneHomes.length > 0;
  const changeZone = (id) => {
    setDepartmentId(id);
    // stops that belong to another zone are dropped from the route being built
    setStops((prev) => prev.filter((s) => !s.areaId || s.areaId === id));
  };

  const toggleStop = (stop) =>
    setStops((prev) => (prev.some((s) => s.key === stop.key) ? prev.filter((s) => s.key !== stop.key) : [...prev, stop]));

  const move = (i, dir) =>
    setStops((prev) => {
      const j = i + dir;
      if (j < 0 || j >= prev.length) return prev;
      const next = prev.slice();
      [next[i], next[j]] = [next[j], next[i]];
      return next;
    });

  const addCustom = () => {
    const lat = parseFloat(customLat);
    const lng = parseFloat(customLng);
    if (!customName.trim() || !Number.isFinite(lat) || !Number.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
      return setError('A custom stop needs a name and valid latitude / longitude.');
    }
    setError(null);
    setStops((prev) => [...prev, { key: stopKey('custom', Date.now()), locationName: customName.trim(), lat, lng, stopType: 'bin' }]);
    setCustomName(''); setCustomLat(''); setCustomLng('');
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    setError(null);
    if (!worker) return setError('Choose a registered worker first.');
    if (!routeName.trim()) return setError('Give the route a name.');
    if (stops.length === 0) return setError('Pick at least one bin or hostel building for the route.');
    if (!(minutesPerStop >= 5)) return setError('Allow at least 5 minutes per stop.');
    const department = DEPARTMENTS.find((d) => d.id === departmentId);
    setBusy(true);
    try {
      await createRoute({
        routeName: routeName.trim(),
        worker,
        department: department?.name || '',
        departmentId,
        startDate,
        startTime,
        minutesPerStop: Number(minutesPerStop),
        stops: stops.map(({ locationName, lat, lng, stopType, householdId }) => ({ locationName, lat, lng, stopType, householdId }))
      });
      if (onEnsureSlots) onEnsureSlots();
      setStops([]);
      setRouteName('');
    } catch (err) {
      setError(`Could not create the route: ${err.message}`);
    } finally {
      setBusy(false);
    }
  };

  // Routes = templates grouped by routeId
  const routes = useMemo(() => {
    const map = new Map();
    templates.filter((t) => t.routeId).forEach((t) => {
      if (!map.has(t.routeId)) map.set(t.routeId, []);
      map.get(t.routeId).push(t);
    });
    return [...map.entries()].map(([id, list]) => ({
      id,
      name: list[0].routeName,
      workerName: list[0].workerName,
      workerId: list[0].workerId,
      stops: list.slice().sort((a, b) => (a.stopOrder || 0) - (b.stopOrder || 0))
    }));
  }, [templates]);

  const slotToday = (t) => schedules.find((s) => s.templateId === t.id && s.date === today);

  // Live progress: every stop scheduled for today, per worker (a stop completes by camera photo or warden QR scan)
  const todaySlots = schedules.filter((s) => s.date === today);
  const doneSlots = todaySlots.filter((s) => s.status === 'Completed').length;
  const perWorker = [...new Set(todaySlots.map((s) => s.workerId))].map((id) => {
    const mine = todaySlots.filter((s) => s.workerId === id);
    return { id, name: mine[0].workerName || id, total: mine.length, done: mine.filter((s) => s.status === 'Completed').length, current: mine.find((s) => s.status === 'In Progress') };
  });

  const selectAllHostels = () =>
    setStops((prev) => {
      const have = new Set(prev.map((s) => s.key));
      const add = zoneHomes
        .map((h) => ({ key: stopKey('household', h.id), locationName: `${h.name} (${h.code})`, lat: Number(h.lat), lng: Number(h.lng), stopType: 'household', householdId: h.id, areaId: h.areaId }))
        .filter((s) => Number.isFinite(s.lat) && Number.isFinite(s.lng) && !have.has(s.key));
      return [...prev, ...add];
    });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <h2 style={{ fontSize: 20, fontWeight: 800, color: '#FFF', display: 'flex', alignItems: 'center', gap: 10 }}>
          <RouteIcon size={22} color="var(--cyan-400)" /> Route Planner
        </h2>
        <p style={{ fontSize: 12.5, color: 'var(--text-secondary)', marginTop: 6, maxWidth: 780 }}>
          Build a route for one worker: pick the bins and hostel blocks in the order they should be collected. It repeats every day and
          appears on the worker's phone as <strong>Today's Route</strong>. A bin is cleared with a camera photo; a hostel block is completed
          when its warden scans the worker's QR code.
        </p>
      </div>

      <div className="glass-panel" style={{ padding: '18px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', flexWrap: 'wrap', gap: 8 }}>
          <h3 style={{ fontSize: 15, fontWeight: 800, color: '#FFF', display: 'flex', alignItems: 'center', gap: 8 }}>
            <Users size={16} color="var(--cyan-400)" /> Today's progress
          </h3>
          <span style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
            <strong style={{ color: '#34D399' }}>{doneSlots}</strong> of {todaySlots.length} stops collected
          </span>
        </div>
        <div style={{ height: 12, borderRadius: 6, background: 'rgba(255,255,255,0.08)', overflow: 'hidden', margin: '10px 0 14px' }}>
          <div style={{ width: `${todaySlots.length ? (doneSlots / todaySlots.length) * 100 : 0}%`, height: '100%', background: 'linear-gradient(90deg,#22D3EE,#34D399)', transition: 'width .5s' }} />
        </div>
        {perWorker.length === 0 ? (
          <p style={{ fontSize: 12.5, color: 'var(--text-muted)' }}>No stops are scheduled for today yet.</p>
        ) : (
          <div style={{ display: 'grid', gap: 10 }}>
            {perWorker.map((w) => (
              <div key={w.id}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12.5, color: 'var(--text-secondary)', marginBottom: 4 }}>
                  <span><strong style={{ color: '#FFF' }}>{w.name}</strong> ({w.id}){w.current ? ` · now at ${w.current.locationName}` : ''}</span>
                  <span>{w.done} / {w.total} · {Math.round((w.done / w.total) * 100)}%</span>
                </div>
                <div style={{ height: 8, borderRadius: 4, background: 'rgba(255,255,255,0.08)', overflow: 'hidden' }}>
                  <div style={{ width: `${(w.done / w.total) * 100}%`, height: '100%', background: '#34D399', transition: 'width .5s' }} />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: 20, alignItems: 'start' }}>
        <form onSubmit={handleCreate} className="glass-panel" style={{ padding: 22, display: 'flex', flexDirection: 'column', gap: 14 }}>
          <h3 style={{ fontSize: 15, fontWeight: 800, color: '#FFF' }}>New route</h3>

          {error && (
            <div style={{ display: 'flex', gap: 8, padding: '10px 12px', borderRadius: 10, background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.4)', color: '#FCA5A5', fontSize: 12.5 }}>
              <AlertCircle size={16} style={{ flexShrink: 0, marginTop: 1 }} /> <div>{error}</div>
              <button type="button" onClick={() => setError(null)} style={{ marginLeft: 'auto', background: 'none', border: 'none', color: 'inherit', cursor: 'pointer' }}><X size={14} /></button>
            </div>
          )}

          <div>
            <label style={labelStyle}>Worker</label>
            <select className="input-field" value={workerId} onChange={(e) => setWorkerId(e.target.value)}>
              <option value="" style={{ background: '#0D1322' }}>{workers.length ? 'Select a registered worker…' : 'No workers registered yet'}</option>
              {workers.map((w) => <option key={w.id} value={w.id} style={{ background: '#0D1322' }}>{w.name} ({w.id})</option>)}
            </select>
          </div>

          <div>
            <label style={labelStyle}>Route name</label>
            <input className="input-field" placeholder="e.g. Morning hostel round" value={routeName} onChange={(e) => setRouteName(e.target.value)} />
          </div>

          <div>
            <label style={labelStyle}>Zone</label>
            <select className="input-field" value={departmentId} onChange={(e) => changeZone(e.target.value)}>
              {DEPARTMENTS.map((d) => <option key={d.id} value={d.id} style={{ background: '#0D1322' }}>{d.name}</option>)}
            </select>
          </div>

          <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
            <div style={{ flex: '1 1 130px' }}>
              <label style={labelStyle}>Starting from</label>
              <input type="date" className="input-field" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            </div>
            <div style={{ flex: '1 1 90px' }}>
              <label style={labelStyle}>First stop at</label>
              <input type="time" className="input-field" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
            </div>
            <div style={{ flex: '1 1 90px' }}>
              <label style={labelStyle}>Min / stop</label>
              <input type="number" min="5" step="5" className="input-field" value={minutesPerStop} onChange={(e) => setMinutesPerStop(e.target.value)} />
            </div>
          </div>

          <div>
            <label style={labelStyle}>Smart bins in this zone ({zoneBins.length})</label>
            <div style={{ display: 'grid', gap: 6, maxHeight: 180, overflowY: 'auto' }}>
              {zoneBins.length === 0 && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>No smart bins in this zone.</span>}
              {zoneBins.map((b) => {
                const stop = { key: stopKey('bin', b.id), locationName: binLabel(b), lat: binLat(b), lng: binLng(b), stopType: 'bin', areaId: b.wardId || b.areaId };
                const ok = Number.isFinite(stop.lat) && Number.isFinite(stop.lng);
                return (
                  <label key={b.id} style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 12.5, color: ok ? 'var(--text-secondary)' : 'var(--text-muted)' }}>
                    <input type="checkbox" disabled={!ok} checked={picked.has(stop.key)} onChange={() => toggleStop(stop)} />
                    <Trash size={13} /> {binLabel(b)}
                  </label>
                );
              })}
            </div>
          </div>

          {showHostels && (
          <div>
            <label style={{ ...labelStyle, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>Hostels in this zone ({zoneHomes.length})</span>
              {zoneHomes.length > 0 && (
                <button type="button" className="btn btn-secondary btn-sm" onClick={selectAllHostels}>Add all hostels to the route</button>
              )}
            </label>
            <div style={{ display: 'grid', gap: 6, maxHeight: 180, overflowY: 'auto' }}>
              {zoneHomes.length === 0 && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>No hostels registered in this zone yet.</span>}
              {zoneHomes.map((h) => {
                const stop = { key: stopKey('household', h.id), locationName: `${h.name} (${h.code})`, lat: Number(h.lat), lng: Number(h.lng), stopType: 'household', householdId: h.id, areaId: h.areaId };
                return (
                  <label key={h.id} style={{ display: 'flex', gap: 8, alignItems: 'center', fontSize: 12.5, color: 'var(--text-secondary)' }}>
                    <input type="checkbox" checked={picked.has(stop.key)} onChange={() => toggleStop(stop)} />
                    <Home size={13} /> {h.name} <span style={{ color: 'var(--text-muted)' }}>· {h.code} · {h.address}</span>
                  </label>
                );
              })}
            </div>
          </div>
          )}

          <div>
            <label style={labelStyle}>Custom stop</label>
            <input className="input-field" placeholder="Location name" value={customName} onChange={(e) => setCustomName(e.target.value)} />
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <input className="input-field" placeholder="Latitude" value={customLat} onChange={(e) => setCustomLat(e.target.value)} />
              <input className="input-field" placeholder="Longitude" value={customLng} onChange={(e) => setCustomLng(e.target.value)} />
              <button type="button" className="btn btn-secondary" onClick={addCustom}><Plus size={16} /></button>
            </div>
          </div>

          <button type="submit" className="btn btn-primary" disabled={busy} style={{ gap: 8 }}>
            <Plus size={16} /> {busy ? 'Creating…' : `Create route (${stops.length} stops)`}
          </button>
        </form>

        <div className="glass-panel" style={{ padding: 22, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <h3 style={{ fontSize: 15, fontWeight: 800, color: '#FFF' }}>Stop order ({stops.length})</h3>
          {stops.length === 0 ? (
            <p style={{ fontSize: 12.5, color: 'var(--text-muted)' }}>Tick bins and hostel buildings on the left. They appear here in the order the worker will visit them.</p>
          ) : (
            <div style={{ display: 'grid', gap: 8 }}>
              {stops.map((s, i) => (
                <div key={s.key} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 10px', borderRadius: 10, background: 'rgba(0,0,0,0.25)' }}>
                  <div style={{ width: 26, height: 26, borderRadius: 13, background: 'rgba(56,189,248,0.2)', color: '#38BDF8', fontWeight: 800, fontSize: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>{i + 1}</div>
                  <div style={{ flex: 1, fontSize: 13, color: '#FFF' }}>
                    {s.locationName}
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{s.stopType === 'household' ? 'Hostel building · resident QR' : 'Bin · camera photo'}</div>
                  </div>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => move(i, -1)} disabled={i === 0}><ArrowUp size={14} /></button>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => move(i, 1)} disabled={i === stops.length - 1}><ArrowDown size={14} /></button>
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => toggleStop(s)}><X size={14} /></button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <h3 style={{ fontSize: 15, fontWeight: 800, color: '#FFF', marginBottom: 12 }}>Active routes ({routes.length})</h3>
        {routes.length === 0 ? (
          <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>No routes yet. Build one above.</p>
        ) : (
          <div style={{ display: 'grid', gap: 14 }}>
            {routes.map((r) => {
              const done = r.stops.filter((t) => slotToday(t)?.status === 'Completed').length;
              const pct = Math.round((done / r.stops.length) * 100);
              return (
                <div key={r.id} style={{ padding: 14, borderRadius: 12, background: 'rgba(0,0,0,0.25)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
                    <div>
                      <div style={{ color: '#FFF', fontWeight: 800 }}>{r.name}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{r.workerName} ({r.workerId}) · every day · {done} / {r.stops.length} collected today</div>
                    </div>
                  </div>
                  <div style={{ height: 8, borderRadius: 4, background: 'rgba(255,255,255,0.08)', overflow: 'hidden', margin: '10px 0' }}>
                    <div style={{ width: `${pct}%`, height: '100%', background: '#34D399', transition: 'width .4s' }} />
                  </div>
                  <div style={{ display: 'grid', gap: 4 }}>
                    {r.stops.map((t) => {
                      const slot = slotToday(t);
                      const finished = slot?.status === 'Completed';
                      return (
                        <div key={t.id} style={{ display: 'flex', gap: 10, alignItems: 'center', fontSize: 12.5, color: 'var(--text-secondary)' }}>
                          <span style={{ width: 22, color: 'var(--text-muted)' }}>{t.stopOrder}.</span>
                          <span style={{ width: 96, color: '#FFF', fontWeight: 700 }}>{t.startTime}–{t.endTime}</span>
                          <span style={{ flex: 1 }}>{t.locationName} <span style={{ color: 'var(--text-muted)' }}>· {t.stopType === 'household' ? 'hostel' : 'bin'}</span></span>
                          <span style={{ color: finished ? '#34D399' : slot?.status === 'In Progress' ? '#FBBF24' : '#38BDF8', fontWeight: 700, display: 'inline-flex', gap: 4, alignItems: 'center' }}>
                            {finished && <CheckCircle2 size={13} />} {slot?.status || 'Scheduled'}
                          </span>
                          {finished && slot?.proofPhoto && (
                            <a href={slot.proofPhoto} target="_blank" rel="noreferrer" style={{ color: '#38BDF8', fontSize: 11.5 }}>photo</a>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
