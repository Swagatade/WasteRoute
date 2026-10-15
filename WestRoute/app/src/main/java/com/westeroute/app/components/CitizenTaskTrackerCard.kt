package com.westeroute.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.firebase.LiveComplaint
import com.westeroute.app.firebase.TaskStage
import com.westeroute.app.location.LocationHelper
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.util.ImageUtil

private val STEP_LABELS = listOf("Reported", "Accepted", "On the way", "Arrived", "Solved")

fun stageTitle(stage: String): String = when (stage) {
    TaskStage.REPORTED -> "Waiting for a nearby worker"
    TaskStage.ACCEPTED -> "Worker accepted your report"
    TaskStage.ON_THE_WAY -> "Truck is on the way"
    TaskStage.ARRIVED -> "Truck has arrived"
    TaskStage.RESOLVED -> "Issue solved"
    else -> stage
}

/** Citizen view of one of their own reports: photos, assigned worker + truck, stage stepper and live progress. */
@Composable
fun CitizenTaskTrackerCard(
    task: LiveComplaint,
    onTrackTruck: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val textMain = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val stepIndex = TaskStage.ORDER.indexOf(task.stage).coerceAtLeast(0)
    val before = remember(task.photoBefore) { ImageUtil.fromDataUri(task.photoBefore) }
    val after = remember(task.photoAfter) { ImageUtil.fromDataUri(task.photoAfter) }
    val resolved = task.stage == TaskStage.RESOLVED
    val tracking = task.stage == TaskStage.ON_THE_WAY || task.stage == TaskStage.ARRIVED || task.stage == TaskStage.ACCEPTED

    val distance = if (task.truckLat != null && task.truckLng != null) {
        LocationHelper.distanceM(task.truckLat, task.truckLng, task.lat, task.lng)
    } else null

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color(0xCC111C2E) else Color.White)
            .border(1.dp, if (isDark) Color(0x3364748B) else Color(0x2294A3B8), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${task.ticketNo} - ${task.category}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = textMain)
                Text(listOf(task.location, formatWhen(task.createdAtMs)).filter { it.isNotBlank() }.joinToString("  -  "), fontSize = 12.sp, color = textSub, maxLines = 2)
            }
            Box(
                Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (resolved) Color(0x3310B981) else Color(0x3338BDF8))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(task.status, fontSize = 11.sp, fontWeight = FontWeight.Black, color = if (resolved) Emerald400 else Cyan400)
            }
        }

        // Photos: before (always) and after proof (once solved)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhotoTile("Your photo", before, Modifier.weight(1f))
            if (resolved) PhotoTile("Proof by worker", after, Modifier.weight(1f))
        }

        Text(stageTitle(task.stage), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (resolved) Emerald400 else textMain)

        if (task.stage == TaskStage.REPORTED) {
            Text(
                text = if (task.notifiedWorkerNames.isNotEmpty())
                    "Calling nearby workers automatically: ${task.notifiedWorkerNames.joinToString()}. The first one to accept gets your task."
                else "No on-duty worker found nearby yet. Workers within 5 km are alerted as soon as they are online.",
                fontSize = 12.sp, color = Cyan400
            )
        }

        // Stage stepper
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            STEP_LABELS.forEachIndexed { i, label ->
                val done = i <= stepIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape)
                            .background(if (done) Emerald400 else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) Icon(Icons.Filled.Check, null, tint = Color(0xFF052E1B), modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 10.sp, color = if (done) textMain else textSub, fontWeight = if (i == stepIndex) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        // Worker + truck + progress bar with a moving truck
        if (!task.assignedWorkerName.isNullOrBlank() && !resolved) {
            Text(
                "${task.assignedWorkerName} - ${task.truckName.ifBlank { "Truck" }}",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textSub
            )
            val liveWorkers by com.westeroute.app.firebase.FirebaseDataRepository.workers.collectAsState()
            WorkerPhoneRow(
                phone = remember(liveWorkers, task.assignedWorkerId, task.assignedWorkerName) {
                    com.westeroute.app.firebase.FirebaseDataRepository.workerPhone(task.assignedWorkerId, task.assignedWorkerName)
                }
            )
        }
        if (task.stage == TaskStage.ON_THE_WAY || task.stage == TaskStage.ARRIVED) {
            TruckProgressBar(progress = task.progress, isDark = isDark)
            Text(
                text = when {
                    task.stage == TaskStage.ARRIVED -> "Truck reached the spot - cleaning in progress"
                    distance != null -> "${task.progress}% of the way - ${LocationHelper.formatDistance(distance)} away - ETA ~${etaMinutes(distance)} min"
                    else -> "${task.progress}% of the way"
                },
                fontSize = 12.sp, color = textSub
            )
        }

        if (onRemove != null) {
            Text(
                "Remove from history",
                modifier = Modifier.clickable(onClick = onRemove).padding(vertical = 4.dp),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)
            )
        }

        if (tracking && task.stage != TaskStage.ACCEPTED) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cyan400.copy(alpha = 0.16f))
                    .clickable(onClick = onTrackTruck).padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Map, null, tint = Cyan400, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Track truck on map", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cyan400)
            }
        }
    }
}

@Composable
internal fun PhotoTile(label: String, bitmap: android.graphics.Bitmap?, modifier: Modifier) {
    val isDark = isSystemInDarkTheme()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), label, Modifier.fillMaxWidth().height(110.dp), contentScale = ContentScale.Crop)
            } else {
                Text("No photo", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
        Text(label, fontSize = 10.5.sp, color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
    }
}

/** Progress bar whose truck icon drives from left (start) to right (the issue). */
@Composable
fun TruckProgressBar(progress: Int, isDark: Boolean, modifier: Modifier = Modifier) {
    val fraction = (progress / 100f).coerceIn(0f, 1f)
    BoxWithConstraints(modifier.fillMaxWidth().height(34.dp)) {
        val iconSize = 28.dp
        val travel = maxWidth - iconSize
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        )
        Box(
            Modifier.align(Alignment.CenterStart).fillMaxWidth(fraction).height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Emerald400)
        )
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = travel * fraction).size(iconSize).clip(CircleShape)
                .background(Cyan400),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocalShipping, "Truck", tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
        }
    }
}
