package com.westeroute.app.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = DeepNavy,
    primaryContainer = Emerald700,
    onPrimaryContainer = Emerald100,
    secondary = Cyan400,
    onSecondary = DeepNavy,
    secondaryContainer = Cyan600,
    onSecondaryContainer = Cyan50,
    tertiary = ElectricBlue400,
    onTertiary = DeepNavy,
    tertiaryContainer = ElectricBlue600,
    onTertiaryContainer = NeutralOffWhite,
    background = DeepNavy,
    onBackground = NeutralWhite,
    surface = DarkGraphite,
    onSurface = NeutralWhite,
    surfaceVariant = NeutralCharcoal,
    onSurfaceVariant = NeutralGray300,
    outline = NeutralGray600,
    outlineVariant = NeutralGray700
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = NeutralWhite,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald700,
    secondary = Cyan600,
    onSecondary = NeutralWhite,
    secondaryContainer = Cyan50,
    onSecondaryContainer = Cyan600,
    tertiary = ElectricBlue600,
    onTertiary = NeutralWhite,
    tertiaryContainer = NeutralGray100,
    onTertiaryContainer = ElectricBlue600,
    background = NeutralOffWhite,
    onBackground = NeutralCharcoal,
    surface = NeutralWhite,
    onSurface = NeutralCharcoal,
    surfaceVariant = NeutralGray100,
    onSurfaceVariant = NeutralGray600,
    outline = NeutralGray300,
    outlineVariant = NeutralGray200
)

@Composable
fun WesteRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    premiumMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val glassStyle = when {
        darkTheme && premiumMode -> DarkGlassStyle
        darkTheme && !premiumMode -> StandardDarkGlassStyle
        !darkTheme && premiumMode -> LightGlassStyle
        else -> StandardLightGlassStyle
    }



    // Cap system font scaling so large accessibility fonts don't break fixed layouts
    val baseDensity = LocalDensity.current
    val cappedDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale.coerceAtMost(1.15f)
    )

    CompositionLocalProvider(
        LocalDensity provides cappedDensity,
        LocalGlassStyle provides glassStyle,
        LocalPremiumMode provides premiumMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
