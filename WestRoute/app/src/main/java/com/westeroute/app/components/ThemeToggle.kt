package com.westeroute.app.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.westeroute.app.theme.Amber400
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

@Composable
fun ThemeToggle(
    isDark: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDark) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "toggle_rotation"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isDark) Cyan400 else Amber400,
        animationSpec = tween(300),
        label = "icon_tint"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isDark) Color(0x331E293B) else Color(0xD9FFFFFF)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        if (isDark) Cyan400.copy(alpha = 0.5f) else Color(0x66CBD5E1),
                        if (isDark) Emerald400.copy(alpha = 0.3f) else Color(0x3394A3B8)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = onToggle
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "theme_icon"
        ) { dark ->
            Icon(
                imageVector = if (dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                contentDescription = if (dark) "Switch to light mode" else "Switch to dark mode",
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation)
            )
        }
    }
}
