import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';
import LoginView from './views/LoginView';
import OverviewView from './views/OverviewView';
import ComplaintsView from './views/ComplaintsView';
import WorkersView from './views/WorkersView';
import WorkerRegistrationView from './views/WorkerRegistrationView';
import MapView from './views/MapView';
import RoutesView from './views/RoutesView';
import HouseholdsView from './views/HouseholdsView';
import ReportsView from './views/ReportsView';
import { 
  INITIAL_COMPLAINTS, 
  INITIAL_WORKERS, 
  INITIAL_SMART_BINS 
} from './data/mockData';
import { getRegisteredWorkersFromStorage } from './firebase/workerAuth';
import { 
  seedInitialFirestoreDataIfNeeded,
  subscribeToComplaints,
  subscribeToSmartBins,
  subscribeToWorkers,
  subscribeToSchedules,
  subscribeToScheduleTemplates,
  ensureDailySchedules,
  subscribeToCollectionLogs,
  subscribeToHouseholds,
  addHostelBlock,
  seedCampusBins,
  subscribeToZones,
  seedZones,
  upsertWorkerInFirestore,
  updateComplaintStatusInFirestore,
  assignWorkerToComplaintInFirestore,
  addComplaintToFirestore
} from './firebase/firestoreService';
import { AlertTriangle, X } from 'lucide-react';

