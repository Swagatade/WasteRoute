package com.wasteroute.app.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.wasteroute.app.components.BinStatus
import com.wasteroute.app.model.CampusMapConfig
import com.wasteroute.app.model.CampusSmartBin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveComplaint(
    val id: String = "",
    val ticketNo: String = "",
    val category: String = "",
    val priority: String = "High",
    val status: String = "Pending",
    val location: String = "",
    val lat: Double = 12.8230,
    val lng: Double = 80.0440,
    val citizenName: String = "Citizen",
    val citizenPhone: String = "",
    val reportedAt: String = "Just now",
    val description: String = "",
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null,
    // --- Dispatch / tracking (shared with the Supervisor web app) ---
    val reporterUid: String = "",
    val stage: String = TaskStage.REPORTED,
    val notifiedWorkerIds: List<String> = emptyList(),
    val notifiedWorkerNames: List<String> = emptyList(),
    val photoBefore: String = "",
    val photoAfter: String = "",
    val photoCount: Int = 0,
    val truckName: String = "",
    val truckLat: Double? = null,
    val truckLng: Double? = null,
    val tripStartDistanceM: Double = 0.0,
    val progress: Int = 0,
    val createdAtMs: Long = 0L,
    val resolvedAtMs: Long = 0L,
    val hiddenFor: List<String> = emptyList()
)

/** Lifecycle of a citizen garbage report. Supervisor `status` is derived: Pending / Assigned / In Progress / Resolved. */
object TaskStage {
    const val REPORTED = "reported"
    const val ACCEPTED = "accepted"
    const val ON_THE_WAY = "on_the_way"
    const val ARRIVED = "arrived"
    const val RESOLVED = "resolved"
    val ORDER = listOf(REPORTED, ACCEPTED, ON_THE_WAY, ARRIVED, RESOLVED)
}

data class LiveWorker(
    val id: String = "",
    val name: String = "",
    val vehicle: String = "Sanitation Vehicle",
    val status: String = "Available",
    val lat: Double = 12.8235,
    val lng: Double = 80.0442,
    val wardName: String = "",
    val phone: String = "",
    /** Areas (household zones) the supervisor assigned to this worker. */
    val assignedAreaIds: List<String> = emptyList(),
    val isMock: Boolean = false
)

/**
 * Shared Realtime Repository connected directly to Firestore (wasteroute-d393c).
 * Synchronizes Smart Bins, Complaints, and Workers seamlessly between Android and Web Supervisor.
 */
object FirebaseDataRepository {
    private const val TAG = "FirebaseDataRepo"
    private const val COLLECTION_COMPLAINTS = "complaints"
    private const val COLLECTION_BINS = "smart_bins"
    private const val COLLECTION_WORKERS = "workers"

