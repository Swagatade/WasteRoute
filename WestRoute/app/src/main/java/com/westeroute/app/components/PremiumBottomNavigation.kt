package com.westeroute.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.Emerald500

enum class CitizenNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    COMPLAINTS("Complaints", Icons.Outlined.Description),
    MAP("Map", Icons.Outlined.Map),
    PROFILE("Profile", Icons.Outlined.Person)
}

@Composable
fun PremiumBottomNavigation(
    selectedTab: CitizenNavTab = CitizenNavTab.HOME,
    onTabSelected: (CitizenNavTab) -> Unit = {},
    onReportClick: () -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val navShape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Nav Pill Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 16.dp else 4.dp,
                    shape = navShape,
                    ambientColor = if (isDark) Color(0x99000000) else Color(0x330F172A),
                    spotColor = if (isDark) Color(0x66000000) else Color(0x33059669)
                )
                .clip(navShape)
                .background(
                    brush = if (isDark) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xF00F172A),
                                Color(0xE61E293B)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xF7FFFFFF),
                                Color(0xEEF8FAFC)
                            )
                        )
                    }
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isDark) Color(0x4D34D399) else Color(0x66059669),
                            if (isDark) Color(0x2638BDF8) else Color(0x400284C7),
                            if (isDark) Color(0x1AFFFFFF) else Color(0x20CBD5E1)
                        )
                    ),
                    shape = navShape
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Home
                NavTabItem(
                    tab = CitizenNavTab.HOME,
                    isSelected = selectedTab == CitizenNavTab.HOME,
                    onClick = { onTabSelected(CitizenNavTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 2: Complaints
                NavTabItem(
                    tab = CitizenNavTab.COMPLAINTS,
                    isSelected = selectedTab == CitizenNavTab.COMPLAINTS,
                    onClick = { onTabSelected(CitizenNavTab.COMPLAINTS) },
                    modifier = Modifier.weight(1f)
                )

                // Spacer for the center elevated Report Button
                Spacer(modifier = Modifier.width(64.dp))

                // Tab 3: Map
                NavTabItem(
                    tab = CitizenNavTab.MAP,
                    isSelected = selectedTab == CitizenNavTab.MAP,
                    onClick = { onTabSelected(CitizenNavTab.MAP) },
                    modifier = Modifier.weight(1f)
                )

                // Tab 4: Profile
                NavTabItem(
                    tab = CitizenNavTab.PROFILE,
                    isSelected = selectedTab == CitizenNavTab.PROFILE,
                    onClick = { onTabSelected(CitizenNavTab.PROFILE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Elevated Center REPORT Action Button (Floating & Overlapping)
        Box(
            modifier = Modifier
                .offset(y = (-18).dp)
                .size(60.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x4D059669),
                    spotColor = Emerald400.copy(alpha = 0.65f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Emerald500,
                            Cyan400
                        )
                    )
                )
                .border(
                    width = 2.5.dp,
                    color = if (isDark) Color(0xFF0F172A) else Color.White,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = Color.White),
                    role = Role.Button,
                    onClick = onReportClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Report Waste Action",
                tint = Color(0xFF090D16),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun NavTabItem(
    tab: CitizenNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val iconOffset by animateDpAsState(
        targetValue = if (isSelected) (-2).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tab_offset"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tab_scale"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (isSelected) {
                if (isDark) Emerald400 else Color(0xFF059669)
            } else {
                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            },
            modifier = Modifier
                .offset(y = iconOffset)
                .scale(scale)
                .size(22.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Selected Label
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = tab.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Emerald400 else Color(0xFF059669)
                )
            )
        }

        // Indicator stretch under active icon
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Emerald400,
                                Cyan400
                            )
                        )
                    )
            )
        }
    }
}
