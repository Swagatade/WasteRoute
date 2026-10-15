import React, { useState } from 'react';
import { 
  Users, 
  Truck, 
  BatteryCharging, 
  Star, 
  Phone, 
  CheckCircle2, 
  Clock, 
  MapPin, 
  Send, 
  Radio, 
  AlertCircle,
  UserPlus
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';

import WorkerPhone from '../components/WorkerPhone';
import { workerPhone } from '../utils/workerContact';

export default function WorkersView({ 
  workers, 
  complaints, 
  onUpdateWorkerStatus,
  onAssignWorker,
  onNavigateToRegistration
}) {
  const [statusFilter, setStatusFilter] = useState('All');
  const [selectedWorkerForDispatch, setSelectedWorkerForDispatch] = useState(null);
  const [selectedComplaintId, setSelectedComplaintId] = useState('');
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [broadcastNotice, setBroadcastNotice] = useState(null);

  const filteredWorkers = workers.filter(w => {
    if (statusFilter === 'All') return true;
    return w.status === statusFilter;
  });

  const unassignedComplaints = complaints.filter(c => c.status === 'Pending' || c.status === 'Under Review');

  const handleDispatch = (e) => {
    e.preventDefault();
    if (!selectedWorkerForDispatch || !selectedComplaintId) return;

    onAssignWorker(
      selectedComplaintId, 
      selectedWorkerForDispatch.id, 
      `${selectedWorkerForDispatch.name} (${selectedWorkerForDispatch.vehicle})`
    );

    // Update worker status to On Route
    onUpdateWorkerStatus(selectedWorkerForDispatch.id, 'On Route');

    setSelectedWorkerForDispatch(null);
    setSelectedComplaintId('');
  };

  const handleSendBroadcast = (e) => {
    e.preventDefault();
    if (!broadcastMessage.trim()) return;
    setBroadcastNotice(`Broadcast dispatched to all ${workers.length} active units: "${broadcastMessage}"`);
    setBroadcastMessage('');
    setTimeout(() => setBroadcastNotice(null), 5000);
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Header and Filter */}
      <div className="glass-panel" style={{ padding: '20px 24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 800 }}>Municipal Workers & Fleet Dispatch</h2>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              Manage sanitation crews, and monitor vehicle telemetry. Tasks are dispatched to nearby workers automatically.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
            {onNavigateToRegistration && (
              <button
                onClick={onNavigateToRegistration}
                className="btn btn-primary btn-sm"
                style={{ gap: '6px', background: 'linear-gradient(135deg, #10B981 0%, #06B6D4 100%)' }}
              >
                <UserPlus size={14} />
                <span>Register Worker</span>
              </button>
            )}

            <div style={{ display: 'flex', gap: '6px' }}>
              {['All', 'Available', 'On Route', 'Busy', 'Off Duty'].map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`btn btn-sm ${statusFilter === st ? 'btn-primary' : 'btn-secondary'}`}
                >
                  {st}
                </button>
              ))}
            </div>
          </div>
        </div>


        {/* Broadcast Radio Message Bar */}
        <form onSubmit={handleSendBroadcast} style={{ display: 'flex', gap: '10px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Radio size={16} color="var(--cyan-400)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
            <input 
              type="text"
              placeholder="Broadcast priority radio announcement to all active vehicle MDTs..."
              value={broadcastMessage}
              onChange={(e) => setBroadcastMessage(e.target.value)}
              className="input-field"
              style={{ paddingLeft: '40px', height: '40px', fontSize: '13px' }}
            />
          </div>
          <button type="submit" className="btn btn-secondary btn-sm" style={{ gap: '6px' }}>
            <Send size={14} />
            <span>Broadcast</span>
          </button>
        </form>

        {broadcastNotice && (
          <div style={{ marginTop: '10px', fontSize: '12px', color: '#34D399', background: 'rgba(16, 185, 129, 0.1)', padding: '6px 12px', borderRadius: '6px' }}>
            📡 {broadcastNotice}
          </div>
        )}
      </div>

      {/* Workers Cards Grid */}
      {filteredWorkers.length === 0 ? (
        <div 
          className="glass-panel" 
          style={{ 
            padding: '52px 24px', 
            textAlign: 'center', 
            display: 'flex', 
            flexDirection: 'column', 
            alignItems: 'center', 
            gap: '14px',
            borderRadius: '16px'
          }}
        >
          <div style={{
            width: '56px',
            height: '56px',
            borderRadius: '16px',
            background: 'rgba(16, 185, 129, 0.1)',
            border: '1px solid rgba(16, 185, 129, 0.25)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'var(--emerald-400)'
          }}>
            <Truck size={28} />
          </div>
          <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#FFF' }}>
            {statusFilter === 'All' ? 'No Fleet Personnel Registered' : `No Workers with Status "${statusFilter}"`}
          </h3>
          <p style={{ fontSize: '13px', color: 'var(--text-secondary)', maxWidth: '440px', lineHeight: 1.5 }}>
            {statusFilter === 'All'
              ? 'No active sanitation workers or vehicles found in the municipal fleet roster. Provision new field personnel with their official @worker.in credentials in the Worker Registration portal.'
              : `There are currently no sanitation workers matching the "${statusFilter}" status filter.`}
          </p>
          {onNavigateToRegistration && (
            <button
              onClick={onNavigateToRegistration}
              className="btn btn-primary"
              style={{ marginTop: '6px', gap: '8px' }}
            >
              <UserPlus size={16} />
              <span>Register Worker Credentials</span>
            </button>
          )}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '18px' }}>
          {filteredWorkers.map((worker) => (
            <div 
              key={worker.id}
              className="glass-panel"
              style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}
            >
              {/* Header: Name, Vehicle, Status */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <h3 style={{ fontSize: '16px', fontWeight: 800 }}>{worker.name}</h3>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                      {worker.id}
                    </span>
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--emerald-400)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px', marginTop: '2px' }}>
                    <Truck size={13} />
                    <span>{worker.vehicle}</span>
                  </div>
                  <div style={{ marginTop: '4px' }}>
                    <WorkerPhone phone={workerPhone(worker)} />
                  </div>
                </div>
                <StatusBadge status={worker.status} />
              </div>

              {/* Vehicle Type & Specs */}
              <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '10px 12px', borderRadius: '10px', fontSize: '12px' }}>
                <div style={{ color: 'var(--text-secondary)' }}>Type: <strong>{worker.vehicleType}</strong></div>
                <div style={{ color: 'var(--text-muted)', marginTop: '2px' }}>Assigned: 📍 {worker.wardName}</div>
              </div>

              {/* Telemetry Stats: Rating, Completed, Battery */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '8px', textAlign: 'center' }}>
                <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '8px', borderRadius: '8px' }}>
                  <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>Rating</span>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#FBBF24', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '2px' }}>
                    <Star size={12} fill="#FBBF24" />
                    <span>{worker.rating}</span>
                  </div>
                </div>

                <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '8px', borderRadius: '8px' }}>
                  <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>Tasks Today</span>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#FFF' }}>
                    {worker.tasksCompletedToday}
                  </div>
                </div>

                <div style={{ background: 'rgba(255, 255, 255, 0.02)', padding: '8px', borderRadius: '8px' }}>
                  <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>Fuel / Batt</span>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: '#34D399', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '2px' }}>
                    <BatteryCharging size={12} />
                    <span>{worker.batteryFuel}</span>
                  </div>
                </div>
              </div>

              {/* Action Bar */}
              <div style={{ display: 'flex', gap: '8px', marginTop: 'auto', paddingTop: '8px', borderTop: '1px solid var(--glass-border-subtle)' }}>
                <select
                  value={worker.status}
                  onChange={(e) => onUpdateWorkerStatus(worker.id, e.target.value)}
                  style={{
                    background: 'rgba(255, 255, 255, 0.05)',
                    border: '1px solid var(--glass-border)',
                    borderRadius: '8px',
                    color: 'var(--text-primary)',
                    fontSize: '11.5px',
                    padding: '4px 8px',
                    outline: 'none',
                    cursor: 'pointer'
                  }}
                >
                  <option value="Available" style={{ background: '#0D1322' }}>Available</option>
                  <option value="On Route" style={{ background: '#0D1322' }}>On Route</option>
                  <option value="Busy" style={{ background: '#0D1322' }}>Busy</option>
                  <option value="Off Duty" style={{ background: '#0D1322' }}>Off Duty</option>
                </select>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Dispatch Modal */}
      {selectedWorkerForDispatch && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.75)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100,
          padding: '24px'
        }}>
          <div className="glass-panel" style={{ width: '100%', maxWidth: '480px', padding: '24px' }}>
            <h3 style={{ fontSize: '18px', fontWeight: 800, marginBottom: '4px' }}>
              Dispatch {selectedWorkerForDispatch.name}
            </h3>
            <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)', marginBottom: '18px' }}>
              Assign an open grievance ticket to {selectedWorkerForDispatch.vehicle}
            </p>

            <form onSubmit={handleDispatch} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
                  Select Complaint / Task:
                </label>
                <select
                  value={selectedComplaintId}
                  onChange={(e) => setSelectedComplaintId(e.target.value)}
                  className="input-field"
                  required
                >
                  <option value="">Select ticket to assign...</option>
                  {unassignedComplaints.map(c => (
                    <option key={c.id} value={c.id} style={{ background: '#0D1322' }}>
                      {c.ticketNo}: {c.category} ({c.priority} Priority) - {c.location}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button 
                  type="button" 
                  onClick={() => setSelectedWorkerForDispatch(null)}
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  disabled={!selectedComplaintId}
                  className="btn btn-primary"
                  style={{ flex: 1 }}
                >
                  Confirm Dispatch
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
