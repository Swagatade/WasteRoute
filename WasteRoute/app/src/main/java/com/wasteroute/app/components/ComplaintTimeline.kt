package com.wasteroute.app.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasteroute.app.theme.Cyan400
import com.wasteroute.app.theme.Emerald400
import com.wasteroute.app.theme.Emerald500

enum class StepStatus {
    COMPLETED,
    IN_PROGRESS,
    PENDING
}

data class TimelineStep(
    val title: String,
    val timestamp: String? = null,
    val status: StepStatus
)

@Composable
fun ComplaintTimeline(
    steps: List<TimelineStep> = listOf(
        TimelineStep("Reported", "4:15 PM", StepStatus.COMPLETED),
        TimelineStep("Worker Assigned", "4:20 PM", StepStatus.COMPLETED),
        TimelineStep("Cleaning in Progress", "Live", StepStatus.IN_PROGRESS),
        TimelineStep("Completed", "Est. 5:00 PM", StepStatus.PENDING)
    ),
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val pulseTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        steps.forEachIndexed { index, step ->
            val isLast = index == steps.size - 1

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Node Icon + Connecting Line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step.status) {
                        StepStatus.COMPLETED -> {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF090D16),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        StepStatus.IN_PROGRESS -> {
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsing Outer Aura
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Cyan400.copy(alpha = 0.35f))
                                )
                                // Solid Center Dot
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Cyan400)
                                )
                            }
                        }
                        StepStatus.PENDING -> {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isDark) Color(0x6664748B) else Color(0x6694A3B8),
                                        shape = CircleShape
                                    )
                                    .background(if (isDark) Color(0x221E293B) else Color(0x22CBD5E1))
                            )
                        }
                    }

                    // Vertical Line
                    if (!isLast) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(26.dp)
                                .background(
                                    if (step.status == StepStatus.COMPLETED) {
                                        Emerald500
                                    } else {
                                        if (isDark) Color(0x3364748B) else Color(0x33CBD5E1)
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Content: Title + Timestamp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (isLast) 0.dp else 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.5.sp,
                            fontWeight = if (step.status == StepStatus.IN_PROGRESS) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = when (step.status) {
                                StepStatus.COMPLETED -> if (isDark) Color.White else Color(0xFF0F172A)
                                StepStatus.IN_PROGRESS -> if (isDark) Cyan400 else Color(0xFF0284C7)
                                StepStatus.PENDING -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            }
                        )
                    )

                    if (step.timestamp != null) {
                        Text(
                            text = step.timestamp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }
                }
            }
        }
    }
}
