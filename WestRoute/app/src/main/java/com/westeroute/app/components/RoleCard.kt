package com.westeroute.app.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.PersonPinCircle
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
import com.westeroute.app.model.UserRole
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Cyan500
import com.westeroute.app.theme.ElectricBlue400
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.Emerald500
import com.westeroute.app.theme.Emerald600
import com.westeroute.app.theme.LocalGlassStyle

@Composable
fun RoleCard(
    role: UserRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val glassStyle = LocalGlassStyle.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "role_scale"
    )

    val arrowOffset by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 350f
        ),
        label = "arrow_offset"
    )

    val cardShape = RoundedCornerShape(26.dp)
    val isCitizen = role == UserRole.CITIZEN

    val primaryAccent = if (isCitizen) {
        if (isDark) Emerald400 else Emerald600
    } else {
        if (isDark) ElectricBlue400 else Color(0xFF2563EB)
    }

    val secondaryAccent = if (isCitizen) Cyan500 else Emerald500

    // Prismatic / Chromatic Dispersion Rainbow Border (White -> Amber -> Emerald -> Cyan -> Blue -> Violet)
    val borderColors = if (isPressed) {
        listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFBBF24),
            primaryAccent.copy(alpha = 0.95f),
            secondaryAccent.copy(alpha = 0.85f),
            Color(0xFFA78BFA),
            Color(0xFFFFFFFF)
        )
    } else {
        if (isDark) {
            listOf(
                Color(0xE6FFFFFF),
                Color(0x80FBBF24),
                primaryAccent.copy(alpha = 0.75f),
                secondaryAccent.copy(alpha = 0.50f),
                Color(0x66A78BFA),
                Color(0x33FFFFFF)
            )
        } else {
            listOf(
                Color(0xFFFFFFFF),
                primaryAccent.copy(alpha = 0.6f),
                secondaryAccent.copy(alpha = 0.4f),
                Color(0x4DA78BFA),
                Color(0x33CBD5E1)
            )
        }
    }

    val icon: ImageVector = if (isCitizen) {
        Icons.Outlined.PersonPinCircle
    } else {
        Icons.Outlined.Engineering
    }

    val titleColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF0F172A)
    val descColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
    val badgeBg = if (isDark) Color(0x4D1E293B) else Color(0xFFE2E8F0)
    val badgeText = if (isDark) Color(0xFFE2E8F0) else Color(0xFF475569)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 16.dp else glassStyle.elevation,
                shape = cardShape,
                ambientColor = Color(0x4D000000),
                spotColor = primaryAccent.copy(alpha = if (isPressed) 0.5f else 0.3f)
            )
            .clip(cardShape)
            .background(brush = glassStyle.backgroundBrush)
            .border(
                width = if (isPressed) 1.6.dp else 1.3.dp,
                brush = Brush.linearGradient(borderColors),
                shape = cardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        // Bottom liquid meniscus refraction
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(cardShape)
                .background(glassStyle.bottomMeniscusBrush)
        )

        // Top convex specular lens reflection
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(cardShape)
                .background(glassStyle.topSheenBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Glow Icon Container
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = if (isDark) 0.28f else 0.18f),
                                    secondaryAccent.copy(alpha = if (isDark) 0.16f else 0.10f)
                                )
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = 0.8f),
                                    secondaryAccent.copy(alpha = 0.5f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Subtitle Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeBg)
                        .border(
                            width = 0.8.dp,
                            color = if (isDark) Color(0x33FFFFFF) else Color(0x33CBD5E1),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = role.subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeText
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title — ExtraBold for crisp glass readability
            Text(
                text = role.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = titleColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description — Medium weight for glass surface legibility
            Text(
                text = role.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.sp,
                    fontSize = 14.sp
                ),
                color = descColor
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Row: "Continue as [Role] →"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = role.actionText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryAccent,
                        letterSpacing = 0.1.sp
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = primaryAccent,
                    modifier = Modifier
                        .size(18.dp)
                        .offset(x = arrowOffset)
                )
            }
        }
    }
}
