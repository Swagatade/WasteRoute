package com.wasteroute.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BinStatus(val label: String, val color: Color) {
    AVAILABLE("Available", Color(0xFF10B981)),      // Emerald Green
    NEARLY_FULL("Nearly Full", Color(0xFFF59E0B)),  // Amber
    FULL("Full", Color(0xFFEF4444))                 // Red
}

data class BinMarkerData(
    val id: String,
    val name: String,
    val distance: String,
    val status: BinStatus,
    val xPercent: Float, // Relative position on canvas 0.0 - 1.0
    val yPercent: Float
)

@Composable
fun BinMapMarker(
    bin: BinMarkerData,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x33000000),
                spotColor = bin.status.color.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isDark) {
                    if (isSelected) Color(0xFF1E293B) else Color(0xEE0F172A)
                } else {
                    if (isSelected) Color.White else Color(0xEEF8FAFC)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.2.dp,
                color = if (isSelected) bin.status.color else bin.status.color.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = bin.status.color),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Status Color Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(bin.status.color)
            )

            Spacer(modifier = Modifier.width(5.dp))

            // Bin Icon
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = if (isDark) Color.White else Color(0xFF0F172A),
                modifier = Modifier.size(13.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Distance Text
            Text(
                text = bin.distance,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            )
        }
    }
}
