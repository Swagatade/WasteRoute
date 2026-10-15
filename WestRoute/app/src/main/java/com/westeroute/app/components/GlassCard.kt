package com.westeroute.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.westeroute.app.theme.LocalGlassStyle

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp? = null,
    maxWidth: Dp = 420.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassStyle = LocalGlassStyle.current
    val actualCornerRadius = cornerRadius ?: glassStyle.cornerRadius
    val shape = RoundedCornerShape(actualCornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = maxWidth)
            .shadow(
                elevation = glassStyle.elevation,
                shape = shape,
                ambientColor = Color(0x4D000000),
                spotColor = Color(0x4010B981)
            )
            .clip(shape)
            .background(brush = glassStyle.backgroundBrush)
            .border(
                width = glassStyle.borderWidth,
                brush = glassStyle.borderBrush,
                shape = shape
            ),
        contentAlignment = Alignment.TopStart
    ) {
        // 1. Ambient liquid prism refraction glow
        glassStyle.liquidPrismGlowBrush?.let { glowBrush ->
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(glowBrush)
            )
        }

        // 2. Bottom curved liquid meniscus refraction
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassStyle.bottomMeniscusBrush)
        )

        // 3. Top convex specular light reflection (-90° light angle, 80% intensity)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassStyle.topSheenBrush)
        )

        // 4. Razor-sharp unblurred child content
        content()
    }
}
