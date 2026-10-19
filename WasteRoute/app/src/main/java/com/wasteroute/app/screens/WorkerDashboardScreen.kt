package com.wasteroute.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wasteroute.app.components.ActiveTaskCard
import com.wasteroute.app.components.CompletedTasksCard
import com.wasteroute.app.components.HouseholdCollectionCounter
import com.wasteroute.app.components.NextTaskCard
import com.wasteroute.app.components.OfflineBanner
import com.wasteroute.app.components.RouteTimeline
import com.wasteroute.app.components.ShiftStatusCard
import com.wasteroute.app.components.TaskLocationCard
import com.wasteroute.app.components.TaskStatusBottomSheet
import com.wasteroute.app.components.WorkerActivityTimeline
import com.wasteroute.app.components.WorkerBottomNavigation
import com.wasteroute.app.components.WorkerHeader
import com.wasteroute.app.components.WorkerNavTab
import com.wasteroute.app.components.WorkerNotificationsPreview
import com.wasteroute.app.components.WorkerProgressMetrics
import com.wasteroute.app.components.WorkerQrCard
import com.wasteroute.app.components.WorkerSummaryCard
import com.wasteroute.app.components.WorkerTaskQueue
import com.wasteroute.app.model.MockWorkerData
import com.wasteroute.app.model.TaskStatus
import com.wasteroute.app.model.UiPerformanceMode
import com.wasteroute.app.model.WorkerProfile
import com.wasteroute.app.model.WorkerTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("FunctionName")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDashboardScreen(
    modifier: Modifier = Modifier,
    profile: WorkerProfile = WorkerProfile(),
    onNavigateToTaskDetail: (String) -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToRoute: () -> Unit = {},
    onNavigateToQr: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onSignOut: () -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM
) {
    val isDark = isSystemInDarkTheme()
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // Local Worker States
    var isOnDuty by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }
    var selectedNavTab by remember { mutableStateOf(WorkerNavTab.HOME) }
    var showStatusSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }
    var mapTarget by remember { mutableStateOf<com.wasteroute.app.model.CampusSmartBin?>(null) }
    var detailSlotId by remember { mutableStateOf<String?>(null) }
    val liveBins by com.wasteroute.app.firebase.FirebaseDataRepository.smartBins.collectAsState()

    // Dynamic Task State
    val liveSchedule by com.wasteroute.app.firebase.CollectionRepository.workerSchedule.collectAsState()
    val liveWorkers by com.wasteroute.app.firebase.FirebaseDataRepository.workers.collectAsState()
    val allComplaints by com.wasteroute.app.firebase.FirebaseDataRepository.complaints.collectAsState()
    val zoneName = liveWorkers.firstOrNull { it.id.equals(profile.id, true) }?.wardName?.ifBlank { null }
        ?: profile.workZone
    val todayDate = remember { com.wasteroute.app.firebase.CollectionRepository.todayString() }
    val tasksList = remember(liveSchedule) {
        // Today's slots plus earlier slots that were never completed, so an assigned task never silently disappears.
        liveSchedule.filter { it.date == todayDate || (it.date < todayDate && !it.isCompleted) }.map { slot ->
            com.wasteroute.app.model.WorkerTask(
                id = slot.id,
                title = slot.locationName.ifBlank { "Garbage collection" },
                location = listOf(slot.locationName, slot.department).filter { it.isNotBlank() }.joinToString(", "),
                distance = "",
                priority = com.wasteroute.app.model.TaskPriority.MEDIUM,
                status = when {
                    slot.isCompleted -> TaskStatus.COMPLETED
                    slot.status == "In Progress" -> TaskStatus.IN_PROGRESS
                    else -> TaskStatus.ASSIGNED
                },
                assignedTime = if (slot.date < todayDate) "Overdue ${slot.date} ${slot.startTime}" else slot.startTime,
                estimatedTravel = if (slot.endTime.isNotBlank()) "until ${slot.endTime}" else "",
                description = slot.notes.ifBlank { "Collect garbage at this point and have the resident scan your QR to confirm." }
            )
        }
    }
    val currentTask = tasksList.firstOrNull { it.status != TaskStatus.COMPLETED } ?: tasksList.lastOrNull()
    val completedCount = tasksList.count { it.status == TaskStatus.COMPLETED }
    // The map target for a slot: the matching campus bin when there is one, else the slot's own point.
    fun slotTarget(slot: com.wasteroute.app.firebase.LiveSchedule): com.wasteroute.app.model.CampusSmartBin =
        liveBins.firstOrNull {
            kotlin.math.abs(it.lat - slot.lat) < 0.0003 && kotlin.math.abs(it.lng - slot.lng) < 0.0003
        } ?: com.wasteroute.app.model.CampusSmartBin(
            id = slot.id, code = "TASK-${slot.id}", name = slot.locationName.ifBlank { "Assigned collection point" },
            zone = slot.department.ifBlank { "Assigned task" }, lat = slot.lat, lng = slot.lng, fillLevel = 0,
            type = "Assigned collection", distanceDisplay = "", status = com.wasteroute.app.components.BinStatus.AVAILABLE
        )
    val remainingCount = tasksList.count { it.status != TaskStatus.COMPLETED }

    // Today's route card: the real stops (supervisor's route), in order, with their collected state.
    val routeStops = tasksList.map { t ->
        val slot = liveSchedule.firstOrNull { it.id == t.id }
        com.wasteroute.app.components.RouteStop(
            title = t.title,
            subtitle = if (slot?.isHousehold == true) "Hostel block - warden scans your QR" else "Bin collection - photo proof",
            distance = slot?.startTime.orEmpty(),
            isCompleted = t.status == TaskStatus.COMPLETED,
            isCurrent = t.id == currentTask?.id && t.status != TaskStatus.COMPLETED
        )
    }
    val todayRouteName = liveSchedule.firstOrNull { it.date == todayDate && it.routeName.isNotBlank() }?.routeName.orEmpty()

    // Homes of the areas the supervisor assigned to this worker (never shown to residents)
    val allHomes by com.wasteroute.app.firebase.HouseholdRepository.all.collectAsState()
    val myAreaIds = liveWorkers.firstOrNull { it.id.equals(profile.id, true) }?.assignedAreaIds.orEmpty()
    val myHomes = allHomes.filter { it.areaId in myAreaIds }
    LaunchedEffect(Unit) { com.wasteroute.app.firebase.HouseholdRepository.listenAll() }

    // Camera proof for a bin stop: the photo completes the stop and every progress bar moves.
    var cameraSlotId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    val launchProofCamera = com.wasteroute.app.util.rememberPhotoCapture(
        maxDim = 960,
        onCancelled = { cameraSlotId = null }
    ) { bitmap ->
        val slot = liveSchedule.firstOrNull { it.id == cameraSlotId }
        cameraSlotId = null
        if (slot != null) {
            photoBusy = true
            com.wasteroute.app.firebase.CollectionRepository.completeWithPhoto(
                slot = slot,
                workerId = profile.id,
                workerName = profile.name.substringBefore(" (").trim(),
                photoDataUri = com.wasteroute.app.util.ImageUtil.toDataUri(bitmap, maxDim = 800, quality = 60)
            ) { _, msg ->
                photoBusy = false
                android.widget.Toast.makeText(appContext, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // Staggered Entrance Animation States
    var headerVisible by remember { mutableStateOf(false) }
    var shiftVisible by remember { mutableStateOf(false) }
    var summaryVisible by remember { mutableStateOf(false) }
    var currentTaskVisible by remember { mutableStateOf(false) }
    var routeVisible by remember { mutableStateOf(false) }
    var taskListVisible by remember { mutableStateOf(false) }
    var qrVisible by remember { mutableStateOf(false) }
    var restVisible by remember { mutableStateOf(false) }

    // Stay online in the background: the phone rings for new nearby reports even when the app is closed.
    LaunchedEffect(profile.id) {
        if (profile.id.isNotBlank()) com.wasteroute.app.notify.WorkerAlertService.start(appContext)
    }

    // Keep the daily routine slots fresh across midnight while the app stays open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15 * 60 * 1000L)
            com.wasteroute.app.firebase.CollectionRepository.refreshToday()
        }
    }

    LaunchedEffect(Unit) {
        headerVisible = true
        delay(100.milliseconds)
        shiftVisible = true
        delay(100.milliseconds)
        summaryVisible = true
        delay(150.milliseconds)
        currentTaskVisible = true
        delay(150.milliseconds)
        routeVisible = true
        delay(150.milliseconds)
        taskListVisible = true
        delay(150.milliseconds)
        qrVisible = true
        restVisible = true
    }

    // Atmospheric Background
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050607), // AMOLED near black
                Color(0xFF090D16),
                Color(0xFF0B132B),
                Color(0xFF050607)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF6F8FA),
                Color(0xFFEFF6FF),
                Color(0xFFF1F5F9),
                Color(0xFFF6F8FA)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Optional Offline Banner
            item(key = "offline_banner") {
                OfflineBanner(
                    isOffline = isOffline,
                    onDismiss = { isOffline = false }
                )
            }

            // 1. Premium Worker Header
            item(key = "header") {
                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(350))
                ) {
                    WorkerHeader(
                        workerName = profile.name,
                        workerRole = profile.role,
                        workerId = profile.id,
                        workerPhone = liveWorkers.firstOrNull { it.id.equals(profile.id, ignoreCase = true) }?.phone.orEmpty(),
                        location = "SRMIST, Kattankulathur",
                        workZone = zoneName,
                        isOnDuty = isOnDuty,
                        onDutyToggle = { isOnDuty = !isOnDuty },
                        hasUnreadNotifications = false,
                        onNotificationClick = onNavigateToNotifications,
                        onProfileClick = {
                            showProfileSheet = true
                            onNavigateToProfile()
                        },
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_summary") {
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Citizen garbage reports: alarm + accept, truck trip with live progress, proof photo
            item(key = "dispatch") {
                com.wasteroute.app.components.WorkerDispatchSection(
                    workerId = profile.id,
                    workerName = profile.name.substringBefore(" (").trim()
                )
            }

            item(key = "spacer_dispatch") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Today's Work Summary (4-Stat Metric Grid)
            item(key = "work_summary") {
                AnimatedVisibility(
                    visible = summaryVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { 30 },
                        animationSpec = tween(400)
                    )
                ) {
                    WorkerSummaryCard(
                        assignedCount = tasksList.size,
                        completedCount = completedCount,
                        inProgressCount = tasksList.count { it.status == TaskStatus.IN_PROGRESS },
                        remainingCount = remainingCount,
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_active_task") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (currentTask == null) {
                item(key = "no_tasks") {
                    com.wasteroute.app.components.GlassCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "No collections scheduled for you today. Open the Route tab to see upcoming slots set by your supervisor.",
                            modifier = Modifier.padding(20.dp),
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }
            } else {
            // 4. Main Current Active Task Card
            item(key = "active_task") {
                AnimatedVisibility(
                    visible = currentTaskVisible,
                    enter = fadeIn(tween(450)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(450)
                    )
                ) {
                    ActiveTaskCard(
                        task = currentTask,
                        isOnDuty = isOnDuty,
                        onNavigateClick = {
                            mapTarget = liveSchedule.firstOrNull { it.id == currentTask.id }?.let { slotTarget(it) }
                            showMapDialog = true
                        },
                        onDetailsClick = { detailSlotId = currentTask.id },
                        onUpdateStatusClick = { showStatusSheet = true },
                        onCameraClick = {
                            cameraSlotId = currentTask.id
                            launchProofCamera()
                        },
                        awaitingResidentQr = liveSchedule.firstOrNull { it.id == currentTask.id }?.isHousehold == true,
                        cameraBusy = photoBusy,
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_location") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 6. Task Location Map Preview
            item(key = "task_location") {
                AnimatedVisibility(
                    visible = currentTaskVisible,
                    enter = fadeIn(tween(450))
                ) {
                    // Real coordinates of the scheduled collection point set by the supervisor
                    val slot = liveSchedule.firstOrNull { it.id == currentTask.id }
                    TaskLocationCard(
                        location = currentTask.location,
                        destinationLat = slot?.lat ?: com.wasteroute.app.model.CampusMapConfig.CAMPUS_LAT,
                        destinationLng = slot?.lng ?: com.wasteroute.app.model.CampusMapConfig.CAMPUS_LNG,
                        spot = slot?.let {
                            com.wasteroute.app.firebase.LiveComplaint(
                                id = it.id, ticketNo = "Collection point", category = it.locationName,
                                priority = "Medium", status = it.status, location = it.locationName, lat = it.lat, lng = it.lng
                            )
                        },
                        onNavigateClick = { showMapDialog = true },
                        onViewLocationClick = { showMapDialog = true },
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_next_task") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 7. Next Task Section (Up Next)
            item(key = "next_task") {
                AnimatedVisibility(
                    visible = routeVisible,
                    enter = fadeIn(tween(500))
                ) {
                    val nextTask = tasksList.firstOrNull { it.status != TaskStatus.COMPLETED && it.id != currentTask.id }
                    if (nextTask != null) {
                        NextTaskCard(
                            task = nextTask,
                            onViewTaskClick = { onNavigateToTaskDetail(nextTask.id) },
                            performanceMode = performanceMode
                        )
                    }
                }
            }

            }

            item(key = "spacer_route") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 8. Today's Route Timeline (only when there is something on the route)
            if (tasksList.isNotEmpty()) item(key = "route_timeline") {
                AnimatedVisibility(
                    visible = routeVisible,
                    enter = fadeIn(tween(500))
                ) {
                    RouteTimeline(
                        stops = routeStops,
                        routeName = todayRouteName,
                        estimatedCompletion = tasksList.lastOrNull()?.estimatedTravel?.removePrefix("until ")?.ifBlank { null } ?: "--",
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_task_queue") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 9. Assigned Tasks Queue with Filter Chips
            item(key = "task_queue") {
                AnimatedVisibility(
                    visible = taskListVisible,
                    enter = fadeIn(tween(500))
                ) {
                    WorkerTaskQueue(
                        tasks = tasksList,
                        onTaskClick = { task -> detailSlotId = task.id },
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_qr") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 10. Worker Collection QR Card
            item(key = "worker_qr") {
                AnimatedVisibility(
                    visible = qrVisible,
                    enter = fadeIn(tween(500))
                ) {
                    WorkerQrCard(
                        workerName = profile.name,
                        workerId = profile.id,
                        workZone = zoneName,
                        onShowFullScreenQr = onNavigateToQr,
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "assigned_homes") {
                com.wasteroute.app.components.AssignedHomesCard(
                    homes = myHomes,
                    areaLabel = myHomes.map { it.areaName }.filter { it.isNotBlank() }.distinct().joinToString(", ")
                        .ifBlank { "your area" },
                    hasAssignedArea = myAreaIds.isNotEmpty(),
                    onMarkCollected = { home ->
                        com.wasteroute.app.firebase.HouseholdRepository.markCollected(
                            home, profile.id, profile.name.substringBefore(" (").trim()
                        ) { _, msg -> android.widget.Toast.makeText(appContext, msg, android.widget.Toast.LENGTH_SHORT).show() }
                    },
                    onShowOnMap = {
                        mapTarget = null
                        showMapDialog = true
                    }
                )
            }

            item(key = "spacer_home_collections") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 11. Household Collection Counter
            item(key = "household_counter") {
                AnimatedVisibility(
                    visible = restVisible,
                    enter = fadeIn(tween(500))
                ) {
                    HouseholdCollectionCounter(
                        done = completedCount,
                        total = tasksList.size,
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // 16. Floating Bottom Navigation Bar
        WorkerBottomNavigation(
            selectedTab = selectedNavTab,
            onTabSelected = { tab ->
                selectedNavTab = tab
                when (tab) {
                    WorkerNavTab.HOME -> {}
                    WorkerNavTab.TASKS -> {
                        showHistory = true
                        onNavigateToTasks()
                    }
                    WorkerNavTab.ROUTE -> {
                        showMapDialog = true
                        onNavigateToRoute()
                    }
                    WorkerNavTab.QR -> onNavigateToQr()
                    WorkerNavTab.PROFILE -> {
                        showProfileSheet = true
                        onNavigateToProfile()
                    }
                }
            },
            performanceMode = performanceMode,
            modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 640.dp)
        )

        if (showHistory && !showReports) {
            com.wasteroute.app.components.WorkerCollectionHistoryDialog(
                allSlots = liveSchedule,
                onOpenReports = { showReports = true },
                onDismiss = {
                    showHistory = false
                    selectedNavTab = WorkerNavTab.HOME
                }
            )
        }

        detailSlotId?.let { id ->
            liveSchedule.firstOrNull { it.id == id }?.let { slot ->
                com.wasteroute.app.components.WorkerTaskDetailDialog(
                    slot = slot,
                    onShowOnMap = {
                        mapTarget = slotTarget(slot)
                        showMapDialog = true
                        detailSlotId = null
                    },
                    onDismiss = { detailSlotId = null }
                )
            }
        }

        if (showHistory && showReports) {
            com.wasteroute.app.components.ComplaintHistoryDialog(
                title = "Task history",
                subtitle = "Citizen reports you accepted and solved",
                tasks = allComplaints.filter { it.assignedWorkerId.equals(profile.id, ignoreCase = true) },
                isWorker = true,
                viewerId = profile.id,
                onRemove = { list ->
                    com.wasteroute.app.firebase.DispatchRepository.hideFromHistory(list.map { it.id }, profile.id)
                },
                onTrack = { },
                onDismiss = { showReports = false }
            )
        }

        // 17. Open-Source OpenStreetMap Campus Telemetry Dialog
        if (showMapDialog) {
            com.wasteroute.app.components.CampusOpenStreetMapDialog(
                initialSelectedBin = mapTarget,
                complaints = allComplaints.filter {
                    (it.assignedWorkerId.equals(profile.id, ignoreCase = true) && it.stage != com.wasteroute.app.firebase.TaskStage.RESOLVED) ||
                        (it.stage == com.wasteroute.app.firebase.TaskStage.REPORTED && it.assignedWorkerId.isNullOrBlank() &&
                            it.notifiedWorkerIds.any { id -> id.equals(profile.id, ignoreCase = true) })
                },
                workers = liveWorkers.filter { it.id.equals(profile.id, ignoreCase = true) },
                households = myHomes,
                onDismiss = { showMapDialog = false; mapTarget = null }
            )
        }

        // 18. Status Update Modal Bottom Sheet
        if (showStatusSheet && currentTask != null) {
            TaskStatusBottomSheet(
                currentStatus = currentTask.status,
                onStatusSelected = { newStatus ->
                    // Real stops: the worker only starts them, COMPLETED needs the resident's QR scan.
                    // Test-route stops (isMock) can be completed here since no resident is involved.
                    if (newStatus == TaskStatus.IN_PROGRESS) {
                        com.wasteroute.app.firebase.CollectionRepository.setScheduleStatus(currentTask.id, "In Progress")
                    } else if (newStatus == TaskStatus.COMPLETED) {
                        if (liveSchedule.firstOrNull { it.id == currentTask.id }?.isMock == true) {
                            com.wasteroute.app.firebase.CollectionRepository.completeMockSlot(currentTask.id, profile.id) { _, msg ->
                                android.widget.Toast.makeText(appContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            android.widget.Toast.makeText(appContext, "Use the camera button to clear a bin, or let the hostel warden scan your QR at a hostel block.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDismiss = { showStatusSheet = false },
                sheetState = sheetState
            )
        }

        // 19. Worker Profile & Account Sheet with Sign Out
        if (showProfileSheet) {
            com.wasteroute.app.components.AccountProfileBottomSheet(
                userName = profile.name,
                workerId = profile.id,
                workZone = zoneName,
                userRole = com.wasteroute.app.model.UserRole.WORKER,
                onSeedTestData = {
                    com.wasteroute.app.firebase.MockDataSeeder.seedTodayRoute(profile.id, profile.name) { _, msg -> android.widget.Toast.makeText(appContext, msg, android.widget.Toast.LENGTH_LONG).show() }
                },
                seedTestDataLabel = "Load today's route (mock)",
                onSignOut = {
                    com.wasteroute.app.firebase.HouseholdRepository.stopListening()
                    onSignOut()
                },
                onDismiss = { showProfileSheet = false }
            )
        }
    }
}
