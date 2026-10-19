package com.wasteroute.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasteroute.app.components.RoleCard
import com.wasteroute.app.components.WasteRouteLogo
import com.wasteroute.app.model.UserRole
import com.wasteroute.app.theme.Emerald400
import com.wasteroute.app.theme.Emerald700
import com.wasteroute.app.theme.NeutralGray400

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("FunctionName")
@Composable
fun RoleSelectionScreen(
    onSelectRole: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isSelecting by remember { mutableStateOf(false) }

    fun handleRoleSelect(role: UserRole) {
        if (isSelecting) return
        isSelecting = true
        scope.launch {
            delay(140.milliseconds) // Allow card click spring animation to play first
            onSelectRole(role)
            isSelecting = false
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val isDark = isSystemInDarkTheme()

    // High contrast typography tokens
    val headingColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val bodyColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val badgeTextColor = if (isDark) Emerald400 else Emerald700
    val badgeBg = if (isDark) {
        Brush.horizontalGradient(listOf(Color(0x2E10B981), Color(0x2206B6D4)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFD1FAE5), Color(0xFFCFFAFE)))
    }
    val badgeBorder = if (isDark) Color(0x6634D399) else Color(0x80059669)

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center Content Container
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(350)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(350)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // WasteRoute Identity Logo
                WasteRouteLogo(
                    size = 80.dp,
                    showGlow = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Brand Name
                Text(
                    text = "WasteRoute",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = headingColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Smart Waste Management Subtitle Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(brush = badgeBg)
                        .border(
                            width = 1.dp,
                            color = badgeBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Smart Waste Management",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Main Heading
                Text(
                    text = "Choose how you want to continue",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = headingColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Supporting text
                Text(
                    text = "Select your role to continue to WasteRoute.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Responsive Role Selection Cards
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val isWide = constraints.hasBoundedWidth && maxWidth > 560.dp

                    if (isWide) {
                        // Side-by-side for tablets / foldables / landscape
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                RoleCard(
                                    role = UserRole.CITIZEN,
                                    onClick = { handleRoleSelect(UserRole.CITIZEN) }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                RoleCard(
                                    role = UserRole.WORKER,
                                    onClick = { handleRoleSelect(UserRole.WORKER) }
                                )
                            }
                        }
                    } else {
                        // Vertical stack for standard phones
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RoleCard(
                                role = UserRole.CITIZEN,
                                onClick = { handleRoleSelect(UserRole.CITIZEN) }
                            )

                            RoleCard(
                                role = UserRole.WORKER,
                                onClick = { handleRoleSelect(UserRole.WORKER) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
