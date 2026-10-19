package com.westeroute.app.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.model.UiPerformanceMode
import com.westeroute.app.theme.Cyan400
import com.westeroute.app.theme.Emerald400

@Composable
fun WorkerQrCard(
    workerName: String = "Worker",
    workerId: String = "",
    workZone: String = "Assigned Ward",
    onShowFullScreenQr: () -> Unit = {},
    performanceMode: UiPerformanceMode = UiPerformanceMode.PREMIUM,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "MY COLLECTION QR",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.6.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (performanceMode == UiPerformanceMode.PREMIUM) 12.dp else 2.dp,
                    shape = shape,
                    ambientColor = if (isDark) Color(0x99000000) else Color(0x33000000),
                    spotColor = if (isDark) Color(0x667C3AED) else Color(0x337C3AED)
                )
                .clip(shape)
                .background(
                    brush = if (isDark) {
                        Brush.linearGradient(
                            listOf(
                                Color(0xF02E1065), // Deep Purple
                                Color(0xF00F172A),
                                Color(0xF00C4A6E)  // Deep Cyan
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                Color(0xF7F5F3FF),
                                Color(0xF7FFFFFF),
                                Color(0xF7ECFEFF)
                            )
                        )
                    }
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFA78BFA),
                            Cyan400
                        )
                    ),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR Mock Display Box
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeImage(
                        content = com.westeroute.app.firebase.WorkerQrPayload.encode(workerId),
                        size = 94.dp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = workerName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                )

                Text(
                    text = "Worker ID: $workerId · $workZone",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Show this QR to residents after household waste collection to confirm service.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onShowFullScreenQr,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C3AED) // Purple 600
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Show Full Screen QR",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
