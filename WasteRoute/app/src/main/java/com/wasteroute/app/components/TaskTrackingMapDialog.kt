package com.wasteroute.app.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.wasteroute.app.firebase.LiveComplaint
import com.wasteroute.app.firebase.LiveWorker
import com.wasteroute.app.location.LocationHelper
import com.wasteroute.app.model.CampusMapConfig
import com.wasteroute.app.theme.Cyan400
import com.wasteroute.app.theme.Emerald400
import kotlinx.coroutines.delay

/**
 * Full-screen map showing the reported issue and the assigned truck moving towards it in real time.
 * [task] must be a live value (re-collected from Firestore) so the truck marker follows the updates.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TaskTrackingMapDialog(
    task: LiveComplaint,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val truckLat = task.truckLat
    val truckLng = task.truckLng

    // Built once per task; the truck moves through JS calls below, so the page never reloads.
    val html = remember(task.id, isDark) {
        val trucks = if (truckLat != null && truckLng != null) {
            listOf(LiveWorker(id = task.assignedWorkerId ?: "", name = task.assignedWorkerName ?: "Worker", vehicle = task.truckName.ifBlank { "Truck" }, status = "On Route", lat = truckLat, lng = truckLng))
        } else emptyList()
        CampusMapConfig.buildOpenStreetMapHtml(
            centerLat = task.lat,
            centerLng = task.lng,
            zoom = 16,
            userLat = truckLat ?: task.lat,
            userLng = truckLng ?: task.lng,
            showUserMarker = false,
            destinationLat = task.lat,
            destinationLng = task.lng,
            bins = emptyList(),
            complaints = listOf(task),
            workers = trucks,
            isDark = isDark
        )
    }

    LaunchedEffect(truckLat, truckLng, webViewRef) {
        val lat = truckLat ?: return@LaunchedEffect
        val lng = truckLng ?: return@LaunchedEffect
        delay(700) // let Leaflet mount on first open
        webViewRef?.evaluateJavascript(
            "if(window.moveWorker){window.moveWorker(0,$lat,$lng);} if(window.setUserLocation){window.setUserLocation($lat,$lng);}",
            null
        )
    }

    val distance = if (truckLat != null && truckLng != null) {
        LocationHelper.distanceM(truckLat, truckLng, task.lat, task.lng)
    } else null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { dialogWindow?.let { WindowCompat.setDecorFitsSystemWindows(it, false) } }

        Surface(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Live truck tracking",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            "${task.ticketNo} - ${task.location}",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(if (isDark) Color(0x33334155) else Color(0x1A0F172A))
                    ) {
                        Icon(Icons.Filled.Close, "Close", tint = if (isDark) Color.White else Color(0xFF0F172A))
                    }
                }

                Box(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).clip(RoundedCornerShape(22.dp))) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 WasteRouteApp/1.0"
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                tag = html
                                loadDataWithBaseURL("https://cdnjs.cloudflare.com/", html, "text/html", "UTF-8", null)
                                webViewRef = this
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                            if (webView.tag != html) {
                                webView.tag = html
                                webView.loadDataWithBaseURL("https://cdnjs.cloudflare.com/", html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when {
                            task.stage == "resolved" -> "Issue solved"
                            distance == null -> "Waiting for the truck to start"
                            distance <= 40 -> "Truck has arrived"
                            else -> "${LocationHelper.formatDistance(distance)} away - ETA ~${etaMinutes(distance)} min"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    LinearProgressIndicator(
                        progress = { (task.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)),
                        color = Emerald400,
                        trackColor = Cyan400.copy(alpha = 0.2f)
                    )
                    Text(
                        "${task.assignedWorkerName ?: "Worker"} - ${task.truckName.ifBlank { "Truck" }}",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                    val liveWorkers by com.wasteroute.app.firebase.FirebaseDataRepository.workers.collectAsState()
                    WorkerPhoneRow(
                        phone = remember(liveWorkers, task.assignedWorkerId, task.assignedWorkerName) {
                            com.wasteroute.app.firebase.FirebaseDataRepository.workerPhone(task.assignedWorkerId, task.assignedWorkerName)
                        }
                    )
                }
            }
        }
    }
}

/** Rough ETA for a campus truck moving at ~20 km/h. */
fun etaMinutes(distanceM: Double): Int = maxOf(1, Math.ceil(distanceM / 330.0).toInt())
