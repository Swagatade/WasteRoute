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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

data class RouteStop(
    val title: String,
    val subtitle: String,
    val distance: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean
)

@Composable
fun RouteTimeline(
    stopsRemaining: Int = 4,
    estimatedCompletion: String = "03:40 PM",
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    val stops = listOf(
        RouteStop(
            title = "SRM University Campus",
            subtitle = "Depot Starting Point",
            distance = "0 m",
            isCompleted = true,
            isCurrent = false
        ),
        RouteStop(
            title = "SRMIST Main Gate",
            subtitle = "Current Task · #WR-2048",
            distance = "450 m",
            isCompleted = false,
            isCurrent = true
        ),
        RouteStop(
            title = "SRM Tech Park",
            subtitle = "Next Task · #WR-2050",
            distance = "850 m",
            isCompleted = false,
            isCurrent = false
        ),
        RouteStop(
            title = "SRM Hostel Zone",
            subtitle = "Assigned Point · #WR-2053",
            distance = "1.1 km",
            isCompleted = false,
            isCurrent = false
        ),
        RouteStop(
            title = "SRM Hospital Area",
            subtitle = "Community Point · #WR-2057",
            distance = "1.2 km",
            isCompleted = false,
            isCurrent = false
        ),
        RouteStop(
            title = "Potheri Main Road",
            subtitle = "Urgent Task · #WR-2055",
            distance = "1.5 km",
            isCompleted = false,
            isCurrent = false
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "TODAY'S ROUTE",
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
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 10.dp else 2.dp,
                    shape = shape,
                    ambientColor = if (isDark) Color(0x99000000) else Color(0x33000000),
                    spotColor = if (isDark) Color(0x66000000) else Color(0x333B82F6)
                )
                .clip(shape)
                .background(if (isDark) Color(0xF00F172A) else Color(0xF8FFFFFF))
                .border(
                    width = 1.2.dp,
                    color = if (isDark) Color(0x3338BDF8) else Color(0x40CBD5E1),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                stops.forEachIndexed { index, stop ->
                    RouteStopRow(
                        stop = stop,
                        isLast = index == stops.size - 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer: stops remaining & ETA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0x331E293B) else Color(0x33CBD5E1))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$stopsRemaining stops remaining",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Cyan400 else Color(0xFF0284C7)
                            )
                        )
                        Text(
                            text = "Est. Finish $estimatedCompletion",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStopRow(
    stop: RouteStop,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Node Dot & Connector Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (stop.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Emerald400),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color(0xFF090D16),
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (stop.isCurrent) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Cyan400.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Cyan400)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(1.4.dp, if (isDark) Color(0x4D94A3B8) else Color(0x40CBD5E1), CircleShape)
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(30.dp)
                        .background(
                            if (stop.isCompleted) Emerald400 else if (isDark) Color(0x3364748B) else Color(0x33CBD5E1)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text & Distance
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stop.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 13.5.sp,
                        fontWeight = if (stop.isCurrent) FontWeight.Black else FontWeight.Bold,
                        color = if (stop.isCurrent) (if (isDark) Cyan400 else Color(0xFF0284C7)) else (if (isDark) Color.White else Color(0xFF0F172A))
                    )
                )
                Text(
                    text = stop.distance,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                )
            }

            Text(
                text = stop.subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                )
            )
        }
    }
}
