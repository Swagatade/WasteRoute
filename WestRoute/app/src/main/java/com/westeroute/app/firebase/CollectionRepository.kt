package com.westeroute.app.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One timetable slot created by a supervisor: "worker X collects garbage at place Y on date D, HH:mm-HH:mm".
 * Stored in Firestore collection `schedules`.
 */
data class LiveSchedule(
    val id: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val department: String = "",
    val locationName: String = "",
    val lat: Double = 12.8230,
    val lng: Double = 80.0440,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "Scheduled",
    val notes: String = ""
) {
    val isCompleted get() = status.equals("Completed", ignoreCase = true)
}

/** QR payload shown by the worker and scanned by the citizen. */
object WorkerQrPayload {
    private const val PREFIX = "WRQR1:"
    fun encode(workerId: String) = PREFIX + workerId.trim().uppercase()
    fun decode(raw: String?): String? {
        val text = raw?.trim() ?: return null
        if (!text.startsWith(PREFIX)) return null
        return text.removePrefix(PREFIX).takeIf { it.isNotBlank() }
    }
}

object CollectionRepository {
    private const val TAG = "CollectionRepo"
    private const val SCHEDULES = "schedules"
    private const val WORKERS = "workers"
    private const val LOGS = "collection_logs"
    private const val TEMPLATES = "schedule_templates"

