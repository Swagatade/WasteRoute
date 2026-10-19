import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getAnalytics, isSupported } from "firebase/analytics";

/**
 * Firebase Web Configuration
 * ALL API keys and project identifiers are loaded exclusively from the .env environment file.
 * No hardcoded keys exist in this source file.
 */
export const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
};

// Validate that required environment variables are present
export const isFirebaseConfigValid = Boolean(
  firebaseConfig.apiKey &&
  firebaseConfig.projectId &&
  firebaseConfig.appId
);

if (!firebaseConfig.measurementId) {
  console.error(
    "VITE_FIREBASE_MEASUREMENT_ID is not set. Analytics is disabled. " +
    "Set it in .env (or the host's build settings) and rebuild; Vite reads it at build time."
  );
}

let app = null;
let auth = null;
let db = null;
let analytics = null;

try {
  if (isFirebaseConfigValid) {
    app = initializeApp(firebaseConfig);
    auth = getAuth(app);
    try {
      db = getFirestore(app);
    } catch (dbErr) {
      console.warn("Firestore initialization error:", dbErr);
    }

    // Initialize Analytics if supported in the current environment
    if (typeof window !== "undefined") {
      isSupported().then((supported) => {
        if (supported) {
          analytics = getAnalytics(app);
        }
      }).catch(() => {
        // Analytics disabled or unsupported in local testing
      });
    }
  } else {
    console.warn("⚠️ Firebase configuration missing required environment variables in .env");
  }
} catch (error) {
  console.error("Firebase initialization error:", error);
}

export { app, auth, db, analytics };
export default app;

