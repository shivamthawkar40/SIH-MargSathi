package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.CalibrationStatus
import com.intellinav.ui.components.MetricCard
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun CalibrationScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val orient by viewModel.orientation.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "In-Vehicle IMU Alignment Wizard", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration Status Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, shape = RoundedCornerShape(12.dp))
                .border(1.dp, CardBorder, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (orient.status == CalibrationStatus.CALIBRATED) Icons.Default.CheckCircle else Icons.Default.Edit,
                        contentDescription = null,
                        tint = if (orient.status == CalibrationStatus.CALIBRATED) MintGreen else AmberWarning,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "STATUS: ${orient.status.name}",
                            color = if (orient.status == CalibrationStatus.CALIBRATED) MintGreen else AmberWarning,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Alignment Confidence: ${String.format("%.0f%%", orient.alignmentConfidence * 100f)}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Angles
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(title = "Pitch (deg)", value = String.format("%.1f°", orient.pitchDeg), modifier = Modifier.weight(1f))
            MetricCard(title = "Roll (deg)", value = String.format("%.1f°", orient.rollDeg), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(title = "Yaw (deg)", value = String.format("%.1f°", orient.yawDeg), modifier = Modifier.weight(1f))
            MetricCard(title = "Vehicle Heading", value = String.format("%.1f°", orient.vehicleHeadingDeg), accentColor = ElectricCyan, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "ALIGNMENT INSTRUCTIONS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                InstructionBullet("1. Mount smartphone securely in vehicle dock or place on flat surface.")
                InstructionBullet("2. Ensure vehicle is parked on flat, level ground.")
                InstructionBullet("3. Tap 'Start Calibration' to record orientation bias parameters.")
                InstructionBullet("4. Wait until confidence reaches >95% before starting navigation.")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.startCalibration() },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "START CALIBRATION", color = DeepSpaceNavy, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { viewModel.resetCalibration() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "RESET ALIGNMENT", color = CoralError, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InstructionBullet(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
