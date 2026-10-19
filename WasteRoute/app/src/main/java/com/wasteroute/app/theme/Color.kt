package com.westeroute.app.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// Brand & Accent Colors
// ==========================================
val Emerald50 = Color(0xFFECFDF5)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald400 = Color(0xFF34D399)
val Emerald500 = Color(0xFF10B981)
val Emerald600 = Color(0xFF059669)
val Emerald700 = Color(0xFF047857)

val Cyan50 = Color(0xFFECFEFF)
val Cyan300 = Color(0xFF67E8F9)
val Cyan400 = Color(0xFF22D3EE)
val Cyan500 = Color(0xFF06B6D4)
val Cyan600 = Color(0xFF0891B2)

val Aqua400 = Color(0xFF38BDF8)
val Aqua500 = Color(0xFF0EA5E9)

val ElectricBlue400 = Color(0xFF60A5FA)
val ElectricBlue500 = Color(0xFF3B82F6)
val ElectricBlue600 = Color(0xFF2563EB)

val Violet400 = Color(0xFFA78BFA)
val Violet500 = Color(0xFF8B5CF6)
val Violet600 = Color(0xFF7C3AED)

val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)

// ==========================================
// Neutrals & Surface Colors
// ==========================================
val NeutralWhite = Color(0xFFFFFFFF)
val NeutralOffWhite = Color(0xFFF8FAFC)
val NeutralGray50 = Color(0xFFF9FAFB)
val NeutralGray100 = Color(0xFFF1F5F9)
val NeutralGray200 = Color(0xFFE2E8F0)
val NeutralGray300 = Color(0xFFCBD5E1)
val NeutralGray400 = Color(0xFF94A3B8)
val NeutralGray500 = Color(0xFF64748B)
val NeutralGray600 = Color(0xFF475569)
val NeutralGray700 = Color(0xFF334155)
val NeutralCharcoal = Color(0xFF1E293B)
val DarkGraphite = Color(0xFF0F172A)
val DeepNavy = Color(0xFF090D16)
val DeepestBackground = Color(0xFF060911)

// ==========================================
// Gradients
// ==========================================
val EmeraldCyanGradient = Brush.linearGradient(
    colors = listOf(Emerald500, Cyan500)
)

val CyanBlueGradient = Brush.linearGradient(
    colors = listOf(Cyan500, ElectricBlue500)
)

val ElectricVioletGradient = Brush.linearGradient(
    colors = listOf(ElectricBlue500, Violet500)
)

val DarkGlassBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xCC0F172A),
        Color(0x99090D16)
    )
)

val LightGlassBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xF2FFFFFF),
        Color(0xCCF1F5F9)
    )
)

val GlassBorderLight = Brush.linearGradient(
    colors = listOf(
        Color(0x99FFFFFF),
        Color(0x3306B6D4),
        Color(0x1A10B981)
    )
)

val GlassBorderDark = Brush.linearGradient(
    colors = listOf(
        Color(0x4D34D399),
        Color(0x3322D3EE),
        Color(0x1AFFFFFF)
    )
)
