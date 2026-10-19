package com.westeroute.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Alarm-style notification + looping ringtone for new tasks, and quiet status notifications for citizens. */
object TaskAlarm {
    private const val ALARM_CHANNEL = "task_alarm_v2"
    private const val ALARM_NOTIFICATION_ID = 4200
    private const val INFO_CHANNEL = "task_updates_v1"
    private val VIBRATION = longArrayOf(0, 600, 300, 600, 300, 900)

    private var ringtone: Ringtone? = null

    private fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        // Silent on purpose: the single looping ringtone (startRinging) is the only alarm sound,
        // so several new tasks never play several sounds at once.
        val alarm = NotificationChannel(ALARM_CHANNEL, "New task alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Heads-up banner when a citizen reports garbage near you"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val info = NotificationChannel(INFO_CHANNEL, "Report updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Progress of the garbage reports you submitted"
        }
        nm.deleteNotificationChannel("task_alarm_v1")
        nm.createNotificationChannels(listOf(alarm, info))
    }

    private fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(ctx: Context): PendingIntent? {
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        } ?: return null
        return PendingIntent.getActivity(ctx, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Worker: loud heads-up notification for a new nearby task. */
    fun notifyNewTask(ctx: Context, @Suppress("UNUSED_PARAMETER") taskId: String, title: String, text: String) {
        val app = ctx.applicationContext
        ensureChannels(app)
        if (!canNotify(app)) return
        val n = NotificationCompat.Builder(app, ALARM_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(app))
            .build()
        // Same id every time: new tasks update this one banner instead of stacking up
        runCatching { NotificationManagerCompat.from(app).notify(ALARM_NOTIFICATION_ID, n) }
    }

    /** Citizen: status change of their report. */
    fun notifyInfo(ctx: Context, taskId: String, title: String, text: String) {
        val app = ctx.applicationContext
        ensureChannels(app)
        if (!canNotify(app)) return
        val n = NotificationCompat.Builder(app, INFO_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(app))
            .build()
        runCatching { NotificationManagerCompat.from(app).notify(taskId.hashCode() + 1, n) }
    }

    /** Rings (looping alarm tone) while the in-app "new task" dialog is on screen. */
    fun startRinging(ctx: Context) {
        if (ringtone?.isPlaying == true) return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = runCatching {
            RingtoneManager.getRingtone(ctx.applicationContext, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                play()
            }
        }.getOrNull()
    }

    fun stopRinging() {
        runCatching { ringtone?.stop() }
        ringtone = null
    }
}
