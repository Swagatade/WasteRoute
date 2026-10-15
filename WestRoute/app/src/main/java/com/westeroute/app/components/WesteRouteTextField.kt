@file:Suppress("FunctionName")

package com.westeroute.app.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Cyan500
import com.westeroute.app.theme.Emerald400
import com.westeroute.app.theme.Emerald500
import com.westeroute.app.theme.LocalPremiumMode
import com.westeroute.app.theme.NeutralGray400
import com.westeroute.app.theme.NeutralGray500
import com.westeroute.app.theme.NeutralGray600

@Composable
fun WesteRouteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val isPremium = LocalPremiumMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val fieldShape = RoundedCornerShape(16.dp)
    val errorColor = Color(0xFFEF4444)

    // Animated icon tint
    val iconTint by animateColorAsState(
        targetValue = when {
            isError -> errorColor
            isFocused -> Emerald400
            isDark -> NeutralGray400
            else -> NeutralGray500
        },
        animationSpec = tween(250),
        label = "icon_tint"
    )

    // Border styling
    val borderBrush = when {
        isError -> SolidColor(errorColor)
        isFocused -> Brush.linearGradient(
            colors = listOf(
                Emerald400,
                Cyan400
            )
        )
        else -> SolidColor(
            if (isDark) Color(0x3394A3B8) else Color(0x3364748B)
        )
    }

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || isError) 1.5.dp else 1.dp,
        animationSpec = tween(200),
        label = "border_width"
    )

    val backgroundColor = if (isDark) {
        if (isFocused) Color(0x331E293B) else Color(0x1F1E293B)
    } else {
        if (isFocused) Color(0xF5FFFFFF) else Color(0xD9FFFFFF)
    }

    val textColor = MaterialTheme.colorScheme.onSurface
    val placeholderColor = if (isDark) NeutralGray500 else NeutralGray400

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (isFocused && isPremium) 4.dp else 0.dp,
                    shape = fieldShape,
                    spotColor = if (isError) errorColor.copy(alpha = 0.25f) else Emerald400.copy(alpha = 0.25f)
                )
                .clip(fieldShape)
                .background(backgroundColor)
                .border(
                    width = borderWidth,
                    brush = borderBrush,
                    shape = fieldShape
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Icon
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Text Input / Placeholder container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = placeholderColor
                            )
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = MaterialTheme.typography.bodyLarge.fontWeight
                        ),
                        singleLine = singleLine,
                        enabled = enabled,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        cursorBrush = SolidColor(Emerald400)
                    )
                }

                // Trailing Icon (e.g. Password Toggle)
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = errorColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

