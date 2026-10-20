package com.wasteroute.app.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasteroute.app.firebase.LiveHousehold
import com.wasteroute.app.theme.Cyan400
import com.wasteroute.app.theme.Emerald400

private val Amber = Color(0xFFF59E0B)

/**
 * Worker / supervisor side: the homes of the areas the supervisor assigned, with today's collection state.
 * Never shown to a resident (their app does not even receive this list).
 */
@Composable
fun AssignedHomesCard(
    homes: List<LiveHousehold>,
    areaLabel: String,
    hasAssignedArea: Boolean,
    onShowOnMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(24.dp)
    val done = homes.count { it.collectedToday() }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "HOSTEL BUILDINGS IN MY AREA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isDark) Color(0xF01E293B) else Color(0xF8FFFFFF))
                .border(1.2.dp, if (isDark) Color(0x3338BDF8) else Color(0x40CBD5E1), shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!hasAssignedArea) {
                Text(
                    text = "Your supervisor has not assigned an area to you yet. Hostel buildings appear here as soon as they do.",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            } else if (homes.isEmpty()) {
                Text(
                    text = "No hostel buildings are registered in $areaLabel yet.",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$done / ${homes.size} buildings collected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = areaLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                    Button(
                        onClick = onShowOnMap,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Color(0xFF0F172A)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Text("On map", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0x33334155) else Color(0x33CBD5E1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(done.toFloat() / homes.size)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Emerald400)
                    )
                }
                homes.forEach { h -> HomeRow(h) }
            }
        }
    }
}

@Composable
private fun HomeRow(h: LiveHousehold) {
    val isDark = isSystemInDarkTheme()
    val collected = h.collectedToday()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(if (collected) Emerald400 else Amber.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (collected) Icons.Filled.Check else Icons.Outlined.Home,
                contentDescription = null,
                tint = if (collected) Color(0xFF052E16) else Amber,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = h.name.ifBlank { h.code },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = listOf(h.code, h.address).filter { it.isNotBlank() }.joinToString(" - "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
        if (collected) {
            Text("Collected", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Emerald400)
        } else {
            // Only the warden's QR scan marks a building collected; workers cannot tick it themselves.
            Text("Waiting for warden scan", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Amber)
        }
    }
}

/**
 * Citizen side: only this resident's own home. Shows whether today's garbage was collected, or lets them
 * link their home with the code the supervisor gave them.
 */
@Composable
fun MyHomeCard(
    homes: List<LiveHousehold>,
    onLinkHome: (code: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onShowOnMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(24.dp)
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = "MY HOSTEL BLOCK",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isDark) Color(0xF01E293B) else Color(0xF8FFFFFF))
                .border(1.2.dp, if (isDark) Color(0x3310B981) else Color(0x4010B981), shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (homes.isEmpty()) {
                Text(
                    text = "Wardens: link your hostel block with the code from your supervisor (for example HB-101). Then scan the worker's QR when they reach your block to complete the collection.",
                    fontSize = 13.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(); message = null },
                    singleLine = true,
                    label = { Text("Block code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onLinkHome(code) { _, msg -> message = msg } },
                    enabled = code.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald400, contentColor = Color(0xFF052E16)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Link my block", fontWeight = FontWeight.ExtraBold) }
                message?.let { Text(it, fontSize = 12.sp, color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) }
            } else {
                homes.forEach { h ->
                    val collected = h.collectedToday()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .background(if (collected) Emerald400 else Amber.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (collected) Icons.Filled.Check else Icons.Outlined.Home,
                                contentDescription = null,
                                tint = if (collected) Color(0xFF052E16) else Amber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = h.name.ifBlank { h.code },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = if (collected) "Garbage collected today" else "Waiting for today's collection",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (collected) Emerald400 else Amber
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(1.dp))
                Button(
                    onClick = onShowOnMap,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("See my block on the map", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
