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
import { INITIAL_COMPLAINTS, INITIAL_SMART_BINS, DEFAULT_ZONES, HOSTEL_NAME_SUGGESTIONS } from "../data/mockData";
import campusData from "../data/campus_map_data.json";

const COLLECTION_COMPLAINTS = "complaints";
const COLLECTION_BINS = "smart_bins";
const COLLECTION_WORKERS = "workers";
const COLLECTION_SCHEDULES = "schedules";
const COLLECTION_LOGS = "collection_logs";
const COLLECTION_TEMPLATES = "schedule_templates";
const COLLECTION_HOUSEHOLDS = "households";
const COLLECTION_ZONES = "zones";

/**
 * Seeds initial SRM KTR smart bins and complaints to Firestore if collections are empty.
 * Guarantees that both the Web Supervisor and Android app share the exact same dataset immediately.
 */
export async function seedInitialFirestoreDataIfNeeded() {
  // Nothing to do on start-up: campus bins / hostel points are written by seedCampusBins / seedCampusHostel
  // the first time their (empty) collection is seen by the signed-in supervisor.
}

/**
 * Writes the campus bin points (UB Building, Tech Park, Java Green, Paari/Kaari Hostels, Main Gate 1, Medical
 * College Hospital, Library) from the project's campus_map_data.json into `smart_bins`.
 * Only call this when the collection is empty; both the web map and the Android app then read it live.
 */
export async function seedCampusBins() {
  if (!db || !isFirebaseConfigValid) return 0;
  const batch = writeBatch(db);
  campusData.smart_bins.forEach((b) => {
    batch.set(doc(db, COLLECTION_BINS, b.id), { ...b, createdAt: serverTimestamp() });
  });
  await batch.commit();
  return campusData.smart_bins.length;
}

/**
 * Writes the one hostel point named in the campus data (Paari / Kaari Hostel Courtyard) into `households`,
 * so the hostel layer is not empty. More blocks are added by clicking the Campus Map.
 */
export async function seedCampusHostel() {
  if (!db || !isFirebaseConfigValid) return 0;
  const hostelBin = campusData.smart_bins.find((b) => b.wardId === "ward-ktr-04");
  if (!hostelBin) return 0;
  await setDoc(doc(db, COLLECTION_HOUSEHOLDS, "HB-101"), {
    id: "HB-101",
    code: "HB-101",
    name: "Paari / Kaari Hostels",
    address: hostelBin.location,
    areaId: "ward-ktr-04",
    areaName: "SRM KTR Residential Zone (Paari & Kaari Hostels)",
    lat: hostelBin.coordinates.lat,
    lng: hostelBin.coordinates.lng,
    ownerUid: null,
    ownerName: null,
    status: "Pending",
    createdAt: serverTimestamp()
  });
  return 1;
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
 * One account is one worker. If a mistyped ID (e.g. "1O1" for "101") left a second record with the same
 * email, show only the real one in every section (registration, fleet, dispatch). The kept record is the
 * one whose ID matches the email name, else the one with the most supervisor-entered details.
 */
export function dedupeWorkersByEmail(workers) {
  const detail = (w) =>
    (w.phone ? 2 : 0) + (w.vehicle && w.vehicle !== 'Unassigned' ? 2 : 0) + (w.wardId && w.wardId !== 'ward-ktr-01' ? 1 : 0);
  const best = new Map();
  const noEmail = [];
  for (const w of workers) {
    const email = (w.email || '').trim().toLowerCase();
    if (!email) { noEmail.push(w); continue; }
    const key = email.split('@')[0].toUpperCase();
    const score = (x) => (String(x.id).toUpperCase() === key ? 100 : 0) + detail(x);
    const current = best.get(email);
    if (!current || score(w) > score(current)) best.set(email, w);
  }
  return [...best.values(), ...noEmail];
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
      onDataUpdated(dedupeWorkersByEmail(workersList));
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
  "locationName", "lat", "lng", "startTime", "endTime", "notes",
  "routeId", "routeName", "stopOrder", "stopType", "householdId"
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
 * Creates the concrete slot for every active daily routine for today (and optionally the next [daysAhead] days; default 0 so no future slots pile up).
 * Slot ids are deterministic ("<templateId>_<date>") and created inside a transaction only when
 * missing, so running this from several devices never duplicates or overwrites a slot.
 */
export async function ensureDailySchedules(templates, existingIds = new Set(), daysAhead = 0) {
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


// ---------------------------------------------------------------------------
// Households (door-to-door round) + areas assigned to workers
// ---------------------------------------------------------------------------

export function subscribeToHouseholds(onDataUpdated, onError = () => {}) {
  if (!db || !isFirebaseConfigValid) {
    onDataUpdated([]);
    return () => {};
  }
  return onSnapshot(
    collection(db, COLLECTION_HOUSEHOLDS),
    (snapshot) => {
      const list = snapshot.docs.map((d) => ({ ...d.data(), id: d.id }));
      list.sort((a, b) => String(a.code || a.id).localeCompare(String(b.code || b.id)));
      onDataUpdated(list);
    },
    (error) => {
      console.warn("Households sync notice:", error);
      onError(error);
    }
  );
}

/**
 * Adds a hostel block (stored in Firestore `households`; every phone and this portal update in realtime).
 * The code (HB-101, HB-102...) is what a hostel resident types in the app to link their block.
 */
export async function addHostelBlock({ name, areaId, areaName, lat, lng, address = "" }, existing = []) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const used = existing
    .map((h) => parseInt(String(h.code || h.id).replace(/^HB-/, ""), 10))
    .filter((n) => Number.isFinite(n));
  const code = `HB-${Math.max(100, ...used) + 1}`;
  await setDoc(doc(db, COLLECTION_HOUSEHOLDS, code), {
    id: code,
    code,
    name,
    address,
    areaId,
    areaName,
    lat,
    lng,
    ownerUid: null,
    ownerName: null,
    status: "Pending",
    createdAt: serverTimestamp()
  });
  return code;
}

export async function deleteHostelBlock(householdId) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  await deleteDoc(doc(db, COLLECTION_HOUSEHOLDS, householdId));
}

export async function removeMockHouseholds(households) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const batch = writeBatch(db);
  households.filter((h) => h.isMock).forEach((h) => batch.delete(doc(db, COLLECTION_HOUSEHOLDS, h.id)));
  await batch.commit();
}

