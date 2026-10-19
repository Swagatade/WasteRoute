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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.Emerald500

@Composable
fun NeighborhoodChart(
    weeklyData: List<Float> = listOf(0.35f, 0.55f, 0.85f, 0.70f, 1.0f, 0.45f, 0.90f),
    days: List<String> = listOf("M", "T", "W", "T", "F", "S", "S"),
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEIGHBORHOOD ACTIVITY",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    color = if (isDark) Cyan400 else Color(0xFF0284C7)
                )
            )

            Text(
                text = "Resolved this week",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 6.dp else 1.dp,
                    shape = shape,
                    ambientColor = Color(0x33000000),
                    spotColor = Color(0x33000000)
                )
                .clip(shape)
                .background(if (isDark) Color(0xCC0F172A) else Color(0xF5FFFFFF))
                .border(
                    width = 1.2.dp,
                    color = if (isDark) Color(0x3394A3B8) else Color(0x40CBD5E1),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mini 7-Day Bar Chart Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                ) {
                    val count = weeklyData.size
                    val spacing = size.width / count
                    val barWidth = 22.dp.toPx()
                    val maxHeight = size.height - 12.dp.toPx()

                    weeklyData.forEachIndexed { i, ratio ->
                        val barHeight = maxHeight * ratio * animProgress.value
                        val x = i * spacing + (spacing - barWidth) / 2
                        val y = size.height - barHeight

                        val isToday = i == count - 1
                        val barBrush = if (isToday) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Cyan400,
                                    Emerald500
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Emerald400.copy(alpha = 0.85f),
                                        Color(0x3334D399)
                                    )
                                } else {
                                    listOf(
                                        Emerald500,
                                        Color(0x4D10B981)
                                    )
                                }
                            )
                        }

                        // Draw Bar Track
                        drawRoundRect(
                            color = if (isDark) Color(0x1F64748B) else Color(0x1FCBD5E1),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )

                        // Draw Filled Bar
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days Row Labels (M T W T F S S)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    days.forEachIndexed { i, day ->
                        val isToday = i == days.size - 1
                        Text(
                            text = day,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = if (isToday) FontWeight.Black else FontWeight.SemiBold,
                                color = if (isToday) {
                                    if (isDark) Cyan400 else Color(0xFF0284C7)
                                } else {
                                    if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
