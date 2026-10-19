package com.westeroute.app.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.TaskPriority
import com.westeroute.app.model.TaskStatus
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.model.WorkerTask
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

@Composable
fun ActiveTaskCard(
    task: WorkerTask,
    isOnDuty: Boolean = true,
    onNavigateClick: () -> Unit = {},
    onDetailsClick: () -> Unit = {},
    onUpdateStatusClick: () -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(28.dp)

    // Pulse animation for active IN_PROGRESS status dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CURRENT TASK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    color = if (isDark) Cyan400 else Color(0xFF0284C7)
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )

            if (!isOnDuty) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Off Duty Locked",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF43F5E)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 18.dp else 4.dp,
                    shape = cardShape,
                    ambientColor = if (isDark) Color(0x99000000) else Color(0x330F172A),
                    spotColor = if (isDark) Color(0x660284C7) else Color(0x330284C7)
                )
                .clip(cardShape)
                .background(
                    brush = if (isDark) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xF00B132B), // Deep Indigo / Navy
                                Color(0xE60F172A),
                                Color(0xF0064E3B)  // Deep Emerald subtle edge
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xF7FFFFFF),
                                Color(0xEEF0F9FF),
                                Color(0xF2ECFDF5)
                            )
                        )
                    }
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Cyan400.copy(alpha = 0.6f),
                            Emerald400.copy(alpha = 0.4f),
                            Color(0x333B82F6)
                        )
                    ),
                    shape = cardShape
                )
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: Thumbnail + Title + ID + Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Task Photo Thumbnail Box
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF334155)
                                    )
                                )
                            )
                            .border(1.dp, Color(0x4038BDF8), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Task Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            )
                            Text(
                                text = task.id,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = task.location,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Metadata Chips Row: Distance, Priority, Live Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distance
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x3338BDF8) else Color(0x200284C7))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "📏 ${task.distance}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Cyan400 else Color(0xFF0284C7)
                            )
                        )
                    }

                    // Priority Badge (Orange / Amber / Red)
                    val priorityColor = when (task.priority) {
                        TaskPriority.LOW -> Emerald400
                        TaskPriority.MEDIUM -> Color(0xFFF59E0B)
                        TaskPriority.HIGH -> Color(0xFFFB923C) // Orange
                        TaskPriority.URGENT -> Color(0xFFF43F5E) // Coral / Red
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(priorityColor.copy(alpha = 0.18f))
                            .border(1.dp, priorityColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${task.priority.displayName.uppercase()} PRIORITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = priorityColor
                            )
                        )
                    }

                    // Status Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (task.status == TaskStatus.COMPLETED) {
                                    Emerald400.copy(alpha = 0.18f)
                                } else {
                                    Cyan400.copy(alpha = 0.18f)
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (task.status == TaskStatus.IN_PROGRESS) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .scale(pulseScale)
                                        .clip(CircleShape)
                                        .background(Cyan400)
                                )
                            }
                            Text(
                                text = task.status.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (task.status == TaskStatus.COMPLETED) Emerald400 else Cyan400
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Steps Timeline Inside Card
                CurrentTaskStepTimeline(status = task.status)

                Spacer(modifier = Modifier.height(20.dp))

                // Off Duty Warning if inactive
                if (!isOnDuty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x26F43F5E))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Go On Duty to start and update assigned tasks.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF43F5E)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons: Navigate / Task Details / Update Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Secondary Button: Navigate
                    OutlinedButton(
                        onClick = onNavigateClick,
                        enabled = isOnDuty,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.2.dp,
                            color = if (isDark) Color(0x6638BDF8) else Color(0x660284C7)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Cyan400 else Color(0xFF0284C7)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Navigate",
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Primary Button: Update Status
                    Button(
                        onClick = onUpdateStatusClick,
                        enabled = isOnDuty,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Update Status",
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTaskStepTimeline(
    status: TaskStatus,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val steps = listOf(
        "Assigned",
        "Accepted",
        "Cleaning In Progress",
        "Upload Proof",
        "Completed"
    )

    val currentStepIndex = when (status) {
        TaskStatus.PENDING -> 0
        TaskStatus.ASSIGNED -> 1
        TaskStatus.IN_PROGRESS -> 2
        TaskStatus.COMPLETED -> 4
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        steps.forEachIndexed { index, stepName ->
            val isDone = index < currentStepIndex
            val isCurrent = index == currentStepIndex
            val isPending = index > currentStepIndex

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Step Dot / Icon
                if (isDone) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Emerald400),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓",
                            color = Color(0xFF090D16),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Cyan400.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Cyan400)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, if (isDark) Color(0x4D94A3B8) else Color(0x40CBD5E1), CircleShape)
                    )
                }

                // Step Name
                Text(
                    text = stepName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isDone -> if (isDark) Emerald400 else Color(0xFF059669)
                            isCurrent -> if (isDark) Cyan400 else Color(0xFF0284C7)
                            else -> if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        }
                    )
                )
            }
        }
    }
}
