package com.westeroute.app.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.westeroute.app.model.CampusMapConfig
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TaskLocationCard(
    location: String,
    destinationLat: Double,
    destinationLng: Double,
    /** Pin shown on the map for the destination (the real reported / scheduled spot). */
    spot: com.westeroute.app.firebase.LiveComplaint? = null,
    onNavigateClick: () -> Unit = {},
    onViewLocationClick: () -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Real GPS position of the worker (no fixed/campus position)
    var me by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.westeroute.app.location.LocationHelper.currentLocation(ctx)?.let { me = it }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        val stop = com.westeroute.app.location.LocationHelper.startUpdates(ctx, 5000L) { la, ln -> me = la to ln }
        onDispose { stop() }
    }
    val liveMeters = me?.let { com.westeroute.app.location.LocationHelper.distanceM(it.first, it.second, destinationLat, destinationLng) }
    val distance = liveMeters?.let { com.westeroute.app.location.LocationHelper.formatDistance(it) + " away" } ?: "GPS not available"
    val estimatedTravel = liveMeters?.let { "${etaMinutes(it)} min" } ?: "--"
    var webRef by remember { mutableStateOf<WebView?>(null) }
    androidx.compose.runtime.LaunchedEffect(me, webRef) {
        val p = me ?: return@LaunchedEffect
        kotlinx.coroutines.delay(700)
        webRef?.evaluateJavascript("if(window.setUserLocation){window.setUserLocation(${p.first},${p.second});}", null)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "TASK LOCATION",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 12.dp else 2.dp,
                    shape = shape,
                    ambientColor = if (isDark) Color(0x99000000) else Color(0x33000000),
                    spotColor = if (isDark) Color(0x66000000) else Color(0x333B82F6)
                )
                .clip(shape)
                .background(if (isDark) Color(0xF00F172A) else Color(0xF5FFFFFF))
                .border(
                    width = 1.2.dp,
                    color = if (isDark) Color(0x3338BDF8) else Color(0x40CBD5E1),
                    shape = shape
                )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Real OpenStreetMap WebView with Route
                val hasFix = me != null
                val cachedTaskHtml = remember(destinationLat, destinationLng, isDark, hasFix) {
                    CampusMapConfig.buildOpenStreetMapHtml(
                        centerLat = me?.let { (it.first + destinationLat) / 2.0 } ?: destinationLat,
                        centerLng = me?.let { (it.second + destinationLng) / 2.0 } ?: destinationLng,
                        zoom = 16,
                        userLat = me?.first ?: destinationLat,
                        userLng = me?.second ?: destinationLng,
                        destinationLat = if (hasFix) destinationLat else null,
                        destinationLng = if (hasFix) destinationLng else null,
                        destinationLabel = location,
                        showUserMarker = hasFix,
                        complaints = listOfNotNull(spot),
                        isDark = isDark
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .background(if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9))
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

                                tag = cachedTaskHtml
                                loadDataWithBaseURL("https://cdnjs.cloudflare.com/", cachedTaskHtml, "text/html", "UTF-8", null)
                                webRef = this
                            }
                        },
                        update = { webView ->
                            webRef = webView
                            if (webView.tag != cachedTaskHtml) {
                                webView.tag = cachedTaskHtml
                                webView.loadDataWithBaseURL("https://cdnjs.cloudflare.com/", cachedTaskHtml, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Floating Map Badge (ETA & Distance)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xD90F172A) else Color(0xE6FFFFFF))
                            .border(
                                1.dp,
                                if (isDark) Color(0x3338BDF8) else Color(0x330284C7),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Emerald400)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (liveMeters != null) "LIVE ROUTE • $estimatedTravel ($distance)" else "Waiting for GPS...",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Cyan400 else Color(0xFF0284C7)
                            )
                        }
                    }
                }

                // Location Details & Navigation Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0x2638BDF8) else Color(0x1F0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Place,
                                contentDescription = null,
                                tint = if (isDark) Cyan400 else Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = location,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            )
                            Text(
                                text = if (liveMeters != null) "$distance • Estimated $estimatedTravel transit" else "Turn on location to see the distance",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewLocationClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0x4D94A3B8) else Color(0x40CBD5E1)
                            )
                        ) {
                            Text(
                                text = "View Details",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
                            )
                        }

                        Button(
                            onClick = { openNavigationTo(ctx, destinationLat, destinationLng) },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Navigation,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Start Navigation",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
