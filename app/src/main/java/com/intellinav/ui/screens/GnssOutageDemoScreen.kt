package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.NavigationMode
import com.intellinav.ui.components.CanvasMapView
import com.intellinav.ui.components.MetricCard
import com.intellinav.ui.components.NavigationBadge
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun GnssOutageDemoScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val mode by viewModel.navigationMode.collectAsState()
    val pos by viewModel.positionEstimate.collectAsState()
    val vel by viewModel.velocity.collectAsState()
    val mapMatch by viewModel.mapMatchState.collectAsState()
    val countdown by viewModel.outageCountdown.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(14.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "GNSS Outage Demonstration", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "SIH26168 Dead Reckoning Live Outage Test", color = TextSecondary, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        NavigationBadge(mode = mode, outageCountdown = countdown, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Live Map View (Dominant)
            CanvasMapView(
                position = pos,
                mapMatchState = mapMatch,
                navigationMode = mode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Duration Controls
            Text(text = "TRIGGER CONTROLLED OUTAGE DURATION", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutageDemoButton("10 SEC", 10, viewModel, Modifier.weight(1f))
                OutageDemoButton("30 SEC", 30, viewModel, Modifier.weight(1f))
                OutageDemoButton("60 SEC", 60, viewModel, Modifier.weight(1f))
                OutageDemoButton("120 SEC", 120, viewModel, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time DR Metrics Grid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Accumulated Drift",
                    value = String.format("%.2f", pos.accumulatedDriftMeters),
                    unit = "meters",
                    accentColor = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Uncertainty Radius",
                    value = String.format("%.2f", pos.uncertaintyMeters),
                    unit = "meters",
                    accentColor = ElectricCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "ESTIMATED SPEED",
                    value = String.format("%.1f", vel.forwardSpeedMs * 3.6f),
                    unit = "km/h",
                    accentColor = MintGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "HEADING",
                    value = String.format("%.0f°", pos.headingDeg),
                    unit = "degrees",
                    accentColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map Matching & NHC Diagnostics
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "LIVE ENGINE CONSTRAINTS & MATCHING", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Matched Road:", color = TextSecondary, fontSize = 12.sp)
                        Text(text = mapMatch.candidateRoadName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Centerline Distance:", color = TextSecondary, fontSize = 12.sp)
                        Text(text = String.format("%.2f m", mapMatch.centerlineDistanceMeters), color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "NHC Constraint:", color = TextSecondary, fontSize = 12.sp)
                        Text(text = if (mapMatch.isNhcActive) "ACTIVE (vy = 0, vz = 0)" else "INACTIVE", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Restore / Force GNSS Fusion
            if (mode == NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE) {
                Button(
                    onClick = { viewModel.restoreGnss() },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = DeepSpaceNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESTORE GNSS SIGNAL & FUSE DRIFT", color = DeepSpaceNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OutageDemoButton(label: String, sec: Int, viewModel: NavigationViewModel, modifier: Modifier = Modifier) {
    Button(
        onClick = { viewModel.triggerOutage(sec) },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier
    ) {
        Text(text = label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

