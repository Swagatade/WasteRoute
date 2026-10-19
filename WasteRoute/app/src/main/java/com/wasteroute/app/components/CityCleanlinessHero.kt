package com.westeroute.app.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

@Composable
fun CityCleanlinessHero(
    score: Int = 82,
    resolvedCount: Int = 8,
    activeCount: Int = 2,
    nearbyBinsCount: Int = 12,
    trendPercent: String = "↑ 6% this week",
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        level = GlassSurfaceLevel.PRIMARY,
        shape = RoundedCornerShape(32.dp),
        performanceMode = performanceMode
    ) {
        // Abstract City Map Pattern (Canvas)
        Canvas(modifier = Modifier.matchParentSize()) {
            val roadColor = if (isDark) Color(0x1434D399) else Color(0x14059669)
            val dotColor = if (isDark) Color(0x2638BDF8) else Color(0x260284C7)

            // Abstract road curved lines
            val path1 = Path().apply {
                moveTo(0f, size.height * 0.3f)
                cubicTo(
                    size.width * 0.3f, size.height * 0.1f,
                    size.width * 0.6f, size.height * 0.6f,
                    size.width, size.height * 0.4f
                )
            }
            drawPath(
                path = path1,
                color = roadColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            val path2 = Path().apply {
                moveTo(0f, size.height * 0.7f)
                cubicTo(
                    size.width * 0.4f, size.height * 0.9f,
                    size.width * 0.7f, size.height * 0.3f,
                    size.width, size.height * 0.8f
                )
            }
            drawPath(
                path = path2,
                color = roadColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Abstract city map nodes / dots
            drawCircle(color = dotColor, radius = 5.dp.toPx(), center = Offset(size.width * 0.15f, size.height * 0.25f))
            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = Offset(size.width * 0.85f, size.height * 0.35f))
            drawCircle(color = dotColor, radius = 6.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.75f))
            drawCircle(color = dotColor, radius = 4.dp.toPx(), center = Offset(size.width * 0.2f, size.height * 0.8f))
        }

        // Hero Card Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, start = 20.dp, end = 20.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Labels
            Text(
                text = "YOUR NEIGHBORHOOD",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp,
                    color = if (isDark) Emerald400 else Color(0xFF059669)
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Cleaner today than yesterday",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Circular Cleanliness Gauge
            CircularCleanlinessIndicator(
                score = score,
                trendPercent = trendPercent,
                resolvedCount = resolvedCount,
                activeCount = activeCount,
                nearbyBinsCount = nearbyBinsCount
            )
        }
    }
}
