@file:Suppress("FunctionName")

package com.wasteroute.app.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.wasteroute.app.model.UiPerformanceMode
import com.wasteroute.app.theme.Cyan400

enum class WorkerNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    TASKS("Tasks", Icons.AutoMirrored.Outlined.Assignment),
    ROUTE("Route", Icons.Outlined.Route),
    QR("QR", Icons.Outlined.QrCode),
    PROFILE("Profile", Icons.Outlined.Person)
}

@Composable
fun WorkerBottomNavigation(
    selectedTab: WorkerNavTab = WorkerNavTab.HOME,
    onTabSelected: (WorkerNavTab) -> Unit = {},
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 16.dp else 4.dp,
                    shape = navShape,
                    ambientColor = if (isDark) Color(0x99000000) else Color(0x330F172A),
                    spotColor = if (isDark) Color(0x66000000) else Color(0x330284C7)
                )
                .clip(navShape)
                .background(
                    brush = if (isDark) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xF00F172A),
                                Color(0xE61E293B)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xF7FFFFFF),
                                Color(0xEEF8FAFC)
                            )
                        )
                    }
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            if (isDark) Color(0x4D38BDF8) else Color(0x660284C7),
                            if (isDark) Color(0x263B82F6) else Color(0x403B82F6),
                            if (isDark) Color(0x1AFFFFFF) else Color(0x20CBD5E1)
                        )
                    ),
                    shape = navShape
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkerNavTab.entries.forEach { tab ->
                    WorkerNavTabItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkerNavTabItem(
    tab: WorkerNavTab,
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
        label = "worker_tab_offset"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "worker_tab_scale"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = if (isSelected) {
                if (isDark) Cyan400 else Color(0xFF0284C7)
            } else {
                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            },
            modifier = Modifier
                .offset(y = iconOffset)
                .scale(scale)
                .size(22.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = tab.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) {
                    if (isDark) Cyan400 else Color(0xFF0284C7)
                } else {
                    if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                }
            )
        )

        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    brush = if (isSelected) Brush.horizontalGradient(listOf(Cyan400, Color(0xFF3B82F6)))
                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                )
        )
    }
}