function DashboardLayout() {
  const { currentUser } = useAuth();

  // App-level state for real-time interactivity
  const [complaints, setComplaints] = useState(INITIAL_COMPLAINTS);
  const [workers, setWorkers] = useState(() => {
    const mockIds = new Set(['WRK-101', 'WRK-102', 'WRK-103', 'WRK-104', 'WRK-105']);
    const cached = getRegisteredWorkersFromStorage();
    if (cached && cached.length > 0) {
      // Purge any legacy mock workers from storage
      const cleanCached = cached.filter(w => !mockIds.has(w.id));
      if (cleanCached.length !== cached.length) {
        try {
          localStorage.setItem('wasteroute_registered_workers', JSON.stringify(cleanCached));
        } catch (e) {
          console.warn('Storage sync notice:', e);
        }
      }
      return cleanCached;
    }
    return [];
  });
  const [bins, setBins] = useState(INITIAL_SMART_BINS);
  const [schedules, setSchedules] = useState([]);
  const [schedulesLoaded, setSchedulesLoaded] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [dayTick, setDayTick] = useState(0);
  const [collectionLogs, setCollectionLogs] = useState([]);
  const [households, setHouseholds] = useState([]);
  const [householdsLoaded, setHouseholdsLoaded] = useState(false);
  const [householdsError, setHouseholdsError] = useState(null);
  const [zones, setZones] = useState([]);
  const [activeTab, setActiveTab] = useState('overview');

  const [selectedWard, setSelectedWard] = useState('all');
  const [selectedComplaint, setSelectedComplaint] = useState(null);
  const [showEmergencyModal, setShowEmergencyModal] = useState(false);
  const [emergencyWard, setEmergencyWard] = useState('ward-01');

  // Real-time bidirectional Firestore sync with Android app
  useEffect(() => {
    seedInitialFirestoreDataIfNeeded();

    const unsubComplaints = subscribeToComplaints((liveComplaints) => {
      // Only real citizen reports made through the app (they carry the reporter's account id).
      // Old sample / placeholder tickets and test data are never shown.
      setComplaints((liveComplaints || []).filter((c) => c.reporterUid && !c.isMock));
    });

    let seededBins = false;
    const unsubBins = subscribeToSmartBins((liveBins) => {
      if (liveBins && liveBins.length > 0) {
        setBins(liveBins);
      } else if (!seededBins) {
        // Empty collection: put the campus bin points into Firestore (the map then updates live)
        seededBins = true;
        seedCampusBins().catch((e) => console.warn('Campus bins seed notice:', e?.message || e));
      }
    });

    const unsubWorkers = subscribeToWorkers((liveWorkers) => {
      // Firestore is the source of truth. Workers that only exist in this browser's cache
      // (a failed earlier sync) are kept visible AND pushed to Firestore again.
      const liveIds = new Set(liveWorkers.map((w) => w.id));
      const unsynced = getRegisteredWorkersFromStorage().filter((w) => !liveIds.has(w.id));
      unsynced.forEach((w) => {
        const { syncError, ...record } = w;
        upsertWorkerInFirestore(record).catch((e) => console.warn('Worker re-sync notice:', e));
      });
      setWorkers([...liveWorkers, ...unsynced]);
    });

    const unsubSchedules = subscribeToSchedules((list) => {
      setSchedules(list);
      setSchedulesLoaded(true);
    });
    const unsubTemplates = subscribeToScheduleTemplates(setTemplates);
    const unsubLogs = subscribeToCollectionLogs(setCollectionLogs);
    let seededZones = false;
    const unsubZones = subscribeToZones((list) => {
      setZones(list);
      if (list.length === 0 && !seededZones) {
        seededZones = true;
        seedZones().catch((e) => console.warn('Zones seed notice:', e?.message || e));
      }
    });
    const unsubHouseholds = subscribeToHouseholds((list) => {
      setHouseholds(list);
      setHouseholdsLoaded(true);
      setHouseholdsError(null);
      // hostel blocks are registered from the official SRM hostel list (Areas & Hostels), not auto-seeded
    }, (err) => setHouseholdsError(err?.code || err?.message || 'error'));

    return () => {
      unsubSchedules();
      unsubTemplates();
      unsubLogs();
      unsubHouseholds();
      unsubZones();
      unsubComplaints();
      unsubBins();
      unsubWorkers();
    };
  }, []);

  // Daily routines -> make sure today's (and the next days') slots exist. Runs on every change and
  // only writes slots that are missing.
  useEffect(() => {
    if (!schedulesLoaded || templates.length === 0) return;
    ensureDailySchedules(templates, new Set(schedules.map((s) => s.id)));
  }, [templates, schedules, schedulesLoaded, dayTick]);

  // Re-check periodically so a tab left open past midnight still creates the new day's slots.
  useEffect(() => {
    const t = setInterval(() => setDayTick((n) => n + 1), 30 * 60 * 1000);
    return () => clearInterval(t);
  }, []);

  // Handle complaint status update
  const handleUpdateComplaintStatus = (complaintId, newStatus) => {
    setComplaints(prev => prev.map(c => {
      if (c.id === complaintId) {
        return { ...c, status: newStatus };
      }
      return c;
    }));
    if (selectedComplaint && selectedComplaint.id === complaintId) {
      setSelectedComplaint(prev => ({ ...prev, status: newStatus }));
    }
    // Sync to Firestore
    updateComplaintStatusInFirestore(complaintId, newStatus);
  };

  // Handle worker assignment to a complaint
  const handleAssignWorker = (complaintId, workerId, workerLabel) => {
    setComplaints(prev => prev.map(c => {
      if (c.id === complaintId) {
        return {
          ...c,
          status: 'Assigned',
          assignedWorkerId: workerId,
          assignedWorkerName: workerLabel
        };
      }
      return c;
    }));

    setWorkers(prev => prev.map(w => {
      if (w.id === workerId) {
        return { ...w, status: 'On Route' };
      }
      return w;
    }));

    if (selectedComplaint && selectedComplaint.id === complaintId) {
      setSelectedComplaint(prev => ({
        ...prev,
        status: 'Assigned',
        assignedWorkerId: workerId,
        assignedWorkerName: workerLabel
      }));
    }
    // Sync to Firestore
    assignWorkerToComplaintInFirestore(complaintId, workerId, workerLabel);
  };

  // Handle worker status change
  const handleUpdateWorkerStatus = (workerId, newStatus) => {
    setWorkers(prev => prev.map(w => {
      if (w.id === workerId) {
        return { ...w, status: newStatus };
      }
      return w;
    }));
  };

  // Handle new worker registration from WorkerRegistrationView
  const handleWorkerRegistered = (newWorker) => {
    setWorkers(prev => [newWorker, ...prev.filter(w => w.id !== newWorker.id)]);
  };


  // Emergency Rapid Response Dispatch
  const handleConfirmEmergencyDispatch = (e) => {
    e.preventDefault();
    const availableWorker = workers.find(w => w.status === 'Available') || workers[0];
    
    // Add new emergency complaint ticket
    const newEmergencyTicket = {
      id: `CMP-${Date.now().toString().slice(-4)}`,
      ticketNo: `WR-EMERGENCY-${Date.now().toString().slice(-4)}`,
      category: 'Hazardous Waste',
      priority: 'Critical',
      status: availableWorker ? 'Assigned' : 'Pending',
      wardId: emergencyWard,
      wardName: emergencyWard === 'ward-01' ? 'Ward 01 - Downtown' : 'Ward 03 - East Tech Park',
      location: 'Central Junction Emergency Site',
      coordinates: { lat: 12.9750, lng: 77.5980 },
      citizen: {
        name: 'Municipal Dispatch Center',
        phone: '100 / Direct Line',
        email: 'dispatch@supervisor.in',
        badge: 'Priority 1 Dispatch'
      },
      reportedAt: 'Just now',
      description: 'Emergency biohazard & road obstruction reported. Rapid intervention crew dispatched.',
      photoBefore: 'https://images.unsplash.com/photo-1611284446314-60a58ac0deb9?auto=format&fit=crop&w=800&q=80',
      assignedWorkerId: availableWorker ? availableWorker.id : null,
      assignedWorkerName: availableWorker ? `${availableWorker.name} (${availableWorker.vehicle})` : null,
      cleaningProof: null
    };

    setComplaints(prev => [newEmergencyTicket, ...prev]);
    addComplaintToFirestore(newEmergencyTicket);
    if (availableWorker) {
      setWorkers(prev => prev.map(w => w.id === availableWorker.id ? { ...w, status: 'Busy' } : w));
    }
    setShowEmergencyModal(false);
    setActiveTab('complaints');
  };

  // Computed counters
  const pendingCount = complaints.filter(c => c.status === 'Pending' || c.status === 'Under Review').length;

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Top Municipal Navigation Bar */}
      <Navbar 
        selectedWard={selectedWard}
        onSelectWard={setSelectedWard}
        onTriggerEmergency={() => setShowEmergencyModal(true)}
        pendingCount={pendingCount}
      />

      {/* Main Content Body: Sidebar + Active Tab View */}
      <div className="app-body" style={{ display: 'flex', flex: 1 }}>
        <Sidebar 
          activeTab={activeTab}
          onSelectTab={setActiveTab}
          pendingComplaintsCount={pendingCount}
        />

        <main className="app-main" style={{
          flex: 1,
          minWidth: 0,
          padding: '28px 36px',
          maxWidth: '1600px',
          width: '100%',
          overflowX: 'hidden'
        }}>
          {activeTab === 'overview' && (
            <OverviewView 
              complaints={complaints}
              workers={workers}
              bins={bins}
              onNavigateTab={setActiveTab}
              onOpenComplaint={(cmp) => {
                setSelectedComplaint(cmp);
                setActiveTab('complaints');
              }}
            />
          )}

          {activeTab === 'complaints' && (
            <ComplaintsView 
              complaints={complaints}
              workers={workers}
              selectedWard={selectedWard}
              onUpdateComplaintStatus={handleUpdateComplaintStatus}
              onAssignWorker={handleAssignWorker}
              selectedComplaint={selectedComplaint}
              onSelectComplaint={setSelectedComplaint}
            />
          )}

          {activeTab === 'workers' && (
            <WorkersView 
              workers={workers}
              complaints={complaints}
              onUpdateWorkerStatus={handleUpdateWorkerStatus}
              onAssignWorker={handleAssignWorker}
              onNavigateToRegistration={() => setActiveTab('worker-registration')}
            />
          )}

          {activeTab === 'routes' && (
            <RoutesView
              workers={workers}
              bins={bins}
              households={households}
              schedules={schedules}
              templates={templates}
              onEnsureSlots={() => ensureDailySchedules(templates, new Set(schedules.map((s) => s.id)))}
            />
          )}

          {activeTab === 'households' && (
            <HouseholdsView
              workers={workers}
              households={households}
              zones={zones}
              loaded={householdsLoaded}
              loadError={householdsError}
              supervisorName={currentUser?.displayName}
            />
          )}

          {activeTab === 'worker-registration' && (
            <WorkerRegistrationView
              workers={workers}
              onWorkerRegistered={handleWorkerRegistered}
              onNavigateToDispatch={() => setActiveTab('workers')}
            />
          )}


          {activeTab === 'map' && (
            <MapView 
              bins={bins}
              complaints={complaints}
              workers={workers}
              schedules={schedules}
              households={households}
              zones={zones}
              onAddHostel={(block) => addHostelBlock(block, households)}
              onSelectComplaint={(cmp) => {
                setSelectedComplaint(cmp);
                setActiveTab('complaints');
              }}
            />
          )}



          {activeTab === 'reports' && (
            <ReportsView 
              complaints={complaints}
              workers={workers}
              bins={bins}
            />
          )}
        </main>
      </div>

      {/* Emergency Rapid Response Modal */}
      {showEmergencyModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.85)',
          backdropFilter: 'blur(10px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100,
          padding: '24px'
        }}>
          <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '480px', padding: '28px', border: '1.5px solid rgba(239, 68, 68, 0.4)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
              <div style={{ width: '38px', height: '38px', borderRadius: '10px', background: 'rgba(239, 68, 68, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <AlertTriangle size={20} color="#EF4444" />
              </div>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#FFF' }}>Emergency Crew Dispatch</h3>
                <span style={{ fontSize: '11px', color: '#F87171', fontWeight: 700 }}>Priority Code 1 Intervention</span>
              </div>
            </div>

            <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)', marginBottom: '18px' }}>
              Instantly mobilize the nearest available compactor or hazmat carrier to a critical incident.
            </p>

            <form onSubmit={handleConfirmEmergencyDispatch} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
                  Target Zone / Ward:
                </label>
                <select 
                  value={emergencyWard}
                  onChange={(e) => setEmergencyWard(e.target.value)}
                  className="input-field"
                >
                  <option value="ward-01" style={{ background: '#0D1322' }}>Ward 01 - Downtown Commercial Plaza</option>
                  <option value="ward-02" style={{ background: '#0D1322' }}>Ward 02 - North Residential Sector</option>
                  <option value="ward-03" style={{ background: '#0D1322' }}>Ward 03 - East Tech Park</option>
                  <option value="ward-04" style={{ background: '#0D1322' }}>Ward 04 - Harbor & Waterfront</option>
                </select>
              </div>

              <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                <button 
                  type="button" 
                  onClick={() => setShowEmergencyModal(false)}
                  className="btn btn-secondary"
                  style={{ flex: 1 }}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary"
                  style={{ flex: 1, background: 'linear-gradient(135deg, #EF4444 0%, #B91C1C 100%)', borderColor: '#EF4444' }}
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

function MainApp() {
  const { currentUser } = useAuth();
  return currentUser ? <DashboardLayout /> : <LoginView />;
}

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
