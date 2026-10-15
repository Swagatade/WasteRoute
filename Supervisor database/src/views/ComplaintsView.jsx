import React, { useState } from 'react';
import { 
  Search, 
  Filter, 
  MapPin, 
  User, 
  Phone, 
  Calendar, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  Truck, 
  Eye, 
  X,
  Send,
  MessageSquare,
  Sparkles
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';

import WorkerPhone from '../components/WorkerPhone';
import { assignedWorkerPhone, notifiedWorkerContacts } from '../utils/workerContact';

export default function ComplaintsView({ 
  complaints, 
  workers, 
  selectedWard, 
  onUpdateComplaintStatus, 
  onAssignWorker,
  selectedComplaint: selectedFromParent,
  onSelectComplaint
}) {
  // Always show the live Firestore version of the open ticket (stage, truck progress, proof photo)
  const selectedComplaint = selectedFromParent
    ? (complaints.find(c => c.id === selectedFromParent.id) || selectedFromParent)
    : null;
  const citizenOf = (c) => c.citizen || {};
  const timeAgo = (c) => {
    if (!c.createdAtMs) return c.reportedAt || '';
    const m = Math.max(0, Math.round((Date.now() - c.createdAtMs) / 60000));
    if (m < 1) return 'just now';
    if (m < 60) return `${m} min ago`;
    if (m < 1440) return `${Math.floor(m / 60)} h ago`;
    return new Date(c.createdAtMs).toLocaleDateString();
  };
  const hasRealPhoto = (u) => typeof u === 'string' && u.startsWith('data:image');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [priorityFilter, setPriorityFilter] = useState('All');
  const [selectedWorkerForAssign, setSelectedWorkerForAssign] = useState('');
  const [supervisorNote, setSupervisorNote] = useState('');

  // Filter complaints
  // Solved tickets leave this list automatically (they stay in the citizen's and worker's history).
  const isSolved = (c) => c.status === 'Resolved' || c.stage === 'resolved';
  const openComplaints = complaints.filter(cmp => !cmp.isMock && !isSolved(cmp));
  const filteredComplaints = openComplaints.filter(cmp => {
    const matchesWard = selectedWard === 'all' || cmp.wardId === selectedWard;
    const matchesStatus = statusFilter === 'All' || cmp.status === statusFilter;
    const matchesPriority = priorityFilter === 'All' || cmp.priority === priorityFilter;
    const matchesSearch = searchTerm === '' || 
      (cmp.ticketNo || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (cmp.category || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (cmp.location || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
      (citizenOf(cmp).name || '').toLowerCase().includes(searchTerm.toLowerCase());

    return matchesWard && matchesStatus && matchesPriority && matchesSearch;
  });

  const handleAssign = (complaintId) => {
    if (!selectedWorkerForAssign) return;
    const worker = workers.find(w => w.id === selectedWorkerForAssign);
    if (worker) {
      onAssignWorker(complaintId, worker.id, `${worker.name} (${worker.vehicle})`);
      setSelectedWorkerForAssign('');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Header & Filter Controls Bar */}
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px', flexWrap: 'wrap', gap: '14px' }}>
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 800 }}>Complaint Review & Management</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              Live citizen-reported tickets. Nearby workers are called automatically; a ticket disappears from here once the worker solves it.
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
              Showing {filteredComplaints.length} of {openComplaints.length} open tickets
            </span>
          </div>
        </div>

        {/* Filters and Search Row */}
        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', alignItems: 'center' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', flex: '1 1 280px' }}>
            <Search size={16} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
            <input 
              type="text"
              placeholder="Search by ticket #, category, citizen or location..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input-field"
              style={{ paddingLeft: '40px', paddingRight: '14px', height: '42px' }}
            />
          </div>

          {/* Status Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600 }}>Status:</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid var(--glass-border)',
                borderRadius: '8px',
                padding: '8px 12px',
                color: 'var(--text-primary)',
                fontSize: '13px',
                outline: 'none',
                height: '42px',
                cursor: 'pointer'
              }}
            >
              <option value="All" style={{ background: '#0D1322' }}>All Statuses</option>
              <option value="Pending" style={{ background: '#0D1322' }}>Pending</option>
              <option value="Under Review" style={{ background: '#0D1322' }}>Under Review</option>
              <option value="Assigned" style={{ background: '#0D1322' }}>Assigned</option>
              <option value="In Progress" style={{ background: '#0D1322' }}>In Progress</option>
              </select>
          </div>

          {/* Priority Filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600 }}>Priority:</span>
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              style={{
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid var(--glass-border)',
                borderRadius: '8px',
                padding: '8px 12px',
                color: 'var(--text-primary)',
                fontSize: '13px',
                outline: 'none',
                height: '42px',
                cursor: 'pointer'
              }}
            >
              <option value="All" style={{ background: '#0D1322' }}>All Priorities</option>
              <option value="Critical" style={{ background: '#0D1322' }}>Critical</option>
              <option value="High" style={{ background: '#0D1322' }}>High</option>
              <option value="Medium" style={{ background: '#0D1322' }}>Medium</option>
              <option value="Low" style={{ background: '#0D1322' }}>Low</option>
            </select>
          </div>
        </div>
      </div>

      {/* Complaints Data Table View */}
      <div className="glass-panel" style={{ padding: '0', overflowX: 'auto' }}>
        <table style={{ width: '100%', minWidth: '820px', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13.5px' }}>
          <thead>
            <tr style={{ background: 'rgba(255, 255, 255, 0.03)', borderBottom: '1px solid var(--glass-border)' }}>
              <th style={{ padding: '16px 20px', color: 'var(--text-muted)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Ticket Info</th>
              <th style={{ padding: '16px 20px', color: 'var(--text-muted)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Category & Priority</th>
              <th style={{ padding: '16px 20px', color: 'var(--text-muted)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Location & Ward</th>
              <th style={{ padding: '16px 20px', color: 'var(--text-muted)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Citizen Info</th>
              <th style={{ padding: '16px 20px', color: 'var(--text-muted)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Status</th>
              <th style={{ padding: '16px 20px', color: 'var(--text-muted)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase' }}>Assigned Fleet</th>
              <th style={{ padding: '16px 20px', color: 'var(--text-muted)', fontWeight: 600, fontSize: '12px', textTransform: 'uppercase', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredComplaints.length === 0 ? (
              <tr>
                <td colSpan={7} style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  No complaints match the specified filters.
                </td>
              </tr>
            ) : (
              filteredComplaints.map((cmp) => (
                <tr 
                  key={cmp.id}
                  style={{
                    borderBottom: '1px solid var(--glass-border-subtle)',
                    transition: 'background 0.2s',
                    cursor: 'pointer'
                  }}
                  onClick={() => onSelectComplaint(cmp)}
                  onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255, 255, 255, 0.03)'}
                  onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}
                >
                  <td style={{ padding: '16px 20px' }}>
                    <div style={{ fontWeight: 700, color: '#FFF', fontFamily: 'var(--font-mono)' }}>
                      {cmp.ticketNo}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      {timeAgo(cmp)}
                    </div>
                  </td>

                  <td style={{ padding: '16px 20px' }}>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {cmp.category}
                    </div>
                    <span style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      color: cmp.priority === 'Critical' ? '#EF4444' : cmp.priority === 'High' ? '#F59E0B' : '#10B981'
                    }}>
                      ● {cmp.priority} Priority
                    </span>
                  </td>

                  <td style={{ padding: '16px 20px' }}>
                    <div style={{ color: 'var(--text-primary)', maxWidth: '220px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {cmp.location}
                    </div>
                    {cmp.wardName && (
                      <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                        {cmp.wardName}
                      </div>
                    )}
                  </td>

                  <td style={{ padding: '16px 20px' }}>
                    <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {citizenOf(cmp).name || 'Citizen'}
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      {citizenOf(cmp).phone || citizenOf(cmp).email || ''}
                    </div>
                  </td>

                  <td style={{ padding: '16px 20px' }}>
                    <StatusBadge status={cmp.status} />
                  </td>

                  <td style={{ padding: '16px 20px' }}>
                    {cmp.assignedWorkerName ? (
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#34D399', fontSize: '12.5px', fontWeight: 600 }}>
                        <Truck size={14} />
                        <span>{cmp.assignedWorkerName}</span>
                        <WorkerPhone phone={assignedWorkerPhone(workers, cmp)} style={{ marginLeft: 6 }} />
                      </div>
                    ) : (
                      <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                        Unassigned
                      </span>
                    )}
                  </td>

                  <td style={{ padding: '16px 20px', textAlign: 'right' }}>
                    <button 
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelectComplaint(cmp);
                      }}
                      className="btn btn-secondary btn-sm"
                    >
                      <Eye size={14} />
                      <span>Inspect</span>
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Complaint Detail Modal / Drawer */}
      {selectedComplaint && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100,
          padding: '24px'
        }}>
          <div className="glass-panel animate-fade-in" style={{
            width: '100%',
            maxWidth: '680px',
            maxHeight: '90vh',
            overflowY: 'auto',
            padding: '28px',
            borderRadius: '24px'
          }}>
            {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '20px' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <h3 style={{ fontSize: '20px', fontWeight: 800 }}>{selectedComplaint.category}</h3>
                  <StatusBadge status={selectedComplaint.status} />
                </div>
                <p style={{ fontSize: '12px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', marginTop: '2px' }}>
                  Ticket No: {selectedComplaint.ticketNo} • Reported {timeAgo(selectedComplaint)}
                </p>
              </div>
              <button 
                onClick={() => onSelectComplaint(null)}
                style={{
                  background: 'rgba(255, 255, 255, 0.08)',
                  border: 'none',
                  borderRadius: '50%',
                  width: '32px',
                  height: '32px',
                  color: '#FFF',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <X size={18} />
              </button>
            </div>

            {/* Before Photo & Location Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '18px', marginBottom: '20px' }}>
              <div>
                <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '6px' }}>
                  Citizen Photo Evidence (Before):
                </span>
                {hasRealPhoto(selectedComplaint.photoBefore) ? (
                  <img src={selectedComplaint.photoBefore} alt="Citizen report"
                    style={{ width: '100%', height: '200px', objectFit: 'cover', borderRadius: '14px', border: '1px solid var(--glass-border)' }} />
                ) : (
                  <div style={{ height: '200px', borderRadius: '14px', border: '1px dashed var(--glass-border)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: '12px' }}>
                    No photo uploaded
                  </div>
                )}

                {hasRealPhoto(selectedComplaint.cleaningProof?.photoAfter) && (
                  <>
                    <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--emerald-400)', display: 'block', margin: '12px 0 6px' }}>
                      Proof of Resolution (After) - by {selectedComplaint.cleaningProof.workerName || 'worker'}:
                    </span>
                    <img
                      src={selectedComplaint.cleaningProof.photoAfter}
                      alt="Resolution proof"
                      style={{ width: '100%', height: '200px', objectFit: 'cover', borderRadius: '14px', border: '1px solid var(--glass-border)' }}
                    />
                  </>
                )}

                <div style={{ marginTop: '12px', background: 'rgba(255, 255, 255, 0.03)', padding: '12px', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Task progress:</span>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: '#FFF', marginTop: '2px' }}>
                    {(selectedComplaint.stage || 'reported').replace(/_/g, ' ')} - {selectedComplaint.progress ?? 0}%
                    {selectedComplaint.truckName ? ` - 🚛 ${selectedComplaint.truckName}` : ''}
                  </div>
                  <div style={{ height: '6px', borderRadius: '6px', background: 'rgba(255,255,255,0.08)', marginTop: '6px', overflow: 'hidden' }}>
                    <div style={{ width: `${selectedComplaint.progress ?? 0}%`, height: '100%', background: 'var(--emerald-400)' }} />
                  </div>
                  <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '6px' }}>
                    📷 {(hasRealPhoto(selectedComplaint.photoBefore) ? 1 : 0) + (hasRealPhoto(selectedComplaint.cleaningProof?.photoAfter) ? 1 : 0)} photo(s)
                    {Array.isArray(selectedComplaint.notifiedWorkerNames) && selectedComplaint.notifiedWorkerNames.length > 0
                      ? ` - 📞 Called automatically: ${selectedComplaint.notifiedWorkerNames.join(', ')}` : ''}
                    {selectedComplaint.assignedWorkerName ? ` - Worker: ${selectedComplaint.assignedWorkerName}` : ' - not accepted yet'}
                  </div>
                  {selectedComplaint.assignedWorkerName && (
                    <div style={{ marginTop: '4px' }}>
                      <WorkerPhone phone={assignedWorkerPhone(workers, selectedComplaint)} />
                    </div>
                  )}
                  {!selectedComplaint.assignedWorkerName && notifiedWorkerContacts(workers, selectedComplaint).map((c, i) => (
                    <div key={i} style={{ marginTop: '4px', fontSize: '11.5px', color: 'var(--text-secondary)' }}>
                      {c.name}: <WorkerPhone phone={c.phone} />
                    </div>
                  ))}
                  {Array.isArray(selectedComplaint.timeline) && selectedComplaint.timeline.map((e, i) => (
                    <div key={i} style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                      • {e.label} ({new Date(e.atMs).toLocaleTimeString()})
                    </div>
                  ))}
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Location:</span>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: '#FFF', marginTop: '2px' }}>
                    📍 {selectedComplaint.location}
                  </div>
                  {selectedComplaint.coordinates ? (
                    <div style={{ fontSize: '11px', color: 'var(--emerald-400)', marginTop: '2px' }}>
                      GPS: {Number(selectedComplaint.coordinates.lat).toFixed(5)}, {Number(selectedComplaint.coordinates.lng).toFixed(5)}
                    </div>
                  ) : null}
                </div>

                <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '12px', borderRadius: '12px', border: '1px solid var(--glass-border)' }}>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Citizen Reporter:</span>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: '#FFF', marginTop: '2px' }}>
                    👤 {citizenOf(selectedComplaint).name || 'Citizen'}
                  </div>
                  {(citizenOf(selectedComplaint).phone || citizenOf(selectedComplaint).email) && (
                    <div style={{ fontSize: '11.5px', color: 'var(--text-secondary)', marginTop: '2px' }}>
                      {citizenOf(selectedComplaint).phone ? '📞' : '✉️'} {citizenOf(selectedComplaint).phone || citizenOf(selectedComplaint).email}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Description */}
            <div style={{ marginBottom: '20px' }}>
              <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>
                Grievance Description:
              </span>
              <p style={{ fontSize: '13.5px', color: 'var(--text-primary)', background: 'rgba(0,0,0,0.3)', padding: '12px', borderRadius: '10px' }}>
                {selectedComplaint.description ? `"${selectedComplaint.description}"` : 'No description provided by the citizen.'}
              </p>
            </div>

            {/* Automatic dispatch: supervisors only monitor. Nearby workers are called by the app. */}
            <div style={{ borderTop: '1px solid var(--glass-border)', paddingTop: '18px' }}>
              <div style={{ background: 'rgba(16, 185, 129, 0.08)', border: '1px solid rgba(16, 185, 129, 0.25)', borderRadius: '12px', padding: '14px' }}>
                <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--emerald-400)' }}>Automatic dispatch (no manual assignment)</div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '6px', lineHeight: 1.6 }}>
                  {selectedComplaint.stage === 'resolved' || selectedComplaint.status === 'Resolved'
                    ? `Solved by ${selectedComplaint.assignedWorkerName || 'a worker'} - proof photo uploaded.`
                    : selectedComplaint.assignedWorkerName
                      ? `${selectedComplaint.assignedWorkerName} accepted this task and is handling it (${(selectedComplaint.stage || '').replace(/_/g, ' ')}).`
                      : Array.isArray(selectedComplaint.notifiedWorkerNames) && selectedComplaint.notifiedWorkerNames.length > 0
                        ? `Nearby workers are being called on their phones: ${selectedComplaint.notifiedWorkerNames.join(', ')}. Waiting for the first one to accept.`
                        : 'No on-duty worker nearby yet. Workers within 5 km are called automatically as soon as they are online.'}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
