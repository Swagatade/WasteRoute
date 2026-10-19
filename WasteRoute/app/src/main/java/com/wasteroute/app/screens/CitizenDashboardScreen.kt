package com.wasteroute.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.wasteroute.app.firebase.FirebaseDataRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wasteroute.app.components.ActivityTimeline
import com.wasteroute.app.components.BentoQuickActions
import com.wasteroute.app.components.CitizenNavTab
import com.wasteroute.app.components.CityCleanlinessHero
import com.wasteroute.app.components.CollectionVerificationCard
import com.wasteroute.app.components.ComplaintJourneyCard
import com.wasteroute.app.components.FloatingReportCard
import com.wasteroute.app.components.ImpactMetrics
import com.wasteroute.app.components.NearbyBinMapCard
import com.wasteroute.app.components.NeighborhoodChart
import com.wasteroute.app.components.NotificationPreview
import androidx.compose.material3.ExperimentalMaterial3Api
import com.wasteroute.app.components.PremiumBottomNavigation
import com.wasteroute.app.components.PremiumCitizenHeader
import com.wasteroute.app.model.UiPerformanceMode
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wasteroute.app.components.ReportGrievanceBottomSheet
import com.wasteroute.app.theme.Emerald400
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Suppress("FunctionName")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDashboardScreen(
    modifier: Modifier = Modifier,
    userName: String = "Citizen",
    location: String = "SRM KTR, Kattankulathur",
    onNavigateToReportWaste: () -> Unit = {
        // Placeholder for future ReportWasteScreen navigation
    },
    onNavigateToComplaints: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onScanQr: () -> Unit = {},
    onSignOut: () -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var selectedTab by remember { mutableStateOf(CitizenNavTab.HOME) }
    var showProfileSheet by remember { mutableStateOf(false) }

    // Live Grievance Camera & Ticket State (SRM KTR Campus)
    val liveComplaints by FirebaseDataRepository.complaints.collectAsState()
    var capturedGrievancePhoto by remember { mutableStateOf<Bitmap?>(null) }
    var showReportGrievanceSheet by remember { mutableStateOf(false) }
    var showCampusMapDialog by remember { mutableStateOf(false) }
    // "See my block on the map": centre on the warden's block and show the bin nearest to it
    var focusMyBlock by remember { mutableStateOf(false) }
    var mapBin by remember { mutableStateOf<com.wasteroute.app.model.CampusSmartBin?>(null) }
    // Worker QR scan -> confirms the doorstep collection in Firestore
    var scanResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val qrScanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract()
    ) { result ->
        val text = result.contents
        if (text != null) {
            com.wasteroute.app.firebase.CollectionRepository.confirmCollection(text, userName) { ok, msg ->
                scanResult = ok to msg
            }
        }
    }
    val launchQrScan = {
        onScanQr()
        qrScanLauncher.launch(
            com.journeyapps.barcodescanner.ScanOptions()
                .setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                .setPrompt("Scan the worker's QR to confirm collection")
                .setBeepEnabled(false)
                .setOrientationLocked(true)
                .setCaptureActivity(PortraitCaptureActivity::class.java)
        )
    }
    var currentComplaintId by remember { mutableStateOf("#WR-KTR-8092") }
    var currentComplaintTitle by remember { mutableStateOf("Overflowing Bin") }
    var currentComplaintLocation by remember { mutableStateOf("Java Green Food Street, SRM KTR") }
    var currentComplaintTimestamp by remember { mutableStateOf("15 Mins ago") }
    var complaintSubmissionNotice by remember { mutableStateOf<String?>(null) }

    // Only the citizen's own reports are shown (assigned worker, truck, live progress, proof photo)
    val myUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val myComplaints = liveComplaints.filter { myUid.isNotBlank() && it.reporterUid == myUid }
    var trackingTaskId by remember { mutableStateOf<String?>(null) }

    // Only this resident's own home (private: other homes are never loaded on a citizen phone)
    val myHomes by com.wasteroute.app.firebase.HouseholdRepository.mine.collectAsState()
    LaunchedEffect(myUid) { com.wasteroute.app.firebase.HouseholdRepository.listenMine(myUid) }
    var showHistory by remember { mutableStateOf(false) }

    // The "ticket lodged" banner only flashes for a second
    LaunchedEffect(complaintSubmissionNotice) {
        if (complaintSubmissionNotice != null) {
            delay(1000)
            complaintSubmissionNotice = null
        }
    }

    // Map: only my reports + the workers being called / driving for them
    val allWorkers by FirebaseDataRepository.workers.collectAsState()
    val activeMine = myComplaints.filter { it.stage != com.wasteroute.app.firebase.TaskStage.RESOLVED }
    val mapWorkers = run {
        val ids = activeMine.flatMap { t ->
            if (t.assignedWorkerId.isNullOrBlank()) t.notifiedWorkerIds else listOf(t.assignedWorkerId!!)
        }.map { it.uppercase() }.toSet()
        allWorkers.filter { it.id.uppercase() in ids }.map { w ->
            val task = activeMine.firstOrNull { it.assignedWorkerId.equals(w.id, ignoreCase = true) }
            if (task != null) {
                w.copy(
                    status = com.wasteroute.app.components.stageTitle(task.stage),
                    lat = task.truckLat ?: w.lat,
                    lng = task.truckLng ?: w.lng
                )
            } else w.copy(status = "Being called")
        }
    }

    // Notify the citizen whenever one of their reports moves to the next stage
    val lastStages = remember { mutableMapOf<String, String>() }
    LaunchedEffect(myComplaints.map { it.id to it.stage }) {
        myComplaints.forEach { c ->
            val prev = lastStages[c.id]
            if (prev != null && prev != c.stage) {
                com.wasteroute.app.notify.TaskAlarm.notifyInfo(
                    context, c.id, "${c.ticketNo}: ${com.wasteroute.app.components.stageTitle(c.stage)}",
                    "${c.category} at ${c.location}"
                )
            }
            lastStages[c.id] = c.stage
        }
    }

    // Live location for the report: starts when the citizen taps the garbage picture and keeps updating
    // (Google location API) while the report sheet is open; the address comes from reverse geocoding.
    val scope = rememberCoroutineScope()
    var reportLat by remember { mutableStateOf<Double?>(null) }
    var reportLng by remember { mutableStateOf<Double?>(null) }
    var reportAddress by remember { mutableStateOf<String?>(null) }
    var trackLocation by remember { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(com.wasteroute.app.location.LocationHelper.hasPermission(context)) }
    var locationRetry by remember { mutableStateOf(0) }
    var lastGeocode by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val applyLocation: (Double, Double) -> Unit = { lat, lng ->
        reportLat = lat
        reportLng = lng
        val last = lastGeocode
        if (last == null || com.wasteroute.app.location.LocationHelper.distanceM(last.first, last.second, lat, lng) > 40) {
            lastGeocode = lat to lng
            scope.launch {
                reportAddress = com.wasteroute.app.location.LocationHelper.address(context, lat, lng)
                    ?: String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)
            }
        }
    }
    DisposableEffect(trackLocation, locationGranted, locationRetry) {
        if (!trackLocation || !locationGranted) return@DisposableEffect onDispose { }
        val first = scope.launch {
            com.wasteroute.app.location.LocationHelper.currentLocation(context)?.let { applyLocation(it.first, it.second) }
        }
        val stop = com.wasteroute.app.location.LocationHelper.startUpdates(context, 3000L) { lat, lng -> applyLocation(lat, lng) }
        onDispose {
            first.cancel()
            stop()
        }
    }

    // Full-resolution camera photo of the garbage
    val launchCamera = com.wasteroute.app.util.rememberPhotoCapture(
        onCancelled = { trackLocation = false }
    ) { bitmap ->
        capturedGrievancePhoto = bitmap
        showReportGrievanceSheet = true
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationGranted = com.wasteroute.app.location.LocationHelper.hasPermission(context)
        locationRetry++
        if (showReportGrievanceSheet) Unit else launchCamera()
    }

    val openCameraAndReport = {
        reportLat = null
        reportLng = null
        reportAddress = null
        lastGeocode = null
        trackLocation = true
        if (com.wasteroute.app.location.LocationHelper.hasPermission(context)) {
            locationGranted = true
            launchCamera()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }


    // Staggered Entrance Animations State
    var headerVisible by remember { mutableStateOf(false) }
    var heroVisible by remember { mutableStateOf(false) }
    var reportVisible by remember { mutableStateOf(false) }
    var complaintVisible by remember { mutableStateOf(false) }
    var actionsVisible by remember { mutableStateOf(false) }
    var mapVisible by remember { mutableStateOf(false) }
    var restVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        headerVisible = true
        delay(100.milliseconds)
        heroVisible = true
        delay(200.milliseconds)
        reportVisible = true
        delay(150.milliseconds)
        complaintVisible = true
        delay(150.milliseconds)
        actionsVisible = true
        delay(200.milliseconds)
        mapVisible = true
        restVisible = true
    }

    // Atmospheric Background
    val backgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF050708), // Near-black AMOLED
                Color(0xFF090D16),
                Color(0xFF0B132B),
                Color(0xFF050708)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF8FAFC), // Slate 50
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0),
                Color(0xFFF8FAFC)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.TopCenter
    ) {
        // Main Vertically Scrollable Content
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 120.dp),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Immersive Smart Header
            item(key = "header") {
                AnimatedVisibility(
                    visible = headerVisible,
                    enter = fadeIn(tween(350))
                ) {
                    PremiumCitizenHeader(
                        userName = userName,
                        location = location,
                        hasUnreadNotifications = false,
                        onNotificationClick = onNavigateToNotifications,
                        onProfileClick = {
                            showProfileSheet = true
                            onNavigateToProfile()
                        }
                    )
                }
            }

            // 3. Floating Report Waste Action (Overlapping Hero)
            item(key = "floating_report") {
                AnimatedVisibility(
                    visible = reportVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        initialOffsetY = { 20 },
                        animationSpec = tween(400)
                    )
                ) {
                    FloatingReportCard(
                        onReportClick = openCameraAndReport
                    )
                }
            }

            item(key = "spacer_complaint") {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 4. Active Live Complaint Journey (with live dynamic complaint details)
            item(key = "complaint") {
                AnimatedVisibility(
                    visible = complaintVisible,
                    enter = fadeIn(tween(450)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(450)
                    )
                ) {
                    // The citizen's own reports only; nothing is shown until they report something.
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        activeMine.take(3).forEach { task ->
                            com.wasteroute.app.components.CitizenTaskTrackerCard(
                                task = task,
                                onTrackTruck = { trackingTaskId = task.id }
                            )
                        }
                    }
                }
            }

            item(key = "spacer_actions") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 5. Smart Quick Actions (Asymmetric Bento Grid)
            item(key = "bento_actions") {
                AnimatedVisibility(
                    visible = actionsVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(500)
                    )
                ) {
                    BentoQuickActions(
                        onMyComplaintsClick = {
                            showHistory = true
                            onNavigateToComplaints()
                        },
                        onNearbyBinsClick = {
                            showCampusMapDialog = true
                            onNavigateToMap()
                        },
                        onScanQrClick = launchQrScan,
                        onCityUpdatesClick = onNavigateToNotifications,
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_map") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 6. Nearby Waste Bin Map Card Preview
            item(key = "nearby_map") {
                AnimatedVisibility(
                    visible = mapVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(500)
                    )
                ) {
                    NearbyBinMapCard(
                        onOpenMapClick = { bin ->
                            mapBin = bin
                            showCampusMapDialog = true
                            onNavigateToMap()
                        },
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "spacer_my_home") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 6b. My home: collected-today status + link-my-home by code
            item(key = "my_home") {
                AnimatedVisibility(
                    visible = mapVisible,
                    enter = fadeIn(tween(500))
                ) {
                    com.wasteroute.app.components.MyHomeCard(
                        homes = myHomes,
                        onLinkHome = { code, done ->
                            com.wasteroute.app.firebase.HouseholdRepository.claimHome(code, userName, done)
                        },
                        onShowOnMap = {
                            mapBin = null
                            focusMyBlock = true
                            showCampusMapDialog = true
                            onNavigateToMap()
                        }
                    )
                }
            }

            item(key = "spacer_collection") {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 7. Household Collection Verification Card
            item(key = "household_qr") {
                AnimatedVisibility(
                    visible = restVisible,
                    enter = fadeIn(tween(500))
                ) {
                    CollectionVerificationCard(
                        onScanWorkerQrClick = launchQrScan,
                        performanceMode = performanceMode
                    )
                }
            }

            item(key = "bottom_padding") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 12. Floating Bottom Navigation (Anchored to Screen Bottom)
        PremiumBottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                selectedTab = tab
                when (tab) {
                    CitizenNavTab.HOME -> {}
                    CitizenNavTab.COMPLAINTS -> {
                        showHistory = true
                        onNavigateToComplaints()
                    }
                    CitizenNavTab.MAP -> {
                        showCampusMapDialog = true
                        onNavigateToMap()
                    }
                    CitizenNavTab.PROFILE -> {
                        showProfileSheet = true
                        onNavigateToProfile()
                    }
                }
            },
            onReportClick = openCameraAndReport,
            performanceMode = performanceMode,
            modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 640.dp)
        )

        // 13. Interactive OpenStreetMap Campus Dialog (SRM KTR Campus & Smart Bins)
        scanResult?.let { (ok, msg) ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { scanResult = null },
                title = { androidx.compose.material3.Text(if (ok) "Collection confirmed" else "Could not confirm") },
                text = { androidx.compose.material3.Text(msg) },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { scanResult = null }) {
                        androidx.compose.material3.Text("OK")
                    }
                }
            )
        }

        if (showCampusMapDialog) {
            com.wasteroute.app.components.CampusOpenStreetMapDialog(
                initialSelectedBin = mapBin?.takeIf { it.id != "NONE" },
                complaints = activeMine,
                workers = mapWorkers,
                households = myHomes,
                focusHome = if (focusMyBlock) myHomes.firstOrNull() else null,
                onDismiss = { showCampusMapDialog = false; focusMyBlock = false }
            )
        }

        // 14. Grievance Reporting Modal Sheet (With Camera Live Photo & Category Picker)
        if (showReportGrievanceSheet) {
            ReportGrievanceBottomSheet(
                photoBitmap = capturedGrievancePhoto,
                initialLocation = reportAddress ?: location,
                isLocating = trackLocation && reportLat == null,
                coordsLabel = if (reportLat != null && reportLng != null) {
                    String.format(java.util.Locale.US, "%.5f, %.5f", reportLat, reportLng)
                } else null,
                onRetryLocation = {
                    if (com.wasteroute.app.location.LocationHelper.hasPermission(context)) {
                        locationGranted = true
                        locationRetry++
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                },
                onUseCampusLocation = {
                    applyLocation(com.wasteroute.app.model.CampusMapConfig.CAMPUS_LAT, com.wasteroute.app.model.CampusMapConfig.CAMPUS_LNG)
                },
                onRetakePhoto = {
                    openCameraAndReport()
                },
                onSubmit = { category, loc, desc ->
                    val randomTicket = "#WR-${(1000..9999).random()}"
                    currentComplaintId = randomTicket
                    currentComplaintTitle = category
                    currentComplaintLocation = loc
                    currentComplaintTimestamp = "Just now"
                    complaintSubmissionNotice = "Ticket $randomTicket lodged with camera photo! Dispatch notified."
                    showReportGrievanceSheet = false
                    trackLocation = false

                    // Save directly to shared Firestore
                    FirebaseDataRepository.lodgeComplaint(
                        ticketNo = randomTicket,
                        category = category,
                        location = loc,
                        description = desc,
                        citizenName = userName,
                        lat = reportLat ?: 12.8225,
                        lng = reportLng ?: 80.0438,
                        photoDataUri = capturedGrievancePhoto?.let { com.wasteroute.app.util.ImageUtil.toDataUri(it) } ?: "",
                        reporterUid = myUid,
                        citizenEmail = com.wasteroute.app.model.CitizenAuthSession.currentUser?.email ?: ""
                    )
                },
                onDismiss = {
                    showReportGrievanceSheet = false
                    trackLocation = false
                }
            )
        }

        if (showHistory) {
            com.wasteroute.app.components.ComplaintHistoryDialog(
                title = "My complaints",
                subtitle = "Every garbage report you sent",
                tasks = myComplaints,
                isWorker = false,
                viewerId = myUid,
                onRemove = { list ->
                    com.wasteroute.app.firebase.DispatchRepository.hideFromHistory(list.map { it.id }, myUid)
                },
                onTrack = { trackingTaskId = it.id },
                onDismiss = {
                    showHistory = false
                    selectedTab = CitizenNavTab.HOME
                }
            )
        }

        // Live truck map for one of the citizen's reports
        myComplaints.firstOrNull { it.id == trackingTaskId }?.let { task ->
            com.wasteroute.app.components.TaskTrackingMapDialog(
                task = task,
                onDismiss = { trackingTaskId = null }
            )
        }

        // 14. Submission Success Banner
        if (complaintSubmissionNotice != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE6064E3B))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = complaintSubmissionNotice ?: "",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 15. Profile & Account Settings Sheet with Sign Out
        if (showProfileSheet) {
            com.wasteroute.app.components.AccountProfileBottomSheet(
                userName = userName,
                userEmail = com.wasteroute.app.model.CitizenAuthSession.currentUser?.email,
                userRole = com.wasteroute.app.model.UserRole.CITIZEN,
                onSeedTestData = {
                    com.wasteroute.app.firebase.MockDataSeeder.seedBins { _, msg -> android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show() }
                },
                onSignOut = {
                    com.wasteroute.app.firebase.HouseholdRepository.stopListening()
                    onSignOut()
                },
                onDismiss = { showProfileSheet = false }
            )
        }
    }
}
