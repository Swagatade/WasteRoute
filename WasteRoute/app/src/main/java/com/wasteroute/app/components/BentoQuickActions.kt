package com.westeroute.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode

@Composable
fun BentoQuickActions(
    onMyComplaintsClick: () -> Unit = {},
    onNearbyBinsClick: () -> Unit = {},
    onScanQrClick: () -> Unit = {},
    onCityUpdatesClick: () -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title
        Text(
            text = "SMART ACTIONS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Card 1: My Complaints (Full Width, Blue -> Indigo Gradient Accent)
        BentoCard(
            title = "My Complaints",
            subtitle = "12 total · 2 active in progress",
            icon = Icons.Outlined.Description,
            gradientColors = listOf(
                Color(0x333B82F6), // Blue 500
                Color(0x266366F1)  // Indigo 500
            ),
            accentColor = Color(0xFF60A5FA),
            onClick = onMyComplaintsClick,
            performanceMode = performanceMode
        )

        // Row for Card 2 & 3 (Asymmetric Split)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 2: Nearby Bins (Teal -> Emerald)
            BentoCompactCard(
                title = "Nearby Bins",
                subtitle = "12 around you",
                icon = Icons.Outlined.DeleteOutline,
                gradientColors = listOf(
                    Color(0x3310B981),
                    Color(0x2606B6D4)
                ),
                accentColor = Color(0xFF34D399),
                modifier = Modifier.weight(1f),
                onClick = onNearbyBinsClick,
                performanceMode = performanceMode
            )

            // Card 3: Scan QR (Purple -> Cyan)
            BentoCompactCard(
                title = "Scan QR",
                subtitle = "Verify pickup",
                icon = Icons.Outlined.QrCodeScanner,
                gradientColors = listOf(
                    Color(0x338B5CF6),
                    Color(0x2606B6D4)
                ),
                accentColor = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f),
                onClick = onScanQrClick,
                performanceMode = performanceMode
            )
        }

        // Card 4: City Updates (Orange -> Coral)
        BentoCard(
            title = "Campus & Ward Updates",
            subtitle = "3 new updates in SRM KTR Green Ward 04",
            icon = Icons.Outlined.Campaign,
            gradientColors = listOf(
                Color(0x33F97316), // Orange
                Color(0x26F43F5E)  // Rose
            ),
            accentColor = Color(0xFFFB923C),
            onClick = onCityUpdatesClick,
            performanceMode = performanceMode
        )
    }
}

@Composable
private fun BentoCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    accentColor: Color,
    onClick: () -> Unit,
    performanceMode: UiPerformanceMode,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 5.dp else 1.dp,
                shape = shape,
                ambientColor = Color(0x22000000),
                spotColor = Color(0x22000000)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            gradientColors[0],
                            Color(0xCC0F172A)
                        )
                    } else {
                        listOf(
                            gradientColors[0],
                            Color(0xF5FFFFFF)
                        )
                    }
                )
            )
            .border(
                width = 1.2.dp,
                color = if (isDark) Color(0x3394A3B8) else Color(0x40CBD5E1),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                role = Role.Button,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                )
                Text(
                    text = subtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BentoCompactCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    accentColor: Color,
    onClick: () -> Unit,
    performanceMode: UiPerformanceMode,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 5.dp else 1.dp,
                shape = shape,
                ambientColor = Color(0x22000000),
                spotColor = Color(0x22000000)
            )
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(
                            gradientColors[0],
                            Color(0xCC0F172A)
                        )
                    } else {
                        listOf(
                            gradientColors[0],
                            Color(0xF5FFFFFF)
                        )
                    }
                )
            )
            .border(
                width = 1.2.dp,
                color = if (isDark) Color(0x3394A3B8) else Color(0x40CBD5E1),
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                role = Role.Button,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            )
        }
    }
}
