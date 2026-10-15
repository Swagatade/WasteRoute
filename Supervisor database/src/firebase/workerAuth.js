import { initializeApp, getApps } from "firebase/app";
import { getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword, updateProfile, signOut } from "firebase/auth";
import { serverTimestamp } from "firebase/firestore";
import { upsertWorkerInFirestore } from "./firestoreService";
import { firebaseConfig, db, isFirebaseConfigValid } from "./config";

export const REQUIRED_WORKER_DOMAIN = "@worker.in";
const STORAGE_KEY_WORKERS = "westroute_registered_workers";

/**
 * Validates if an email address belongs to the official @worker.in municipal domain.
 */
export function validateWorkerDomain(email) {
  const clean = (email || "").trim().toLowerCase();
  const domain = REQUIRED_WORKER_DOMAIN.toLowerCase();
  return clean.endsWith(domain) && clean.length > domain.length;
}

/**
 * Normalizes email or worker ID to ensure standard @worker.in format.
 */
export function formatWorkerEmail(input) {
  const clean = (input || "").trim().toLowerCase();
  if (!clean) return "";
  if (clean.endsWith(REQUIRED_WORKER_DOMAIN)) return clean;
  // If user only typed username or workerId, append @worker.in
  const sanitized = clean.replace(/[^a-z0-9._-]/g, "");
  return `${sanitized}${REQUIRED_WORKER_DOMAIN}`;
}

/**
 * Retrieves the secondary Firebase Auth instance for worker provisioning.
 * This guarantees the current Supervisor session remains active and is NOT replaced.
 */
function getWorkerAdminAuth() {
  const appName = "SecondaryWorkerProvisioningApp";
  const existingApps = getApps();
  let secondaryApp = existingApps.find((app) => app.name === appName);

  if (!secondaryApp) {
    secondaryApp = initializeApp(firebaseConfig, appName);
  }

  return getAuth(secondaryApp);
}

/**
 * Registers a new worker in Firebase Authentication and the Municipal Fleet Registry.
 * 
 * @param {Object} workerData
 * @param {string} workerData.workerId e.g. "WRK-106"
 * @param {string} workerData.name e.g. "Arun Patel"
 * @param {string} workerData.email e.g. "arun.patel@worker.in"
 * @param {string} workerData.password e.g. "worker123"
 * @param {string} workerData.phone e.g. "+91 98801 88771"
 * @param {string} workerData.vehicle e.g. "Compactor Truck #06"
 * @param {string} workerData.vehicleType e.g. "Heavy Compactor (12 Ton)"
 * @param {string} workerData.wardId e.g. "ward-01"
 * @param {string} workerData.wardName e.g. "Ward 01 - Downtown"
 */
