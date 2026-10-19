package com.wasteroute.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wasteroute.app.components.AppBackground
import com.wasteroute.app.navigation.AppNavigation
import com.wasteroute.app.theme.WasteRouteTheme

@Composable
fun WasteRouteApp() {
    val darkTheme = isSystemInDarkTheme()

    WasteRouteTheme(
        darkTheme = darkTheme,
        premiumMode = true
    ) {
        AppBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            AppNavigation()
        }
    }
}
