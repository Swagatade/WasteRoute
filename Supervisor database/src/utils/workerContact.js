// One place that reads a worker's mobile number, so every worker section shows the same value.
// Firestore `workers/{id}.phone` is the source of truth (written at registration); older records may use other keys.

export function workerPhone(worker) {
  if (!worker) return '';
  const v = worker.phone || worker.mobile || worker.mobileNumber || worker.phoneNumber || worker.contact || '';
  return String(v).trim();
}

/** Finds a worker by Firestore id (case-insensitive) or, failing that, by name. */
export function findWorker(workers = [], idOrName) {
  if (!idOrName) return null;
  const key = String(idOrName).trim().toLowerCase();
  return (
    workers.find((w) => String(w.id || '').toLowerCase() === key) ||
    workers.find((w) => String(w.workerId || '').toLowerCase() === key) ||
    workers.find((w) => String(w.name || '').toLowerCase() === key) ||
    null
  );
}

/** Mobile number of the worker who accepted a complaint ('' if none / not on file). */
export function assignedWorkerPhone(workers, complaint) {
  if (!complaint) return '';
  const w =
    findWorker(workers, complaint.assignedWorkerId) ||
    findWorker(workers, String(complaint.assignedWorkerName || '').replace(/\s*\(.*\)\s*$/, ''));
  return workerPhone(w);
}

/** Mobile numbers of the workers called for a complaint: [{ name, phone }] */
export function notifiedWorkerContacts(workers, complaint) {
  const ids = Array.isArray(complaint?.notifiedWorkerIds) ? complaint.notifiedWorkerIds : [];
  const names = Array.isArray(complaint?.notifiedWorkerNames) ? complaint.notifiedWorkerNames : [];
  return names.map((name, i) => ({
    name,
    phone: workerPhone(findWorker(workers, ids[i]) || findWorker(workers, name)),
  }));
}
