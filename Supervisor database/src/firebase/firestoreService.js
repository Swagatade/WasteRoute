import { 
  collection, 
  doc, 
  getDocs, 
  setDoc, 
  updateDoc, 
  addDoc, 
  deleteDoc,
  runTransaction,
  writeBatch,
  query,
  where,
  onSnapshot, 
  serverTimestamp 
} from "firebase/firestore";
import { db, isFirebaseConfigValid } from "./config";
import { INITIAL_COMPLAINTS, INITIAL_SMART_BINS } from "../data/mockData";

const COLLECTION_COMPLAINTS = "complaints";
const COLLECTION_BINS = "smart_bins";
const COLLECTION_WORKERS = "workers";
const COLLECTION_SCHEDULES = "schedules";
const COLLECTION_LOGS = "collection_logs";
const COLLECTION_TEMPLATES = "schedule_templates";

/**
 * Seeds initial SRM KTR smart bins and complaints to Firestore if collections are empty.
 * Guarantees that both the Web Supervisor and Android app share the exact same dataset immediately.
 */
export async function seedInitialFirestoreDataIfNeeded() {
  // Mock seeding removed: Firestore holds only real data.
}

/**
 * Subscribes to real-time changes in the complaints collection.
 * Triggers callback whenever a new complaint is lodged (e.g. from Android app) or updated.
 */
export function subscribeToComplaints(onDataUpdated) {
  if (!db || !isFirebaseConfigValid) {
    onDataUpdated(INITIAL_COMPLAINTS);
    return () => {};
  }

  const complaintsRef = collection(db, COLLECTION_COMPLAINTS);
  const unsubscribe = onSnapshot(
    complaintsRef,
    (snapshot) => {
      if (snapshot.empty) {
        onDataUpdated(INITIAL_COMPLAINTS);
        return;
      }
      const complaintsList = snapshot.docs.map((docSnap) => ({
        id: docSnap.id,
        ...docSnap.data()
      }));
      onDataUpdated(complaintsList);
    },
    (error) => {
      console.warn("Complaints real-time sync notice:", error);
      onDataUpdated(INITIAL_COMPLAINTS);
    }
  );

  return unsubscribe;
}

/**
 * Subscribes to real-time changes in the smart_bins collection.
 */
export function subscribeToSmartBins(onDataUpdated) {
  if (!db || !isFirebaseConfigValid) {
    onDataUpdated(INITIAL_SMART_BINS);
    return () => {};
  }

  const binsRef = collection(db, COLLECTION_BINS);
  const unsubscribe = onSnapshot(
    binsRef,
    (snapshot) => {
      if (snapshot.empty) {
        onDataUpdated(INITIAL_SMART_BINS);
        return;
      }
      const binsList = snapshot.docs.map((docSnap) => ({
        id: docSnap.id,
        ...docSnap.data()
      }));
      onDataUpdated(binsList);
    },
    (error) => {
      console.warn("Smart Bins real-time sync notice:", error);
      onDataUpdated(INITIAL_SMART_BINS);
    }
  );

  return unsubscribe;
}

/**
 * Subscribes to real-time changes in the workers collection.
 */
export function subscribeToWorkers(onDataUpdated) {
  if (!db || !isFirebaseConfigValid) {
    return () => {};
  }

  const workersRef = collection(db, COLLECTION_WORKERS);
  const unsubscribe = onSnapshot(
    workersRef,
    (snapshot) => {
      // Always report the server state (including an empty list) so the UI never shows stale data.
      const workersList = snapshot.docs.map((docSnap) => ({
        ...docSnap.data(),
        id: docSnap.id
      }));
      onDataUpdated(workersList);
    },
    (error) => {
      console.warn("Workers real-time sync notice:", error);
    }
  );

  return unsubscribe;
}

/**
 * Writes (merges) a worker record into Firestore and waits for the server to confirm.
 * Throws on failure so callers can tell the supervisor instead of silently losing the record.
 */
export async function upsertWorkerInFirestore(worker) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const id = worker.workerId || worker.id;
  await setDoc(doc(db, COLLECTION_WORKERS, id), worker, { merge: true });
}

/**
 * Real-time timetable: every collection slot a supervisor has scheduled.
 */
