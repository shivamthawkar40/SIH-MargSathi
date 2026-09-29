package com.intellinav.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.NavigationMode
import com.intellinav.data.model.SensorHardwareMode

@Composable
fun NavigationBadge(
    mode: NavigationMode,
    modifier: Modifier = Modifier,
    outageCountdown: Int = 0
) {
    val (bgColor, textColor, label) = when (mode) {
        NavigationMode.GNSS_AVAILABLE -> Triple(Color(0x3300E676), Color(0xFF00E676), "GNSS AVAILABLE")
        NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE -> {
            val countStr = if (outageCountdown > 0) " (${outageCountdown}s)" else ""
            Triple(Color(0x33FF9100), Color(0xFFFF9100), "GNSS LOST — DEAD RECKONING ACTIVE$countStr")
        }
        NavigationMode.GNSS_RESTORED_FUSION_RECOVERING -> Triple(Color(0x3300E5FF), Color(0xFF00E5FF), "GNSS RESTORED — FUSION RECOVERING")
        NavigationMode.UNAVAILABLE -> Triple(Color(0x33FF5252), Color(0xFFFF5252), "SYSTEM UNAVAILABLE")
        NavigationMode.DEMO_REPLAY -> Triple(Color(0x33B388FF), Color(0xFFB388FF), "DEMO / REPLAY MODE")
    }

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .border(1.dp, textColor.copy(alpha = 0.6f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(textColor, shape = RoundedCornerShape(5.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun HardwareModeBadge(
    mode: SensorHardwareMode,
    modifier: Modifier = Modifier
) {
    val color = Color(mode.badgeColorHex)
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mode.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
