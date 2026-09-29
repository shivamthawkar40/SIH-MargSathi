package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.ui.components.CanvasMapView
import com.intellinav.ui.components.MetricCard
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun MapMatchingScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val pos by viewModel.positionEstimate.collectAsState()
    val mode by viewModel.navigationMode.collectAsState()
    val mapMatch by viewModel.mapMatchState.collectAsState()
    val settings by viewModel.settings.collectAsState()

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
                Text(text = "Map Matching & Road Constraints", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "HMM probabilistic road snapping & NHC filter", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Map View
        CanvasMapView(
            position = pos,
            mapMatchState = mapMatch,
            navigationMode = mode,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Grid
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Centerline Distance",
                value = String.format("%.2f", mapMatch.centerlineDistanceMeters),
                unit = "m",
                accentColor = MintGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Match Confidence",
                value = String.format("%.0f%%", mapMatch.matchConfidence * 100f),
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Road details card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "CANDIDATE ROAD SEGMENT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = mapMatch.candidateRoadName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "Road Type: ${mapMatch.roadType}", color = TextSecondary, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = CardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(text = "Non-Holonomic Constraints (NHC)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = if (settings.nhcEnabled) "NHC ACTIVE (vy=0, vz=0)" else "NHC INACTIVE", color = if (settings.nhcEnabled) MintGreen else TextMuted, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.nhcEnabled,
                        onCheckedChange = { viewModel.toggleNhc(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MintGreen, checkedTrackColor = SurfaceSlate)
                    )
                }
            }
        }
    }
}