    private val db by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }

    private val _workerSchedule = MutableStateFlow<List<LiveSchedule>>(emptyList())

    /** All of the signed-in worker's slots (every date), sorted by date then start time. */
    val workerSchedule: StateFlow<List<LiveSchedule>> = _workerSchedule.asStateFlow()

    private var scheduleListener: ListenerRegistration? = null
    private var listeningFor: String? = null
    private var templateListener: ListenerRegistration? = null
    private var lastTemplates: List<DocumentSnapshot> = emptyList()

    fun todayString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Starts (or restarts) the realtime timetable listener for one worker. */
    fun listenToWorkerSchedule(workerId: String) {
        val id = workerId.trim().uppercase()
        if (id.isBlank() || id == listeningFor) return
        val firestore = db ?: return
        scheduleListener?.remove()
        templateListener?.remove()
        listeningFor = id
        _workerSchedule.value = emptyList()
        scheduleListener = firestore.collection(SCHEDULES)
            .whereEqualTo("workerId", id)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "Schedule listener: ${err.message}")
                    return@addSnapshotListener
                }
                _workerSchedule.value = snap?.documents?.map { it.toSchedule() }
                    ?.sortedWith(compareBy({ it.date }, { it.startTime }))
                    ?: emptyList()
            }
        // Daily routines: make sure today's slot of each routine exists (created once per day, per routine).
        templateListener = firestore.collection(TEMPLATES)
            .whereEqualTo("workerId", id)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "Template listener: ${err.message}")
                    return@addSnapshotListener
                }
                lastTemplates = snap?.documents ?: emptyList()
                materializeToday(lastTemplates)
            }
    }

    /** Call periodically (e.g. while the dashboard is open) so a new day gets its slots after midnight. */
    fun refreshToday() = materializeToday(lastTemplates)

    private fun materializeToday(templates: List<DocumentSnapshot>, onDone: () -> Unit = {}) {
        val firestore = db
        if (firestore == null || templates.isEmpty()) { onDone(); return }
        val today = todayString()
        var pending = 0
        val eligible = templates.filter {
            it.getBoolean("active") != false && (it.getString("startDate") ?: "") <= today
        }
        if (eligible.isEmpty()) { onDone(); return }
        pending = eligible.size
        eligible.forEach { t ->
            val ref = firestore.collection(SCHEDULES).document("${t.id}_$today")
            firestore.runTransaction { tx ->
                if (!tx.get(ref).exists()) {
                    tx.set(
                        ref,
                        hashMapOf<String, Any?>(
                            "templateId" to t.id,
                            "workerId" to t.getString("workerId"),
                            "workerName" to t.getString("workerName"),
                            "department" to t.getString("department"),
                            "departmentId" to t.getString("departmentId"),
                            "locationName" to t.getString("locationName"),
                            "lat" to t.getDouble("lat"),
                            "lng" to t.getDouble("lng"),
                            "startTime" to t.getString("startTime"),
                            "endTime" to t.getString("endTime"),
                            "notes" to (t.getString("notes") ?: ""),
                            "date" to today,
                            "status" to "Scheduled",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                null
            }.addOnCompleteListener {
                if (it.exception != null) Log.w(TAG, "Daily slot create: ${it.exception?.message}")
                pending -= 1
                if (pending == 0) onDone()
            }
        }
    }

    fun stopListening() {
        scheduleListener?.remove()
        scheduleListener = null
        templateListener?.remove()
        templateListener = null
        lastTemplates = emptyList()
        listeningFor = null
        _workerSchedule.value = emptyList()
    }

    /**
     * Makes sure the signed-in worker has a record in `workers` so the Supervisor portal lists them,
     * even if the Auth account was created before the Firestore record existed.
     * Never overwrites supervisor-managed fields (ward, vehicle, phone, status) on an existing record.
     */
    fun ensureWorkerRecord(workerId: String, displayName: String, email: String) {
        val firestore = db ?: return
        val id = workerId.trim().uppercase()
        if (id.isBlank()) return
        val ref = firestore.collection(WORKERS).document(id)
        ref.get().addOnSuccessListener { snap ->
            val data = hashMapOf<String, Any?>(
                "id" to id,
                "workerId" to id,
                "email" to email.lowercase(),
                "uid" to FirebaseAuth.getInstance().currentUser?.uid,
                "lastLoginAt" to FieldValue.serverTimestamp()
            )
            if (!snap.exists()) {
                data["name"] = displayName.ifBlank { "Worker $id" }
                data["status"] = "Available"
                data["vehicle"] = "Unassigned"
                data["vehicleType"] = ""
                data["wardId"] = "ward-ktr-01"
                data["wardName"] = "SRM KTR Campus Zone"
                data["phone"] = ""
                data["rating"] = 5.0
                data["tasksCompletedToday"] = 0
                data["registeredAt"] = FieldValue.serverTimestamp()
                data["isLiveFirebase"] = true
                data["currentLocation"] = hashMapOf("lat" to 12.8235, "lng" to 80.0442, "street" to "SRM KTR Campus")
            }
            ref.set(data, SetOptions.merge())
                .addOnFailureListener { Log.w(TAG, "ensureWorkerRecord write failed: ${it.message}") }
        }.addOnFailureListener { Log.w(TAG, "ensureWorkerRecord read failed: ${it.message}") }
    }

    /** Worker marks a slot as started. Completion is only possible through the citizen QR scan. */
    fun setScheduleStatus(scheduleId: String, status: String) {
        if (status.equals("Completed", ignoreCase = true)) return
        db?.collection(SCHEDULES)?.document(scheduleId)
            ?.update(mapOf("status" to status))
            ?.addOnFailureListener { Log.w(TAG, "setScheduleStatus failed: ${it.message}") }
    }

    /**
     * Citizen confirms a doorstep pickup by scanning the worker's QR.
     * Completes the worker's next open slot for today, writes an audit log and bumps the worker's counter.
     * [onResult] receives a human readable message (success or failure).
     */
    fun confirmCollection(
        rawQr: String?,
        citizenName: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val workerId = WorkerQrPayload.decode(rawQr)
        if (workerId == null) {
            onResult(false, "This is not a WestRoute worker QR code.")
            return
        }
        val firestore = db
        val citizenUid = FirebaseAuth.getInstance().currentUser?.uid
        if (firestore == null || citizenUid == null) {
            onResult(false, "Please sign in as a citizen to confirm a collection.")
            return
        }
        val workerRef = firestore.collection(WORKERS).document(workerId)
        workerRef.get().addOnSuccessListener { workerSnap ->
            if (!workerSnap.exists()) {
                onResult(false, "Worker $workerId is not registered.")
                return@addOnSuccessListener
            }
            val workerName = workerSnap.getString("name") ?: workerId
            firestore.collection(TEMPLATES).whereEqualTo("workerId", workerId).get()
                .addOnCompleteListener { tplTask ->
                  materializeToday(tplTask.result?.documents ?: emptyList()) {
            firestore.collection(SCHEDULES)
                .whereEqualTo("workerId", workerId)
                .whereEqualTo("date", todayString())
                .get()
                .addOnSuccessListener { q ->
                    val open = q.documents.map { it.toSchedule() }
                        .filter { !it.isCompleted }
                        .sortedWith(
                            compareByDescending<LiveSchedule> { it.status == "In Progress" }
                                .thenBy { it.startTime }
                        )
                        .firstOrNull()
                    if (open == null) {
                        onResult(false, "$workerName has no pending collection scheduled for today.")
                        return@addOnSuccessListener
                    }
                    val batch = firestore.batch()
                    batch.update(
                        firestore.collection(SCHEDULES).document(open.id),
                        mapOf(
                            "status" to "Completed",
                            "completedAt" to FieldValue.serverTimestamp(),
                            "completedByCitizenUid" to citizenUid,
                            "completedByCitizenName" to citizenName
                        )
                    )
                    batch.set(
                        firestore.collection(LOGS).document(),
                        hashMapOf(
                            "scheduleId" to open.id,
                            "workerId" to workerId,
                            "workerName" to workerName,
                            "locationName" to open.locationName,
                            "department" to open.department,
                            "date" to open.date,
                            "citizenUid" to citizenUid,
                            "citizenName" to citizenName,
                            "confirmedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(workerRef, "tasksCompletedToday", FieldValue.increment(1))
                    batch.commit()
                        .addOnSuccessListener {
                            val where = open.locationName.ifBlank { "your location" }
                            onResult(true, "Collection at $where confirmed for $workerName. Thank you!")
                        }
                        .addOnFailureListener { onResult(false, "Could not save confirmation: ${it.message}") }
                }
                .addOnFailureListener { onResult(false, "Could not load today's schedule: ${it.message}") }
                  }
                }
        }.addOnFailureListener { onResult(false, "Could not verify worker: ${it.message}") }
    }

    private fun DocumentSnapshot.toSchedule() = LiveSchedule(
        id = id,
        workerId = getString("workerId") ?: "",
        workerName = getString("workerName") ?: "",
        department = getString("department") ?: "",
        locationName = getString("locationName") ?: "",
        lat = getDouble("lat") ?: 12.8230,
        lng = getDouble("lng") ?: 80.0440,
        date = getString("date") ?: "",
        startTime = getString("startTime") ?: "",
        endTime = getString("endTime") ?: "",
        status = getString("status") ?: "Scheduled",
        notes = getString("notes") ?: ""
    )
}
