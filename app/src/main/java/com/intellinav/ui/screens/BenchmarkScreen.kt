package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
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
fun BenchmarkScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val metrics by viewModel.benchmarkMetrics.collectAsState()

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
                Text(text = "Dead Reckoning Benchmark", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Calculated from IO-VNBD dataset ground truth", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.runBenchmark() },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = DeepSpaceNavy)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "RUN BENCHMARK EVALUATION", color = DeepSpaceNavy, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!metrics.hasGroundTruth) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, shape = RoundedCornerShape(12.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ground truth unavailable — tap 'RUN BENCHMARK EVALUATION' to load IO-VNBD dataset metrics.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = "RMSE Position Error",
                            value = String.format("%.2f", metrics.rmsePositionErrorMeters),
                            unit = "m",
                            accentColor = MintGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Max Position Error",
                            value = String.format("%.2f", metrics.maxPositionErrorMeters),
                            unit = "m",
                            accentColor = AmberWarning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = "Drift Rate",
                            value = String.format("%.2f%%", metrics.driftRatePercent),
                            unit = "/min",
                            accentColor = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Avg Speed Error",
                            value = String.format("%.2f", metrics.avgSpeedErrorMs),
                            unit = "m/s",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = "Total Distance",
                            value = String.format("%.0f", metrics.totalDistanceMeters),
                            unit = "m",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Outage Duration",
                            value = "${metrics.outageDurationSec}",
                            unit = "sec",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "BENCHMARK SUMMARY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = metrics.note, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
