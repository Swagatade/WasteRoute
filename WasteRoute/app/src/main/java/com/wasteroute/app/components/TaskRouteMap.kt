package com.wasteroute.app.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wasteroute.app.firebase.LiveComplaint
import com.wasteroute.app.firebase.TaskStage
import com.wasteroute.app.model.CampusMapConfig
import kotlinx.coroutines.delay

/**
 * Small map inside an assigned task: the reported spot and a dashed route from the worker (or the moving
 * truck) to it, so the worker can see at a glance where to go.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun TaskRouteMap(
    task: LiveComplaint,
    myLoc: Pair<Double, Double>?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var web by remember { mutableStateOf<WebView?>(null) }

    // While the truck is driving, follow the truck; otherwise follow the worker's own GPS.
    val driving = task.stage == TaskStage.ON_THE_WAY || task.stage == TaskStage.ARRIVED
    val from: Pair<Double, Double>? =
        if (driving && task.truckLat != null && task.truckLng != null) task.truckLat to task.truckLng else myLoc

    val hasFrom = from != null
    val centerLat = from?.let { (it.first + task.lat) / 2.0 } ?: task.lat
    val centerLng = from?.let { (it.second + task.lng) / 2.0 } ?: task.lng

    // Built once per task (and when the first GPS fix arrives). Later position updates go through
    // setUserLocation below, so the page is not reloaded - and its tiles not re-fetched - on every GPS tick.
    val html = remember(task.id, task.lat, task.lng, isDark, hasFrom) {
        CampusMapConfig.buildOpenStreetMapHtml(
            centerLat = centerLat,
            centerLng = centerLng,
            zoom = 16,
            userLat = from?.first ?: task.lat,
            userLng = from?.second ?: task.lng,
            showUserMarker = from != null,
            destinationLat = if (from != null) task.lat else null,
            destinationLng = if (from != null) task.lng else null,
            bins = emptyList(),
            complaints = listOf(task),
            isDark = isDark
        )
    }

    LaunchedEffect(from, web) {
        val p = from ?: return@LaunchedEffect
        // Leaflet loads from the CDN, so retry until the page's setUserLocation exists
        delay(400)
        web?.evaluateJavascript(
            "(function f(n){if(window.setUserLocation){window.setUserLocation(${p.first},${p.second});}" +
                "else if(n>0){setTimeout(function(){f(n-1);},500);}})(20);",
            null
        )
    }

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
                // Let the map pan/zoom without the list scrolling underneath it
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    false
                }
                tag = html
                loadDataWithBaseURL("https://cdnjs.cloudflare.com/", html, "text/html", "UTF-8", null)
                web = this
            }
        },
        update = { webView ->
            web = webView
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL("https://cdnjs.cloudflare.com/", html, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(16.dp))
    )
}

/** Opens turn-by-turn navigation to the reported spot (Google Maps app, or the browser as a fallback). */
fun openNavigationTo(ctx: Context, lat: Double, lng: Double) {
    val maps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng")).apply {
        setPackage("com.google.android.apps.maps")
    }
    val intent = if (maps.resolveActivity(ctx.packageManager) != null) maps else
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"))
    runCatching { ctx.startActivity(intent) }
}