export function subscribeToSchedules(onDataUpdated) {
  if (!db || !isFirebaseConfigValid) {
    onDataUpdated([]);
    return () => {};
  }
  return onSnapshot(
    collection(db, COLLECTION_SCHEDULES),
    (snapshot) => {
      const list = snapshot.docs
        .map((d) => ({ ...d.data(), id: d.id }))
        .sort((a, b) => `${a.date} ${a.startTime}`.localeCompare(`${b.date} ${b.startTime}`));
      onDataUpdated(list);
    },
    (error) => {
      console.warn("Schedules real-time sync notice:", error);
    }
  );
}

/**
 * Citizen-confirmed pickups (written by the Android app when a resident scans a worker QR).
 */
export function subscribeToCollectionLogs(onDataUpdated) {
  if (!db || !isFirebaseConfigValid) {
    onDataUpdated([]);
    return () => {};
  }
  return onSnapshot(
    collection(db, COLLECTION_LOGS),
    (snapshot) => {
      const list = snapshot.docs.map((d) => ({ ...d.data(), id: d.id }));
      list.sort((a, b) => (b.confirmedAt?.seconds || 0) - (a.confirmedAt?.seconds || 0));
      onDataUpdated(list);
    },
    (error) => {
      console.warn("Collection logs sync notice:", error);
    }
  );
}

/**
 * Assigns a worker to a place + time slot. Appears instantly on the worker's phone (timetable + map).
 */
export async function addScheduleToFirestore(schedule) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const ref = await addDoc(collection(db, COLLECTION_SCHEDULES), {
    ...schedule,
    status: "Scheduled",
    createdAt: serverTimestamp()
  });
  return ref.id;
}

export async function deleteScheduleFromFirestore(scheduleId) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  await deleteDoc(doc(db, COLLECTION_SCHEDULES, scheduleId));
}

/**
 * Creates a new complaint in Firestore (accessible to both Android and Web).
 */
export async function addComplaintToFirestore(complaintData) {
  if (!db || !isFirebaseConfigValid) return null;

  try {
    const docId = complaintData.id || `CMP-${Date.now().toString().slice(-4)}`;
    const docRef = doc(db, COLLECTION_COMPLAINTS, docId);
    await setDoc(docRef, {
      ...complaintData,
      id: docId,
      createdAt: serverTimestamp()
    });
    return docId;
  } catch (err) {
    console.error("Failed to add complaint to Firestore:", err);
    throw err;
  }
}

/**
 * Updates a complaint's status in Firestore.
 */
export async function updateComplaintStatusInFirestore(complaintId, newStatus) {
  if (!db || !isFirebaseConfigValid) return;

  try {
    const docRef = doc(db, COLLECTION_COMPLAINTS, complaintId);
    const stageByStatus = { 'Pending': 'reported', 'Under Review': 'reported', 'Assigned': 'accepted', 'In Progress': 'on_the_way', 'Resolved': 'resolved' };
    const patch = { status: newStatus, updatedAt: serverTimestamp() };
    if (stageByStatus[newStatus]) patch.stage = stageByStatus[newStatus];
    if (newStatus === 'Resolved') { patch.progress = 100; patch.resolvedAtMs = Date.now(); }
    await updateDoc(docRef, patch);
  } catch (err) {
    console.warn(`Could not update complaint ${complaintId} in Firestore:`, err);
  }
}

/**
 * Assigns a worker to a complaint in Firestore.
 */
