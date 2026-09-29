package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun AiModelDatasetScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val aiFeatures by viewModel.aiFeatures.collectAsState()

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
                Text(text = "AI Model & Dataset Architecture", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "IO-VNBD Inertial Benchmark AI Pipeline", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Honest Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface, shape = RoundedCornerShape(12.dp))
                .border(1.dp, AmberWarning, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "AI MODEL PIPELINE STATUS", color = AmberWarning, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = aiFeatures.statusText, color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feature Metrics
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Acc Variance",
                value = String.format("%.3f", aiFeatures.accVariance),
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Gyro Power",
                value = String.format("%.3f", aiFeatures.gyroPower),
                accentColor = MintGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Vibration Energy",
                value = String.format("%.3f", aiFeatures.vibrationEnergy),
                accentColor = AmberWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "AI Speed Estimate",
                value = String.format("%.1f", aiFeatures.estimatedAiSpeedMs * 3.6f),
                unit = "km/h",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PurpleAccent)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "IO-VNBD DATASET & PIPELINE FLOW", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Dataset: IO-VNBD — Inertial and Odometry benchmark dataset for ground vehicle positioning (github.com/onyekpeu/IO-VNBD)",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        PipelineStep("1. IO-VNBD Benchmark Dataset Preprocessing")
                        PipelineStep("2. Sensor Synchronization & Gravity Separation")
                        PipelineStep("3. Feature Extraction (Acc Variance, Spectral Energy, Turning Rate)")
                        PipelineStep("4. Noise & Pothole Vibration Filtering")
                        PipelineStep("5. AI Speed & Forward Velocity Estimation")
                        PipelineStep("6. INS Trajectory Correction & Drift Dampening")
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineStep(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(SurfaceSlate, shape = RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Text(text = text, color = TextPrimary, fontSize = 12.sp)
    }
}
