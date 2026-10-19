package com.westeroute.app.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.material.icons.outlined.MyLocation
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.westeroute.app.firebase.FirebaseDataRepository
import com.westeroute.app.model.CampusMapConfig
import com.westeroute.app.model.CampusSmartBin
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

data class CampusPoi(
    val name: String,
    val lat: Double,
    val lng: Double,
    val zoom: Int = 18
)

private val CAMPUS_POIS = listOf(
    CampusPoi("Campus Center", 12.8230, 80.0440, 16),
    CampusPoi("UB Building", 12.8232, 80.0445, 18),
    CampusPoi("Tech Park Tower", 12.8248, 80.0456, 18),
    CampusPoi("Java Green Food St", 12.8225, 80.0438, 18),
    CampusPoi("Paari / Kaari Hostels", 12.8210, 80.0415, 18),
    CampusPoi("Main Gate 1 (GST Rd)", 12.8258, 80.0470, 18),
    CampusPoi("Medical Hospital", 12.8202, 80.0485, 18)
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CampusOpenStreetMapDialog(
    initialSelectedBin: CampusSmartBin? = null,
    /** Complaint pins to show (null = every complaint). Citizens/workers pass only their own. */
    complaints: List<com.westeroute.app.firebase.LiveComplaint>? = null,
    /** Truck / called-worker markers to show (null = every worker). */
    workers: List<com.westeroute.app.firebase.LiveWorker>? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Realtime synced telemetry from Firestore
    val liveBins by FirebaseDataRepository.smartBins.collectAsState()
    val liveComplaints by FirebaseDataRepository.complaints.collectAsState()
    val liveWorkers by FirebaseDataRepository.workers.collectAsState()

    val shownComplaints = complaints ?: liveComplaints
    val shownWorkers = workers ?: liveWorkers

    // A bin picked by card or map marker; otherwise the one passed in, else the nearest to the live location
    var pickedBin by remember { mutableStateOf(initialSelectedBin) }
    var activeFilter by remember { mutableStateOf("all") } // all | bins | complaints | trucks
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var userLoc by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val selectedBin = pickedBin?.let { p -> liveBins.firstOrNull { it.code == p.code } ?: p }
        ?: userLoc?.let { u -> liveBins.minByOrNull { distanceMeters(u.first, u.second, it.lat, it.lng) } }
        ?: liveBins.firstOrNull() ?: CampusMapConfig.EMPTY_BIN
    val liveBinsNow by rememberUpdatedState(liveBins)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Real device location (GPS + network), no mock coordinates
    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose { }
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { loc -> userLoc = loc.latitude to loc.longitude }
        try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
                if (lm.isProviderEnabled(provider)) {
                    lm.getLastKnownLocation(provider)?.let { if (userLoc == null) userLoc = it.latitude to it.longitude }
                    lm.requestLocationUpdates(provider, 3000L, 5f, listener, Looper.getMainLooper())
                }
            }
        } catch (_: SecurityException) {
        }
        onDispose { lm.removeUpdates(listener) }
    }


    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { win ->
                WindowCompat.setDecorFitsSystemWindows(win, false)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SRM KTR Campus Map",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3310B981))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FIRESTORE SYNC",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emerald400
                                )
                            }
                        }
                        Text(
                            text = "${liveBins.size} Bin Locations",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x33334155) else Color(0x1A0F172A))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Campus POI Navigation Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CAMPUS_POIS) { poi ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x26334155) else Color(0x1A0F172A))
                                .border(
                                    1.dp,
                                    if (isDark) Color(0x33475569) else Color(0x2094A3B8),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    webViewRef?.loadUrl(
                                        "javascript:if(window.campusMap){ window.campusMap.flyTo([${poi.lat}, ${poi.lng}], ${poi.zoom}); }"
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = Cyan400,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = poi.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive OpenStreetMap Leaflet View
                // Rebuilt only when pins appear/change status; truck positions move through JS (below)
                val cachedDialogHtml = remember(
                    liveBins,
                    shownComplaints.map { it.id to it.status },
                    shownWorkers.map { it.id to it.status },
                    isDark
                ) {
                    CampusMapConfig.buildOpenStreetMapHtml(
                        complaints = shownComplaints,
                        workers = shownWorkers,
                        centerLat = selectedBin.lat,
                        centerLng = selectedBin.lng,
                        zoom = 17,
                        userLat = userLoc?.first ?: CampusMapConfig.CAMPUS_LAT,
                        userLng = userLoc?.second ?: CampusMapConfig.CAMPUS_LNG,
                        showUserMarker = true,
                        routeToBin = selectedBin,
                        bins = liveBins,
                        isDark = isDark
                    )
                }

                // Push every new real GPS fix into the page
                LaunchedEffect(userLoc, webViewRef, cachedDialogHtml) {
                    val loc = userLoc ?: return@LaunchedEffect
                    delay(400)
                    webViewRef?.pushUserLocation(loc.first, loc.second)
                }

                // Picking a bin (card or marker) moves the map and draws my location -> that bin
                LaunchedEffect(selectedBin.code, webViewRef, cachedDialogHtml) {
                    if (selectedBin.id == "NONE") return@LaunchedEffect
                    delay(500)
                    webViewRef?.focusBinOnMap(selectedBin.lat, selectedBin.lng)
                }

                // Move truck markers live as workers drive
                LaunchedEffect(shownWorkers.map { it.lat to it.lng }, webViewRef) {
                    delay(700)
                    shownWorkers.forEachIndexed { i, w ->
                        webViewRef?.evaluateJavascript("if(window.moveWorker){window.moveWorker($i,${w.lat},${w.lng});}", null)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.5.dp, if (isDark) Color(0x33475569) else Color(0x33CBD5E1), RoundedCornerShape(22.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                addJavascriptInterface(
                                    MapBinBridge { code -> liveBinsNow.firstOrNull { it.code == code }?.let { pickedBin = it } },
                                    "WRBridge"
                                )

                                tag = cachedDialogHtml
                                loadDataWithBaseURL("https://cdnjs.cloudflare.com/", cachedDialogHtml, "text/html", "UTF-8", null)
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                            if (webView.tag != cachedDialogHtml) {
                                webView.tag = cachedDialogHtml
                                webView.loadDataWithBaseURL("https://cdnjs.cloudflare.com/", cachedDialogHtml, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Live location status chip
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xCC0F172A) else Color(0xCCFFFFFF))
                            .clickable {
                                if (!hasLocationPermission) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = null,
                                tint = if (userLoc != null) Emerald400 else Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    userLoc != null -> "Live device location"
                                    !hasLocationPermission -> "Tap to allow location"
                                    else -> "Finding GPS..."
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }

                    // Bottom info panel overlaid on the map so it can never be pushed off-screen
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 28.dp)
                    ) {
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(liveBins, key = { it.id }) { bin ->
                                val selected = bin.id == selectedBin.id
                                Column(
                                    modifier = Modifier
                                        .width(150.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isDark) Color(0xF01E293B) else Color(0xF0FFFFFF))
                                        .border(
                                            if (selected) 1.5.dp else 1.dp,
                                            if (selected) Cyan400 else (if (isDark) Color(0x3364748B) else Color(0x3394A3B8)),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { pickedBin = bin }
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = bin.code,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                    Text(
                                        text = bin.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = userLoc?.let { formatDistance(distanceMeters(it.first, it.second, bin.lat, bin.lng)) }
                                            ?: bin.distanceDisplay,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Cyan400
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Selected Bin Detail Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isDark) Color(0xF21E293B) else Color(0xF2FFFFFF))
                                .border(1.dp, if (isDark) Color(0x3364748B) else Color(0x2094A3B8), RoundedCornerShape(18.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = selectedBin.code,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                                )
                                            )
                                        }
                                        Text(
                                            text = selectedBin.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                            )
                                        )
                                    }

                                    // Launch Google Maps External Navigation (from the real device location)
                                    Button(
                                        onClick = {
                                            val gmmIntentUri = Uri.parse("google.navigation:q=${selectedBin.lat},${selectedBin.lng}")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                                setPackage("com.google.android.apps.maps")
                                            }
                                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(mapIntent)
                                            } else {
                                                val browserIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${selectedBin.lat},${selectedBin.lng}")
                                                )
                                                context.startActivity(browserIntent)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Navigation,
                                                contentDescription = null,
                                                tint = Color(0xFF0F172A),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Route",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Zone: ${selectedBin.zone}",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = userLoc?.let { "${formatDistance(distanceMeters(it.first, it.second, selectedBin.lat, selectedBin.lng))} away" }
                                            ?: "Distance: ${selectedBin.distanceDisplay}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Cyan400
                                    )
                                }
                                Text(
                                    text = "Type: ${selectedBin.type}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDark) Emerald400 else Color(0xFF059669)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val out = FloatArray(1)
    android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, out)
    return out[0].toDouble()
}

private fun formatDistance(m: Double): String =
    if (m < 1000) "${m.toInt()} m" else String.format(java.util.Locale.US, "%.1f km", m / 1000.0)
