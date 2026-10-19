@file:Suppress("FunctionName")

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.material3.CircularProgressIndicator
import com.westeroute.app.theme.Emerald400

private val GoogleIconVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "GoogleIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = PathParser().parsePathString("M12,5c1.5,0 2.9,0.5 4,1.5l3-3C17.2,1.8 14.8,1 12,1 7.5,1 3.7,3.6 1.9,7.3l3.7,2.9C6.5,7.3 9,5 12,5z").toNodes(),
            fill = SolidColor(Color(0xFFEA4335))
        )
        addPath(
            pathData = PathParser().parsePathString("M23.5,12.3c0,-0.8 -0.1,-1.6 -0.2,-2.3H12v4.5h6.5c-0.3,1.5 -1.1,2.8 -2.4,3.7l3.7,2.9c2.2,-2 3.7,-5 3.7,-8.8z").toNodes(),
            fill = SolidColor(Color(0xFF4285F4))
        )
        addPath(
            pathData = PathParser().parsePathString("M5.6,14.8c-0.2,-0.7 -0.4,-1.5 -0.4,-2.3 0,-0.8 0.1,-1.6 0.4,-2.3L1.9,7.3C0.7,9.7 0,10.8 0,12.5s0.7,2.8 1.9,5.2l3.7,-2.9z").toNodes(),
            fill = SolidColor(Color(0xFFFBBC05))
        )
        addPath(
            pathData = PathParser().parsePathString("M12,23c3.2,0 6,-1.1 8,-3l-3.7,-2.9c-1.1,0.7 -2.5,1.2 -4.3,1.2 -3,0 -5.5,-2.3 -6.4,-5.2L1.9,16c1.8,3.7 5.6,7 10.1,7z").toNodes(),
            fill = SolidColor(Color(0xFF34A853))
        )
    }.build()
}

enum class SocialProvider(val label: String, val iconRes: Int = 0) {
    GOOGLE("Continue with Google", 0)
}

@Composable
fun SocialAuthButton(
    provider: SocialProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isLoading && enabled) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "social_scale"
    )

    val shape = RoundedCornerShape(16.dp)
    val backgroundColor = if (isDark) {
        if (isPressed && !isLoading && enabled) Color(0x33334155) else Color(0x1F1E293B)
    } else {
        if (isPressed && !isLoading && enabled) Color(0xFFF1F5F9) else Color(0xD9FFFFFF)
    }

    val borderColor = if (isDark) Color(0x3394A3B8) else Color(0x33CBD5E1)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Emerald400
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connecting to Google...",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                if (provider == SocialProvider.GOOGLE) {
                    Icon(
                        imageVector = GoogleIconVector,
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (provider.iconRes != 0) {
                    Icon(
                        painter = painterResource(id = provider.iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = text ?: provider.label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