export async function registerWorkerWithFirebase(workerData) {
  const {
    workerId,
    name,
    email,
    password,
    phone = "",
    vehicle = "Standard Sanitation Unit #01",
    vehicleType = "Medium Tipper (6 Ton)",
    wardId = "ward-01",
    wardName = "Ward 01 - Downtown Commercial"
  } = workerData;

  const cleanWorkerId = (workerId || "").trim().toUpperCase();
  if (!cleanWorkerId) {
    throw new Error("Worker ID is mandatory (e.g. WRK-106 or WR-W-205).");
  }

  const cleanEmail = (email || "").trim().toLowerCase();
  if (!validateWorkerDomain(cleanEmail)) {
    throw new Error(
      `Worker email must end with "${REQUIRED_WORKER_DOMAIN}" (e.g. ${cleanWorkerId.toLowerCase()}${REQUIRED_WORKER_DOMAIN}).`
    );
  }

  if (!password || password.length < 6) {
    throw new Error("Worker password must be at least 6 characters.");
  }

  let firebaseUid = `worker-${Date.now()}`;
  let isLiveAuthRegistered = false;
  let alreadyExisted = false;

  // Helper for non-blocking timeout
  const withTimeout = (promise, ms = 3000) =>
    Promise.race([
      promise,
      new Promise((resolve) => setTimeout(() => resolve(null), ms))
    ]);

  // 1. Register with Firebase Authentication using isolated secondary app
  if (isFirebaseConfigValid) {
    try {
      const secondaryAuth = getWorkerAdminAuth();
      const userCredential = await createUserWithEmailAndPassword(secondaryAuth, cleanEmail, password);
      const newFirebaseUser = userCredential.user;
      firebaseUid = newFirebaseUser.uid;
      isLiveAuthRegistered = true;

      // Update profile with worker name (with timeout so it never hangs)
      if (name) {
        try {
          await withTimeout(
            updateProfile(newFirebaseUser, {
              displayName: `${name} (${cleanWorkerId})`
            }),
            3000
          );
        } catch (pErr) {
          console.warn("Profile update notice:", pErr);
        }
      }

      // Immediately sign out secondary auth so it remains clean
      try {
        await withTimeout(signOut(secondaryAuth), 2000);
      } catch (sErr) {
        console.warn("Secondary auth signout notice:", sErr);
      }
    } catch (err) {
      if (err.code === "auth/email-already-in-use") {
        // If email was already created (e.g. from previous attempt), treat as registered
        console.info(`Worker email ${cleanEmail} already exists in Firebase Auth.`);
        isLiveAuthRegistered = true;
        alreadyExisted = true;
        // Link the existing account: with the right password we can recover its real uid.
        try {
          const secondaryAuth = getWorkerAdminAuth();
          const cred = await withTimeout(signInWithEmailAndPassword(secondaryAuth, cleanEmail, password), 4000);
          if (cred && cred.user) {
            firebaseUid = cred.user.uid;
            await withTimeout(signOut(secondaryAuth), 2000);
          }
        } catch (linkErr) {
          console.info("Existing worker account found; uid will be linked on the worker's first login.", linkErr?.code);
        }
      } else if (err.code === "auth/weak-password") {
        throw new Error("Password is too weak. Please use at least 6 alphanumeric characters.");
      } else if (err.code === "auth/network-request-failed") {
        console.warn("Firebase Auth network error. Falling back to municipal offline storage.");
      } else {
        throw new Error(`Firebase Auth Error: ${err.message}`);
      }
    }
  }

  // 2. Prepare Municipal Worker Record
  const newWorkerRecord = {
    id: cleanWorkerId,
    workerId: cleanWorkerId,
    uid: firebaseUid,
    name: name.trim() || `Worker ${cleanWorkerId}`,
    email: cleanEmail,
    vehicle: vehicle.trim(),
    vehicleType: vehicleType.trim(),
    status: "Available",
    wardId: wardId,
    wardName: wardName,
    phone: phone.trim(),
    rating: 5.0,
    tasksCompletedToday: 0,
    batteryFuel: "100%",
    registeredAt: new Date().toISOString(),
    syncError: null,
    isLiveFirebase: isLiveAuthRegistered,
    alreadyExisted: alreadyExisted,
    currentLocation: {
      lat: 12.8235,
      lng: 80.0442,
      street: "SRM KTR Campus"
    }
  };

  // 3. Save to Firestore and WAIT for confirmation, so a failure is reported instead of silently lost.
  if (db && isFirebaseConfigValid) {
    try {
      const { syncError, ...record } = newWorkerRecord;
      await Promise.race([
        upsertWorkerInFirestore({ ...record, registeredAt: serverTimestamp() }),
        new Promise((_, reject) => setTimeout(() => reject(new Error("Firestore did not respond (offline or database not created).")), 8000))
      ]);
    } catch (fsErr) {
      console.warn("Could not save to Firestore collection 'workers':", fsErr);
      newWorkerRecord.syncError = fsErr?.message || String(fsErr);
    }
  }

  // 4. Update local storage cache immediately
  saveWorkerToLocalStorage(newWorkerRecord);

  return newWorkerRecord;
}


/**
 * Loads registered workers from local storage cache.
 */
export function getRegisteredWorkersFromStorage() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_WORKERS);
    return raw ? JSON.parse(raw) : [];
  } catch (e) {
    console.error("Failed to parse stored workers:", e);
    return [];
  }
}

/**
 * Saves or updates a registered worker in local storage cache.
 */
export function saveWorkerToLocalStorage(worker) {
  try {
    const existing = getRegisteredWorkersFromStorage();
    const filtered = existing.filter((w) => w.id !== worker.id && w.email !== worker.email);
    const updated = [worker, ...filtered];
    localStorage.setItem(STORAGE_KEY_WORKERS, JSON.stringify(updated));
  } catch (e) {
    console.error("Failed to cache worker in localStorage:", e);
  }
}
