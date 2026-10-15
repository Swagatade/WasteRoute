package com.westeroute.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.westeroute.app.components.AppBackground
import com.westeroute.app.navigation.AppNavigation
import com.westeroute.app.theme.WesteRouteTheme

@Composable
fun WesteRouteApp() {
    val darkTheme = isSystemInDarkTheme()

    WesteRouteTheme(
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
