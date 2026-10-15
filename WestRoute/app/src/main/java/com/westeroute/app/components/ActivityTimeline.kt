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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.Emerald500

data class ActivityItemData(
    val title: String,
    val description: String,
    val timestamp: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun ActivityTimeline(
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    val todayItems = listOf(
        ActivityItemData(
            title = "Complaint Updated",
            description = "Roadside Garbage · Cleaning in progress",
            timestamp = "4:22 PM",
            icon = Icons.Outlined.PendingActions,
            color = Cyan400
        ),
        ActivityItemData(
            title = "QR Verified",
            description = "Household collection confirmed with Team 03",
            timestamp = "8:14 AM",
            icon = Icons.Outlined.QrCode,
            color = Color(0xFFA78BFA)
        )
    )

    val yesterdayItems = listOf(
        ActivityItemData(
            title = "Complaint Completed",
            description = "Overflowing Bin · Java Block verified resolved",
            timestamp = "Yesterday, 3:45 PM",
            icon = Icons.Outlined.CheckCircle,
            color = Emerald400
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section Header
        Text(
            text = "RECENT CITIZEN ACTIVITY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 5.dp else 1.dp,
                    shape = shape,
                    ambientColor = Color(0x22000000),
                    spotColor = Color(0x22000000)
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
                // Today Label
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Cyan400 else Color(0xFF0284C7)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                todayItems.forEachIndexed { index, item ->
                    TimelineItemRow(
                        item = item,
                        isLast = false
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Yesterday Label
                Text(
                    text = "Yesterday",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                yesterdayItems.forEachIndexed { index, item ->
                    TimelineItemRow(
                        item = item,
                        isLast = true
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineItemRow(
    item: ActivityItemData,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Dot and Connector Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(item.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(13.dp)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(if (isDark) Color(0x3364748B) else Color(0x33CBD5E1))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                )
                Text(
                    text = item.timestamp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            )
        }
    }
}
