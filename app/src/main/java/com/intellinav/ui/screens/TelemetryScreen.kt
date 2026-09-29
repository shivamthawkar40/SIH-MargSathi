package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.intellinav.ui.components.MetricCard
import com.intellinav.ui.components.TelemetryCard
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun TelemetryScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val raw by viewModel.rawSensor.collectAsState()
    val filtered by viewModel.filteredSensor.collectAsState()
    val gnss by viewModel.gnss.collectAsState()
    val health by viewModel.systemHealth.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Engineering Telemetry Monitor", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Sensor stream: 100Hz • UI presentation rate: 10Hz (100ms)", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TelemetryCard(
                    title = "ACCELEROMETER",
                    x = raw.accX,
                    y = raw.accY,
                    z = raw.accZ,
                    magnitude = raw.accMagnitude,
                    unit = "m/s²",
                    accentColor = ElectricCyan
                )
            }
            item {
                TelemetryCard(
                    title = "GYROSCOPE",
                    x = raw.gyroX,
                    y = raw.gyroY,
                    z = raw.gyroZ,
                    magnitude = raw.gyroMagnitude,
                    unit = "rad/s",
                    accentColor = MintGreen
                )
            }
            item {
                TelemetryCard(
                    title = "MAGNETOMETER",
                    x = raw.magX,
                    y = raw.magY,
                    z = raw.magZ,
                    magnitude = raw.magMagnitude,
                    unit = "µT",
                    accentColor = PurpleAccent
                )
            }
            item {
                TelemetryCard(
                    title = "FILTERED ACCELERATION (EMA LPF)",
                    x = filtered.accX,
                    y = filtered.accY,
                    z = filtered.accZ,
                    magnitude = filtered.accMagnitude,
                    unit = "m/s²",
                    accentColor = AmberWarning
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Vibration Index",
                        value = String.format("%.2f", filtered.vibrationIndex),
                        unit = "var",
                        accentColor = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "GNSS Satellites",
                        value = "${gnss.satellitesUsed}",
                        unit = "sats",
                        accentColor = MintGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "GNSS Lat",
                        value = String.format("%.5f", gnss.latitude),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "GNSS Lon",
                        value = String.format("%.5f", gnss.longitude),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Sensor Stream Rate",
                        value = String.format("%.0f", health.sensorRateHz),
                        unit = "Hz",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Processing Latency",
                        value = "${health.latencyMs}",
                        unit = "ms",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
