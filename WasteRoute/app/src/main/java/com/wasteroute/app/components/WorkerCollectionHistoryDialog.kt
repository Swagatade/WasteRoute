package com.wasteroute.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wasteroute.app.firebase.CollectionRepository
import com.wasteroute.app.firebase.LiveSchedule

private fun LiveSchedule.stateLabel(today: String) = when {
    isCompleted -> "Completed"
    date < today -> "Overdue"
    status == "In Progress" -> "In Progress"
    else -> "Scheduled"
}

private fun stateColor(label: String) = when (label) {
    "Completed" -> Color(0xFF10B981)
    "In Progress" -> Color(0xFFF59E0B)
    "Overdue" -> Color(0xFFEF4444)
    else -> Color(0xFF38BDF8)
}

/**
 * Every collection slot the supervisor (or the daily routine) gave this worker, with whether it was
 * completed and when. Slots stay here for good, so finished work is never lost from the app.
 */
@Composable
fun WorkerCollectionHistoryDialog(
    allSlots: List<LiveSchedule>,
    onOpenReports: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF0B1020) else Color(0xFFF6F8FA)
    val fg = if (isDark) Color.White else Color(0xFF0F172A)
    val muted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val card = if (isDark) Color(0xFF131A2E) else Color.White
    val today = CollectionRepository.todayString()
    // Only work that is due: today and earlier. Slots for future days are never shown to the worker.
    val slots = allSlots.filter { it.date <= today }
    val sorted = slots.sortedWith(compareByDescending<LiveSchedule> { it.date }.thenByDescending { it.startTime })
    val done = slots.count { it.isCompleted }
    val pendingCount = slots.count { !it.isCompleted }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = bg) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Task history", color = fg, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Collection slots assigned to you", color = muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close", tint = fg) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryChip("Completed", done, Color(0xFF10B981), Modifier.weight(1f), card, muted)
                    SummaryChip("Pending", pendingCount, Color(0xFFF59E0B), Modifier.weight(1f), card, muted)
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onOpenReports) { Text("Citizen reports history", color = Color(0xFF38BDF8)) }
                if (sorted.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text("No collection slots yet.", color = muted)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        items(sorted, key = { it.id }) { slot ->
                            val label = slot.stateLabel(today)
                            val accent = stateColor(label)
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(card).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(slot.locationName.ifBlank { "Collection point" }, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${slot.date}  ${slot.startTime}${if (slot.endTime.isNotBlank()) "-${slot.endTime}" else ""}", color = muted, fontSize = 12.sp)
                                    if (slot.isCompleted && slot.completedAtMs > 0L) {
                                        Text(
                                            "Completed ${formatWhen(slot.completedAtMs)}" + if (slot.completedBy.isNotBlank()) " · ${slot.completedBy}" else "",
                                            color = Color(0xFF10B981), fontSize = 11.sp
                                        )
                                    }
                                }
                                Text(
                                    label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(accent.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, accent: Color, modifier: Modifier, card: Color, muted: Color) {
    Column(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(card).padding(12.dp)) {
        Text("$count", color = accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = muted, fontSize = 11.sp)
    }
}

/** Details of one assigned collection slot, with a shortcut to show it on the map. */
@Composable
fun WorkerTaskDetailDialog(
    slot: LiveSchedule,
    onShowOnMap: () -> Unit,
    onDismiss: () -> Unit
) {
    val today = CollectionRepository.todayString()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(slot.locationName.ifBlank { "Collection point" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Status: ${slot.stateLabel(today)}")
                Text("Date: ${slot.date}")
                Text("Time: ${slot.startTime}${if (slot.endTime.isNotBlank()) " - ${slot.endTime}" else ""}")
                if (slot.department.isNotBlank()) Text("Zone: ${slot.department}")
                Text("Assigned to: ${slot.workerName.ifBlank { slot.workerId }}")
                Text("Location: %.5f, %.5f".format(slot.lat, slot.lng))
                if (slot.notes.isNotBlank()) Text("Notes: ${slot.notes}")
                if (slot.isCompleted && slot.completedAtMs > 0L) Text("Completed: ${formatWhen(slot.completedAtMs)}")
            }
        },
        confirmButton = { TextButton(onClick = onShowOnMap) { Text("Show on map") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