/** Supervisor confirms (or un-confirms) that a hostel building's garbage was collected today. */
export async function setHouseholdCollected(householdId, collected, supervisorName = "Supervisor") {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  await updateDoc(doc(db, COLLECTION_HOUSEHOLDS, householdId), collected
    ? {
        status: "Collected",
        collectedDate: toDateString(),
        collectedAtMs: Date.now(),
        collectedBy: "SUPERVISOR",
        collectedByName: supervisorName
      }
    : { status: "Pending", collectedDate: "", collectedAtMs: 0, collectedBy: "", collectedByName: "" });
}

/** Which areas a worker covers. The worker app shows the homes of exactly these areas. */
export async function setWorkerAreas(workerId, areaIds) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  await updateDoc(doc(db, COLLECTION_WORKERS, workerId), { assignedAreaIds: areaIds, areasUpdatedAt: serverTimestamp() });
}

// ---------------------------------------------------------------------------
// Routes: an ordered list of stops (bins / homes) for one worker, repeated every day
// ---------------------------------------------------------------------------

const addMinutes = (hhmm, mins) => {
  const [h, m] = hhmm.split(":").map(Number);
  const total = Math.min(23 * 60 + 59, h * 60 + m + mins);
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
};

/**
 * Creates a route: one daily routine per stop, in order, sharing a routeId/routeName.
 * Reuses the routine -> daily slot pipeline, so the worker's phone shows the stops as Today's Route.
 * stops: [{ locationName, lat, lng, stopType: 'bin'|'household', householdId? }]
 */
export async function createRoute({ routeName, worker, department, departmentId, startDate, startTime, minutesPerStop, stops, notes = "" }) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const routeId = `RT-${Date.now().toString(36).toUpperCase()}`;
  const batch = writeBatch(db);
  stops.forEach((stop, i) => {
    const start = addMinutes(startTime, i * minutesPerStop);
    const end = addMinutes(start, minutesPerStop - 1);
    batch.set(doc(collection(db, COLLECTION_TEMPLATES)), {
      workerId: worker.id,
      workerName: worker.name,
      department,
      departmentId,
      locationName: stop.locationName,
      lat: stop.lat,
      lng: stop.lng,
      startTime: start,
      endTime: end,
      notes,
      routeId,
      routeName,
      stopOrder: i + 1,
      stopType: stop.stopType || "bin",
      householdId: stop.householdId || null,
      startDate,
      active: true,
      createdAt: serverTimestamp()
    });
  });
  await batch.commit();
  return routeId;
}

