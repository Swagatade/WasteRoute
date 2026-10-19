package com.wasteroute.app.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasteroute.app.model.UiPerformanceMode
import com.wasteroute.app.theme.Cyan400
import com.wasteroute.app.theme.Emerald400

@Composable
fun WorkerProgressMetrics(
    tasksCompleted: Int = 3,
    homeCollections: Int = 24,
    photosSubmitted: Int = 8,
    routeKm: Double = 12.6,
    efficiencyPercentage: Int = 76,
    tasksHandledRatio: String = "6 of 8 tasks handled",
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    val animatedEfficiency by animateFloatAsState(
        targetValue = efficiencyPercentage.toFloat() / 100f,
        animationSpec = tween(durationMillis = 1200),
        label = "efficiency_anim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "TODAY'S PROGRESS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 4 Stat Cards in 2x2 Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatTile(
                value = String.format("%02d", tasksCompleted),
                label = "Tasks Completed",
                valueColor = Emerald400,
                modifier = Modifier.weight(1f)
            )
            MetricStatTile(
                value = "$homeCollections",
                label = "Home Collections",
                valueColor = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatTile(
                value = String.format("%02d", photosSubmitted),
                label = "Photos Submitted",
                valueColor = Cyan400,
                modifier = Modifier.weight(1f)
            )
            MetricStatTile(
                value = "$routeKm km",
                label = "Route Covered",
                valueColor = Color(0xFF60A5FA),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Worker Efficiency Ring Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 10.dp else 2.dp,
                    shape = shape,
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000)
                )
                .clip(shape)
                .background(if (isDark) Color(0xF01E293B) else Color(0xF8FFFFFF))
                .border(
                    width = 1.2.dp,
                    color = if (isDark) Color(0x3338BDF8) else Color(0x40CBD5E1),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WORKER EFFICIENCY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            color = Cyan400
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Outstanding Shift Pace",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    )

                    Text(
                        text = tasksHandledRatio,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                        )
                    )
                }

                // Canvas Circular Progress Gauge
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val trackColor = if (isDark) Color(0x33334155) else Color(0x33CBD5E1)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background Track Ring
                        drawArc(
                            color = trackColor,
                            startAngle = 140f,
                            sweepAngle = 260f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Progress Arc Ring
                        drawArc(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF3B82F6), Cyan400, Emerald400)
                            ),
                            startAngle = 140f,
                            sweepAngle = 260f * animatedEfficiency,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$efficiencyPercentage%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = "Pace",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricStatTile(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isDark) Color(0xF01E293B) else Color(0xF8FFFFFF))
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x3394A3B8) else Color(0x40CBD5E1),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            )
        }
    }
}
