package com.westeroute.app.components

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.westeroute.app.location.LocationHelper
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.westeroute.app.firebase.FirebaseDataRepository
import com.westeroute.app.model.CampusMapConfig
import com.westeroute.app.model.CampusSmartBin
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NearbyBinMapCard(
    onOpenMapClick: (CampusSmartBin) -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    // Realtime synced bins & complaints from Firestore
    val liveBins by FirebaseDataRepository.smartBins.collectAsState()
    val liveComplaints by FirebaseDataRepository.complaints.collectAsState()
    val liveWorkers by FirebaseDataRepository.workers.collectAsState()

    // Real device location: always shown on the map, and the nearest bin is routed to by default
    val ctx = LocalContext.current
    var userLoc by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var hasLoc by remember { mutableStateOf(LocationHelper.hasPermission(ctx)) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLoc = LocationHelper.hasPermission(ctx)
    }
    LaunchedEffect(Unit) {
        if (!hasLoc) permLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
    LaunchedEffect(hasLoc) {
        if (hasLoc) LocationHelper.currentLocation(ctx)?.let { userLoc = it }
    }
    DisposableEffect(hasLoc) {
        if (!hasLoc) return@DisposableEffect onDispose { }
        val stop = LocationHelper.startUpdates(ctx, 5000L) { lat, lng -> userLoc = lat to lng }
        onDispose { stop() }
    }

    // A bin the user picked (card or map marker); otherwise the nearest one to the live location
    var pickedCode by remember { mutableStateOf<String?>(null) }
    val nearestBin = userLoc?.let { u ->
        liveBins.minByOrNull { LocationHelper.distanceM(u.first, u.second, it.lat, it.lng) }
    }
    val selectedBin = liveBins.firstOrNull { it.code == pickedCode }
        ?: nearestBin ?: liveBins.firstOrNull() ?: CampusMapConfig.EMPTY_BIN
    val selectedDistance = userLoc?.let { LocationHelper.formatDistance(LocationHelper.distanceM(it.first, it.second, selectedBin.lat, selectedBin.lng)) }
        ?: selectedBin.distanceDisplay
    var webRef by remember { mutableStateOf<WebView?>(null) }
    val liveBinsNow by rememberUpdatedState(liveBins)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEARBY WASTE INFRASTRUCTURE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    color = if (isDark) Cyan400 else Color(0xFF0284C7)
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Emerald400)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "SRM KTR CAMPUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Real Interactive OpenStreetMap Leaflet Container
        // Rebuilt only when bins change or the first GPS fix arrives; later GPS fixes and bin picks go through JS
        val hasFix = userLoc != null
        val cachedHtml = remember(liveBins, isDark, hasFix) {
            val u = userLoc
            CampusMapConfig.buildOpenStreetMapHtml(
                centerLat = u?.let { (it.first + selectedBin.lat) / 2.0 } ?: selectedBin.lat,
                centerLng = u?.let { (it.second + selectedBin.lng) / 2.0 } ?: selectedBin.lng,
                zoom = 16,
                userLat = u?.first ?: CampusMapConfig.CAMPUS_LAT,
                userLng = u?.second ?: CampusMapConfig.CAMPUS_LNG,
                showUserMarker = true,
                routeToBin = if (u != null && selectedBin.id != "NONE") selectedBin else null,
                bins = liveBins,
                isDark = isDark
            )
        }

        LaunchedEffect(userLoc, webRef, cachedHtml) {
            val p = userLoc ?: return@LaunchedEffect
            delay(400)
            webRef?.pushUserLocation(p.first, p.second)
        }
        LaunchedEffect(selectedBin.code, webRef, cachedHtml) {
            if (selectedBin.id == "NONE") return@LaunchedEffect
            delay(400)
            webRef?.focusBinOnMap(selectedBin.lat, selectedBin.lng)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 10.dp else 2.dp,
                    shape = shape,
                    ambientColor = Color(0x33000000),
                    spotColor = Color(0x33000000)
                )
                .clip(shape)
                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isDark) Color(0x4D34D399) else Color(0x66059669),
                            if (isDark) Color(0x4D38BDF8) else Color(0x660284C7)
                        )
                    ),
                    shape = shape
                )
        ) {
            // Embedded Real OpenStreetMap WebView
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
                            MapBinBridge { code -> pickedCode = liveBinsNow.firstOrNull { it.code == code }?.code },
                            "WRBridge"
                        )

                        tag = cachedHtml
                        loadDataWithBaseURL("https://cdnjs.cloudflare.com/", cachedHtml, "text/html", "UTF-8", null)
                        webRef = this
                    }
                },
                update = { webView ->
                    webRef = webView
                    if (webView.tag != cachedHtml) {
                        webView.tag = cachedHtml
                        webView.loadDataWithBaseURL("https://cdnjs.cloudflare.com/", cachedHtml, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Floating Overlay Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xD90F172A) else Color(0xD9FFFFFF))
                        .border(
                            1.dp,
                            if (isDark) Color(0x33475569) else Color(0x33CBD5E1),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BIN LOCATIONS",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Emerald400 else Color(0xFF059669)
                        )
                    }
                }

                // Expand Fullscreen Map Action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF059669))
                            )
                        )
                        .clickable(
                            role = Role.Button,
                            onClick = { onOpenMapClick(selectedBin) }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Fullscreen,
                            contentDescription = "Expand Fullscreen Map",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Full Map",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Selected Bin Detail Card
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0xF20F172A) else Color(0xF2FFFFFF))
                    .border(
                        1.dp,
                        if (isDark) Color(0x4D34D399) else Color(0x4D059669),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedBin.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                ),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${selectedBin.code} • $selectedDistance",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Open Full Map & Directions
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0x3338BDF8) else Color(0x260284C7))
                            .clickable(
                                role = Role.Button,
                                onClick = { onOpenMapClick(selectedBin) }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Explore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Cyan400 else Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isDark) Cyan400 else Color(0xFF0284C7),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
