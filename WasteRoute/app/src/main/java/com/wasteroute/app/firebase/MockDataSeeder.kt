package com.wasteroute.app.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Test data is limited to smart BINS. Workers, complaints, photos and locations always come from real
 * sign-ins, the real camera and real GPS. Safe to run more than once.
 */
object MockDataSeeder {

    private data class Bin(val name: String, val lat: Double, val lng: Double, val fill: Int)

    private val bins = listOf(
        Bin("Java Green Food Street Bin", 12.8225, 80.0438, 35),
        Bin("UB Building Back Lane Bin", 12.8232, 80.0445, 62),
        Bin("Tech Park Tower Gate Bin", 12.8248, 80.0456, 78),
        Bin("Paari Hostel Road Bin", 12.8210, 80.0415, 91),
        Bin("Main Gate 1 (GST Road) Bin", 12.8258, 80.0470, 48),
        Bin("Medical Hospital Parking Bin", 12.8202, 80.0485, 88)
    )

    private data class Stop(val place: String, val lat: Double, val lng: Double, val start: String, val end: String, val note: String)

    private val routeStops = listOf(
        Stop("UB Building Ground Plaza", 12.8232, 80.0445, "08:30", "09:00", "Dry recyclables and paper. Bin is almost full."),
        Stop("Central Library Square Avenue", 12.8239, 80.0430, "09:15", "09:45", "Paper and cardboard."),
        Stop("Tech Park Tower A Ground Hub", 12.8248, 80.0456, "10:15", "10:45", "E-waste and mixed dry waste."),
        Stop("Java Green Food Street Pavilion", 12.8225, 80.0438, "11:30", "12:00", "Organic food waste. Peak after lunch."),
        Stop("Paari / Kaari Hostel Courtyard", 12.8210, 80.0415, "13:00", "13:30", "General domestic waste."),
        Stop("SRM Main Gate 1 (GST Road Transit)", 12.8258, 80.0470, "14:30", "15:00", "Plastic and bottles."),
        Stop("Medical College Hospital Gate 3", 12.8202, 80.0485, "15:30", "16:00", "Non-hazardous hospital waste.")
    )

    /**
     * Loads today's route for the signed-in worker: one `schedules` slot per campus stop, flagged `isMock`.
     * Slots go through the normal timetable path (Route tab, Today's Route, QR confirmation).
     * Safe to run more than once: nothing is written if today's mock route already exists for this worker.
     */
    fun seedTodayRoute(workerId: String, workerName: String, onDone: (Boolean, String) -> Unit) {
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
            ?: return onDone(false, "No connection to Firestore.")
        val id = workerId.trim().uppercase()
        if (id.isBlank()) return onDone(false, "Sign in as a worker first.")
        val today = CollectionRepository.todayString()
        db.collection("schedules")
            .whereEqualTo("workerId", id)
            .whereEqualTo("date", today)
            .whereEqualTo("isMock", true)
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) return@addOnSuccessListener onDone(true, "Today's test route is already loaded.")
                val batch = db.batch()
                routeStops.forEachIndexed { i, s ->
                    batch.set(
                        db.collection("schedules").document("MOCK-$id-$today-${i + 1}"),
                        hashMapOf<String, Any?>(
                            "workerId" to id, "workerName" to workerName.ifBlank { "Worker $id" },
                            "department" to "SRM KTR Campus", "locationName" to s.place,
                            "lat" to s.lat, "lng" to s.lng, "date" to today,
                            "startTime" to s.start, "endTime" to s.end, "notes" to s.note,
                            "status" to "Scheduled", "isMock" to true,
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit()
                    .addOnSuccessListener { onDone(true, "Test route loaded (${routeStops.size} stops today).") }
                    .addOnFailureListener { onDone(false, "Could not load test route: ${it.message}") }
            }
            .addOnFailureListener { onDone(false, "Could not check today's route: ${it.message}") }
    }

    fun seedBins(onDone: (Boolean, String) -> Unit) {
        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
            ?: return onDone(false, "No connection to Firestore.")
        val batch = db.batch()
        bins.forEachIndexed { i, b ->
            batch.set(
                db.collection("smart_bins").document("MOCK-BIN-0${i + 1}"),
                hashMapOf<String, Any?>(
                    "code" to "KTR-BIN-0${i + 1}", "name" to b.name, "zone" to "SRM KTR Campus",
                    "fillLevel" to b.fill, "type" to "Mixed Solid Waste",
                    "coordinates" to hashMapOf("lat" to b.lat, "lng" to b.lng), "isMock" to true
                ),
                SetOptions.merge()
            )
        }
        batch.commit()
            .addOnSuccessListener { onDone(true, "Test bins loaded (${bins.size}).") }
            .addOnFailureListener { onDone(false, "Could not load test bins: ${it.message}") }
    }
}
