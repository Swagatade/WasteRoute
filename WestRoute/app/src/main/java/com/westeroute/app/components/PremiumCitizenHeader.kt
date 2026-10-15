package com.westeroute.app.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.Emerald500

@Suppress("FunctionName")
@Composable
fun PremiumCitizenHeader(
    modifier: Modifier = Modifier,
    userName: String = "Citizen",
    location: String = "SRM KTR, Kattankulathur",
    hasUnreadNotifications: Boolean = true,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()

    val notifInteraction = remember { MutableInteractionSource() }
    val notifPressed by notifInteraction.collectIsPressedAsState()
    val notifScale by animateFloatAsState(
        targetValue = if (notifPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "notif_scale"
    )

    val profileInteraction = remember { MutableInteractionSource() }
    val profilePressed by profileInteraction.collectIsPressedAsState()
    val profileScale by animateFloatAsState(
        targetValue = if (profilePressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "profile_scale"
    )

    val headerGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x33064E3B), // Deep Teal / Emerald blob
                Color(0x260E7490), // Cyan 700 blob
                Color.Transparent
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x2610B981), // Light Emerald
                Color(0x1A06B6D4), // Light Cyan
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(headerGradient)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Greeting, Name, Location Pill, Activity Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Good Evening 👋",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // User Name
                Text(
                    text = userName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Location Pill + Activity Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Translucent Location Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDark) Color(0x33334155) else Color(0x33CBD5E1)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0x4D94A3B8) else Color(0x4094A3B8),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📍 $location",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Emerald400 else Emerald500
                            )
                        )
                    }
                }
            }

            // Right Actions: Notification Bell + Circular Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Frosted Notification Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(notifScale)
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x33000000),
                            spotColor = Color(0x33000000)
                        )
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0x4D1E293B) else Color(0xF2FFFFFF)
                        )
                        .border(
                            width = 1.2.dp,
                            color = if (isDark) Color(0x4D94A3B8) else Color(0x4DCBD5E1),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = notifInteraction,
                            indication = ripple(bounded = true, radius = 22.dp),
                            role = Role.Button,
                            onClick = onNotificationClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )

                    // Small Coral Notification Dot
                    if (hasUnreadNotifications) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF43F5E)) // Coral Red
                                .border(1.5.dp, if (isDark) Color(0xFF1E293B) else Color.White, CircleShape)
                        )
                    }
                }

                // Profile Avatar with Gradient Ring
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .scale(profileScale)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Emerald400,
                                    Cyan400
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = profileInteraction,
                            indication = ripple(bounded = true, radius = 23.dp),
                            role = Role.Button,
                            onClick = onProfileClick
                        )
                        .padding(2.5.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF059669),
                                    Color(0xFF0284C7)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
