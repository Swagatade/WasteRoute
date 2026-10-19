package com.westeroute.app.components

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView

/** Lets a bin marker tapped inside the Leaflet page select the same bin as its card in Compose. */
class MapBinBridge(private val onSelect: (String) -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onBinSelected(code: String) {
        main.post { onSelect(code) }
    }
}

/** Routes my location -> the bin on the map. Retries until Leaflet has finished loading from the CDN. */
fun WebView.focusBinOnMap(lat: Double, lng: Double) {
    evaluateJavascript(
        "(function f(n){if(!(window.focusBin&&window.focusBin($lat,$lng))&&n>0){setTimeout(function(){f(n-1);},500);}})(20);",
        null
    )
}

/** Pushes the live GPS fix into the map page, retrying until the page is ready. */
fun WebView.pushUserLocation(lat: Double, lng: Double) {
    evaluateJavascript(
        "(function f(n){if(window.setUserLocation){window.setUserLocation($lat,$lng);}else if(n>0){setTimeout(function(){f(n-1);},500);}})(20);",
        null
    )
}
