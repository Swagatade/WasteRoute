package com.westeroute.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.westeroute.app.model.UiPerformanceMode

enum class GlassSurfaceLevel {
    PRIMARY, // Level 1: Deep translucent, gradient border, rich shadow
    SOFT,    // Level 2: Gentle tint, thin border, soft elevation
    PLAIN    // Level 3: Minimal, clean, utility background
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    level: GlassSurfaceLevel = GlassSurfaceLevel.PRIMARY,
    shape: Shape = RoundedCornerShape(24.dp),
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    elevation: Dp = if (level == GlassSurfaceLevel.PRIMARY) 8.dp else 3.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val backgroundBrush = when (level) {
        GlassSurfaceLevel.PRIMARY -> {
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xCC0F172A), // 80% Slate 900
                        Color(0xB31E293B), // 70% Slate 800
                        Color(0x990A0F1D)  // 60% Darker
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xE6FFFFFF), // 90% White
                        Color(0xCCF1F5F9), // 80% Slate 100
                        Color(0xB3E2E8F0)  // 70% Slate 200
                    )
                )
            }
        }
        GlassSurfaceLevel.SOFT -> {
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x991E293B),
                        Color(0x660F172A)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xF5FFFFFF),
                        Color(0xE6F8FAFC)
                    )
                )
            }
        }
        GlassSurfaceLevel.PLAIN -> {
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x661E293B),
                        Color(0x4D0F172A)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF1F5F9)
                    )
                )
            }
        }
    }

    val borderBrush = when (level) {
        GlassSurfaceLevel.PRIMARY -> {
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x8034D399), // Emerald highlight
                        Color(0x4D38BDF8), // Cyan highlight
                        Color(0x26FFFFFF),
                        Color(0x10FFFFFF)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x99FFFFFF),
                        Color(0x6610B981),
                        Color(0x3306B6D4),
                        Color(0x20CBD5E1)
                    )
                )
            }
        }
        GlassSurfaceLevel.SOFT -> {
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x4D94A3B8),
                        Color(0x1A64748B)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x80FFFFFF),
                        Color(0x33CBD5E1)
                    )
                )
            }
        }
        GlassSurfaceLevel.PLAIN -> {
            if (isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x2664748B),
                        Color(0x1A334155)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x40CBD5E1),
                        Color(0x20E2E8F0)
                    )
                )
            }
        }
    }

    val shadowColor = if (isDark) {
        if (level == GlassSurfaceLevel.PRIMARY) Color(0x66000000) else Color(0x33000000)
    } else {
        if (level == GlassSurfaceLevel.PRIMARY) Color(0x1A0F172A) else Color(0x0D0F172A)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (performanceMode == UiPerformanceMode.PREMIUM) elevation else 1.dp,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(brush = backgroundBrush, shape = shape)
            .border(
                width = if (level == GlassSurfaceLevel.PRIMARY) 1.2.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            ),
        content = content
    )
}
