import React, { createContext, useContext, useState, useEffect } from 'react';
import { 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signOut as firebaseSignOut, 
  onAuthStateChanged,
  updateProfile
} from 'firebase/auth';
import { auth, isFirebaseConfigValid } from '../firebase/config';

const AuthContext = createContext(null);

const REQUIRED_SUPERVISOR_DOMAIN = import.meta.env.VITE_SUPERVISOR_ALLOWED_DOMAIN || '@supervisor.in';
const STORAGE_KEY = 'westroute_supervisor_session';

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authError, setAuthError] = useState(null);

  // Initialize and check existing session
  useEffect(() => {
    // Check local cached session first
    const cachedSession = localStorage.getItem(STORAGE_KEY);
    if (cachedSession) {
      try {
        const parsed = JSON.parse(cachedSession);
        if (parsed?.email?.toLowerCase().endsWith(REQUIRED_SUPERVISOR_DOMAIN.toLowerCase()) && !parsed.isOfflineMode) {
          setCurrentUser(parsed);
        } else {
          localStorage.removeItem(STORAGE_KEY);
        }
      } catch (e) {
        localStorage.removeItem(STORAGE_KEY);
      }
    }

    // Subscribe to Firebase Auth state change if auth is available
    if (auth && isFirebaseConfigValid) {
      const unsubscribe = onAuthStateChanged(auth, (firebaseUser) => {
        if (firebaseUser) {
          const email = firebaseUser.email || '';
          if (email.toLowerCase().endsWith(REQUIRED_SUPERVISOR_DOMAIN.toLowerCase())) {
            const userObj = {
              uid: firebaseUser.uid,
              email: firebaseUser.email,
              displayName: firebaseUser.displayName || email.split('@')[0].toUpperCase(),
              photoURL: firebaseUser.photoURL || null,
              role: 'Municipal Waste Operations Supervisor',
              wardAssignment: 'Downtown & Central Metropolitan Ward',
              isLiveFirebase: true
            };
            setCurrentUser(userObj);
            localStorage.setItem(STORAGE_KEY, JSON.stringify(userObj));
          } else {
            // Non-supervisor account detected
            firebaseSignOut(auth).catch(() => {});
            setCurrentUser(null);
            localStorage.removeItem(STORAGE_KEY);
            setAuthError(`Access Denied: The account ${email} is not authorized. Only official ${REQUIRED_SUPERVISOR_DOMAIN} addresses have supervisor clearance.`);
          }
        } else {
          setCurrentUser(null);
          localStorage.removeItem(STORAGE_KEY);
        }
        setLoading(false);
      });

      return () => unsubscribe();
    } else {
      setLoading(false);
    }
  }, []);

  /**
   * Validates if email belongs to the official @supervisor.in municipal domain
   */
  const validateSupervisorDomain = (email) => {
    const clean = (email || '').trim().toLowerCase();
    const domain = REQUIRED_SUPERVISOR_DOMAIN.toLowerCase();
    return clean.endsWith(domain) && clean.length > domain.length;
  };

  /**
   * Supervisor Sign-In with Domain Validation + Firebase Auth
   */
  const login = async (email, password) => {
    setAuthError(null);
    const cleanEmail = (email || '').trim();

    if (!validateSupervisorDomain(cleanEmail)) {
      const err = `Invalid Supervisor Email: Official municipal accounts must end with "${REQUIRED_SUPERVISOR_DOMAIN}" (e.g. officer${REQUIRED_SUPERVISOR_DOMAIN})`;
      setAuthError(err);
      throw new Error(err);
    }

    if (!password || password.length < 6) {
      const err = 'Password must be at least 6 characters.';
      setAuthError(err);
      throw new Error(err);
    }

    // Try Live Firebase Auth
    if (auth && isFirebaseConfigValid) {
      try {
        const userCredential = await signInWithEmailAndPassword(auth, cleanEmail, password);
        const fbUser = userCredential.user;
        const userObj = {
          uid: fbUser.uid,
          email: fbUser.email,
          displayName: fbUser.displayName || cleanEmail.split('@')[0].replace('.', ' ').toUpperCase(),
          role: 'Municipal Waste Operations Supervisor',
          wardAssignment: 'All Metropolitan Wards (Full Authority)',
          isLiveFirebase: true
        };
        setCurrentUser(userObj);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(userObj));
        return userObj;
        } catch (err) {
        // If user not found in Firebase, network error, or invalid password
        let message = err.message;
        if (err.code === 'auth/network-request-failed') {
          message = 'Firebase Network Connection Failed: Unable to reach Firebase auth servers. Please check your internet connection.';
        } else if (err.code === 'auth/user-not-found' || err.code === 'auth/invalid-credential') {
          message = 'No registered supervisor found with these credentials in Firebase Auth. You can register this account below or check the password.';
        } else if (err.code === 'auth/wrong-password') {
          message = 'Incorrect password. Please verify and try again.';
        } else if (err.code === 'auth/too-many-requests') {
          message = 'Access temporarily disabled due to multiple failed attempts. Try again later.';
        }
        setAuthError(message);
        throw new Error(message);
      }
    } else {
      const err = 'Firebase Authentication is not available. Please verify Firebase configuration.';
      setAuthError(err);
      throw new Error(err);
    }
  };

  /**
   * Supervisor Registration with Domain Validation + Firebase Auth
   */
  const register = async (email, password, fullName = '') => {
    setAuthError(null);
    const cleanEmail = (email || '').trim();

    if (!validateSupervisorDomain(cleanEmail)) {
      const err = `Registration Restricted: Supervisors must be provisioned under "${REQUIRED_SUPERVISOR_DOMAIN}" (e.g. yourname${REQUIRED_SUPERVISOR_DOMAIN})`;
      setAuthError(err);
      throw new Error(err);
    }

    if (!password || password.length < 6) {
      const err = 'Password must be at least 6 characters.';
      setAuthError(err);
      throw new Error(err);
    }

    if (auth && isFirebaseConfigValid) {
      try {
        const userCredential = await createUserWithEmailAndPassword(auth, cleanEmail, password);
        const fbUser = userCredential.user;

        if (fullName) {
          await updateProfile(fbUser, { displayName: fullName });
        }

        const userObj = {
          uid: fbUser.uid,
          email: fbUser.email,
          displayName: fullName || cleanEmail.split('@')[0].toUpperCase(),
          role: 'Municipal Waste Operations Supervisor',
          wardAssignment: 'New Operations Ward',
          isLiveFirebase: true
        };

        setCurrentUser(userObj);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(userObj));
        return userObj;
      } catch (err) {
        let message = err.message;
        if (err.code === 'auth/network-request-failed') {
          message = 'Firebase Network Connection Failed: Unable to reach Firebase auth servers. Please check your internet connection.';
        } else if (err.code === 'auth/email-already-in-use') {
          message = 'This supervisor email is already registered in Firebase. Please sign in instead.';
        } else if (err.code === 'auth/weak-password') {
          message = 'Password is too weak. Please use at least 6 characters.';
        }
        setAuthError(message);
        throw new Error(message);
      }
    } else {
      const err = 'Firebase Authentication is not available. Cannot register supervisor account offline.';
      setAuthError(err);
      throw new Error(err);
    }
  };

  /**
   * Supervisor Sign-Out
   */
  const logout = async () => {
    try {
      if (auth) {
        await firebaseSignOut(auth);
      }
    } catch (e) {
      console.warn("Sign out error", e);
    }
    setCurrentUser(null);
    setAuthError(null);
    localStorage.removeItem(STORAGE_KEY);
  };

  const value = {
    currentUser,
    loading,
    authError,
    setAuthError,
    login,
    register,
    logout,
    requiredDomain: REQUIRED_SUPERVISOR_DOMAIN,
    isFirebaseLive: isFirebaseConfigValid
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