/** Removes every stop of a route (upcoming slots go with them, completed history stays). */
export async function deleteRoute(routeTemplates) {
  for (const t of routeTemplates) {
    await deleteScheduleTemplate(t.id);
  }
}

// ---------------------------------------------------------------------------
// Zones / areas (Firestore `zones`): the hostel zone, academic zone, ... workers are assigned to
// ---------------------------------------------------------------------------

export function subscribeToZones(onDataUpdated) {
  if (!db || !isFirebaseConfigValid) {
    onDataUpdated([]);
    return () => {};
  }
  return onSnapshot(
    collection(db, COLLECTION_ZONES),
    (snapshot) => onDataUpdated(snapshot.docs.map((d) => ({ ...d.data(), id: d.id })).sort((a, b) => (a.order || 99) - (b.order || 99))),
    (error) => console.warn("Zones sync notice:", error)
  );
}

/** Writes the campus zones once, when the collection is empty. */
export async function seedZones() {
  if (!db || !isFirebaseConfigValid) return 0;
  const batch = writeBatch(db);
  DEFAULT_ZONES.forEach((z, i) => batch.set(doc(db, COLLECTION_ZONES, z.id), { ...z, order: i + 1, createdAt: serverTimestamp() }));
  await batch.commit();
  return DEFAULT_ZONES.length;
}

export async function addZone({ name, kind, group, lat, lng }, existing = []) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const id = `zone-${Date.now().toString(36)}`;
  await setDoc(doc(db, COLLECTION_ZONES, id), { id, name, kind, ...(group ? { group } : {}), lat, lng, order: existing.length + 1, createdAt: serverTimestamp() });
  return id;
}

export async function deleteZone(zoneId) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  await deleteDoc(doc(db, COLLECTION_ZONES, zoneId));
}

/**
 * Adds several hostel blocks to a zone at once from a list of names (one per line).
 * They are placed in a small ring around the zone's centre; fine-tune any block by removing it and
 * re-adding it with a click on the Campus Map.
 */
export async function addHostelBlocks(names, zone, existing = []) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const used = existing
    .map((h) => parseInt(String(h.code || h.id).replace(/^HB-/, ""), 10))
    .filter((n) => Number.isFinite(n));
  let next = Math.max(100, ...used);
  const batch = writeBatch(db);
  const count = names.length;
  names.forEach((name, i) => {
    next += 1;
    const code = `HB-${next}`;
    const angle = (2 * Math.PI * i) / Math.max(count, 1);
    const r = count > 1 ? 0.00035 : 0;
    batch.set(doc(db, COLLECTION_HOUSEHOLDS, code), {
      id: code,
      code,
      name,
      address: zone.name,
      areaId: zone.id,
      areaName: zone.name,
      lat: Number((zone.lat + r * Math.sin(angle)).toFixed(6)),
      lng: Number((zone.lng + r * Math.cos(angle)).toFixed(6)),
      ownerUid: null,
      ownerName: null,
      status: "Pending",
      createdAt: serverTimestamp()
    });
  });
  await batch.commit();
  return names.length;
}

/**
 * One click: makes sure the hostel zone exists in Firestore (fixing an older zone name) and registers every
 * official SRM hostel of that group that is not registered yet. Returns how many hostels were added.
 */
export async function registerSrmHostels(zone, existing = []) {
  if (!db || !isFirebaseConfigValid) throw new Error("Firebase is not configured.");
  const order = DEFAULT_ZONES.findIndex((z) => z.id === zone.id) + 1;
  await setDoc(doc(db, COLLECTION_ZONES, zone.id), { ...zone, order, updatedAt: serverTimestamp() }, { merge: true });
  const have = new Set(existing.filter((h) => h.areaId === zone.id).map((h) => String(h.name).trim().toLowerCase()));
  const todo = (HOSTEL_NAME_SUGGESTIONS[zone.group] || []).filter((n) => !have.has(n.toLowerCase()));
  if (!todo.length) return 0;
  return addHostelBlocks(todo, zone, existing);
}
