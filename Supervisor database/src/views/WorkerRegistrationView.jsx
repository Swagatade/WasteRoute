import React, { useState } from 'react';
import { 
  UserPlus, 
  ShieldCheck, 
  Key, 
  Copy, 
  Check, 
  Truck, 
  Phone, 
  Mail, 
  RefreshCw, 
  Search, 
  AlertCircle, 
  CheckCircle2, 
  Eye, 
  EyeOff, 
  Sparkles,
  ArrowRight,
  Radio
} from 'lucide-react';
import { 
  REQUIRED_WORKER_DOMAIN, 
  validateWorkerDomain, 
  formatWorkerEmail, 
  registerWorkerWithFirebase 
} from '../firebase/workerAuth';
import { MUNICIPAL_WARDS } from '../data/mockData';
import StatusBadge from '../components/StatusBadge';

import { upsertWorkerInFirestore } from '../firebase/firestoreService';
import WorkerPhone from '../components/WorkerPhone';
import { workerPhone } from '../utils/workerContact';

export default function WorkerRegistrationView({ 
  workers, 
  onWorkerRegistered,
  onNavigateToDispatch 
}) {
  // Form State
  const [workerId, setWorkerId] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [phone, setPhone] = useState('');
  const [vehicle, setVehicle] = useState('Compactor Truck #05');
  const [vehicleType, setVehicleType] = useState('Heavy Compactor (12 Ton)');
  const [wardId, setWardId] = useState('ward-01');

  // UI state
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [successData, setSuccessData] = useState(null);
  const [copiedKey, setCopiedKey] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [editingPhoneId, setEditingPhoneId] = useState(null);
  const [editPhoneValue, setEditPhoneValue] = useState('');

  // Writes the mobile number straight to Firestore workers/{id}; the live listener refreshes every worker section.
  const savePhone = async (worker) => {
    if (editPhoneValue.replace(/\D/g, '').length < 10) {
      setErrorMsg('Enter a valid mobile number (10 digits).');
      return;
    }
    try {
      await upsertWorkerInFirestore({ id: worker.id, workerId: worker.id, phone: editPhoneValue.trim() });
      setEditingPhoneId(null);
      setErrorMsg(null);
    } catch (err) {
      setErrorMsg(err.message || 'Could not save the mobile number to Firestore.');
    }
  };
  const [filterWard, setFilterWard] = useState('all');

  // Auto-generate Worker ID
  const handleGenerateId = () => {
    const randomNum = Math.floor(100 + Math.random() * 900);
    const newId = `WRK-${randomNum}`;
    setWorkerId(newId);
    if (!email || email.includes('@worker.in')) {
      setEmail(`${newId.toLowerCase()}${REQUIRED_WORKER_DOMAIN}`);
    }
  };

  // Generate a recommended default password
  const handleGeneratePassword = () => {
    const randomPin = Math.floor(1000 + Math.random() * 9000);
    const generated = `Work@${randomPin}`;
    setPassword(generated);
  };

  // When workerId changes, update suggested email if empty
  const handleWorkerIdChange = (e) => {
    const val = e.target.value.toUpperCase();
    setWorkerId(val);
    if (val && (!email || email === `${workerId.toLowerCase()}${REQUIRED_WORKER_DOMAIN}`)) {
      setEmail(`${val.toLowerCase()}${REQUIRED_WORKER_DOMAIN}`);
    }
  };

  // Fast-fill email with worker ID
  const handleSyncEmailWithId = () => {
    if (workerId.trim()) {
      setEmail(`${workerId.trim().toLowerCase()}${REQUIRED_WORKER_DOMAIN}`);
    } else {
      setEmail(`worker${Math.floor(100 + Math.random() * 900)}${REQUIRED_WORKER_DOMAIN}`);
    }
  };

  // Check email validity
  const isEmailDomainValid = email ? validateWorkerDomain(email) : false;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessData(null);

    const cleanId = workerId.trim().toUpperCase();
    const cleanEmail = email.trim().toLowerCase();

    if (!cleanId) {
      setErrorMsg('Please specify a Worker ID (e.g. WRK-106 or WR-W-205).');
      return;
    }

    if (!validateWorkerDomain(cleanEmail)) {
      setErrorMsg(`Worker email must end with "${REQUIRED_WORKER_DOMAIN}" (e.g. ${cleanId.toLowerCase()}${REQUIRED_WORKER_DOMAIN}).`);
      return;
    }

    if (!password || password.length < 6) {
      setErrorMsg('Password must be at least 6 characters.');
      return;
    }

    if (phone.replace(/\D/g, '').length < 10) {
      setErrorMsg('Enter the worker mobile number (10 digits).');
      return;
    }

    const selectedWardObj = MUNICIPAL_WARDS.find(w => w.id === wardId) || { name: 'Ward 01 - Downtown' };

    setIsLoading(true);
    try {
      const newWorker = await registerWorkerWithFirebase({
        workerId: cleanId,
        name: fullName.trim() || `Worker ${cleanId}`,
        email: cleanEmail,
        password: password,
        phone: phone.trim(),
        vehicle: vehicle.trim() || 'Compactor Truck',
        vehicleType: vehicleType,
        wardId: wardId,
        wardName: selectedWardObj.name
      });

      // Notify parent app
      if (onWorkerRegistered) {
        onWorkerRegistered(newWorker);
      }

      setSuccessData({
        ...newWorker,
        plainPassword: password
      });

      // Clear sensitive form inputs
      setWorkerId('');
      setFullName('');
      setEmail('');
      setPassword('');
      setPhone('');
    } catch (err) {
      setErrorMsg(err.message || 'Worker registration failed.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCopyCredentials = () => {
    if (!successData) return;
    const credsText = `WasteRoute Municipal Worker Access Credentials:\nWorker ID: ${successData.id}\nEmail: ${successData.email}\nPassword: ${successData.plainPassword}\nAssigned Vehicle: ${successData.vehicle}\nDomain: ${REQUIRED_WORKER_DOMAIN}\nMobile App: WasteRoute Android (Worker Portal)`;
    navigator.clipboard.writeText(credsText).then(() => {
      setCopiedKey(true);
      setTimeout(() => setCopiedKey(false), 3000);
    });
  };

  // Filtered workers list
  const filteredWorkers = workers.filter(w => {
    const matchesSearch = 
      (w.name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (w.id || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (w.email || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (w.vehicle || '').toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesWard = filterWard === 'all' || w.wardId === filterWard;
    return matchesSearch && matchesWard;
  });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Top Banner */}
      <div className="glass-panel" style={{ padding: '24px 28px', position: 'relative', overflow: 'hidden' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
              <div style={{
                width: '36px',
                height: '36px',
                borderRadius: '10px',
                background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.25) 0%, rgba(6, 182, 212, 0.2) 100%)',
                border: '1px solid rgba(16, 185, 129, 0.4)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}>
                <UserPlus size={20} color="var(--emerald-400)" />
              </div>
              <h2 style={{ fontSize: '22px', fontWeight: 800 }}>Worker Registration & Credential Provisioning</h2>
            </div>
            <p style={{ fontSize: '13.5px', color: 'var(--text-secondary)', maxWidth: '780px' }}>
              Register municipal field staff and sanitation crew credentials. Only verified workers with official <strong style={{ color: 'var(--cyan-400)' }}>{REQUIRED_WORKER_DOMAIN}</strong> accounts provisioned here can log into the WasteRoute Android application.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <span style={{
              background: 'rgba(6, 182, 212, 0.12)',
              border: '1px solid rgba(6, 182, 212, 0.35)',
              color: 'var(--cyan-400)',
              padding: '6px 14px',
              borderRadius: '9999px',
              fontSize: '12px',
              fontWeight: 700,
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              <ShieldCheck size={14} />
              Domain: {REQUIRED_WORKER_DOMAIN}
            </span>

            <button
              onClick={onNavigateToDispatch}
              className="btn btn-secondary btn-sm"
              style={{ gap: '6px' }}
            >
              <span>View Fleet Dispatch</span>
              <ArrowRight size={14} />
            </button>
          </div>
        </div>
      </div>

      {/* Main Grid: Form on Left, Registry List on Right */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(380px, 460px) 1fr', gap: '24px', alignItems: 'start' }}>
        
        {/* Left Column: Registration Form */}
        <div className="glass-panel" style={{ padding: '26px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '18px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Sparkles size={18} color="var(--emerald-400)" />
              <h3 style={{ fontSize: '17px', fontWeight: 800 }}>New Worker Provisioning</h3>
            </div>
            <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Firebase Auth Connected</span>
          </div>

          {errorMsg && (
            <div style={{
              background: 'rgba(239, 68, 68, 0.12)',
              border: '1px solid rgba(239, 68, 68, 0.35)',
              borderRadius: '10px',
              padding: '12px 14px',
              color: '#F87171',
              fontSize: '12.5px',
              marginBottom: '18px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '10px'
            }}>
              <AlertCircle size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>{errorMsg}</div>
            </div>
          )}

          {successData && (
            <div style={{
              background: 'rgba(16, 185, 129, 0.12)',
              border: '1px solid rgba(16, 185, 129, 0.4)',
              borderRadius: '12px',
              padding: '16px',
              marginBottom: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: '10px'
            }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#34D399', fontWeight: 800, fontSize: '14px' }}>
                <CheckCircle2 size={18} />
                <span>{successData.alreadyExisted ? 'Worker Verified & Active in Firebase!' : 'Worker Provisioned Successfully!'}</span>
              </div>
              <p style={{ fontSize: '12px', color: 'var(--text-secondary)', lineHeight: '1.4' }}>
                {successData.alreadyExisted
                  ? 'Account is active in Firebase Authentication and municipal fleet database. The worker can log into the Android app.'
                  : 'Account registered in Firebase Authentication & municipal database. The worker can immediately sign into the Android application.'}
              </p>
              {successData.syncError && (
                <p style={{ fontSize: '12px', color: '#FBBF24', lineHeight: '1.4' }}>
                  ⚠ Login account is ready, but the fleet record could not be saved to Firestore ({successData.syncError}).
                  Deploy <code>firestore.rules</code> / create the Firestore database, then register again to sync. The worker will also
                  appear automatically after their first login on the Android app.
                </p>
              )}


              <div style={{
                background: 'rgba(0, 0, 0, 0.35)',
                border: '1px solid var(--glass-border-subtle)',
                borderRadius: '8px',
                padding: '10px 12px',
                fontSize: '12px',
                fontFamily: 'var(--font-mono)',
                display: 'flex',
                flexDirection: 'column',
                gap: '4px'
              }}>
                <div><span style={{ color: 'var(--text-muted)' }}>ID:</span> <strong style={{ color: '#FFF' }}>{successData.id}</strong></div>
                <div><span style={{ color: 'var(--text-muted)' }}>Email:</span> <strong style={{ color: 'var(--cyan-400)' }}>{successData.email}</strong></div>
                <div><span style={{ color: 'var(--text-muted)' }}>Password:</span> <strong style={{ color: '#FBBF24' }}>{successData.plainPassword}</strong></div>
                <div><span style={{ color: 'var(--text-muted)' }}>Vehicle:</span> <span style={{ color: '#FFF' }}>{successData.vehicle}</span></div>
              </div>

              <button
                type="button"
                onClick={handleCopyCredentials}
                className="btn btn-secondary btn-sm"
                style={{ width: '100%', justifyContent: 'center', gap: '6px', marginTop: '4px' }}
              >
                {copiedKey ? <Check size={14} color="#34D399" /> : <Copy size={14} />}
                <span>{copiedKey ? 'Credentials Copied to Clipboard!' : 'Copy Credentials for Field Worker'}</span>
              </button>
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            
            {/* Field 1: Worker ID */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>
                  Worker ID <span style={{ color: '#EF4444' }}>*</span>
                </label>
                <button
                  type="button"
                  onClick={handleGenerateId}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--cyan-400)',
                    fontSize: '11px',
                    fontWeight: 600,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '4px'
                  }}
                >
                  <RefreshCw size={11} />
                  <span>Auto-Generate</span>
                </button>
              </div>
              <input
                type="text"
                placeholder="e.g. WRK-106 or WR-W-205"
                value={workerId}
                onChange={handleWorkerIdChange}
                required
                className="input-field"
                style={{ fontFamily: 'var(--font-mono)', letterSpacing: '0.04em' }}
              />
            </div>

            {/* Field 2: Full Name */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Worker Full Name <span style={{ color: '#EF4444' }}>*</span>
              </label>
              <input
                type="text"
                placeholder="e.g. Rahul Kumar or Arun Patel"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                required
                className="input-field"
              />
            </div>

            {/* Field 3: Worker Email with @worker.in badge */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>
                  Worker Email (Special <span style={{ color: 'var(--cyan-400)' }}>{REQUIRED_WORKER_DOMAIN}</span>) <span style={{ color: '#EF4444' }}>*</span>
                </label>
                <button
                  type="button"
                  onClick={handleSyncEmailWithId}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--emerald-400)',
                    fontSize: '11px',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  Use ID prefix
                </button>
              </div>

              <div style={{ position: 'relative' }}>
                <input
                  type="email"
                  placeholder={`worker${REQUIRED_WORKER_DOMAIN}`}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="input-field"
                  style={{
                    borderColor: email ? (isEmailDomainValid ? 'rgba(16, 185, 129, 0.5)' : 'rgba(239, 68, 68, 0.5)') : undefined
                  }}
                />
              </div>

              <div style={{ marginTop: '5px', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                {isEmailDomainValid ? (
                  <span style={{ color: '#34D399', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Check size={12} /> Valid municipal worker domain ({REQUIRED_WORKER_DOMAIN})
                  </span>
                ) : (
                  <span style={{ color: 'var(--text-muted)' }}>
                    Must end with <strong>{REQUIRED_WORKER_DOMAIN}</strong> (e.g. rahul{REQUIRED_WORKER_DOMAIN})
                  </span>
                )}
              </div>
            </div>

            {/* Field 4: Password */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)' }}>
                  Initial Password <span style={{ color: '#EF4444' }}>*</span>
                </label>
                <button
                  type="button"
                  onClick={handleGeneratePassword}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: '#FBBF24',
                    fontSize: '11px',
                    fontWeight: 600,
                    cursor: 'pointer'
                  }}
                >
                  Suggest PIN
                </button>
              </div>

              <div style={{ position: 'relative' }}>
                <input
                  type={showPassword ? 'text' : 'password'}
                  placeholder="At least 6 characters"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={6}
                  className="input-field"
                  style={{ paddingRight: '40px' }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    color: 'var(--text-muted)',
                    cursor: 'pointer'
                  }}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            {/* Field 5: Phone Number */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Mobile Number <span style={{ color: '#EF4444' }}>*</span>
              </label>
              <input
                type="tel"
                placeholder="+91 98801 00000"
                required
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="input-field"
              />
            </div>

            {/* Field 6: Vehicle Name & Type */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                  Assigned Vehicle
                </label>
                <input
                  type="text"
                  placeholder="e.g. Tipper #07"
                  value={vehicle}
                  onChange={(e) => setVehicle(e.target.value)}
                  className="input-field"
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                  Vehicle Class
                </label>
                <select
                  value={vehicleType}
                  onChange={(e) => setVehicleType(e.target.value)}
                  className="input-field"
                >
                  <option value="Heavy Compactor (12 Ton)" style={{ background: '#0D1322' }}>Heavy Compactor</option>
                  <option value="Zero-Emission EV (4 Ton)" style={{ background: '#0D1322' }}>Electric EV Tipper</option>
                  <option value="Rapid Support Van #02" style={{ background: '#0D1322' }}>Rapid Support Van</option>
                  <option value="Hazmat Waste Carrier" style={{ background: '#0D1322' }}>Hazmat Carrier</option>
                  <option value="Medium Tipper (6 Ton)" style={{ background: '#0D1322' }}>Medium Tipper</option>
                </select>
              </div>
            </div>

            {/* Field 7: Operating Ward */}
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Assigned Operating Ward
              </label>
              <select
                value={wardId}
                onChange={(e) => setWardId(e.target.value)}
                className="input-field"
              >
                {MUNICIPAL_WARDS.filter(w => w.id !== 'all').map(w => (
                  <option key={w.id} value={w.id} style={{ background: '#0D1322' }}>
                    {w.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={isLoading}
              className="btn btn-primary"
              style={{
                marginTop: '10px',
                height: '46px',
                fontSize: '14px',
                fontWeight: 700,
                justifyContent: 'center',
                gap: '8px'
              }}
            >
              {isLoading ? (
                <>
                  <RefreshCw size={16} className="animate-spin" />
                  <span>Connecting with Firebase...</span>
                </>
              ) : (
                <>
                  <UserPlus size={16} />
                  <span>Register Worker with Firebase</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Right Column: Registered Workers Directory */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          
          {/* Search & Ward Filter Bar */}
          <div className="glass-panel" style={{ padding: '16px 20px', display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
            <div style={{ position: 'relative', flex: 1, minWidth: '220px' }}>
              <Search size={16} color="var(--text-muted)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
              <input
                type="text"
                placeholder="Search workers by ID, name, email or vehicle..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="input-field"
                style={{ paddingLeft: '38px', height: '38px', fontSize: '13px' }}
              />
            </div>

            <select
              value={filterWard}
              onChange={(e) => setFilterWard(e.target.value)}
              className="input-field"
              style={{ width: 'auto', minWidth: '180px', height: '38px', fontSize: '12.5px' }}
            >
              <option value="all" style={{ background: '#0D1322' }}>All Wards & Zones</option>
              {MUNICIPAL_WARDS.filter(w => w.id !== 'all').map(w => (
                <option key={w.id} value={w.id} style={{ background: '#0D1322' }}>
                  {w.name}
                </option>
              ))}
            </select>
          </div>

          {/* Directory Count */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0 4px' }}>
            <span style={{ fontSize: '13px', fontWeight: 700, color: 'var(--text-secondary)' }}>
              Registered Municipal Field Personnel ({filteredWorkers.length})
            </span>
            <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
              Domain Policy: <strong style={{ color: 'var(--cyan-400)' }}>{REQUIRED_WORKER_DOMAIN}</strong> Enforced
            </span>
          </div>

          {/* Workers Cards / Table */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {filteredWorkers.map((worker) => {
              const displayEmail = worker.email || `${worker.id.toLowerCase()}${REQUIRED_WORKER_DOMAIN}`;
              return (
                <div
                  key={worker.id}
                  className="glass-panel"
                  style={{
                    padding: '18px 20px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '16px',
                    flexWrap: 'wrap'
                  }}
                >
                  {/* Left: ID, Name, Email */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '14px', minWidth: '240px' }}>
                    <div style={{
                      width: '42px',
                      height: '42px',
                      borderRadius: '12px',
                      background: 'rgba(255, 255, 255, 0.04)',
                      border: '1px solid var(--glass-border)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontFamily: 'var(--font-mono)',
                      fontSize: '11px',
                      fontWeight: 800,
                      color: 'var(--emerald-400)'
                    }}>
                      {worker.id.split('-').pop() || 'WRK'}
                    </div>

                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <h4 style={{ fontSize: '15px', fontWeight: 800, color: '#FFF' }}>{worker.name}</h4>
                        <span style={{
                          fontSize: '11px',
                          fontFamily: 'var(--font-mono)',
                          background: 'rgba(255, 255, 255, 0.06)',
                          padding: '1px 6px',
                          borderRadius: '4px',
                          color: 'var(--text-muted)'
                        }}>
                          {worker.id}
                        </span>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px', fontSize: '12px' }}>
                        <span style={{ color: 'var(--cyan-400)', display: 'flex', alignItems: 'center', gap: '4px', fontWeight: 600 }}>
                          <Mail size={12} />
                          {displayEmail}
                        </span>
                        {editingPhoneId === worker.id ? (
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                            <input
                              type="tel"
                              autoFocus
                              value={editPhoneValue}
                              onChange={(e) => setEditPhoneValue(e.target.value)}
                              placeholder="+91 98801 00000"
                              className="input-field"
                              style={{ padding: '3px 8px', fontSize: 12, width: 150 }}
                            />
                            <button type="button" className="btn btn-secondary btn-sm" style={{ padding: '3px 8px', fontSize: 11 }}
                              onClick={() => savePhone(worker)}>Save</button>
                            <button type="button" className="btn btn-secondary btn-sm" style={{ padding: '3px 8px', fontSize: 11 }}
                              onClick={() => setEditingPhoneId(null)}>Cancel</button>
                          </span>
                        ) : (
                          <>
                            <WorkerPhone phone={workerPhone(worker)} />
                            <button type="button" onClick={() => { setEditingPhoneId(worker.id); setEditPhoneValue(workerPhone(worker)); }}
                              style={{ background: 'none', border: 'none', color: 'var(--emerald-400)', fontSize: 11, cursor: 'pointer', padding: 0 }}>
                              {workerPhone(worker) ? 'Edit' : 'Add mobile'}
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Middle: Vehicle & Ward */}
                  <div style={{ fontSize: '12px', minWidth: '180px' }}>
                    <div style={{ color: '#FFF', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Truck size={13} color="var(--emerald-400)" />
                      <span>{worker.vehicle}</span>
                    </div>
                    <div style={{ color: 'var(--text-muted)', marginTop: '2px' }}>
                      📍 {worker.wardName || 'Downtown Ward'}
                    </div>
                  </div>

                  {/* Right: Status & Action */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <StatusBadge status={worker.status || 'Available'} />

                    <button
                      type="button"
                      onClick={() => {
                        const copyStr = `Worker ID: ${worker.id}\nEmail: ${displayEmail}\nVehicle: ${worker.vehicle}`;
                        navigator.clipboard.writeText(copyStr);
                      }}
                      title="Copy credentials summary"
                      className="btn btn-secondary btn-sm"
                      style={{ padding: '6px 10px', fontSize: '11.5px' }}
                    >
                      <Copy size={13} />
                      <span>Copy</span>
                    </button>
                  </div>
                </div>
              );
            })}

            {filteredWorkers.length === 0 && (
              <div className="glass-panel" style={{ padding: '44px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                <UserPlus size={32} color="var(--emerald-400)" style={{ margin: '0 auto 12px auto', opacity: 0.8 }} />
                <h4 style={{ fontSize: '15px', fontWeight: 700, color: '#FFF', marginBottom: '6px' }}>
                  {workers.length === 0 ? 'No Workers Registered Yet' : 'No Matching Personnel Found'}
                </h4>
                <p style={{ fontSize: '12.5px', color: 'var(--text-secondary)', maxWidth: '340px', margin: '0 auto' }}>
                  {workers.length === 0 
                    ? 'Use the registration form on the left to provision official @worker.in municipal field worker credentials.'
                    : 'Try adjusting your search query or ward filter to find field workers.'}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
