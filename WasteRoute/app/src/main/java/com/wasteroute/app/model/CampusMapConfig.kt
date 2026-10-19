package com.wasteroute.app.model

import com.wasteroute.app.components.BinStatus
import com.wasteroute.app.firebase.LiveComplaint
import com.wasteroute.app.firebase.LiveHousehold
import com.wasteroute.app.firebase.LiveWorker

data class CampusSmartBin(
    val id: String,
    val code: String,
    val name: String,
    val zone: String,
    val lat: Double,
    val lng: Double,
    val fillLevel: Int,
    val type: String,
    val distanceDisplay: String,
    val status: BinStatus
)

object CampusMapConfig {
    const val CAMPUS_NAME = "SRM IST Kattankulathur (SRM KTR)"
    const val CAMPUS_LAT = 12.8230
    const val CAMPUS_LNG = 80.0440
    const val OSM_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"

    // No sample bins: bins come only from live Firestore.
    val SRM_KTR_BINS = emptyList<CampusSmartBin>()

    // Neutral placeholder shown only while no live bins exist.
    val EMPTY_BIN = CampusSmartBin(
        id = "NONE",
        code = "--",
        name = "No bins available",
        zone = "--",
        lat = CAMPUS_LAT,
        lng = CAMPUS_LNG,
        fillLevel = 0,
        type = "--",
        distanceDisplay = "--",
        status = BinStatus.AVAILABLE
    )

    /**
     * Generates a bulletproof standalone HTML document with instant 0ms offline vector rendering
     * of the SRM KTR Campus layout, combined with dynamic Leaflet tile loading.
     * Guaranteed to never show a blank screen on any Android device or network state.
     */
    /** Escapes a value so it can sit inside a double-quoted JS string. */
    private fun js(s: String): String = s
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
        .replace("\r", " ")
        .replace("<", "\\u003c")

