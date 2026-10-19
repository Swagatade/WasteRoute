package com.wasteroute.app.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One hostel building on the collection round. Stored in Firestore collection `households` (mock data is loaded by the
 * supervisor). Workers and supervisors see every home in their area; a resident only ever sees the home they
 * linked with its code (enforced by firestore.rules, not just by the UI).
 */
data class LiveHousehold(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val address: String = "",
    val areaId: String = "",
    val areaName: String = "",
    val lat: Double = 12.8230,
    val lng: Double = 80.0440,
    val ownerUid: String = "",
    val ownerName: String = "",
    val collectedDate: String = "",
    val collectedAtMs: Long = 0L,
    val collectedByName: String = ""
) {
    /** Garbage was picked up today; resets itself at midnight because it compares the date. */
    fun collectedToday(today: String = CollectionRepository.todayString()) = collectedDate == today
}

object HouseholdRepository {
    private const val TAG = "HouseholdRepo"
    private const val HOUSEHOLDS = "households"

    private val db by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }

    private val _all = MutableStateFlow<List<LiveHousehold>>(emptyList())
    private val _mine = MutableStateFlow<List<LiveHousehold>>(emptyList())

    /** Every home (worker / supervisor view). Empty for residents: the rules deny them the full list. */
    val all: StateFlow<List<LiveHousehold>> = _all.asStateFlow()

    /** The signed-in resident's own home(s). */
    val mine: StateFlow<List<LiveHousehold>> = _mine.asStateFlow()

    private var allListener: ListenerRegistration? = null
    private var mineListener: ListenerRegistration? = null
    private var mineFor: String? = null

    /** Worker phone: all homes, live. */
    fun listenAll() {
        if (allListener != null) return
        val firestore = db ?: return
        allListener = firestore.collection(HOUSEHOLDS).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "Households listener: ${err.message}")
                return@addSnapshotListener
            }
            _all.value = snap?.documents?.map { it.toHousehold() }?.sortedBy { it.code } ?: emptyList()
        }
    }

    /** Citizen phone: only homes whose ownerUid is the signed-in citizen. */
    fun listenMine(uid: String) {
        if (uid.isBlank() || uid == mineFor) return
        val firestore = db ?: return
        mineListener?.remove()
        mineFor = uid
        mineListener = firestore.collection(HOUSEHOLDS).whereEqualTo("ownerUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "My home listener: ${err.message}")
                    mineFor = null
                    return@addSnapshotListener
                }
                _mine.value = snap?.documents?.map { it.toHousehold() } ?: emptyList()
            }
    }

    fun stopListening() {
        allListener?.remove(); allListener = null
        mineListener?.remove(); mineListener = null
        mineFor = null
        _all.value = emptyList()
        _mine.value = emptyList()
    }

    /** Worker confirms the pickup at a home. */
    fun markCollected(household: LiveHousehold, workerId: String, workerName: String, onResult: (Boolean, String) -> Unit) {
        val firestore = db ?: return onResult(false, "No connection to Firestore.")
        firestore.collection(HOUSEHOLDS).document(household.id).update(
            mapOf(
                "status" to "Collected",
                "collectedDate" to CollectionRepository.todayString(),
                "collectedAtMs" to System.currentTimeMillis(),
                "collectedBy" to workerId.trim().uppercase(),
                "collectedByName" to workerName
            )
        ).addOnSuccessListener { onResult(true, "${household.name.ifBlank { household.code }} marked as collected.") }
            .addOnFailureListener { onResult(false, "Could not save: ${it.message}") }
    }

    /** Resident links their home using the code the supervisor gave them (e.g. HH-101). */
    fun claimHome(code: String, citizenName: String, onResult: (Boolean, String) -> Unit) {
        val firestore = db ?: return onResult(false, "No connection to Firestore.")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(false, "Please sign in first.")
        val clean = code.trim().uppercase()
        if (clean.isBlank()) return onResult(false, "Enter your block code.")
        firestore.collection(HOUSEHOLDS).document(clean).update(
            mapOf("ownerUid" to uid, "ownerName" to citizenName, "claimedAtMs" to System.currentTimeMillis())
        ).addOnSuccessListener {
            listenMine(uid)
            onResult(true, "Block $clean linked. Scan the worker's QR when they reach your block.")
        }.addOnFailureListener {
            Log.w(TAG, "claim failed: ${it.message}")
            onResult(false, "That code is wrong or already linked to another resident.")
        }
    }

    /** Fields a resident's QR scan writes on their own home (merged into the confirmCollection batch). */
    fun collectedFields(citizenName: String): Map<String, Any> = mapOf(
        "status" to "Collected",
        "collectedDate" to CollectionRepository.todayString(),
        "collectedAtMs" to System.currentTimeMillis(),
        "collectedBy" to "QR",
        "collectedByName" to citizenName
    )

    internal fun homesOf(uid: String, onLoaded: (List<DocumentSnapshot>) -> Unit) {
        val firestore = db ?: return onLoaded(emptyList())
        firestore.collection(HOUSEHOLDS).whereEqualTo("ownerUid", uid).get()
            .addOnSuccessListener { onLoaded(it.documents) }
            .addOnFailureListener { onLoaded(emptyList()) }
    }

    private fun DocumentSnapshot.toHousehold() = LiveHousehold(
        id = id,
        code = getString("code") ?: id,
        name = getString("name") ?: "",
        address = getString("address") ?: "",
        areaId = getString("areaId") ?: "",
        areaName = getString("areaName") ?: "",
        lat = getDouble("lat") ?: 12.8230,
        lng = getDouble("lng") ?: 80.0440,
        ownerUid = getString("ownerUid") ?: "",
        ownerName = getString("ownerName") ?: "",
        collectedDate = getString("collectedDate") ?: "",
        collectedAtMs = getLong("collectedAtMs") ?: 0L,
        collectedByName = getString("collectedByName") ?: ""
    )
}
