import React from 'react';
import { 
  LayoutDashboard, 
  FileText, 
  Users, 
  UserPlus,
  MapPin, 
  CheckCircle2, 
  BarChart3, 
  Truck, 
  Trash2, 
  HelpCircle,
  Cpu,
  Route as RouteIcon,
  Home
} from 'lucide-react';

export default function Sidebar({ activeTab, onSelectTab, pendingComplaintsCount = 0 }) {
  const navItems = [
    { 
      id: 'overview', 
      label: 'Operations Overview', 
      icon: LayoutDashboard,
      badge: null
    },
    { 
      id: 'complaints', 
      label: 'Complaint Review', 
      icon: FileText,
      badge: pendingComplaintsCount > 0 ? { count: pendingComplaintsCount, type: 'danger' } : null
    },
    { 
      id: 'workers', 
      label: 'Worker & Fleet Dispatch', 
      icon: Users,
      badge: null
    },
    { 
      id: 'routes',
      label: 'Route Planner',
      icon: RouteIcon,
      badge: null
    },
    {
      id: 'households',
      label: 'Areas & Hostels',
      icon: Home,
      badge: null
    },
    {
      id: 'worker-registration', 
      label: 'Worker Registration', 
      icon: UserPlus,
      badge: { count: '@worker.in', type: 'info' }
    },
    { 
      id: 'map', 
      label: 'Campus Map & Bins', 
      icon: MapPin,
      badge: null
    },

    { 
      id: 'reports', 
      label: 'Reports & Analytics', 
      icon: BarChart3,
      badge: null
    }
  ];

  return (
    <aside className="app-sidebar" style={{
      width: 'var(--sidebar-width)',
      height: 'calc(100vh - var(--header-height))',
      background: 'rgba(9, 13, 22, 0.9)',
      borderRight: '1px solid var(--glass-border)',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      padding: '24px 14px',
      position: 'sticky',
      top: 'var(--header-height)',
      zIndex: 40
    }}>
      <div>
        <div style={{ padding: '0 12px 14px 12px', fontSize: '11px', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
          Municipal Operations
        </div>

        <nav className="app-sidebar-nav" style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;

            return (
              <button
                key={item.id}
                onClick={() => onSelectTab(item.id)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  width: '100%',
                  padding: '11px 14px',
                  borderRadius: '12px',
                  background: isActive ? 'linear-gradient(135deg, rgba(16, 185, 129, 0.18) 0%, rgba(6, 182, 212, 0.08) 100%)' : 'transparent',
                  border: isActive ? '1px solid rgba(16, 185, 129, 0.35)' : '1px solid transparent',
                  color: isActive ? '#FFFFFF' : 'var(--text-secondary)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all var(--transition-fast)'
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = 'rgba(255, 255, 255, 0.04)';
                    e.currentTarget.style.color = '#FFFFFF';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.background = 'transparent';
                    e.currentTarget.style.color = 'var(--text-secondary)';
                  }
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Icon 
                    size={19} 
                    color={isActive ? '#10B981' : 'currentColor'} 
                  />
                  <span style={{ 
                    fontFamily: 'var(--font-display)', 
                    fontWeight: isActive ? 700 : 500, 
                    fontSize: '13.5px' 
                  }}>
                    {item.label}
                  </span>
                </div>

                {item.badge && (
                  <span style={{
                    fontSize: '10.5px',
                    fontWeight: 700,
                    padding: '2px 7px',
                    borderRadius: '9999px',
                    background: item.badge.type === 'danger' ? 'rgba(239, 68, 68, 0.2)' : item.badge.type === 'warning' ? 'rgba(245, 158, 11, 0.2)' : 'rgba(6, 182, 212, 0.2)',
                    color: item.badge.type === 'danger' ? '#F87171' : item.badge.type === 'warning' ? '#FBBF24' : '#22D3EE',
                    border: `1px solid ${item.badge.type === 'danger' ? 'rgba(239, 68, 68, 0.4)' : item.badge.type === 'warning' ? 'rgba(245, 158, 11, 0.4)' : 'rgba(6, 182, 212, 0.4)'}`
                  }}>
                    {item.badge.count}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

    </aside>
  );
}