    private val firestore by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization notice (offline mode active): ${e.message}")
            null
        }
    }

    private val _smartBins = MutableStateFlow(emptyList<CampusSmartBin>())
    val smartBins: StateFlow<List<CampusSmartBin>> = _smartBins.asStateFlow()

    private val defaultComplaints = emptyList<LiveComplaint>()

    private val _complaints = MutableStateFlow(defaultComplaints)
    val complaints: StateFlow<List<LiveComplaint>> = _complaints.asStateFlow()

    // No sample trucks: only real registered workers (same as the Supervisor web app)
    private val defaultWorkers = emptyList<LiveWorker>()

    private val _workers = MutableStateFlow(defaultWorkers)
    val workers: StateFlow<List<LiveWorker>> = _workers.asStateFlow()

    private var binsListener: ListenerRegistration? = null
    private var complaintsListener: ListenerRegistration? = null
    private var workersListener: ListenerRegistration? = null

    init {
        startRealtimeListeners()
    }

    fun startRealtimeListeners() {
        val db = firestore ?: return

        // 1. Listen to Smart Bins collection
        try {
            binsListener?.remove()
            binsListener = db.collection(COLLECTION_BINS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Smart bins listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val parsedBins = snapshot.documents.mapNotNull { doc ->
                            val id = doc.id
                            val code = doc.getString("code") ?: id
                            val name = doc.getString("name") ?: doc.getString("location") ?: "Smart Bin $code"
                            val zone = doc.getString("wardName") ?: doc.getString("zone") ?: "SRM KTR Campus"
                            val fillLevel = (doc.getLong("fillLevel") ?: 50L).toInt()
                            val type = doc.getString("type") ?: "Mixed Solid Waste"
                            val distanceDisplay = doc.getString("distance") ?: ""
                            
                            val coordsMap = doc.get("coordinates") as? Map<*, *>
                            val lat = (coordsMap?.get("lat") as? Number)?.toDouble() 
                                ?: doc.getDouble("lat") ?: 12.8230
                            val lng = (coordsMap?.get("lng") as? Number)?.toDouble() 
                                ?: doc.getDouble("lng") ?: 80.0440

                            val status = when {
                                fillLevel >= 85 -> BinStatus.FULL
                                fillLevel >= 70 -> BinStatus.NEARLY_FULL
                                else -> BinStatus.AVAILABLE
                            }

                            CampusSmartBin(
                                id = id,
                                code = code,
                                name = name,
                                zone = zone,
                                lat = lat,
                                lng = lng,
                                fillLevel = fillLevel,
                                type = type,
                                distanceDisplay = distanceDisplay,
                                status = status
                            )
                        }
                        // Always mirror the server, even an empty list: a deleted bin must leave the map.
                        _smartBins.value = parsedBins
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach smart bins listener: ${e.message}")
        }

        // 2. Listen to Complaints collection
        try {
            complaintsListener?.remove()
            complaintsListener = db.collection(COLLECTION_COMPLAINTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Complaints listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val parsedComplaints = snapshot.documents.mapNotNull { doc ->
                            if (doc.getBoolean("isMock") == true) return@mapNotNull null
                            val coordsMap = doc.get("coordinates") as? Map<*, *>
                            val lat = (coordsMap?.get("lat") as? Number)?.toDouble()
                                ?: doc.getDouble("lat") ?: 12.8230
                            val lng = (coordsMap?.get("lng") as? Number)?.toDouble()
                                ?: doc.getDouble("lng") ?: 80.0440

                            val citizenMap = doc.get("citizen") as? Map<*, *>
                            val citizenName = citizenMap?.get("name")?.toString()
                                ?: doc.getString("citizenName") ?: "Citizen"

                            val status = doc.getString("status") ?: "Pending"
                            // Complaints touched only by the supervisor have no `stage`: derive it from `status`.
                            val stage = doc.getString("stage") ?: when (status.lowercase()) {
                                "assigned" -> TaskStage.ACCEPTED
                                "in progress" -> TaskStage.ON_THE_WAY
                                "resolved" -> TaskStage.RESOLVED
                                else -> TaskStage.REPORTED
                            }
                            val truckMap = doc.get("truckLocation") as? Map<*, *>
                            val proofMap = doc.get("cleaningProof") as? Map<*, *>

                            LiveComplaint(
                                id = doc.id,
                                ticketNo = doc.getString("ticketNo") ?: doc.id,
                                category = doc.getString("category") ?: "Waste Report",
                                priority = doc.getString("priority") ?: "High",
                                status = status,
                                location = doc.getString("location") ?: "SRM KTR Campus",
                                lat = lat,
                                lng = lng,
                                citizenName = citizenName,
                                reportedAt = doc.getString("reportedAt") ?: "",
                                description = doc.getString("description") ?: "",
                                assignedWorkerId = doc.getString("assignedWorkerId"),
                                assignedWorkerName = doc.getString("assignedWorkerName"),
                                reporterUid = doc.getString("reporterUid") ?: "",
                                stage = stage,
                                notifiedWorkerIds = (doc.get("notifiedWorkerIds") as? List<*>)
                                    ?.mapNotNull { it?.toString() } ?: emptyList(),
                                notifiedWorkerNames = (doc.get("notifiedWorkerNames") as? List<*>)
                                    ?.mapNotNull { it?.toString() } ?: emptyList(),
                                photoBefore = doc.getString("photoBefore") ?: "",
                                photoAfter = proofMap?.get("photoAfter")?.toString() ?: "",
                                photoCount = (doc.getLong("photoCount") ?: 0L).toInt(),
                                truckName = doc.getString("truckName") ?: "",
                                truckLat = (truckMap?.get("lat") as? Number)?.toDouble(),
                                truckLng = (truckMap?.get("lng") as? Number)?.toDouble(),
                                tripStartDistanceM = doc.getDouble("tripStartDistanceM") ?: 0.0,
                                progress = (doc.getLong("progress") ?: 0L).toInt(),
                                createdAtMs = doc.getLong("createdAtMs") ?: 0L,
                                resolvedAtMs = doc.getLong("resolvedAtMs") ?: 0L,
                                hiddenFor = (doc.get("hiddenFor") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            )
                        }.sortedByDescending { it.createdAtMs }
                        _complaints.value = parsedComplaints
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach complaints listener: ${e.message}")
        }

        // 3. Listen to Workers collection
        try {
            workersListener?.remove()
            workersListener = db.collection(COLLECTION_WORKERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Workers listener notice: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val parsedWorkers = snapshot.documents.mapNotNull { doc ->
                            if (doc.getBoolean("isMock") == true) return@mapNotNull null
                            val coordsMap = doc.get("currentLocation") as? Map<*, *>
                            val lat = (coordsMap?.get("lat") as? Number)?.toDouble()
                                ?: doc.getDouble("lat") ?: 12.8235
                            val lng = (coordsMap?.get("lng") as? Number)?.toDouble()
                                ?: doc.getDouble("lng") ?: 80.0442

                            LiveWorker(
                                id = doc.id,
                                name = doc.getString("name") ?: "Worker",
                                vehicle = doc.getString("vehicle") ?: "Collection Unit",
                                status = doc.getString("status") ?: "Available",
                                lat = lat,
                                lng = lng,
                                wardName = doc.getString("wardName") ?: "",
                                phone = (doc.getString("phone") ?: doc.getString("mobile") ?: doc.getString("phoneNumber") ?: "").trim(),
                                assignedAreaIds = (doc.get("assignedAreaIds") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                                isMock = doc.getBoolean("isMock") ?: false
                            )
                        }
                        _workers.value = parsedWorkers
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach workers listener: ${e.message}")
        }
    }

    /** Mobile number of a worker by Firestore id (or name as fallback); empty when not on file. */
    fun workerPhone(id: String?, name: String? = null): String {
        val list = _workers.value
        val w = list.firstOrNull { !id.isNullOrBlank() && it.id.equals(id, ignoreCase = true) }
            ?: list.firstOrNull { !name.isNullOrBlank() && it.name.equals(name.substringBefore(" (").trim(), ignoreCase = true) }
        return w?.phone.orEmpty()
    }

    /**
     * Picks who gets the "new task" alarm: available, real workers within [radiusM] of the issue
     * (up to 5, nearest first). If nobody is that close, the 5 nearest available workers are used.
     */
    fun nearbyWorkers(lat: Double, lng: Double, radiusM: Double = 5000.0): List<LiveWorker> {
        val eligible = _workers.value
            .filter { !it.isMock && !it.status.equals("Off Duty", true) && !it.status.equals("Offline", true) }
            .map { it to com.wasteroute.app.location.LocationHelper.distanceM(lat, lng, it.lat, it.lng) }
            .sortedBy { it.second }
        val inRadius = eligible.filter { it.second <= radiusM }
        return (if (inRadius.isNotEmpty()) inRadius else eligible).take(8).map { it.first }
    }

    /**
     * Lodges a new complaint from the citizen Android app and writes it directly
     * to the shared Firestore collection. Immediately appears in Web Supervisor.
     */
    fun lodgeComplaint(
        ticketNo: String,
        category: String,
        location: String,
        description: String,
        citizenName: String = "Citizen",
        citizenPhone: String = "",
        citizenEmail: String = "",
        lat: Double = 12.8230,
        lng: Double = 80.0440,
        photoDataUri: String = "",
        reporterUid: String = "",
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        // Nearby, available, real (non-mock) workers get the alarm notification.
        val called = nearbyWorkers(lat, lng)
        val notified = called.map { it.id }
        val now = System.currentTimeMillis()
        val newComplaint = LiveComplaint(
            id = ticketNo.replace("#", ""),
            ticketNo = ticketNo,
            category = category,
            priority = if (category.contains("Hazard", ignoreCase = true)) "Critical" else "High",
            status = "Pending",
            location = location,
            lat = lat,
            lng = lng,
            citizenName = citizenName,
            citizenPhone = citizenPhone,
            reportedAt = "",
            description = description,
            reporterUid = reporterUid,
            stage = TaskStage.REPORTED,
            notifiedWorkerIds = notified,
            notifiedWorkerNames = called.map { it.name },
            photoBefore = photoDataUri,
            photoCount = if (photoDataUri.isNotBlank()) 1 else 0,
            createdAtMs = now
        )

        // Optimistically update local state immediately
        _complaints.value = listOf(newComplaint) + _complaints.value.filter { it.ticketNo != ticketNo }

        val db = firestore
        if (db == null) {
            onSuccess()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to newComplaint.id,
                    "ticketNo" to ticketNo,
                    "category" to category,
                    "priority" to newComplaint.priority,
                    "status" to "Pending",
                    "location" to location,
                    "coordinates" to hashMapOf("lat" to lat, "lng" to lng),
                    "citizen" to hashMapOf(
                        "name" to citizenName,
                        "phone" to citizenPhone,
                        "email" to citizenEmail
                    ),
                    "description" to description,
                    "photoBefore" to photoDataUri,
                    "photoCount" to newComplaint.photoCount,
                    "reporterUid" to reporterUid,
                    "stage" to TaskStage.REPORTED,
                    "notifiedWorkerIds" to notified,
                    "notifiedWorkerNames" to called.map { it.name },
                    "progress" to 0,
                    "createdAtMs" to now,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "timeline" to listOf(
                        hashMapOf(
                            "stage" to TaskStage.REPORTED,
                            "label" to "Reported by $citizenName - ${notified.size} nearby worker(s) called automatically",
                            "atMs" to now
                        )
                    ),
                    "assignedWorkerId" to null,
                    "cleaningProof" to null
                )

                db.collection(COLLECTION_COMPLAINTS)
                    .document(newComplaint.id)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.i(TAG, "Complaint $ticketNo successfully saved to Firestore!")
                        onSuccess()
                    }
                    .addOnFailureListener { err ->
                        Log.w(TAG, "Could not write complaint to Firestore: ${err.message}")
                        onError(err)
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Exception writing complaint: ${e.message}")
                onError(e)
            }
        }
    }
}
