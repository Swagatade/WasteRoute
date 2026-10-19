package com.wasteroute.app.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wasteroute.app.R
import com.wasteroute.app.theme.Cyan400
import com.wasteroute.app.theme.Emerald400
import com.wasteroute.app.theme.LocalPremiumMode
import kotlinx.coroutines.delay

private const val LOGO_GLOW_DELAY_MILLIS = 480L

@Composable
fun WasteRouteLogo(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    showGlow: Boolean = true
) {
    val isPremium = LocalPremiumMode.current

    var glowReady by remember { mutableStateOf(false) }
    LaunchedEffect(showGlow, isPremium) {
        if (showGlow && isPremium) {
            // Let the logo image and opening motion settle before creating the
            // continuously animated glow layer.
            delay(LOGO_GLOW_DELAY_MILLIS)
            glowReady = true
        } else {
            glowReady = false
        }
    }

    val glowAlpha: State<Float>? = if (glowReady && showGlow && isPremium) {
        val infiniteTransition = rememberInfiniteTransition(label = "logo_glow")
        infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_alpha"
        )
    } else {
        null
    }

    val cornerRadius = size * 0.28f
    val shape = RoundedCornerShape(cornerRadius)
    val glowBrush = remember(size) {
        Brush.radialGradient(
            colors = listOf(
                Emerald400.copy(alpha = 0.7f),
                Cyan400.copy(alpha = 0.4f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier.size(size + if (showGlow && isPremium) 16.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow in premium mode
        if (showGlow && isPremium) {
            Box(
                modifier = Modifier
                    .size(size + 16.dp)
                    .graphicsLayer {
                        // Graphics-layer alpha updates do not recompose the
                        // logo image or rebuild its gradients every frame.
                        alpha = glowAlpha?.value ?: 0.65f
                    }
                    .background(brush = glowBrush)
            )
        }

        // Logo Image Container
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (isPremium) 8.dp else 4.dp,
                    shape = shape,
                    spotColor = Emerald400.copy(alpha = 0.35f)
                )
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F1E36),
                            Color(0xFF070B14)
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Emerald400.copy(alpha = 0.7f),
                            Cyan400.copy(alpha = 0.4f),
                            Color(0x33FFFFFF)
                        )
                    ),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.wasteroute_logo),
                contentDescription = "WasteRoute Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
            )
        }
    }
}
