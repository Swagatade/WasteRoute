package com.westeroute.app.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.westeroute.app.theme.Cyan400

/** Worker mobile number line; tapping opens the dialer. Shows nothing's-on-file text when [phone] is blank. */
@Composable
fun WorkerPhoneRow(phone: String, modifier: Modifier = Modifier, fontSize: Int = 12) {
    val ctx = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val has = phone.isNotBlank()
    val color = if (has) (if (isDark) Cyan400 else Color(0xFF0284C7)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
    Row(
        modifier = if (has) modifier.clickable {
            val num = phone.filter { it.isDigit() || it == '+' }
            runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        } else modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Phone, null, tint = color, modifier = Modifier.size((fontSize + 1).dp))
        Spacer(Modifier.width(5.dp))
        Text(
            if (has) phone else "Mobile number not on file",
            fontSize = fontSize.sp, fontWeight = if (has) FontWeight.SemiBold else FontWeight.Normal, color = color
        )
    }
}
