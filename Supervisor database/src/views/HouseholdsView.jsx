import React, { useEffect, useRef, useState } from 'react';
import { Home, CheckCircle2, Clock, Users, AlertCircle, ShieldCheck, Plus } from 'lucide-react';
import { DEFAULT_ZONES, HOSTEL_NAME_SUGGESTIONS } from '../data/mockData';
import {
  registerSrmHostels,
  removeMockHouseholds,
  setWorkerAreas,
  toDateString
} from '../firebase/firestoreService';

const HOSTEL_ZONES = DEFAULT_ZONES.filter((z) => z.kind === 'hostel');
const GROUP_TITLE = { boys: "Men's hostels (Boys)", girls: "Women's hostels (Girls)" };

export default function HouseholdsView({ workers, households, loaded, loadError }) {
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState(null);
  const today = toDateString();
  const isDone = (h) => h.collectedDate === today;

  const run = async (fn, okMessage) => {
    setBusy(true);
    setNotice(null);
    try {
      const result = await fn();
      setNotice({ ok: true, text: typeof okMessage === 'function' ? okMessage(result) : okMessage });
    } catch (err) {
      const denied = /permission|insufficient/i.test(err.message || '');
      setNotice({
        ok: false,
        text: denied
          ? 'Firebase refused the write: publish the updated Firestore rules (Firebase console → Firestore → Rules) and try again.'
          : `Could not save: ${err.message}`
      });
    } finally {
      setBusy(false);
    }
  };

  // Sample blocks written by earlier versions are removed once; only real hostels stay.
  const cleaned = useRef(false);
  useEffect(() => {
    if (!loaded || cleaned.current || !households.some((h) => h.isMock)) return;
    cleaned.current = true;
    run(() => removeMockHouseholds(households), 'Removed old sample blocks.');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loaded, households]);

  const register = (zone) =>
    run(() => registerSrmHostels(zone, households), (n) => (n ? `${n} ${GROUP_TITLE[zone.group].toLowerCase()} registered in Firestore.` : 'Already registered.'));

  const toggleArea = (worker, areaId) => {
    const current = worker.assignedAreaIds || [];
    const next = current.includes(areaId) ? current.filter((x) => x !== areaId) : [...current, areaId];
    // A zone belongs to one worker at a time: assigning it here takes it away from anyone else.
    const takeFrom = current.includes(areaId)
      ? []
      : workers.filter((w) => w.id !== worker.id && (w.assignedAreaIds || []).includes(areaId));
    return run(async () => {
      await setWorkerAreas(worker.id, next);
      await Promise.all(takeFrom.map((w) => setWorkerAreas(w.id, (w.assignedAreaIds || []).filter((x) => x !== areaId))));
    }, `${worker.name}: ${next.length} zone(s) assigned.`);
  };

  const doneTotal = households.filter(isDone).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <h2 style={{ fontSize: 20, fontWeight: 800, color: '#FFF', display: 'flex', alignItems: 'center', gap: 10 }}>
          <Home size={22} color="var(--cyan-400)" /> Hostels
        </h2>
        <p style={{ fontSize: 12.5, color: 'var(--text-secondary)', marginTop: 6, maxWidth: 760 }}>
          1. Register the hostels (official SRM list). 2. Tick which workers cover each zone. 3. Each hostel turns Collected when its warden scans the worker's QR code at the block.
          Only supervisors and the assigned workers see this list; a warden sees only their own hostel.
        </p>
        <div style={{ display: 'flex', gap: 10, marginTop: 14, flexWrap: 'wrap' }}>
          <Stat label="Hostels" value={households.length} />
          <Stat label="Collected today" value={doneTotal} color="#34D399" />
          <Stat label="Waiting" value={households.length - doneTotal} color="#FBBF24" />
        </div>
        <div style={{ marginTop: 14 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: 'var(--text-muted)', marginBottom: 6 }}>
            <span>Today's collection progress</span>
            <span style={{ color: '#34D399', fontWeight: 800 }}>{households.length ? Math.round((doneTotal / households.length) * 100) : 0}%</span>
          </div>
          <div style={{ height: 10, borderRadius: 5, background: 'rgba(255,255,255,0.08)', overflow: 'hidden' }}>
            <div style={{ width: `${households.length ? (doneTotal / households.length) * 100 : 0}%`, height: '100%', background: 'linear-gradient(90deg,#22D3EE,#34D399)', transition: 'width .5s' }} />
          </div>
        </div>
        {loadError && (
          <div style={{ marginTop: 12, display: 'flex', gap: 8, padding: '10px 12px', borderRadius: 10, fontSize: 12.5, background: 'rgba(239,68,68,0.12)', border: '1px solid rgba(239,68,68,0.4)', color: '#FCA5A5' }}>
            <AlertCircle size={16} style={{ flexShrink: 0, marginTop: 1 }} />
            <div>
              Firebase is refusing to read the hostels ({String(loadError)}). Publish the updated Firestore rules:
              Firebase console → Firestore Database → Rules → paste the contents of <code>firestore.rules</code> → Publish. The hostels are already saved in Firestore and will appear straight away.
            </div>
          </div>
        )}
        {notice && (
          <div style={{
            marginTop: 12, display: 'flex', gap: 8, padding: '10px 12px', borderRadius: 10, fontSize: 12.5,
            background: notice.ok ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.12)',
            border: `1px solid ${notice.ok ? 'rgba(16,185,129,0.4)' : 'rgba(239,68,68,0.4)'}`,
            color: notice.ok ? '#6EE7B7' : '#FCA5A5'
          }}>
            {notice.ok ? <ShieldCheck size={16} /> : <AlertCircle size={16} />} {notice.text}
          </div>
        )}
      </div>

      {HOSTEL_ZONES.map((zone) => {
        const names = HOSTEL_NAME_SUGGESTIONS[zone.group] || [];
        const mine = households.filter((h) => h.areaId === zone.id);
        const missing = names.filter((n) => !mine.some((h) => String(h.name).trim().toLowerCase() === n.toLowerCase()));
        const done = mine.filter(isDone).length;
        const assigned = workers.filter((w) => (w.assignedAreaIds || []).includes(zone.id));
        return (
          <div key={zone.id} className="glass-panel" style={{ padding: '20px 24px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
              <div>
                <h3 style={{ fontSize: 16, fontWeight: 800, color: '#FFF', display: 'flex', alignItems: 'center', gap: 8 }}>
                  <i style={{ width: 14, height: 14, borderRadius: 4, background: zone.group === 'girls' ? '#EC4899' : '#2563EB', display: 'inline-block' }} />
                  {GROUP_TITLE[zone.group]}
                </h3>
                <div style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
                  {mine.length} of {names.length} registered · {done} collected today
                </div>
              </div>
              {missing.length > 0 && (
                <button className="btn btn-primary" disabled={busy} onClick={() => register(zone)} style={{ gap: 8 }}>
                  <Plus size={16} /> Register {missing.length} {zone.group} hostel{missing.length > 1 ? 's' : ''}
                </button>
              )}
            </div>

            {mine.length > 0 && (
              <>
                <div style={{ height: 8, borderRadius: 4, background: 'rgba(255,255,255,0.08)', overflow: 'hidden', marginTop: 12 }}>
                  <div style={{ width: `${(done / mine.length) * 100}%`, height: '100%', background: '#34D399', transition: 'width .5s' }} />
                </div>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', margin: '14px 0' }}>
                  <span style={{ fontSize: 12, color: 'var(--text-muted)', display: 'inline-flex', alignItems: 'center', gap: 5 }}>
                    <Users size={14} /> Workers for this zone:
                  </span>
                  {workers.length === 0 && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>no workers registered yet</span>}
                  {workers.map((w) => {
                    const on = (w.assignedAreaIds || []).includes(zone.id);
                    return (
                      <button
                        key={w.id}
                        className="btn btn-secondary btn-sm"
                        disabled={busy}
                        onClick={() => toggleArea(w, zone.id)}
                        style={{ borderColor: on ? 'rgba(16,185,129,0.6)' : undefined, background: on ? 'rgba(16,185,129,0.18)' : undefined, color: on ? '#6EE7B7' : undefined }}
                      >
                        {on ? '✓ ' : ''}{w.name}
                      </button>
                    );
                  })}
                  {assigned.length === 0 && workers.length > 0 && <span style={{ fontSize: 11.5, color: '#FBBF24' }}>none assigned</span>}
                </div>

                <div style={{ display: 'grid', gap: 6 }}>
                  {mine.map((h) => (
                    <div key={h.id} style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap', padding: '8px 12px', borderRadius: 10, background: 'rgba(0,0,0,0.25)' }}>
                      {isDone(h) ? <CheckCircle2 size={18} color="#34D399" /> : <Clock size={18} color="#FBBF24" />}
                      <div style={{ flex: 1, minWidth: 160, color: '#FFF', fontWeight: 700, fontSize: 13.5 }}>
                        {h.name} <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>· {h.code}</span>
                        {isDone(h) && h.collectedByName && <span style={{ marginLeft: 8, color: '#6EE7B7', fontWeight: 600, fontSize: 11.5 }}>by {h.collectedByName}</span>}
                      </div>
                      <span style={{ fontSize: 12, fontWeight: 700, color: isDone(h) ? '#34D399' : '#FBBF24' }}>
                        {isDone(h)
                          ? `Collected${h.collectedAtMs ? ' · ' + new Date(h.collectedAtMs).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}`
                          : 'Waiting for warden scan'}
                      </span>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        );
      })}
    </div>
  );
}

function Stat({ label, value, color = '#FFF' }) {
  return (
    <div style={{ padding: '10px 16px', borderRadius: 10, background: 'rgba(0,0,0,0.3)', minWidth: 110 }}>
      <div style={{ fontSize: 22, fontWeight: 800, color }}>{value}</div>
      <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{label}</div>
    </div>
  );
}
