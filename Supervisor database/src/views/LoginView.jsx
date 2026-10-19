import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Lock, 
  Mail, 
  User, 
  AlertCircle, 
  ArrowRight, 
  CheckCircle2, 
  Building2,
  Sparkles,
  Info,
  Eye,
  EyeOff
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function LoginView() {
  const { login, register, authError, setAuthError, requiredDomain, isFirebaseLive } = useAuth();
  
  const [isRegisterMode, setIsRegisterMode] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successNotice, setSuccessNotice] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setAuthError(null);
    setSuccessNotice(null);

    // Validation
    const cleanEmail = email.trim();
    if (!cleanEmail.toLowerCase().endsWith(requiredDomain.toLowerCase())) {
      setAuthError(`Unauthorized Domain: Supervisors must sign in with their official "${requiredDomain}" email account.`);
      return;
    }

    if (password.length < 6) {
      setAuthError('Password must contain at least 6 characters.');
      return;
    }

    setIsSubmitting(true);
    try {
      if (isRegisterMode) {
        await register(cleanEmail, password, fullName);
        setSuccessNotice('Supervisor account successfully registered in Firebase! Loading dashboard...');
      } else {
        await login(cleanEmail, password);
      }
    } catch (err) {
      // Error message is set in AuthContext
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px',
      position: 'relative',
      background: 'radial-gradient(ellipse at 50% 20%, #0F1D36 0%, #070B13 70%)'
    }}>
      {/* Background Decorative Rings */}
      <div style={{
        position: 'absolute',
        width: '540px',
        height: '540px',
        borderRadius: '50%',
        border: '1px solid rgba(16, 185, 129, 0.1)',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        pointerEvents: 'none'
      }} />
      <div style={{
        position: 'absolute',
        width: '780px',
        height: '780px',
        borderRadius: '50%',
        border: '1px solid rgba(6, 182, 212, 0.05)',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        pointerEvents: 'none'
      }} />

      <div className="glass-panel" style={{
        width: '100%',
        maxWidth: '460px',
        padding: '38px',
        borderRadius: '24px',
        position: 'relative',
        zIndex: 10
      }}>
        {/* Brand Header */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div style={{
            width: '58px',
            height: '58px',
            borderRadius: '16px',
            background: 'linear-gradient(135deg, #064E3B 0%, #10B981 100%)',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 24px rgba(16, 185, 129, 0.35)',
            border: '1.5px solid rgba(255, 255, 255, 0.2)',
            marginBottom: '14px'
          }}>
            <ShieldCheck size={32} color="#FFFFFF" />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginBottom: '4px' }}>
            <h1 style={{ fontSize: '24px', fontWeight: 800 }}>WasteRoute</h1>
            <span style={{
              background: 'rgba(16, 185, 129, 0.15)',
              color: '#34D399',
              fontSize: '11px',
              fontWeight: 800,
              padding: '2px 8px',
              borderRadius: '6px',
              border: '1px solid rgba(16, 185, 129, 0.35)'
            }}>
              SUPERVISOR
            </span>
          </div>

          <p style={{ fontSize: '13.5px', color: 'var(--text-secondary)' }}>
            Official Municipal Waste Authority Operations Console
          </p>
        </div>

        {/* Special Domain Requirement Notice Banner */}
        <div style={{
          background: 'rgba(6, 182, 212, 0.08)',
          border: '1px solid rgba(6, 182, 212, 0.25)',
          borderRadius: '12px',
          padding: '12px 14px',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'flex-start',
          gap: '10px'
        }}>
          <Info size={18} color="var(--cyan-400)" style={{ flexShrink: 0, marginTop: '2px' }} />
          <div style={{ fontSize: '12px', color: 'var(--text-secondary)', lineHeight: '1.45' }}>
            <strong style={{ color: '#22D3EE' }}>Restricted Access:</strong> Only supervisor emails ending with <code style={{ color: '#34D399', background: 'rgba(0,0,0,0.3)', padding: '1px 5px', borderRadius: '4px' }}>{requiredDomain}</code> can authenticate to this dashboard.
          </div>
        </div>

        {/* Error Alert */}
        {authError && (
          <div style={{
            background: 'rgba(239, 68, 68, 0.12)',
            border: '1px solid rgba(239, 68, 68, 0.4)',
            borderRadius: '12px',
            padding: '14px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '10px'
          }}>
            <AlertCircle size={18} color="#F87171" style={{ flexShrink: 0, marginTop: '2px' }} />
            <span style={{ fontSize: '12.5px', color: '#FCA5A5', lineHeight: '1.4' }}>
              {authError}
            </span>
          </div>
        )}

        {/* Success Alert */}
        {successNotice && (
          <div style={{
            background: 'rgba(16, 185, 129, 0.12)',
            border: '1px solid rgba(16, 185, 129, 0.4)',
            borderRadius: '12px',
            padding: '12px 14px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
          }}>
            <CheckCircle2 size={18} color="#34D399" />
            <span style={{ fontSize: '12.5px', color: '#A7F3D0' }}>
              {successNotice}
            </span>
          </div>
        )}

        {/* Authentication Form */}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {isRegisterMode && (
            <div>
              <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
                Full Name & Rank
              </label>
              <div style={{ position: 'relative' }}>
                <User size={16} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="text"
                  placeholder="e.g. Officer Swagata Dey"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="input-field"
                  style={{ paddingLeft: '40px' }}
                  required={isRegisterMode}
                />
              </div>
            </div>
          )}

          <div>
            <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '6px' }}>
              Supervisor Email ({requiredDomain})
            </label>
            <div style={{ position: 'relative' }}>
              <Mail size={16} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
              <input
                type="email"
                placeholder={`officer${requiredDomain}`}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="input-field"
                style={{ paddingLeft: '40px' }}
                required
              />
            </div>
          </div>

          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
              <label style={{ fontSize: '12.5px', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Access Password
              </label>
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--emerald-400)',
                  fontSize: '11.5px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  padding: 0
                }}
              >
                {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
                <span>{showPassword ? 'Hide Password' : 'Show Password'}</span>
              </button>
            </div>
            <div style={{ position: 'relative' }}>
              <Lock size={16} color="var(--text-muted)" style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)' }} />
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="input-field"
                style={{ paddingLeft: '40px', paddingRight: '44px' }}
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                title={showPassword ? 'Hide Password' : 'Show Password'}
                style={{
                  position: 'absolute',
                  right: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'transparent',
                  border: 'none',
                  cursor: 'pointer',
                  color: showPassword ? 'var(--emerald-400)' : 'var(--text-muted)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: '4px'
                }}
              >
                {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
              </button>
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="btn btn-primary btn-lg"
            style={{ width: '100%', marginTop: '6px' }}
          >
            {isSubmitting ? (
              <span>Authenticating with Firebase...</span>
            ) : (
              <>
                <span>{isRegisterMode ? 'Register & Access Dashboard' : 'Sign In as Supervisor'}</span>
                <ArrowRight size={17} />
              </>
            )}
          </button>
        </form>

        {/* Mode Switcher */}
        <div style={{ textAlign: 'center', marginTop: '22px' }}>
          <button
            type="button"
            onClick={() => {
              setIsRegisterMode(!isRegisterMode);
              setAuthError(null);
            }}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--emerald-400)',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              textDecoration: 'underline'
            }}
          >
            {isRegisterMode ? '← Already provisioned? Back to Sign In' : 'Need new supervisor credentials? Register here →'}
          </button>
        </div>

        {/* Firebase Environment Metadata Footer */}
        <div style={{
          marginTop: '22px',
          padding: '10px 14px',
          background: 'rgba(0, 0, 0, 0.35)',
          borderRadius: '10px',
          display: 'flex',
          justifyContent: 'space-between',
          fontSize: '10.5px',
          color: 'var(--text-muted)'
        }}>
          <span>Firebase Project: <strong style={{ color: '#E2E8F0' }}>wasteroute-d393c</strong></span>
          <span style={{ color: isFirebaseLive ? '#34D399' : '#F59E0B' }}>
            {isFirebaseLive ? '● Live SDK Active' : '● Local Mode'}
          </span>
        </div>
      </div>
    </div>
  );
}
