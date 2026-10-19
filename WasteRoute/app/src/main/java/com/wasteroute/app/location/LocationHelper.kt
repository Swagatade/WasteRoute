package com.wasteroute.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Google location API (fused provider) helpers shared by the citizen and worker apps. */
object LocationHelper {

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val out = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, out)
        return out[0].toDouble()
    }

    fun formatDistance(m: Double): String =
        if (m < 1000) "${m.toInt()} m" else String.format(Locale.US, "%.1f km", m / 1000.0)

    /** One fresh GPS fix (falls back to the last known one). Null if permission/GPS is unavailable. */
    suspend fun currentLocation(ctx: Context): Pair<Double, Double>? {
        if (!hasPermission(ctx)) return null
        return withTimeoutOrNull(12_000L) { fusedLocation(ctx) } ?: systemLastKnown(ctx)
    }

    /** Fallback when Google Play services gives nothing: newest last-known fix from any system provider. */
    @SuppressLint("MissingPermission")
    private fun systemLastKnown(ctx: Context): Pair<Double, Double>? = runCatching {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        lm.getProviders(true)
            .mapNotNull { lm.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }.getOrNull()

    @SuppressLint("MissingPermission")
    private suspend fun fusedLocation(ctx: Context): Pair<Double, Double>? {
        return suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { fresh ->
                    if (fresh != null) {
                        cont.resume(fresh.latitude to fresh.longitude)
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last?.let { it.latitude to it.longitude }) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    /** Human readable street address for a coordinate (Android Geocoder), or null when offline. */
    @Suppress("DEPRECATION")
    suspend fun address(ctx: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            Geocoder(ctx, Locale.getDefault()).getFromLocation(lat, lng, 1)
                ?.firstOrNull()?.getAddressLine(0)
        }.getOrNull()
    }

    /** Continuous GPS updates. Returns a function that stops them. */
    @SuppressLint("MissingPermission")
    fun startUpdates(ctx: Context, intervalMs: Long, onLocation: (Double, Double) -> Unit): () -> Unit {
        if (!hasPermission(ctx)) return {}
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(5f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { onLocation(it.latitude, it.longitude) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        return { client.removeLocationUpdates(callback) }
    }
}