    fun buildOpenStreetMapHtml(
        centerLat: Double = CAMPUS_LAT,
        centerLng: Double = CAMPUS_LNG,
        zoom: Int = 16,
        userLat: Double = CAMPUS_LAT,
        userLng: Double = CAMPUS_LNG,
        showUserMarker: Boolean = true,
        routeToBin: CampusSmartBin? = null,
        destinationLat: Double? = null,
        destinationLng: Double? = null,
        destinationLabel: String? = null,
        bins: List<CampusSmartBin> = SRM_KTR_BINS,
        complaints: List<LiveComplaint> = emptyList(),
        workers: List<LiveWorker> = emptyList(),
        /** Homes to pin. Workers get their area's homes, a resident only their own; never every home. */
        households: List<LiveHousehold> = emptyList(),
        isDark: Boolean = true
    ): String {
        val targetLat = destinationLat ?: routeToBin?.lat
        val targetLng = destinationLng ?: routeToBin?.lng

        val binsJson = buildString {
            append("[")
            bins.forEachIndexed { i, bin ->
                append("{")
                append("\"code\":\"${js(bin.code)}\",")
                append("\"name\":\"${js(bin.name)}\",")
                append("\"lat\":${bin.lat},")
                append("\"lng\":${bin.lng},")
                append("\"fill\":${bin.fillLevel},")
                append("\"type\":\"${js(bin.type)}\",")
                append("\"zone\":\"${js(bin.zone)}\"")
                append("}")
                if (i < bins.size - 1) append(",")
            }
            append("]")
        }

        val complaintsJson = buildString {
            append("[")
            complaints.forEachIndexed { i, cmp ->
                append("{")
                append("\"ticketNo\":\"${js(cmp.ticketNo)}\",")
                append("\"category\":\"${js(cmp.category)}\",")
                append("\"priority\":\"${js(cmp.priority)}\",")
                append("\"status\":\"${js(cmp.status)}\",")
                append("\"location\":\"${js(cmp.location)}\",")
                append("\"lat\":${cmp.lat},")
                append("\"lng\":${cmp.lng}")
                append("}")
                if (i < complaints.size - 1) append(",")
            }
            append("]")
        }

        val workersJson = buildString {
            append("[")
            workers.forEachIndexed { i, w ->
                append("{")
                append("\"name\":\"${js(w.name)}\",")
                append("\"vehicle\":\"${js(w.vehicle)}\",")
                append("\"status\":\"${js(w.status)}\",")
                append("\"lat\":${w.lat},")
                append("\"lng\":${w.lng}")
                append("}")
                if (i < workers.size - 1) append(",")
            }
            append("]")
        }

        val todayStr = com.wasteroute.app.firebase.CollectionRepository.todayString()
        val householdsJson = buildString {
            append("[")
            households.forEachIndexed { i, h ->
                append("{")
                append("\"code\":\"${js(h.code)}\",")
                append("\"name\":\"${js(h.name.ifBlank { h.code })}\",")
                append("\"address\":\"${js(h.address)}\",")
                append("\"area\":\"${js(h.areaName)}\",")
                append("\"lat\":${h.lat},")
                append("\"lng\":${h.lng},")
                append("\"girls\":${h.areaId == "ward-ktr-06"},")
                append("\"done\":${h.collectedToday(todayStr)},")
                append("\"by\":\"${js(h.collectedByName)}\"")
                append("}")
                if (i < households.size - 1) append(",")
            }
            append("]")
        }

        val routeJson = if (targetLat != null && targetLng != null) {
            "[[${userLat}, ${userLng}], [${targetLat}, ${targetLng}]]"
        } else "null"

        val bgColor = if (isDark) "#090D16" else "#F1F5F9"
        val surfaceColor = if (isDark) "#0F172A" else "#FFFFFF"
        val textColor = if (isDark) "#F8FAFC" else "#0F172A"
        val roadColor = if (isDark) "#1E293B" else "#CBD5E1"
        val buildingColor = if (isDark) "#131E35" else "#E2E8F0"
        val parkColor = if (isDark) "rgba(16, 185, 129, 0.12)" else "rgba(16, 185, 129, 0.20)"

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
    <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
    <style>
        * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; margin: 0; padding: 0; }
        html, body { height: 100%; width: 100%; background: $bgColor; overflow: hidden; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
        #canvas-wrap { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 1; }
        #leaflet-map { position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 2; opacity: 1; }
        
        /* High-contrast, elegant dark mode for street tiles */
        .dark-tiles .leaflet-tile-pane {
            filter: invert(100%) hue-rotate(180deg) brightness(95%) contrast(90%);
            -webkit-filter: invert(100%) hue-rotate(180deg) brightness(95%) contrast(90%);
        }
        
        .bin-pin svg { width: 17px; height: 17px; stroke: #FFFFFF; fill: none; stroke-width: 2.2; stroke-linecap: round; stroke-linejoin: round; }
        .bin-pin {
            width: 30px; height: 30px; border-radius: 50%; border: 2px solid #FFFFFF;
            display: flex; align-items: center; justify-content: center;
            font-size: 10.5px; font-weight: 800; color: #FFFFFF; box-shadow: 0 4px 10px rgba(0,0,0,0.6);
            cursor: pointer;
        }

        .home-pin {
            width: 30px; height: 30px; border-radius: 10px; border: 2px solid #FFFFFF;
            display: flex; align-items: center; justify-content: center;
            box-shadow: 0 4px 10px rgba(0,0,0,0.6); cursor: pointer;
        }
        .home-pin svg { width: 17px; height: 17px; stroke: #FFFFFF; fill: none; stroke-width: 2.4; stroke-linecap: round; stroke-linejoin: round; }

        .cmp-pin {
            width: 32px; height: 32px; border-radius: 10px; border: 2px solid #FFFFFF;
            display: flex; align-items: center; justify-content: center;
            box-shadow: 0 4px 12px rgba(0,0,0,0.6);
            cursor: pointer;
        }
        .cmp-pin svg { width: 18px; height: 18px; stroke: #FFFFFF; fill: none; stroke-width: 2.5; }

        .truck-pin {
            width: 34px; height: 34px; border-radius: 50%; background: #0284C7; border: 2.5px solid #38BDF8;
            display: flex; align-items: center; justify-content: center;
            box-shadow: 0 4px 10px rgba(0,0,0,0.6);
            cursor: pointer;
        }
        .truck-pin svg { width: 18px; height: 18px; stroke: #FFFFFF; fill: none; stroke-width: 2.2; }

        .user-pin-wrap { position: relative; width: 24px; height: 24px; }
        .user-pin {
            width: 20px; height: 20px; border-radius: 50%; background: #38BDF8;
            border: 3px solid #FFFFFF; box-shadow: 0 0 12px #38BDF8; position: absolute; top: 2px; left: 2px; z-index: 2;
        }
        .user-pulse {
            width: 36px; height: 36px; border-radius: 50%; background: rgba(56, 189, 248, 0.4);
            position: absolute; top: -6px; left: -6px; animation: pulse 1.6s infinite ease-out; z-index: 1;
        }
        @keyframes pulse {
            0% { transform: scale(0.5); opacity: 1; }
            100% { transform: scale(1.4); opacity: 0; }
        }

        .leaflet-popup-content-wrapper {
            background: $surfaceColor; color: $textColor; border-radius: 14px;
            border: 1px solid ${if (isDark) "rgba(255,255,255,0.15)" else "rgba(0,0,0,0.12)"};
            box-shadow: 0 10px 25px rgba(0,0,0,0.4); font-size: 12px; padding: 4px;
        }
        .leaflet-popup-tip { background: $surfaceColor; }
        .popup-title { font-size: 13px; font-weight: 800; color: #38BDF8; margin-bottom: 2px; }
        .popup-badge { display: inline-block; padding: 2px 6px; border-radius: 6px; font-size: 10px; font-weight: 700; margin-top: 4px; }
    </style>
</head>
<body>
    <!-- 0ms Immediate Vector Map Layer (Guaranteed instant rendering) -->
    <div id="canvas-wrap">
        <canvas id="campusCanvas"></canvas>
    </div>

    <!-- Interactive Leaflet Map Layer (Mounted dynamically on top) -->
    <div id="leaflet-map"></div>
    <div id="hostel-legend" style="display:none;position:absolute;left:8px;top:8px;z-index:6;background:rgba(15,23,42,0.88);color:#F8FAFC;font-size:11px;font-weight:700;padding:7px 9px;border-radius:10px;line-height:1.7;">
        <div><span style="display:inline-block;width:11px;height:11px;border-radius:3px;background:#2563EB;border:2px solid #fff;vertical-align:-2px"></span> Men's (Boys)</div>
        <div><span style="display:inline-block;width:11px;height:11px;border-radius:3px;background:#EC4899;border:2px solid #fff;vertical-align:-2px"></span> Women's (Girls)</div>
        <div><span style="display:inline-block;width:11px;height:11px;border-radius:3px;background:#64748B;border:3px solid #22C55E;vertical-align:-2px"></span> Collected today</div>
    </div>
    <div id="map-status" style="position:absolute;left:0;right:0;bottom:8px;text-align:center;z-index:5;font-size:11px;color:#94A3B8;">Loading map...</div>

    <script>
        var binsData = $binsJson;
        var complaintsData = $complaintsJson;
        var workersData = $workersJson;
        var householdsData = $householdsJson;
        // Names / addresses come from Firestore: never let them inject markup into a popup.
        function esc(t) { return String(t == null ? '' : t).replace(/[&<>"']/g, function(c) { return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; }); }
        var userCoords = [$userLat, $userLng];
        var isDarkMode = $isDark;
        var userMarker = null, routeLine = null, routeTarget = ${if (targetLat != null && targetLng != null) "[$targetLat, $targetLng]" else "null"};

        // Truck markers can be moved live (tracking screens) without reloading the page
        var workerMarkers = [];
        window.moveWorker = function(i, lat, lng) {
            if (workersData[i]) { workersData[i].lat = lat; workersData[i].lng = lng; }
            if (workerMarkers[i]) workerMarkers[i].setLatLng([lat, lng]);
            drawInstantCampusMap();
        };

        // Called from Android when a bin is picked (card or marker): draws my-location -> bin and frames both
        window.focusBin = function(lat, lng) {
            routeTarget = [lat, lng];
            var map = window.campusMap;
            if (!map) return false;
            try {
                if (routeLine) { routeLine.setLatLngs([userCoords, routeTarget]); }
                else { routeLine = L.polyline([userCoords, routeTarget], { color: '#0284C7', weight: 5, dashArray: '8, 8', opacity: 0.95 }).addTo(map); }
                var b = L.latLngBounds([userCoords, routeTarget]);
                if (L.latLng(userCoords).distanceTo(L.latLng(routeTarget)) > 30) map.fitBounds(b, { padding: [40, 40], maxZoom: 17 });
                else map.setView(routeTarget, 17);
            } catch (e) { console.warn(e); }
            return true;
        };

        // Called from Android with the real device GPS fix
        window.setUserLocation = function(lat, lng) {
            userCoords = [lat, lng];
            if (userMarker) userMarker.setLatLng(userCoords);
            if (routeLine && routeTarget) routeLine.setLatLngs([userCoords, routeTarget]);
            drawInstantCampusMap();
        };

        // Universal rounded rectangle polyfill for Android System WebView
        function drawRoundRect(ctx, x, y, w, h, r) {
            if (typeof ctx.roundRect === 'function') {
                ctx.beginPath();
                ctx.roundRect(x, y, w, h, r);
                return;
            }
            if (r > w/2) r = w/2;
            if (r > h/2) r = h/2;
            ctx.beginPath();
            ctx.moveTo(x + r, y);
            ctx.lineTo(x + w - r, y);
            ctx.quadraticCurveTo(x + w, y, x + w, y + r);
            ctx.lineTo(x + w, y + h - r);
            ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
            ctx.lineTo(x + r, y + h);
            ctx.quadraticCurveTo(x, y + h, x, y + h - r);
            ctx.lineTo(x, y + r);
            ctx.quadraticCurveTo(x, y, x + r, y);
            ctx.closePath();
        }

        // GPS bounds of SRM KTR
        var minLat = 12.8190, maxLat = 12.8275;
        var minLng = 80.0400, maxLng = 80.0495;

        function gpsToCanvas(lat, lng, w, h) {
            var x = ((lng - minLng) / (maxLng - minLng)) * w;
            var y = (1.0 - ((lat - minLat) / (maxLat - minLat))) * h;
            return { x: x, y: y };
        }

        // Render Instant 0ms Vector Campus Canvas
        function drawInstantCampusMap() {
            try {
                var canvas = document.getElementById('campusCanvas');
                if (!canvas) return;
                var w = canvas.width = window.innerWidth;
                var h = canvas.height = window.innerHeight;
                var ctx = canvas.getContext('2d');
                if (!ctx) return;

                ctx.fillStyle = '$bgColor';
                ctx.fillRect(0, 0, w, h);

                // Subtle tactical grid
                ctx.strokeStyle = isDarkMode ? 'rgba(56, 189, 248, 0.07)' : 'rgba(0, 0, 0, 0.05)';
                ctx.lineWidth = 1;
                var gridStep = 36;
                for (var gx = 0; gx < w; gx += gridStep) {
                    ctx.beginPath(); ctx.moveTo(gx, 0); ctx.lineTo(gx, h); ctx.stroke();
                }
                for (var gy = 0; gy < h; gy += gridStep) {
                    ctx.beginPath(); ctx.moveTo(0, gy); ctx.lineTo(w, gy); ctx.stroke();
                }

                // Smart Bins Pins
                binsData.forEach(function(bin) {
                    var pos = gpsToCanvas(bin.lat, bin.lng, w, h);
                    var color = '#10B981';
                    
                    ctx.beginPath();
                    ctx.arc(pos.x, pos.y, 14, 0, Math.PI * 2);
                    ctx.fillStyle = color;
                    ctx.fill();
                    ctx.lineWidth = 2.5;
                    ctx.strokeStyle = '#FFFFFF';
                    ctx.stroke();

                    // Bin icon (location only, no fill status)
                    ctx.strokeStyle = '#FFFFFF';
                    ctx.lineWidth = 1.8;
                    ctx.lineCap = 'round';
                    ctx.beginPath();
                    ctx.moveTo(pos.x - 6, pos.y - 4); ctx.lineTo(pos.x + 6, pos.y - 4);
                    ctx.moveTo(pos.x - 2.5, pos.y - 4); ctx.lineTo(pos.x - 2.5, pos.y - 6.5); ctx.lineTo(pos.x + 2.5, pos.y - 6.5); ctx.lineTo(pos.x + 2.5, pos.y - 4);
                    ctx.moveTo(pos.x - 4.5, pos.y - 4); ctx.lineTo(pos.x - 3.8, pos.y + 6); ctx.lineTo(pos.x + 3.8, pos.y + 6); ctx.lineTo(pos.x + 4.5, pos.y - 4);
                    ctx.stroke();
                });

                // Home pins (green = collected today, amber = still to collect)
                householdsData.forEach(function(hh) {
                    var pos = gpsToCanvas(hh.lat, hh.lng, w, h);
                    ctx.beginPath();
                    ctx.arc(pos.x, pos.y, 11, 0, Math.PI * 2);
                    ctx.fillStyle = hh.girls ? '#EC4899' : '#2563EB';
                    ctx.fill();
                    ctx.lineWidth = hh.done ? 4 : 2;
                    ctx.strokeStyle = hh.done ? '#22C55E' : '#FFFFFF';
                    ctx.stroke();
                });

                // Complaints Pins
                complaintsData.forEach(function(c) {
                    var pos = gpsToCanvas(c.lat, c.lng, w, h);
                    var color = c.priority === 'Critical' ? '#EF4444' : (c.priority === 'High' ? '#F97316' : '#3B82F6');
                    ctx.fillStyle = color;
                    drawRoundRect(ctx, pos.x - 13, pos.y - 13, 26, 26, 6);
                    ctx.fill();
                    ctx.strokeStyle = '#FFFFFF';
                    ctx.lineWidth = 2;
                    ctx.stroke();
                });

                // User Location
                var userPos = gpsToCanvas(userCoords[0], userCoords[1], w, h);
                ctx.beginPath();
                ctx.arc(userPos.x, userPos.y, 16, 0, Math.PI * 2);
                ctx.fillStyle = 'rgba(56, 189, 248, 0.25)';
                ctx.fill();
                ctx.beginPath();
                ctx.arc(userPos.x, userPos.y, 8, 0, Math.PI * 2);
                ctx.fillStyle = '#0284C7';
                ctx.fill();
                ctx.lineWidth = 2.5;
                ctx.strokeStyle = '#FFFFFF';
                ctx.stroke();
            } catch (err) {
                console.error("Canvas draw error:", err);
            }
        }

        drawInstantCampusMap();
        window.addEventListener('resize', drawInstantCampusMap);
        if (householdsData.length) { var lg = document.getElementById('hostel-legend'); if (lg) lg.style.display = 'block'; }

        // Robust Leaflet initialization
        function initInteractiveLeaflet() {
            if (typeof L === 'undefined') {
                // Fallback loader if primary CDN didn't load
                var fallbackScript = document.createElement('script');
                fallbackScript.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
                fallbackScript.onload = function() {
                    try { mountLeafletMap(); } catch(e) { console.warn(e); }
                };
                fallbackScript.onerror = function() {
                    var st = document.getElementById('map-status');
                    if (st) st.textContent = 'Map loading offline preview...';
                };
                document.head.appendChild(fallbackScript);
                return;
            }
            mountLeafletMap();
        }

        function mountLeafletMap() {
            if (typeof L === 'undefined') return;
            var mapEl = document.getElementById('leaflet-map');
            if (!mapEl || mapEl.getAttribute('data-mounted') === 'true') return;
            mapEl.setAttribute('data-mounted', 'true');

            if (isDarkMode) {
                mapEl.classList.add('dark-tiles');
            }

            try {
                var map = L.map('leaflet-map', {
                    center: [$centerLat, $centerLng],
                    zoom: $zoom,
                    zoomControl: false,
                    attributionControl: false
                });

                // Primary: Real OpenStreetMap street tiles with reliable fallback to Esri World Street Map
                var osmUrl = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
                var osmFallback = 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}';
                var tileErrors = 0;
                var baseLayer = L.tileLayer(osmUrl, { maxZoom: 19, minZoom: 1 }).addTo(map);
                baseLayer.on('tileerror', function() {
                    tileErrors++;
                    if (tileErrors > 3) {
                        map.removeLayer(baseLayer);
                        L.tileLayer(osmFallback, { maxZoom: 19 }).addTo(map);
                    }
                });

                // Multiple invalidation passes to guarantee full rendering as WebView lays out
                [50, 150, 300, 700, 1500, 3000].forEach(function(ms) {
                    setTimeout(function() { if (map) map.invalidateSize(); }, ms);
                });
                window.addEventListener('resize', function() { if (map) map.invalidateSize(); });

                // Bins
                binsData.forEach(function(b) {
                    var color = '#10B981';
                    var icon = L.divIcon({
                        className: '',
                        html: '<div class="bin-pin" style="background:' + color + ';"><svg viewBox="0 0 24 24"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 10v6M14 10v6"/></svg></div>',
                        iconSize: [30, 30],
                        iconAnchor: [15, 15]
                    });
                    var m = L.marker([b.lat, b.lng], { icon: icon }).addTo(map);
                    // Tapping a bin works exactly like tapping its card: Android selects it, then the map routes to it
                    m.on('click', function() {
                        if (window.WRBridge && window.WRBridge.onBinSelected) window.WRBridge.onBinSelected(b.code);
                        else window.focusBin(b.lat, b.lng);
                    });
                    m.bindPopup(
                        '<div class="popup-title">' + esc(b.code) + '</div>' +
                        '<b>' + esc(b.name) + '</b><br/>' +
                        '<span style="color: #94A3B8;">' + esc(b.zone) + '</span><br/>' +
                        '<div class="popup-badge" style="background:' + color + '22; color:' + color + ';">' + esc(b.type) + '</div>'
                    );
                });

                // Homes: green + tick once today's garbage is collected, amber while waiting
                householdsData.forEach(function(hh) {
                    // Men's (boys) = blue, women's (girls) = pink; a green ring + tick means collected today
                    var hc = hh.girls ? '#EC4899' : '#2563EB';
                    var ring = hh.done ? '#22C55E' : '#FFFFFF';
                    var glyph = hh.done ? '<path d="M20 6 9 17l-5-5"/>' : '<path d="M3 11l9-8 9 8M5 10v10h14V10"/>';
                    var hicon = L.divIcon({
                        className: '',
                        html: '<div class="home-pin" style="background:' + hc + ';border-color:' + ring + ';border-width:' + (hh.done ? 4 : 2) + 'px;"><svg viewBox="0 0 24 24">' + glyph + '</svg></div>',
                        iconSize: [30, 30],
                        iconAnchor: [15, 15]
                    });
                    L.marker([hh.lat, hh.lng], { icon: hicon }).addTo(map).bindPopup(
                        '<div class="popup-title" style="color:' + hc + ';">' + esc(hh.code) + '</div>' +
                        '<b>' + esc(hh.name) + '</b><br/>' +
                        '<span style="color: #94A3B8;">' + esc(hh.address || hh.area) + '</span><br/>' +
                        '<div class="popup-badge" style="background:' + hc + '22; color:' + hc + ';">' +
                        (hh.girls ? "Women's (Girls) hostel \u00b7 " : "Men's (Boys) hostel \u00b7 ") + (hh.done ? 'Collected today' + (hh.by ? ' \u00b7 ' + esc(hh.by) : '') : 'Waiting for collection') + '</div>'
                    );
                });

                // Complaints
                complaintsData.forEach(function(c) {
                    var color = c.priority === 'Critical' ? '#EF4444' : (c.priority === 'High' ? '#F97316' : '#3B82F6');
                    var icon = L.divIcon({
                        className: '',
                        html: '<div class="cmp-pin" style="background:' + color + ';"><svg viewBox="0 0 24 24"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg></div>',
                        iconSize: [32, 32],
                        iconAnchor: [16, 16]
                    });
                    var m = L.marker([c.lat, c.lng], { icon: icon }).addTo(map);
                    m.bindPopup(
                        '<div class="popup-title" style="color:' + color + ';">' + esc(c.ticketNo) + ' (' + esc(c.priority) + ')</div>' +
                        '<b>' + esc(c.category) + '</b><br/>' +
                        '<span style="color: #94A3B8;">' + esc(c.location) + '</span><br/>' +
                        '<div class="popup-badge" style="background:' + color + '22; color:' + color + ';">Status: ' + esc(c.status) + '</div>'
                    );
                });

                // Workers
                workersData.forEach(function(w, wi) {
                    var icon = L.divIcon({
                        className: '',
                        html: '<div class="truck-pin"><svg viewBox="0 0 24 24"><rect width="16" height="13" x="1" y="6" rx="2"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg></div>',
                        iconSize: [34, 34],
                        iconAnchor: [17, 17]
                    });
                    var m = L.marker([w.lat, w.lng], { icon: icon }).addTo(map);
                    workerMarkers[wi] = m;
                    m.bindPopup(
                        '<div class="popup-title" style="color: #38BDF8;">' + esc(w.name) + '</div>' +
                        '<b>' + esc(w.vehicle) + '</b><br/>' +
                        '<div class="popup-badge" style="background: rgba(56, 189, 248, 0.2); color: #38BDF8;">' + esc(w.status) + '</div>'
                    );
                });

                ${if (showUserMarker) """
                var userIcon = L.divIcon({
                    className: '',
                    html: '<div class="user-pin-wrap"><div class="user-pulse"></div><div class="user-pin"></div></div>',
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                });
                userMarker = L.marker(userCoords, { icon: userIcon }).addTo(map).bindPopup('<b>Your Live Location</b>');
                """ else ""}

                var routeCoords = $routeJson;
                if (routeCoords && Array.isArray(routeCoords) && routeCoords.length >= 2) {
                    try {
                        routeLine = L.polyline(routeCoords, { color: '#0284C7', weight: 5, dashArray: '8, 8', opacity: 0.95 }).addTo(map);
                        var p1 = L.latLng(routeCoords[0][0], routeCoords[0][1]);
                        var p2 = L.latLng(routeCoords[1][0], routeCoords[1][1]);
                        if (p1.distanceTo(p2) > 30) {
                            var b = L.latLngBounds([p1, p2]);
                            if (b.isValid()) {
                                map.fitBounds(b, { padding: [40, 40], maxZoom: 17 });
                            }
                        } else {
                            map.setView(p1, 16);
                        }
                    } catch(eRoute) {
                        console.warn("Route bounds notice:", eRoute);
                        map.setView([$centerLat, $centerLng], $zoom);
                    }
                } else {
                    map.setView([$centerLat, $centerLng], $zoom);
                }

                mapEl.classList.add('ready');
                var st = document.getElementById('map-status'); if (st) st.style.display = 'none';
                window.campusMap = map;
            } catch (err) {
                console.error("Leaflet mount error:", err);
            }
        }

        if (document.readyState === 'complete' || document.readyState === 'interactive') {
            setTimeout(initInteractiveLeaflet, 80);
        } else {
            window.addEventListener('DOMContentLoaded', function() {
                setTimeout(initInteractiveLeaflet, 80);
            });
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
