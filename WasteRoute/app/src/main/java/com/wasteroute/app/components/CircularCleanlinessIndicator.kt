package com.westeroute.app.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.Emerald500

@Composable
fun CircularCleanlinessIndicator(
    score: Int = 82,
    trendPercent: String = "↑ 6% this week",
    resolvedCount: Int = 8,
    activeCount: Int = 2,
    nearbyBinsCount: Int = 12,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(score) {
        animatedProgress.animateTo(
            targetValue = score / 100f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    val trackColor = if (isDark) Color(0x33334155) else Color(0x33CBD5E1)
    val ringGradient = Brush.sweepGradient(
        colors = listOf(
            Emerald400,
            Cyan400,
            Emerald500,
            Emerald400
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular Gauge Canvas
        Box(
            modifier = Modifier.size(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                val strokeWidth = 14.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Background Track Arc (270 degrees)
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Foreground Progress Arc
                val currentSweep = 270f * animatedProgress.value
                if (currentSweep > 0f) {
                    drawArc(
                        brush = ringGradient,
                        startAngle = 135f,
                        sweepAngle = currentSweep,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Center Content: Score, Cleanliness Label, Trend
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${(animatedProgress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                )

                Text(
                    text = "Cleanliness Score",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tiny Trend Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x3310B981) else Color(0x2610B981))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = trendPercent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Emerald400 else Emerald500
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Three Informational Mini Pills Around Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoMetricChip(
                count = "$resolvedCount",
                label = "Resolved",
                accentColor = Emerald400
            )

            InfoMetricChip(
                count = "$activeCount",
                label = "Active Issues",
                accentColor = Cyan400
            )

            InfoMetricChip(
                count = "$nearbyBinsCount",
                label = "Nearby Bins",
                accentColor = Color(0xFFA78BFA) // Light Purple
            )
        }
    }
}

@Composable
private fun InfoMetricChip(
    count: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x331E293B) else Color(0x40F1F5F9))
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x3394A3B8) else Color(0x33CBD5E1),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
            )
        )
    }
}
