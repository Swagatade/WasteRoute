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

@Composable
fun ImpactMetrics(
    reportsResolved: String = "08",
    collectionsVerified: String = "03",
    wasteHandled: String = "24 kg",
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Title
        Text(
            text = "YOUR IMPACT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Emerald400 else Color(0xFF059669)
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // 3 Compact Metric Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                value = reportsResolved,
                title = "Resolved",
                subtitle = "Reports",
                accentColor = Emerald400,
                modifier = Modifier.weight(1f),
                performanceMode = performanceMode
            )

            MetricCard(
                value = collectionsVerified,
                title = "Verified",
                subtitle = "Pickups",
                accentColor = Cyan400,
                modifier = Modifier.weight(1f),
                performanceMode = performanceMode
            )

            MetricCard(
                value = wasteHandled,
                title = "Waste",
                subtitle = "Cleaned",
                accentColor = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f),
                performanceMode = performanceMode
            )
        }

        // Civic Contribution Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0x3310B981) else Color(0x2010B981))
                .border(
                    1.dp,
                    if (isDark) Color(0x4D34D399) else Color(0x40059669),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌱", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Cleaner City Contribution",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    )
                    Text(
                        text = "You're actively helping improve sanitation in your neighborhood.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    performanceMode: UiPerformanceMode,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 4.dp else 1.dp,
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
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            )
            Text(
                text = subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            )
        }
    }
}