export async function assignWorkerToComplaintInFirestore(complaintId, workerId, workerName) {
  if (!db || !isFirebaseConfigValid) return;

  try {
    const docRef = doc(db, COLLECTION_COMPLAINTS, complaintId);
    await updateDoc(docRef, {
      status: "Assigned",
      stage: "accepted",
      assignedWorkerId: workerId,
      assignedWorkerName: workerName,
      assignedAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  } catch (err) {
    console.warn(`Could not assign worker to complaint ${complaintId} in Firestore:`, err);
  }
}

/**
 * Updates a Smart Bin fill level or status in Firestore.
 */
export async function updateBinInFirestore(binId, updates) {
  if (!db || !isFirebaseConfigValid) return;

  try {
    const docRef = doc(db, COLLECTION_BINS, binId);
    await updateDoc(docRef, {
      ...updates,
      updatedAt: serverTimestamp()
    });
  } catch (err) {
    console.warn(`Could not update bin ${binId} in Firestore:`, err);
  }
}


// ---------------------------------------------------------------------------
// Daily recurring routines ("schedule_templates") -> concrete daily slots ("schedules")
// ---------------------------------------------------------------------------

const SLOT_FIELDS = [
  "workerId", "workerName", "department", "departmentId",
  "locationName", "lat", "lng", "startTime", "endTime", "notes"
];

export function toDateString(d = new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function pickSlotFields(obj) {
  const out = {};
  SLOT_FIELDS.forEach((k) => { if (obj[k] !== undefined) out[k] = obj[k]; });
  return out;
}

export function subscribeToScheduleTemplates(onDataUpdated) {
  if (!db || !isFirebaseConfigValid) {
    onDataUpdated([]);
    return () => {};
  }
  return onSnapshot(
    collection(db, COLLECTION_TEMPLATES),
    (snapshot) => onDataUpdated(snapshot.docs.map((d) => ({ ...d.data(), id: d.id }))),
    (error) => console.warn("Schedule templates sync notice:", error)
  );
}

export async function addScheduleTemplate(template) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const ref = await addDoc(collection(db, COLLECTION_TEMPLATES), {
    ...template,
    active: true,
    createdAt: serverTimestamp()
  });
  return ref.id;
}

/** Edits a daily routine and applies the change to every upcoming, not-yet-completed slot it generated. */
export async function updateScheduleTemplate(templateId, fields) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const batch = writeBatch(db);
  batch.update(doc(db, COLLECTION_TEMPLATES, templateId), { ...fields, updatedAt: serverTimestamp() });

  const today = toDateString();
  const snap = await getDocs(query(collection(db, COLLECTION_SCHEDULES), where("templateId", "==", templateId)));
  const slotChanges = pickSlotFields(fields);
  snap.docs.forEach((d) => {
    const data = d.data();
    if (data.date >= today && data.status !== "Completed" && Object.keys(slotChanges).length) {
      batch.update(d.ref, slotChanges);
    }
  });
  await batch.commit();
}

/** Deletes a daily routine and its upcoming, not-yet-completed slots (history is kept). */
export async function deleteScheduleTemplate(templateId) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const batch = writeBatch(db);
  batch.delete(doc(db, COLLECTION_TEMPLATES, templateId));
  const today = toDateString();
  const snap = await getDocs(query(collection(db, COLLECTION_SCHEDULES), where("templateId", "==", templateId)));
  snap.docs.forEach((d) => {
    const data = d.data();
    if (data.date >= today && data.status !== "Completed") batch.delete(d.ref);
  });
  await batch.commit();
}

/** Edits a single day's slot only. */
export async function updateScheduleSlot(scheduleId, fields) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  await updateDoc(doc(db, COLLECTION_SCHEDULES, scheduleId), { ...fields, updatedAt: serverTimestamp() });
}

const inFlight = new Set();

/**
 * Creates the concrete slot for every active daily routine for today and the next [daysAhead] days.
 * Slot ids are deterministic ("<templateId>_<date>") and created inside a transaction only when
 * missing, so running this from several devices never duplicates or overwrites a slot.
 */
export async function ensureDailySchedules(templates, existingIds = new Set(), daysAhead = 2) {
  if (!db || !isFirebaseConfigValid) return;
  const dates = [];
  for (let i = 0; i <= daysAhead; i++) {
    const d = new Date();
    d.setDate(d.getDate() + i);
    dates.push(toDateString(d));
  }
  for (const t of templates) {
    if (t.active === false) continue;
    for (const date of dates) {
      if (t.startDate && date < t.startDate) continue;
      const id = `${t.id}_${date}`;
      if (existingIds.has(id) || inFlight.has(id)) continue;
      inFlight.add(id);
      try {
        const ref = doc(db, COLLECTION_SCHEDULES, id);
        await runTransaction(db, async (tx) => {
          const snap = await tx.get(ref);
          if (snap.exists()) return;
          tx.set(ref, {
            ...pickSlotFields(t),
            templateId: t.id,
            date,
            status: "Scheduled",
            createdAt: serverTimestamp()
          });
        });
      } catch (e) {
        console.warn("Daily slot generation notice:", id, e?.message || e);
      } finally {
        inFlight.delete(id);
      }
    }
  }
}
