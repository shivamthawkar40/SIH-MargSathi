package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun SensorFusionScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val pos by viewModel.positionEstimate.collectAsState()
    val health by viewModel.systemHealth.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(text = "Sensor Fusion Engine", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        FusionCard(
            title = "Error-State Kalman Filter",
            status = if (health.drReady) "ACTIVE" else "INITIALIZING",
            details = "Fusing GNSS @ 1Hz with IMU @ 100Hz"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "COVARIANCE MATRIX (P)", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                CovarianceRow("P_xx (Lat Var)", pos.covPxX)
                CovarianceRow("P_xy (Cross)", pos.covPxY)
                CovarianceRow("P_yy (Lon Var)", pos.covPyY)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "FUSION PARAMETERS", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        FusionParam("Measurement Noise (R)", "2.25 m²")
        FusionParam("Process Noise (Q)", "0.04 m²/s²")
        FusionParam("Heading Uncertainty", String.format("%.2f°", pos.uncertaintyMeters * 0.5f))
    }
}

@Composable
private fun FusionCard(title: String, status: String, details: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MergeType, contentDescription = null, tint = MintGreen, modifier = Modifier.size(32.dp))
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = status, color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = details, color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CovarianceRow(label: String, value: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = String.format("%.4f", value), color = TextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun FusionParam(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = ElectricCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
