package com.wasteroute.app.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.wasteroute.app.R
import com.wasteroute.app.theme.DeepNavy

private val LightBaseBg = Color(0xFFEDF2F7)

/**
 * Hardware-accelerated background that displays the exact custom
 * waste management illustration matching the active theme (day / night).
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val baseBackgroundColor = if (isDark) DeepNavy else LightBaseBg
    val bgDrawableRes = if (isDark) R.drawable.bg_night else R.drawable.bg_day

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBackgroundColor)
    ) {
        // High-resolution exact theme illustration
        Image(
            painter = painterResource(id = bgDrawableRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        content()
    }
}

