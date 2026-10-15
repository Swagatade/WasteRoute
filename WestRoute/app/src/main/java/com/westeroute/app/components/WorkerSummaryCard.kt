package com.westeroute.app.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Emerald400

@Composable
fun WorkerSummaryCard(
    assignedCount: Int = 8,
    completedCount: Int = 3,
    inProgressCount: Int = 1,
    remainingCount: Int = 4,
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "TODAY'S WORK",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Assigned (Blue Accent)
            SummaryMetricTile(
                count = String.format("%02d", assignedCount),
                label = "Assigned",
                countColor = Color(0xFF60A5FA), // Blue 400
                bgColor = if (isDark) Color(0x261E3A8A) else Color(0xFFEFF6FF),
                borderColor = if (isDark) Color(0x333B82F6) else Color(0x403B82F6),
                performanceMode = performanceMode,
                modifier = Modifier.weight(1f)
            )

            // 2. Completed (Emerald Accent)
            SummaryMetricTile(
                count = String.format("%02d", completedCount),
                label = "Completed",
                countColor = Emerald400,
                bgColor = if (isDark) Color(0x26064E3B) else Color(0xFFECFDF5),
                borderColor = if (isDark) Color(0x3310B981) else Color(0x4010B981),
                performanceMode = performanceMode,
                modifier = Modifier.weight(1f)
            )

            // 3. In Progress (Amber Accent)
            SummaryMetricTile(
                count = String.format("%02d", inProgressCount),
                label = "In Progress",
                countColor = Color(0xFFFBBF24), // Amber 400
                bgColor = if (isDark) Color(0x2678350F) else Color(0xFFFFFBEB),
                borderColor = if (isDark) Color(0x33F59E0B) else Color(0x40F59E0B),
                performanceMode = performanceMode,
                modifier = Modifier.weight(1f)
            )

            // 4. Remaining (Neutral Accent)
            SummaryMetricTile(
                count = String.format("%02d", remainingCount),
                label = "Remaining",
                countColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                bgColor = if (isDark) Color(0x261E293B) else Color(0xFFF1F5F9),
                borderColor = if (isDark) Color(0x33475569) else Color(0x4094A3B8),
                performanceMode = performanceMode,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricTile(
    count: String,
    label: String,
    countColor: Color,
    bgColor: Color,
    borderColor: Color,
    performanceMode: UiPerformanceMode,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 6.dp else 1.dp,
                shape = shape,
                ambientColor = Color(0x1A000000),
                spotColor = Color(0x1A000000)
            )
            .clip(shape)
            .background(bgColor)
            .border(width = 1.2.dp, color = borderColor, shape = shape)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = count,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = countColor
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            )
        }
    }
}
