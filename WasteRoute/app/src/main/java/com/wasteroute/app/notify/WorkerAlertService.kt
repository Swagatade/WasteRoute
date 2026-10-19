package com.wasteroute.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wasteroute.app.auth.UserSessionManager
import com.wasteroute.app.firebase.FirebaseDataRepository
import com.wasteroute.app.firebase.TaskStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps a signed-in worker "online": while it runs, a new citizen report that calls this worker rings the
 * alarm and posts a heads-up notification even if the app is closed or in the background.
 * Started from the worker dashboard, stopped on sign out.
 */
class WorkerAlertService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val alerted = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val (workerId, _) = UserSessionManager.getSavedWorkerDetails(this)
        if (workerId.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        scope.coroutineContext.cancelChildren()
        scope.launch {
            combine(FirebaseDataRepository.complaints, FirebaseDataRepository.workers) { c, w -> c to w }
                .collectLatest { (complaints, workers) ->
                    val me = workers.firstOrNull { it.id.equals(workerId, ignoreCase = true) }
                    val incoming = complaints.filter {
                        it.stage == TaskStage.REPORTED && it.status != "Resolved" && it.assignedWorkerId.isNullOrBlank() &&
                            it.notifiedWorkerIds.any { id -> id.equals(workerId, ignoreCase = true) } ||
                            (me != null && it.stage == TaskStage.REPORTED && it.assignedWorkerId.isNullOrBlank() &&
                                !it.ticketNo.startsWith("#WR-MOCK") &&
                                com.wasteroute.app.location.LocationHelper.distanceM(me.lat, me.lng, it.lat, it.lng) <= 5000.0)
                    }
                    val fresh = incoming.filter { it.id !in alerted }
                    if (fresh.isNotEmpty()) {
                        alerted.addAll(fresh.map { it.id })
                        fresh.forEach {
                            TaskAlarm.notifyNewTask(
                                this@WorkerAlertService, it.id, "New garbage task nearby",
                                "${it.category} at ${it.location}. Tap to accept."
                            )
                        }
                        TaskAlarm.startRinging(this@WorkerAlertService)
                    }
                    if (incoming.isEmpty()) TaskAlarm.stopRinging()
                }
        }
        return START_STICKY
    }

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Online for tasks", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("WasteRoute - you are online")
            .setContentText("You will be called when garbage is reported near you.")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Android 15 limits data-sync foreground services; stop cleanly, the dashboard restarts it when opened. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        TaskAlarm.stopRinging()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "worker_online_v1"
        private const val NOTIFICATION_ID = 4101

        fun start(ctx: Context) {
            runCatching { ContextCompat.startForegroundService(ctx, Intent(ctx, WorkerAlertService::class.java)) }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, WorkerAlertService::class.java))
        }
    }
}
