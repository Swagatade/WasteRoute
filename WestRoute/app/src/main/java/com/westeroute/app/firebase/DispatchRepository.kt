package com.westeroute.app.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.westeroute.app.location.LocationHelper

/**
 * Worker-side workflow for a citizen garbage report. Everything is written to the same
 * `complaints` / `workers` documents the Supervisor web app reads, so the supervisor sees
 * accept, truck position, progress and the proof photo in real time.
 *
 *   reported -> accepted -> on_the_way -> arrived -> resolved
 *   (Pending)   (Assigned)  (In Progress) (In Progress) (Resolved)
 */
object DispatchRepository {
    private const val TAG = "DispatchRepo"
    private const val COMPLAINTS = "complaints"
    private const val WORKERS = "workers"

    /** Truck is considered arrived when it is this close to the issue. */
    const val ARRIVAL_RADIUS_M = 40.0

    private val db by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }

    private fun event(stage: String, label: String) =
        hashMapOf("stage" to stage, "label" to label, "atMs" to System.currentTimeMillis())

    /** First worker to accept wins; everyone else gets "already accepted". */
    fun acceptTask(
        taskId: String,
        workerId: String,
        workerName: String,
        vehicle: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val firestore = db ?: return onResult(false, "No connection to the server.")
        val ref = firestore.collection(COMPLAINTS).document(taskId)
        firestore.runTransaction { tx ->
            val snap = tx.get(ref)
            val owner = snap.getString("assignedWorkerId")
            if (!owner.isNullOrBlank() && !owner.equals(workerId, ignoreCase = true)) {
                throw FirebaseFirestoreException(
                    "Another worker already accepted this task.",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION
                )
            }
            if (snap.getString("stage") == TaskStage.RESOLVED || snap.getString("status") == "Resolved") {
                throw FirebaseFirestoreException(
                    "This task is already resolved.",
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION
                )
            }
            tx.update(
                ref, mapOf(
                    "assignedWorkerId" to workerId,
                    "assignedWorkerName" to workerName,
                    "truckName" to vehicle,
                    "status" to "Assigned",
                    "stage" to TaskStage.ACCEPTED,
                    "acceptedAtMs" to System.currentTimeMillis(),
                    "assignedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "timeline" to FieldValue.arrayUnion(event(TaskStage.ACCEPTED, "Accepted by $workerName ($vehicle)"))
                )
            )
            null
        }.addOnSuccessListener {
            firestore.collection(WORKERS).document(workerId)
                .update("status", "Busy")
                .addOnFailureListener { Log.w(TAG, "worker status: ${it.message}") }
            onResult(true, "Task accepted. Start the trip when you are ready.")
        }.addOnFailureListener { onResult(false, it.message ?: "Could not accept the task.") }
    }

    /** Truck leaves: records the starting distance so progress can be shown as a percentage. */
    fun startTrip(task: LiveComplaint, workerId: String, lat: Double, lng: Double, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val firestore = db ?: return onResult(false, "No connection to the server.")
        val start = LocationHelper.distanceM(lat, lng, task.lat, task.lng).coerceAtLeast(1.0)
        firestore.collection(COMPLAINTS).document(task.id).update(
            mapOf(
                "status" to "In Progress",
                "stage" to TaskStage.ON_THE_WAY,
                "truckLocation" to mapOf("lat" to lat, "lng" to lng),
                "tripStartDistanceM" to start,
                "distanceToIssueM" to start,
                "progress" to 0,
                "tripStartedAtMs" to System.currentTimeMillis(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "timeline" to FieldValue.arrayUnion(
                    event(TaskStage.ON_THE_WAY, "Truck on the way (${LocationHelper.formatDistance(start)} away)")
                )
            )
        ).addOnSuccessListener { onResult(true, "Trip started") }
            .addOnFailureListener { onResult(false, it.message ?: "Could not start the trip.") }
        firestore.collection(WORKERS).document(workerId).update("status", "On Route")
        publishWorkerLocation(workerId, lat, lng)
    }

    /** Called with every GPS fix (or demo step) while the truck is travelling. */
    fun pushTruckLocation(task: LiveComplaint, lat: Double, lng: Double) {
        val firestore = db ?: return
        val dist = LocationHelper.distanceM(lat, lng, task.lat, task.lng)
        val start = task.tripStartDistanceM.takeIf { it > 0 } ?: dist.coerceAtLeast(1.0)
        val arrived = dist <= ARRIVAL_RADIUS_M
        val progress = if (arrived) 100 else ((1.0 - dist / start) * 100).toInt().coerceIn(0, 99)
        val update = hashMapOf<String, Any>(
            "truckLocation" to mapOf("lat" to lat, "lng" to lng),
            "distanceToIssueM" to dist,
            "progress" to progress,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (arrived && task.stage == TaskStage.ON_THE_WAY) {
            update["stage"] = TaskStage.ARRIVED
            update["timeline"] = FieldValue.arrayUnion(event(TaskStage.ARRIVED, "Truck arrived at the location"))
        }
        firestore.collection(COMPLAINTS).document(task.id).update(update)
            .addOnFailureListener { Log.w(TAG, "truck update: ${it.message}") }
    }

    /** Keeps the worker's position fresh in `workers` (used for "nearby worker" matching + supervisor map). */
    fun publishWorkerLocation(workerId: String, lat: Double, lng: Double) {
        db?.collection(WORKERS)?.document(workerId)?.update(
            mapOf("currentLocation" to mapOf("lat" to lat, "lng" to lng, "street" to "Live GPS"))
        )?.addOnFailureListener { Log.w(TAG, "worker location: ${it.message}") }
    }

    /** Removes solved tickets from ONE person's history (the ticket itself stays for everyone else). */
    fun hideFromHistory(ids: List<String>, viewerId: String, onResult: (Boolean) -> Unit = {}) {
        val firestore = db ?: return onResult(false)
        if (ids.isEmpty() || viewerId.isBlank()) return onResult(false)
        val batch = firestore.batch()
        ids.forEach {
            batch.update(firestore.collection(COMPLAINTS).document(it), "hiddenFor", FieldValue.arrayUnion(viewerId))
        }
        batch.commit().addOnSuccessListener { onResult(true) }.addOnFailureListener {
            Log.w(TAG, "hide history: ${it.message}")
            onResult(false)
        }
    }

    /** Worker submits the "after" photo as proof and closes the task. */
    fun resolveTask(
        task: LiveComplaint,
        workerId: String,
        workerName: String,
        afterPhotoDataUri: String,
        lat: Double?,
        lng: Double?,
        onResult: (Boolean, String) -> Unit
    ) {
        val firestore = db ?: return onResult(false, "No connection to the server.")
        val now = System.currentTimeMillis()
        firestore.collection(COMPLAINTS).document(task.id).update(
            mapOf(
                "status" to "Resolved",
                "stage" to TaskStage.RESOLVED,
                "progress" to 100,
                "photoCount" to 2,
                "resolvedAtMs" to now,
                "cleaningProof" to mapOf(
                    "photoAfter" to afterPhotoDataUri,
                    "resolvedAtMs" to now,
                    "workerId" to workerId,
                    "workerName" to workerName,
                    "lat" to lat,
                    "lng" to lng
                ),
                "updatedAt" to FieldValue.serverTimestamp(),
                "timeline" to FieldValue.arrayUnion(event(TaskStage.RESOLVED, "Issue solved - proof photo uploaded by $workerName"))
            )
        ).addOnSuccessListener {
            firestore.collection(WORKERS).document(workerId).update(
                mapOf("status" to "Available", "tasksCompletedToday" to FieldValue.increment(1))
            )
            onResult(true, "Task marked as solved. Great work!")
        }.addOnFailureListener { onResult(false, it.message ?: "Could not save the proof.") }
    }
}
