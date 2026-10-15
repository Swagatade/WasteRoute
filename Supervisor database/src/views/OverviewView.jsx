import React from 'react';
import { 
  AlertTriangle, 
  CheckCircle2, 
  Clock, 
  Trash2, 
  Truck, 
  TrendingUp, 
  Users, 
  Activity,
  ArrowUpRight,
  ShieldAlert,
  Zap,
  Sparkles
} from 'lucide-react';
import StatusBadge from '../components/StatusBadge';

export default function OverviewView({ 
  complaints, 
  workers, 
  bins, 
  onNavigateTab,
  onOpenComplaint 
}) {
  const pendingCount = complaints.filter(c => c.status === 'Pending').length;
  const inProgressCount = complaints.filter(c => c.status === 'In Progress' || c.status === 'Assigned' || c.status === 'Under Review').length;
  const resolvedCount = complaints.filter(c => c.status === 'Resolved').length;
  const activeWorkers = workers.filter(w => w.status === 'Available' || w.status === 'On Route');

  const statsCards = [
    {
      title: 'Pending Complaints',
      value: pendingCount,
      change: `${pendingCount} awaiting review`,
      isWarning: pendingCount > 0,
      icon: AlertTriangle,
      color: '#EF4444',
      bgGlow: 'rgba(239, 68, 68, 0.15)',
      targetTab: 'complaints'
    },
    {
      title: 'Active Fleet Crews',
      value: `${activeWorkers.length}/${workers.length}`,
      change: workers.length > 0 ? `${Math.round((activeWorkers.length / workers.length) * 100)}% fleet deployment` : '0 units deployed',
      isWarning: false,
      icon: Truck,
      color: '#10B981',
      bgGlow: 'rgba(16, 185, 129, 0.15)',
      targetTab: 'workers'
    },
    {
      title: 'Bin Locations',
      value: bins.length,
      change: 'Registered on the map',
      isWarning: false,
      icon: Trash2,
      color: '#F59E0B',
      bgGlow: 'rgba(245, 158, 11, 0.15)',
      targetTab: 'map'
    },
    {
      title: 'Resolved Today',
      value: resolvedCount,
      change: `${complaints.filter(c => c.status === 'Resolved' && c.cleaningProof?.photoAfter).length} with worker proof photo`,
      isWarning: false,
      icon: CheckCircle2,
      color: '#06B6D4',
      bgGlow: 'rgba(6, 182, 212, 0.15)',
      targetTab: 'reports'
    }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Welcome Banner */}
      <div className="glass-panel" style={{
        padding: '24px 28px',
        background: 'linear-gradient(135deg, rgba(6, 78, 59, 0.4) 0%, rgba(13, 19, 34, 0.85) 60%)',
        border: '1px solid rgba(16, 185, 129, 0.25)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
            <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--emerald-400)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Municipal Command Deck
            </span>
            <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--emerald-400)' }} />
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Real-Time Data Feed</span>
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 800, color: '#FFF' }}>
            Operations & Fleet Overview
          </h2>
          <p style={{ fontSize: '13.5px', color: 'var(--text-secondary)', marginTop: '4px' }}>
            Monitor citizen grievance tickets, dispatch sanitation trucks, and view bin locations.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button 
            onClick={() => onNavigateTab('complaints')}
            className="btn btn-primary"
          >
            <span>Review Complaints</span>
            <ArrowUpRight size={16} />
          </button>
          <button 
            onClick={() => onNavigateTab('map')}
            className="btn btn-secondary"
          >
            <span>Live Map View</span>
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
        gap: '18px'
      }}>
        {statsCards.map((card, i) => {
          const Icon = card.icon;
          return (
            <div 
              key={i} 
              className="glass-panel glass-panel-interactive"
              onClick={() => onNavigateTab(card.targetTab)}
              style={{
                padding: '20px',
                cursor: 'pointer',
                position: 'relative',
                overflow: 'hidden'
              }}
            >
              <div style={{
                position: 'absolute',
                top: 0,
                right: 0,
                width: '90px',
                height: '90px',
                background: card.bgGlow,
                filter: 'blur(30px)',
                borderRadius: '50%',
                pointerEvents: 'none'
              }} />

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '14px' }}>
                <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  {card.title}
                </span>
                <div style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '10px',
                  background: card.bgGlow,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: `1px solid ${card.color}30`
                }}>
                  <Icon size={18} color={card.color} />
                </div>
              </div>

              <div style={{ fontSize: '32px', fontWeight: 800, color: '#FFF', letterSpacing: '-0.02em', marginBottom: '4px' }}>
                {card.value}
              </div>

              <div style={{ fontSize: '12px', color: card.isWarning ? '#F87171' : 'var(--emerald-400)', fontWeight: 500 }}>
                {card.change}
              </div>
            </div>
          );
        })}
      </div>

      {/* Two Column Section: Live Priority Tickets & Bin Locations */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '20px' }}>
        {/* Left: Recent Complaints Queue */}
        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
            <div>
              <h3 style={{ fontSize: '16.5px', fontWeight: 700 }}>Active Complaint Stream</h3>
              <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Latest citizen submissions requiring supervisor action</p>
            </div>
            <button 
              onClick={() => onNavigateTab('complaints')}
              className="btn btn-secondary btn-sm"
            >
              View All ({complaints.filter(c => c.status !== 'Resolved').length})
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {complaints.filter(c => c.status !== 'Resolved').slice(0, 4).map((cmp) => (
              <div 
                key={cmp.id}
                onClick={() => onOpenComplaint(cmp)}
                style={{
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: '12px',
                  padding: '14px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  cursor: 'pointer',
                  transition: 'background 0.2s'
                }}
                onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255, 255, 255, 0.06)'}
                onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255, 255, 255, 0.03)'}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                  <img 
                    src={cmp.photoBefore} 
                    alt={cmp.category}
                    style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: '10px',
                      objectFit: 'cover',
                      border: '1px solid var(--glass-border)'
                    }}
                  />
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 700, fontSize: '13.5px', color: '#FFF' }}>
                        {cmp.category}
                      </span>
                      <span style={{ fontSize: '11px', color: 'var(--text-muted)', fontFamily: 'var(--font-mono)' }}>
                        {cmp.ticketNo}
                      </span>
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
                      📍 {cmp.location}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <StatusBadge status={cmp.status} />
                  <span style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>
                    {cmp.reportedAt}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Right: Bin Locations Card */}
        <div className="glass-panel" style={{ padding: '22px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '18px' }}>
            <div>
              <h3 style={{ fontSize: '16.5px', fontWeight: 700 }}>Bin Locations</h3>
              <p style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Where the bins are on campus</p>
            </div>
            <button 
              onClick={() => onNavigateTab('map')}
              className="btn btn-secondary btn-sm"
            >
              Full Map
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {bins.length === 0 && (
              <p style={{ fontSize: '12.5px', color: 'var(--text-muted)' }}>No bin locations added yet.</p>
            )}
            {bins.slice(0, 6).map((bin) => (
              <div
                key={bin.id}
                style={{
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid var(--glass-border)',
                  borderRadius: '12px',
                  padding: '12px 14px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px'
                }}
              >
                <Trash2 size={16} color="#10B981" />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13px', color: '#FFF' }}>{bin.code}</div>
                  <div style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>📍 {bin.location || bin.name}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
