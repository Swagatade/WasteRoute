package com.westeroute.app.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.westeroute.app.firebase.CollectionRepository
import com.westeroute.app.firebase.FirebaseDataRepository
import com.westeroute.app.firebase.LiveSchedule
import org.json.JSONArray
import org.json.JSONObject

/**
 * Worker's department map + timetable. Shows every slot a supervisor scheduled for this worker:
 * numbered stops on the map in time order, and the same list below with live completion status.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WorkerTimetableScreen(
    workerId: String,
    workerName: String,
    onNavigateBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val schedule by CollectionRepository.workerSchedule.collectAsState()
    val workers by FirebaseDataRepository.workers.collectAsState()
    val today = remember { CollectionRepository.todayString() }

    LaunchedEffect(workerId) { CollectionRepository.listenToWorkerSchedule(workerId) }

    val department = workers.firstOrNull { it.id.equals(workerId, true) }?.wardName
        ?: schedule.firstOrNull()?.department.orEmpty()
    val upcoming = schedule.filter { it.date >= today }
    val todays = upcoming.filter { it.date == today }
    val mapStops = if (todays.isNotEmpty()) todays else upcoming.take(10)

    val bg = if (isDark) Color(0xFF0B1020) else Color(0xFFF6F8FA)
    val fg = if (isDark) Color.White else Color(0xFF0F172A)
    val muted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val card = if (isDark) Color(0xFF131A2E) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x1A000000))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = fg)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("My Route & Timetable", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    listOf(workerName, department).filter { it.isNotBlank() }.joinToString(" • "),
                    color = muted, fontSize = 12.sp
                )
            }
        }

        // Department map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(card)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                    }
                },
                update = { web ->
                    val html = buildRouteHtml(mapStops)
                    if (web.tag != html) {
                        web.tag = html
                        web.loadDataWithBaseURL("https://westroute.local/", html, "text/html", "utf-8", null)
                    }
                }
            )
        }

        Spacer(Modifier.height(12.dp))

        if (upcoming.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No collection slots have been scheduled for you yet.\nYour supervisor will assign your timetable.",
                    color = muted, fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(upcoming, key = { it.id }) { slot ->
                    ScheduleRow(slot, isToday = slot.date == today, fg = fg, muted = muted, card = card)
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(slot: LiveSchedule, isToday: Boolean, fg: Color, muted: Color, card: Color) {
    val accent = when {
        slot.isCompleted -> Color(0xFF10B981)
        slot.status == "In Progress" -> Color(0xFFF59E0B)
        else -> Color(0xFF38BDF8)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(78.dp)) {
            Text(slot.startTime, color = fg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(if (slot.endTime.isNotBlank()) "to ${slot.endTime}" else "", color = muted, fontSize = 11.sp)
            Text(if (isToday) "Today" else slot.date, color = muted, fontSize = 11.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(slot.locationName.ifBlank { "Collection point" }, color = fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (slot.department.isNotBlank()) Text(slot.department, color = muted, fontSize = 12.sp)
            if (slot.notes.isNotBlank()) Text(slot.notes, color = muted, fontSize = 11.sp)
        }
        Text(
            slot.status,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun buildRouteHtml(stops: List<LiveSchedule>): String {
    val arr = JSONArray()
    stops.forEach {
        arr.put(
            JSONObject()
                .put("lat", it.lat).put("lng", it.lng)
                .put("name", it.locationName).put("time", it.startTime)
                .put("done", it.isCompleted)
        )
    }
    // Defend against "</script>" in supervisor-entered text.
    val json = arr.toString().replace("</", "<\\/")
    return """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>html,body,#m{margin:0;height:100%;width:100%}
.pin{background:#38bdf8;color:#0b1020;border-radius:50%;width:26px;height:26px;line-height:26px;text-align:center;font:700 12px sans-serif;border:2px solid #fff;box-shadow:0 1px 4px #0006}
.pin.done{background:#10b981}</style></head><body><div id="m"></div><script>
var stops=$json;
var map=L.map('m',{zoomControl:true}).setView([12.8230,80.0440],16);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);
var pts=[];
stops.forEach(function(s,i){
  var ic=L.divIcon({className:'',html:'<div class="pin'+(s.done?' done':'')+'">'+(i+1)+'</div>',iconSize:[26,26],iconAnchor:[13,13]});
  var m=L.marker([s.lat,s.lng],{icon:ic}).addTo(map);
  var el=document.createElement('div');el.textContent=s.time+'  '+s.name;m.bindPopup(el);
  pts.push([s.lat,s.lng]);
});
if(pts.length>1){L.polyline(pts,{color:'#38bdf8',weight:3,dashArray:'6 6'}).addTo(map);}
if(pts.length){map.fitBounds(pts,{padding:[40,40],maxZoom:17});}
</script></body></html>
""".trimIndent()
}
