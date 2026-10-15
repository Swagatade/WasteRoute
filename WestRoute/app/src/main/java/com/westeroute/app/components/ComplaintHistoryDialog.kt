package com.westeroute.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.westeroute.app.firebase.LiveComplaint
import com.westeroute.app.firebase.TaskStage
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.util.ImageUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatWhen(ms: Long): String =
    if (ms <= 0L) "" else SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ms))

private enum class HistoryFilter(val label: String) { ALL("All"), ACTIVE("Active"), SOLVED("Solved") }

/**
 * Full history of complaints, newest first. Citizens see their own reports (with live tracking);
 * workers see every task they accepted, with before/after photos. Data comes from Firestore, so it is kept.
 */
@Composable
fun ComplaintHistoryDialog(
    title: String,
    subtitle: String,
    tasks: List<LiveComplaint>,
    isWorker: Boolean,
    viewerId: String,
    onRemove: (List<LiveComplaint>) -> Unit,
    onTrack: (LiveComplaint) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var pendingRemove by remember { mutableStateOf<List<LiveComplaint>?>(null) }
    val sorted = tasks.filter { t -> t.hiddenFor.none { it.equals(viewerId, ignoreCase = true) } }
        .sortedByDescending { maxOf(it.createdAtMs, it.resolvedAtMs) }
    val activeCount = sorted.count { it.stage != TaskStage.RESOLVED }
    val solvedCount = sorted.size - activeCount
    val shown = when (filter) {
        HistoryFilter.ALL -> sorted
        HistoryFilter.ACTIVE -> sorted.filter { it.stage != TaskStage.RESOLVED }
        HistoryFilter.SOLVED -> sorted.filter { it.stage == TaskStage.RESOLVED }
    }

    pendingRemove?.let { list ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text(if (list.size == 1) "Remove from history?" else "Remove ${list.size} solved items?") },
            text = { Text("They will disappear from your history only. Nothing is deleted for anyone else.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onRemove(list)
                    pendingRemove = null
                }) { Text("Remove", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingRemove = null }) { Text("Cancel") } }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) } }

        Surface(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            color = if (isDark) Color(0xFF0B1120) else Color(0xFFF1F5F9)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color.White else Color(0xFF0F172A))
                        Text(subtitle, fontSize = 12.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(if (isDark) Color(0x33334155) else Color(0x1A0F172A))
                    ) {
                        Icon(Icons.Filled.Close, "Close", tint = if (isDark) Color.White else Color(0xFF0F172A))
                    }
                }

                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryFilter.values().forEach { f ->
                        val count = when (f) {
                            HistoryFilter.ALL -> sorted.size
                            HistoryFilter.ACTIVE -> activeCount
                            HistoryFilter.SOLVED -> solvedCount
                        }
                        val selected = f == filter
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .background(if (selected) Emerald400.copy(alpha = 0.2f) else if (isDark) Color(0x1A334155) else Color(0x0D0F172A))
                                .border(1.dp, if (selected) Emerald400 else Color(0x3364748B), RoundedCornerShape(12.dp))
                                .clickable { filter = f }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "${f.label} ($count)", fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (selected) Emerald400 else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }

                if (solvedCount > 0) {
                    Text(
                        "Clear all solved ($solvedCount)",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { pendingRemove = sorted.filter { it.stage == TaskStage.RESOLVED } },
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)
                    )
                }

                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (isWorker) "No tasks yet. Tasks you accept and solve will be kept here."
                            else "No complaints yet. Tap the garbage picture to report one.",
                            fontSize = 14.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(shown, key = { it.id }) { task ->
                            val removable = task.stage == TaskStage.RESOLVED
                            val remove: (() -> Unit)? = if (removable) ({ pendingRemove = listOf(task) }) else null
                            if (isWorker) WorkerHistoryCard(task, remove)
                            else CitizenTaskTrackerCard(task = task, onTrackTruck = { onTrack(task) }, onRemove = remove)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerHistoryCard(task: LiveComplaint, onRemove: (() -> Unit)? = null) {
    val isDark = isSystemInDarkTheme()
    val textMain = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val solved = task.stage == TaskStage.RESOLVED
    val before = remember(task.photoBefore) { ImageUtil.fromDataUri(task.photoBefore) }
    val after = remember(task.photoAfter) { ImageUtil.fromDataUri(task.photoAfter) }

    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xCC111C2E) else Color.White)
            .border(1.dp, if (isDark) Color(0x3364748B) else Color(0x2294A3B8), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${task.ticketNo} - ${task.category}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = textMain)
                Text(task.location, fontSize = 12.sp, color = textSub, maxLines = 2)
            }
            Box(
                Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (solved) Color(0x3310B981) else Color(0x3338BDF8))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    if (solved) "Solved" else stageTitle(task.stage), fontSize = 10.5.sp, fontWeight = FontWeight.Black,
                    color = if (solved) Emerald400 else Cyan400
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhotoTile("Citizen photo", before, Modifier.weight(1f))
            if (solved) PhotoTile("My proof photo", after, Modifier.weight(1f))
        }
        Text(
            buildString {
                append("Reported by ${task.citizenName}")
                if (task.createdAtMs > 0) append("  -  ${formatWhen(task.createdAtMs)}")
                if (solved && task.resolvedAtMs > 0) append("\nSolved ${formatWhen(task.resolvedAtMs)}")
                if (task.truckName.isNotBlank()) append("\nTruck: ${task.truckName}")
            },
            fontSize = 12.sp, color = textSub
        )
        if (onRemove != null) {
            Text(
                "Remove from history",
                modifier = Modifier.clickable(onClick = onRemove).padding(vertical = 4.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)
            )
        }
    }
}
