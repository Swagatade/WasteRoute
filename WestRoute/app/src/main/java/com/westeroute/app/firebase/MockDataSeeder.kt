package com.westeroute.app.firebase

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
