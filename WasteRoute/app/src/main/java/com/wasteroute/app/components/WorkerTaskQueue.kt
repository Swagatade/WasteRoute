@file:Suppress("FunctionName")

package com.wasteroute.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasteroute.app.model.TaskPriority
import com.wasteroute.app.model.TaskStatus
import com.wasteroute.app.model.UiPerformanceMode
import com.wasteroute.app.model.WorkerTask
import com.wasteroute.app.theme.Cyan400
import com.wasteroute.app.theme.Emerald400

@Composable
fun WorkerTaskQueue(
    tasks: List<WorkerTask>,
    onTaskClick: (WorkerTask) -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val filterTabs = listOf("All", "Urgent", "Assigned", "In Progress", "Completed")
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredTasks = remember(selectedFilter, tasks) {
        when (selectedFilter) {
            "Urgent" -> tasks.filter { it.priority == TaskPriority.URGENT }
            "Assigned" -> tasks.filter { it.status == TaskStatus.ASSIGNED }
            "In Progress" -> tasks.filter { it.status == TaskStatus.IN_PROGRESS }
            "Completed" -> tasks.filter { it.status == TaskStatus.COMPLETED }
            else -> tasks
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section Title & Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ASSIGNED TASKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )

            Text(
                text = "${filteredTasks.size} tasks",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Cyan400 else Color(0xFF0284C7)
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterTabs) { filter ->
                val isSelected = filter == selectedFilter

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) {
                                if (isDark) Cyan400 else Color(0xFF0284C7)
                            } else {
                                if (isDark) Color(0x331E293B) else Color(0x33CBD5E1)
                            }
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = filter,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color(0xFF090D16) else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vertical List of Task Cards
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            filteredTasks.forEach { task ->
                WorkerTaskItemCard(
                    task = task,
                    onClick = { onTaskClick(task) },
                    performanceMode = performanceMode
                )
            }
        }
    }
}

@Composable
private fun WorkerTaskItemCard(
    task: WorkerTask,
    onClick: () -> Unit,
    performanceMode: UiPerformanceMode,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)

    val priorityColor = when (task.priority) {
        TaskPriority.LOW -> Emerald400
        TaskPriority.MEDIUM -> Color(0xFFF59E0B)
        TaskPriority.HIGH -> Color(0xFFFB923C)
        TaskPriority.URGENT -> Color(0xFFF43F5E)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 5.dp else 1.dp,
                shape = shape,
                ambientColor = Color(0x1A000000),
                spotColor = Color(0x1A000000)
            )
            .clip(shape)
            .background(if (isDark) Color(0xF01E293B) else Color(0xF8FFFFFF))
            .border(
                width = 1.dp,
                color = if (isDark) Color(0x3394A3B8) else Color(0x40CBD5E1),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(priorityColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = priorityColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = task.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                        )
                        Text(
                            text = task.id,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }

                    Text(
                        text = "📍 ${task.location}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📏 ${task.distance}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Cyan400 else Color(0xFF0284C7)
                            )
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.priority.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = priorityColor
                                )
                            )
                        }

                        Text(
                            text = "Assigned ${task.assignedTime}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
