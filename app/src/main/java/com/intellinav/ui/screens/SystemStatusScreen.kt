package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.SensorAvailabilityStatus
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun SystemStatusScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val health by viewModel.systemHealth.collectAsState()
    val gnss by viewModel.gnss.collectAsState()
    val orient by viewModel.orientation.collectAsState()

    val isFullImuAvailable = health.accStatus == SensorAvailabilityStatus.AVAILABLE &&
            health.gyroStatus == SensorAvailabilityStatus.AVAILABLE &&
            health.magStatus == SensorAvailabilityStatus.AVAILABLE

    val systemStateLabel = when {
        isFullImuAvailable && health.calibrationState == com.intellinav.data.model.CalibrationStatus.CALIBRATED -> "SYSTEM READY"
        isFullImuAvailable -> "CALIBRATION REQUIRED"
        health.accStatus == SensorAvailabilityStatus.AVAILABLE -> "SYSTEM LIMITED (NO IMU GYRO/MAG)"
        else -> "SENSOR LIMITATION / UNAVAILABLE"
    }

    val systemStateColor = when {
        systemStateLabel == "SYSTEM READY" -> MintGreen
        systemStateLabel.startsWith("SYSTEM LIMITED") -> AmberWarning
        else -> CoralError
    }

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
            Text(text = "System Health & Sensor Audit", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Overall Health Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, shape = RoundedCornerShape(12.dp))
                .border(1.dp, CardBorder, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (systemStateLabel == "SYSTEM READY") Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = systemStateColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = systemStateLabel, color = systemStateColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (!isFullImuAvailable) "Operating via Accelerometer tilt & GNSS bearing fallback" else "All 9-DOF sensors active & synchronized",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                HealthStatusRow(
                    "GNSS Receiver",
                    if (health.gnssStatus == SensorAvailabilityStatus.AVAILABLE) "AVAILABLE (${gnss.satellitesUsed} sats, ${gnss.accuracyMeters}m acc)" else "UNAVAILABLE / DISABLED",
                    health.gnssStatus == SensorAvailabilityStatus.AVAILABLE
                )
            }
            item {
                HealthStatusRow(
                    "3-Axis Accelerometer",
                    if (health.accStatus == SensorAvailabilityStatus.AVAILABLE) "AVAILABLE (ACTIVE)" else "NOT PRESENT ON DEVICE",
                    health.accStatus == SensorAvailabilityStatus.AVAILABLE
                )
            }
            item {
                HealthStatusRow(
                    "3-Axis Gyroscope",
                    if (health.gyroStatus == SensorAvailabilityStatus.AVAILABLE) "AVAILABLE (ACTIVE)" else "NOT PRESENT ON DEVICE",
                    health.gyroStatus == SensorAvailabilityStatus.AVAILABLE
                )
            }
            item {
                HealthStatusRow(
                    "3-Axis Magnetometer",
                    if (health.magStatus == SensorAvailabilityStatus.AVAILABLE) "AVAILABLE (ACTIVE)" else "NOT PRESENT ON DEVICE",
                    health.magStatus == SensorAvailabilityStatus.AVAILABLE
                )
            }
            item {
                HealthStatusRow(
                    "Heading Source",
                    orient.headingSource,
                    orient.isHeadingReliable
                )
            }
            item {
                HealthStatusRow(
                    "In-Vehicle Alignment",
                    if (health.calibrationState == com.intellinav.data.model.CalibrationStatus.CALIBRATED) "CALIBRATED" else "NOT CALIBRATED",
                    health.calibrationState == com.intellinav.data.model.CalibrationStatus.CALIBRATED
                )
            }
            item {
                HealthStatusRow(
                    "Dead Reckoning Engine",
                    if (health.drReady) "ACTIVE / READY" else "INACTIVE",
                    health.drReady
                )
            }
            item {
                HealthStatusRow(
                    "Map Matching Engine",
                    if (health.mapMatchActive) "ACTIVE" else "DISABLED",
                    health.mapMatchActive
                )
            }
            item {
                HealthStatusRow(
                    "Non-Holonomic Constraints (NHC)",
                    if (health.nhcActive) "ACTIVE" else "DISABLED",
                    health.nhcActive
                )
            }
            item {
                HealthStatusRow(
                    "AI Speed Inference Pipeline",
                    "PROTOTYPE MODEL PENDING (Feature Extractor Active)",
                    false,
                    isPendingAi = true
                )
            }
        }
    }
}

@Composable
private fun HealthStatusRow(
    title: String,
    statusText: String,
    isOk: Boolean,
    isPendingAi: Boolean = false
) {
    val icon = if (isPendingAi) Icons.Default.Warning else if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning
    val iconColor = if (isPendingAi) AmberWarning else if (isOk) MintGreen else CoralError

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, shape = RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, shape = RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = statusText, color = if (isPendingAi) AmberWarning else TextSecondary, fontSize = 12.sp)
            }
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
    }
}
