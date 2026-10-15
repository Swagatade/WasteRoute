package com.westeroute.app.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.westeroute.app.firebase.DispatchRepository
import com.westeroute.app.firebase.FirebaseDataRepository
import com.westeroute.app.firebase.LiveComplaint
import com.westeroute.app.firebase.TaskStage
import com.westeroute.app.location.LocationHelper
import com.westeroute.app.notify.TaskAlarm
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.util.ImageUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Worker side of the citizen-report flow:
 *  - rings an alarm + notification when a citizen reports garbage near the worker,
 *  - lets the worker accept (first one wins), drive the truck, and upload the "solved" proof photo,
 *  - streams the worker's GPS so the citizen and supervisor see the truck moving.
 * Only tasks assigned to this worker are shown.
 */
@Composable
fun WorkerDispatchSection(
    workerId: String,
    workerName: String,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val complaints by FirebaseDataRepository.complaints.collectAsState()
    val workers by FirebaseDataRepository.workers.collectAsState()

    val me = workers.firstOrNull { it.id.equals(workerId, ignoreCase = true) }
    val vehicle = me?.vehicle?.takeIf { it.isNotBlank() && !it.equals("Unassigned", ignoreCase = true) } ?: "Collection Truck"

    var myLoc by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var hasLoc by remember { mutableStateOf(LocationHelper.hasPermission(ctx)) }
    var demoTaskId by remember { mutableStateOf<String?>(null) }
    var trackingTaskId by remember { mutableStateOf<String?>(null) }
    var proofTaskId by remember { mutableStateOf<String?>(null) }
    var proofBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val dismissed = remember { mutableStateListOf<String>() }
    val alerted = remember { mutableStateListOf<String>() }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

    // ---- Permissions (location, notifications, camera for the proof photo) -------------------
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLoc = LocationHelper.hasPermission(ctx)
    }
    LaunchedEffect(Unit) {
        val need = buildList {
            if (!hasLoc) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (need.isNotEmpty()) permLauncher.launch(need.toTypedArray())
    }

    // Full-resolution proof photo
    val openCamera = com.westeroute.app.util.rememberPhotoCapture(
        onCancelled = { if (proofBitmap == null) proofTaskId = null }
    ) { bmp -> proofBitmap = bmp }

    // ---- Task lists (privacy: only tasks this worker accepted / was assigned) -----------------
    val mine = complaints.filter { it.assignedWorkerId.equals(workerId, ignoreCase = true) }
    val active = mine.filter { it.stage != TaskStage.RESOLVED }
    val solvedCount = mine.count { it.stage == TaskStage.RESOLVED }
    val activeNow by rememberUpdatedState(active)
    val demoNow by rememberUpdatedState(demoTaskId)

    // ---- Live GPS: publish worker position, and move the truck of every trip in progress --------
    LaunchedEffect(hasLoc, workerId) {
        if (hasLoc) LocationHelper.currentLocation(ctx)?.let {
            myLoc = it
            DispatchRepository.publishWorkerLocation(workerId, it.first, it.second)
        }
    }
    DisposableEffect(hasLoc, workerId) {
        if (!hasLoc) return@DisposableEffect onDispose { }
        val stop = LocationHelper.startUpdates(ctx, 8000L) { lat, lng ->
            myLoc = lat to lng
            DispatchRepository.publishWorkerLocation(workerId, lat, lng)
            activeNow.filter { it.stage == TaskStage.ON_THE_WAY && it.id != demoNow }
                .forEach { DispatchRepository.pushTruckLocation(it, lat, lng) }
        }
        onDispose { stop() }
    }

    // ---- Demo drive (testing without moving): truck advances towards the issue every 2.5 s ----
    LaunchedEffect(demoTaskId) {
        val id = demoTaskId ?: return@LaunchedEffect
        while (true) {
            val t = activeNow.firstOrNull { it.id == id } ?: break
            if (t.stage != TaskStage.ON_THE_WAY) break
            val curLat = t.truckLat ?: myLoc?.first ?: (t.lat - 0.004)
            val curLng = t.truckLng ?: myLoc?.second ?: (t.lng - 0.003)
            val d = LocationHelper.distanceM(curLat, curLng, t.lat, t.lng)
            val step = maxOf(t.tripStartDistanceM * 0.2, 25.0)
            val f = if (d <= step) 1.0 else step / d
            DispatchRepository.pushTruckLocation(t, curLat + (t.lat - curLat) * f, curLng + (t.lng - curLng) * f)
            delay(2500)
        }
        demoTaskId = null
    }

    // ---- Incoming alarm -------------------------------------------------------------------------
    // A worker is called automatically when a citizen reports garbage that nobody has accepted yet and
    // (a) the system picked this worker, or (b) the spot is within 5 km of this worker's live position.
    val here = myLoc ?: me?.let { it.lat to it.lng }
    val incoming = complaints.filter {
        val calledByName = it.notifiedWorkerIds.any { id -> id.equals(workerId, ignoreCase = true) }
        val isTestTicket = it.ticketNo.startsWith("#WR-MOCK")
        val nearMe = here != null && !isTestTicket &&
            LocationHelper.distanceM(here.first, here.second, it.lat, it.lng) <= 5000.0
        it.stage == TaskStage.REPORTED && it.status != "Resolved" && it.assignedWorkerId.isNullOrBlank() &&
            it.id !in dismissed && (calledByName || nearMe)
    }
    LaunchedEffect(incoming.map { it.id }) {
        val fresh = incoming.filter { it.id !in alerted }
        if (fresh.isNotEmpty()) {
            alerted.addAll(fresh.map { it.id })
            fresh.forEach { t ->
                val away = myLoc?.let { " - ${LocationHelper.formatDistance(LocationHelper.distanceM(it.first, it.second, t.lat, t.lng))} away" } ?: ""
                TaskAlarm.notifyNewTask(ctx, t.id, "New garbage task nearby", "${t.category} at ${t.location}$away. Tap to accept.")
            }
            TaskAlarm.startRinging(ctx)
        }
        if (incoming.isEmpty()) TaskAlarm.stopRinging()
    }
    DisposableEffect(Unit) { onDispose { TaskAlarm.stopRinging() } }

    incoming.firstOrNull()?.let { t ->
        val photo = remember(t.photoBefore) { ImageUtil.fromDataUri(t.photoBefore) }
        val away = myLoc?.let { LocationHelper.formatDistance(LocationHelper.distanceM(it.first, it.second, t.lat, t.lng)) }
        AlertDialog(
            onDismissRequest = { },
            title = { Text("New garbage task nearby", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (photo != null) {
                        Image(
                            photo.asImageBitmap(), "Reported photo",
                            Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text("${t.category} - ${t.ticketNo}", fontWeight = FontWeight.Bold)
                    // Where the citizen took the photo, and the route from the worker
                    TaskRouteMap(task = t, myLoc = myLoc)
                    Text(t.location + (away?.let { "  ($it away)" } ?: ""))
                    Text("Reported by ${t.citizenName}. The first worker to accept gets the task.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    TaskAlarm.stopRinging()
                    DispatchRepository.acceptTask(t.id, workerId, workerName, vehicle) { _, msg -> toast(msg) }
                }) { Text("Accept task", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    TaskAlarm.stopRinging()
                    dismissed.add(t.id)
                }) { Text("Not now") }
            }
        )
    }

    // ---- Proof photo dialog -----------------------------------------------------------------------
    val proofTask = active.firstOrNull { it.id == proofTaskId }
    val proof = proofBitmap
    if (proofTask != null && proof != null) {
        AlertDialog(
            onDismissRequest = { proofBitmap = null; proofTaskId = null },
            title = { Text("Proof of cleaning", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(
                        proof.asImageBitmap(), "Proof photo",
                        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text("This photo is sent to the citizen and the supervisor, and the task is marked solved.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = ImageUtil.toDataUri(proof)
                    DispatchRepository.resolveTask(proofTask, workerId, workerName, uri, myLoc?.first, myLoc?.second) { _, msg -> toast(msg) }
                    proofBitmap = null
                    proofTaskId = null
                }) { Text("Submit & mark solved", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { proofBitmap = null; openCamera() }) { Text("Retake") }
            }
        )
    }

    active.firstOrNull { it.id == trackingTaskId }?.let { t ->
        TaskTrackingMapDialog(task = t, onDismiss = { trackingTaskId = null })
    }

    // ---- UI -------------------------------------------------------------------------------------------
    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "CITIZEN REPORTS", fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp,
                color = if (isDark) Cyan400 else Color(0xFF0284C7)
            )
            Text(
                "${active.size} active - $solvedCount solved", fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }

        if (active.isEmpty()) {
            Text(
                "No citizen report assigned to you yet. When someone reports garbage near you, an alarm rings and you can accept it here.",
                fontSize = 12.5.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            )
        }

        active.forEach { task ->
            DispatchTaskCard(
                task = task,
                myLoc = myLoc,
                demoRunning = demoTaskId == task.id,
                onStartTrip = {
                    scope.launch {
                        val loc = myLoc ?: LocationHelper.currentLocation(ctx) ?: (task.lat - 0.004 to task.lng - 0.003)
                        DispatchRepository.startTrip(task, workerId, loc.first, loc.second) { ok, msg -> if (!ok) toast(msg) }
                    }
                },
                onDemoDrive = { demoTaskId = if (demoTaskId == task.id) null else task.id },
                onArrived = { DispatchRepository.pushTruckLocation(task, task.lat, task.lng) },
                onTrack = { trackingTaskId = task.id },
                onProof = { proofTaskId = task.id; proofBitmap = null; openCamera() }
            )
        }
    }
}

@Composable
private fun DispatchTaskCard(
    task: LiveComplaint,
    myLoc: Pair<Double, Double>?,
    demoRunning: Boolean,
    onStartTrip: () -> Unit,
    onDemoDrive: () -> Unit,
    onArrived: () -> Unit,
    onTrack: () -> Unit,
    onProof: () -> Unit
) {
    val ctx = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val textMain = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val photo = remember(task.photoBefore) { ImageUtil.fromDataUri(task.photoBefore) }
    val toIssue = (task.truckLat?.let { LocationHelper.distanceM(it, task.truckLng ?: 0.0, task.lat, task.lng) })
        ?: myLoc?.let { LocationHelper.distanceM(it.first, it.second, task.lat, task.lng) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
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
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x3338BDF8)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(stageTitle(task.stage), fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Cyan400)
            }
        }

        if (photo != null) {
            Image(
                photo.asImageBitmap(), "Citizen photo",
                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Where to go: route map from the worker / truck to the reported spot
        Text(
            "ROUTE TO THE SPOT", fontSize = 10.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp,
            color = if (isDark) Cyan400 else Color(0xFF0284C7)
        )
        TaskRouteMap(task = task, myLoc = myLoc)

        Text(
            buildString {
                append("Truck: ${task.truckName.ifBlank { "Collection Truck" }}")
                toIssue?.let { append("  -  ${LocationHelper.formatDistance(it)} to the spot") }
                append("  -  Reported by ${task.citizenName}")
            },
            fontSize = 12.sp, color = textSub
        )

        if (task.stage == TaskStage.ON_THE_WAY || task.stage == TaskStage.ARRIVED) {
            TruckProgressBar(progress = task.progress, isDark = isDark)
            Text("${task.progress}% of the way", fontSize = 12.sp, color = textSub)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Navigate in Google Maps", Color(0xFF38BDF8), { openNavigationTo(ctx, task.lat, task.lng) })
            when (task.stage) {
                TaskStage.ACCEPTED -> ActionButton("Start trip with truck", Emerald400, onStartTrip)
                TaskStage.ON_THE_WAY -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton("Track map", Cyan400, onTrack, Modifier.weight(1f))
                        ActionButton(if (demoRunning) "Stop demo" else "Demo drive", Color(0xFFF59E0B), onDemoDrive, Modifier.weight(1f))
                    }
                    ActionButton("I have arrived", Cyan400, onArrived)
                }
                TaskStage.ARRIVED -> ActionButton("Track map", Cyan400, onTrack)
            }
            if (task.stage == TaskStage.ON_THE_WAY || task.stage == TaskStage.ARRIVED) {
                ActionButton("Take proof photo & mark solved", Emerald400, onProof)
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
