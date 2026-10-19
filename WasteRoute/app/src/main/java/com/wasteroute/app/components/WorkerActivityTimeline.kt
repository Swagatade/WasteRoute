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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

data class WorkerActivityItem(
    val title: String,
    val location: String,
    val timestamp: String,
    val nodeColor: Color
)

@Composable
fun WorkerActivityTimeline(
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    val activities = listOf(
        WorkerActivityItem(
            title = "Task #WR-2048 started",
            location = "SRMIST Main Gate",
            timestamp = "12:14 PM",
            nodeColor = Cyan400
        ),
        WorkerActivityItem(
            title = "Arrived at location",
            location = "SRMIST Main Gate",
            timestamp = "12:08 PM",
            nodeColor = Color(0xFF38BDF8)
        ),
        WorkerActivityItem(
            title = "Task #WR-2046 completed",
            location = "Kattankulathur Railway Station",
            timestamp = "11:24 AM",
            nodeColor = Emerald400
        ),
        WorkerActivityItem(
            title = "Household collection verified",
            location = "Potheri Residential Area",
            timestamp = "10:52 AM",
            nodeColor = Color(0xFFA78BFA)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "RECENT ACTIVITY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 8.dp else 1.dp,
                    shape = shape,
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000)
                )
                .clip(shape)
                .background(if (isDark) Color(0xF00F172A) else Color(0xF8FFFFFF))
                .border(
                    width = 1.2.dp,
                    color = if (isDark) Color(0x3394A3B8) else Color(0x40CBD5E1),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                activities.forEachIndexed { index, activity ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(activity.nodeColor)
                            )

                            if (index < activities.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(30.dp)
                                        .background(if (isDark) Color(0x3364748B) else Color(0x33CBD5E1))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = if (index == activities.size - 1) 0.dp else 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activity.title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                )
                                Text(
                                    text = activity.timestamp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                )
                            }
                            Text(
                                text = "📍 ${activity.location}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
