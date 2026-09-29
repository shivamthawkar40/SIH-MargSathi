package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.ui.components.MetricCard
import com.intellinav.ui.theme.*

@Composable
fun EdgeEngineScreen(
    onBack: () -> Unit
) {
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
            Column {
                Text(text = "Edge Navigation Engine Architecture", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Multi-IMU sensor interface & edge processing model", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard(
                        title = "Smartphone IMU",
                        value = "10 Hz",
                        unit = "UI update",
                        accentColor = ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "FOG Edge IMU",
                        value = "200 Hz",
                        unit = "Arch",
                        accentColor = MintGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, shape = RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "STANDARDIZED SENSOR PIPELINE", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        ArchitectureNode("1. Smartphone IMU (Acc + Gyro + Mag)")
                        ArchitectureNode("2. External MEMS & FOG High-Rate IMU Interface")
                        ArchitectureNode("3. Sensor Synchronization & Calibration Alignment")
                        ArchitectureNode("4. AI-Assisted Noise & Vibration Rejection")
                        ArchitectureNode("5. ESKF Sensor Fusion & Strapdown INS Mechanization")
                        ArchitectureNode("6. HMM Map Matching & Non-Holonomic Constraints (NHC)")
                        ArchitectureNode("7. Continuous Dead Reckoning Navigation Output")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "DEPLOYMENT SPECIFICATIONS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Smartphone deployment operates at 100Hz hardware sampling with 10Hz presentation throttling to prevent UI flicker. Dedicated vehicle edge nodes support up to 200Hz Fiber Optic Gyro (FOG) input.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchitectureNode(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(SurfaceSlate, shape = RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(text = text, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
