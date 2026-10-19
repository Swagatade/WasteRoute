import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, 
  MapPin, 
  Clock, 
  Bell, 
  LogOut, 
  Radio, 
  AlertTriangle,
  ChevronDown,
  Sparkles,
  UserCheck
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { MUNICIPAL_WARDS } from '../data/mockData';

export default function Navbar({ 
  selectedWard, 
  onSelectWard, 
  onTriggerEmergency,
  pendingCount = 0
}) {
  const { currentUser, logout, isFirebaseLive } = useAuth();
  const [currentTime, setCurrentTime] = useState(new Date());
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <header className="app-navbar" style={{
      height: 'var(--header-height)',
      borderBottom: '1px solid var(--glass-border)',
      background: 'rgba(9, 13, 22, 0.85)',
      backdropFilter: 'blur(20px)',
      position: 'sticky',
      top: 0,
      zIndex: 50,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 28px'
    }}>
      {/* Left: Ward Selection & Active Authority */}
      <div className="app-navbar-left" style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '38px',
            height: '38px',
            borderRadius: '10px',
            background: 'linear-gradient(135deg, #064E3B 0%, #10B981 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 4px 12px rgba(16, 185, 129, 0.3)',
            border: '1px solid rgba(255, 255, 255, 0.15)'
          }}>
            <ShieldCheck size={22} color="#FFFFFF" />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ 
                fontFamily: 'var(--font-display)', 
                fontWeight: 800, 
                fontSize: '17px', 
                letterSpacing: '-0.02em',
                color: '#FFFFFF'
              }}>
                WasteRoute
              </span>
              <span style={{
                background: 'rgba(16, 185, 129, 0.15)',
                color: '#34D399',
                fontSize: '11px',
                fontWeight: 700,
                padding: '2px 8px',
                borderRadius: '6px',
                border: '1px solid rgba(16, 185, 129, 0.3)'
              }}>
                SUPERVISOR
              </span>
            </div>
            <p style={{ fontSize: '11.5px', color: 'var(--text-muted)' }}>
              Municipal Waste Operations Console
            </p>
          </div>
        </div>

        <div style={{ height: '24px', width: '1px', background: 'var(--glass-border)' }} />

        {/* Ward Filter Selector */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <MapPin size={16} color="var(--emerald-400)" />
          <select 
            value={selectedWard}
            onChange={(e) => onSelectWard(e.target.value)}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid var(--glass-border)',
              borderRadius: '8px',
              padding: '6px 12px',
              color: 'var(--text-primary)',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              outline: 'none'
            }}
          >
            {MUNICIPAL_WARDS.map(w => (
              <option key={w.id} value={w.id} style={{ background: '#0D1322', color: '#FFF' }}>
                {w.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Right: Live, Notifications & Supervisor Chip */}
      <div className="app-navbar-right" style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {/* Firebase Live Cloud Status Pill */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          background: 'rgba(16, 185, 129, 0.08)',
          border: '1px solid rgba(16, 185, 129, 0.25)',
          padding: '6px 12px',
          borderRadius: '9999px',
          fontSize: '12px'
        }}>
          <span style={{
            width: '8px',
            height: '8px',
            borderRadius: '50%',
            background: isFirebaseLive ? '#10B981' : '#F59E0B',
            boxShadow: `0 0 8px ${isFirebaseLive ? '#10B981' : '#F59E0B'}`
          }} />
          <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
            {isFirebaseLive ? 'Firebase Live: wasteroute-d393c' : 'Offline Mode'}
          </span>
        </div>

        {/* Live Municipal Clock */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '6px',
          color: 'var(--text-muted)',
          fontSize: '12.5px',
          fontFamily: 'var(--font-mono)'
        }}>
          <Clock size={14} />
          <span>{currentTime.toLocaleTimeString()}</span>
        </div>

        {/* Emergency Hazard Button */}
        <button 
          onClick={onTriggerEmergency}
          className="btn btn-sm btn-outline-danger"
          style={{ gap: '6px' }}
        >
          <AlertTriangle size={15} />
          <span>Dispatch Crew</span>
        </button>

        {/* Notifications Icon with Badge */}
        <div style={{ position: 'relative' }}>
          <button 
            onClick={() => setShowNotifications(!showNotifications)}
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '10px',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid var(--glass-border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              cursor: 'pointer',
              color: 'var(--text-primary)',
              transition: 'background 0.2s'
            }}
          >
            <Bell size={18} />
            {pendingCount > 0 && (
              <span style={{
                position: 'absolute',
                top: '-3px',
                right: '-3px',
                background: '#EF4444',
                color: '#FFF',
                fontSize: '10px',
                fontWeight: 700,
                width: '18px',
                height: '18px',
                borderRadius: '50%',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                boxShadow: '0 0 8px rgba(239, 68, 68, 0.5)'
              }}>
                {pendingCount}
              </span>
            )}
          </button>

          {showNotifications && (
            <div style={{
              position: 'absolute',
              top: '48px',
              right: 0,
              width: '320px',
              background: '#0D1322',
              border: '1px solid var(--glass-border)',
              borderRadius: '14px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.6)',
              padding: '16px',
              zIndex: 100
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                <span style={{ fontWeight: 700, fontSize: '13.5px' }}>Operations Alerts</span>
                <span style={{ fontSize: '11px', color: '#34D399' }}>Live Telemetry</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ padding: '8px', background: 'rgba(16, 185, 129, 0.1)', borderRadius: '8px', fontSize: '12px' }}>
                  <strong style={{ color: '#34D399' }}>Field Unit:</strong> Route completion proof uploaded for review.
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Supervisor User Profile Capsule */}
        <div style={{ position: 'relative' }}>
          <div 
            onClick={() => setShowProfileMenu(!showProfileMenu)}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              padding: '6px 12px',
              borderRadius: '12px',
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid var(--glass-border)',
              cursor: 'pointer'
            }}
          >
            <div style={{
              width: '30px',
              height: '30px',
              borderRadius: '50%',
              background: 'linear-gradient(135deg, #10B981, #06B6D4)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 700,
              fontSize: '12.5px',
              color: '#FFF'
            }}>
              {currentUser?.displayName?.charAt(0) || 'S'}
            </div>
            <div style={{ textAlign: 'left' }}>
              <div style={{ fontSize: '12.5px', fontWeight: 700, color: '#FFF' }}>
                {currentUser?.displayName || 'Supervisor'}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--emerald-400)', fontFamily: 'var(--font-mono)' }}>
                {currentUser?.email}
              </div>
            </div>
            <ChevronDown size={14} color="var(--text-muted)" />
          </div>

          {showProfileMenu && (
            <div style={{
              position: 'absolute',
              top: '52px',
              right: 0,
              width: '240px',
              background: '#0D1322',
              border: '1px solid var(--glass-border)',
              borderRadius: '14px',
              boxShadow: '0 20px 40px rgba(0,0,0,0.6)',
              padding: '12px',
              zIndex: 100
            }}>
              <div style={{ padding: '8px', borderBottom: '1px solid var(--glass-border)', marginBottom: '8px' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Role Clearance:</span>
                <p style={{ fontSize: '12.5px', fontWeight: 600, color: 'var(--emerald-400)' }}>
                  {currentUser?.role}
                </p>
              </div>
              <button 
                onClick={logout}
                className="btn btn-sm btn-outline-danger" 
                style={{ width: '100%', justifyContent: 'flex-start' }}
              >
                <LogOut size={15} />
                <span>Sign Out of Portal</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
