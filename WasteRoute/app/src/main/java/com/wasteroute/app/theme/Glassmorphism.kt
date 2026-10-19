package com.wasteroute.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ultra-Glossy Liquid Glass styling tokens with chromatic prismatic refraction,
 * liquid convex lens specular sheen, and deep 3D meniscus reflections.
 */
@Immutable
data class GlassStyle(
    val backgroundColor: Color,
    val backgroundBrush: Brush,
    val borderBrush: Brush,
    val borderWidth: Dp = 1.4.dp,
    val cornerRadius: Dp = 28.dp,
    val elevation: Dp = 14.dp,
    val topSheenBrush: Brush,
    val bottomMeniscusBrush: Brush,
    val liquidPrismGlowBrush: Brush? = null
)

// Dark Liquid Crystal Glass (High Depth, Liquid Meniscus, Iridescent Prismatic Rim)
val DarkGlassStyle = GlassStyle(
    backgroundColor = Color(0xD90D172A),
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xF010203A),
            Color(0xDB080E18)
        )
    ),
    // Iridescent Chromatic Dispersion Rim: Specular White -> Amber -> Emerald -> Cyan -> Electric Blue -> Violet
    borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xE6FFFFFF), // Pure white top-left light catch
            Color(0x99FBBF24), // Soft amber dispersion
            Color(0xB334D399), // Emerald refraction
            Color(0xB322D3EE), // Cyan refraction
            Color(0x9960A5FA), // Electric blue
            Color(0x80A78BFA), // Violet chromatic tail
            Color(0x40FFFFFF)
        )
    ),
    borderWidth = 1.4.dp,
    cornerRadius = 28.dp,
    elevation = 16.dp,
    // Top Convex Liquid Lens Specular Sheen (-90° light angle, 80% intensity)
    topSheenBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0x52FFFFFF),
            Color(0x22FFFFFF),
            Color(0x06FFFFFF),
            Color.Transparent
        ),
        startY = 0f,
        endY = 140f
    ),
    // Bottom Curved Liquid Meniscus Refraction
    bottomMeniscusBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x0A22D3EE),
            Color(0x2634D399),
            Color(0x4D22D3EE)
        )
    ),
    liquidPrismGlowBrush = Brush.radialGradient(
        colors = listOf(
            Color(0x3334D399),
            Color(0x1A22D3EE),
            Color.Transparent
        )
    )
)

// Light Liquid Crystal Glass (Rich, High Contrast, Deep Sculpted Crystal)
val LightGlassStyle = GlassStyle(
    backgroundColor = Color(0xFFFFFFFF),
    backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFF8FAFC)
        )
    ),
    // Multi-tone chromatic rim on sculpted crystal
    borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFFFFF), // Bright specular white rim
            Color(0xB306B6D4), // Saturated Cyan refraction
            Color(0xB310B981), // Saturated Emerald refraction
            Color(0x808B5CF6), // Violet refraction
            Color(0x4D94A3B8)  // Soft slate rim for crisp definition
        )
    ),
    borderWidth = 1.4.dp,
    cornerRadius = 28.dp,
    elevation = 14.dp,
    topSheenBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xCCFFFFFF),
            Color(0x66FFFFFF),
            Color.Transparent
        ),
        startY = 0f,
        endY = 120f
    ),
    bottomMeniscusBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color(0x1F06B6D4),
            Color(0x3310B981)
        )
    ),
    liquidPrismGlowBrush = Brush.radialGradient(
        colors = listOf(
            Color(0x2E06B6D4),
            Color(0x1F10B981),
            Color.Transparent
        )
    )
)

val StandardDarkGlassStyle = DarkGlassStyle.copy(
    elevation = 6.dp,
    backgroundColor = Color(0xF50D172A)
)

val StandardLightGlassStyle = LightGlassStyle.copy(
    elevation = 6.dp,
    backgroundColor = Color(0xFFFFFFFF)
)

val LocalGlassStyle = staticCompositionLocalOf { DarkGlassStyle }
val LocalPremiumMode = staticCompositionLocalOf { true }
