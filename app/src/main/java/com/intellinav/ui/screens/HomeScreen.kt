package com.intellinav.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.NavigationMode
import com.intellinav.ui.components.CanvasMapView
import com.intellinav.ui.components.MetricCard
import com.intellinav.ui.components.NavigationBadge
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NavigationViewModel,
    onNavigate: (String) -> Unit
) {
    val mode by viewModel.navigationMode.collectAsState()
    val pos by viewModel.positionEstimate.collectAsState()
    val mapMatch by viewModel.mapMatchState.collectAsState()
    val vel by viewModel.velocity.collectAsState()
    val orient by viewModel.orientation.collectAsState()
    val outageCountdown by viewModel.outageCountdown.collectAsState()

    var showEngineeringSheet by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. TOP HEADER: Standalone Product Identity (SIH/ISRO removed from Home view)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "INTELLINAV",
                    color = ElectricCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Seamless Navigation Beyond GNSS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            NavigationBadge(mode = mode, outageCountdown = outageCountdown)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isWideScreen) {
            // Wide Screen / Tablet Layout
            Row(modifier = Modifier.weight(1f)) {
                CanvasMapView(
                    position = pos,
                    mapMatchState = mapMatch,
                    navigationMode = mode,
                    showDetailedTrails = false,
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    HomeControlsPanel(
                        viewModel = viewModel,
                        mode = mode,
                        pos = pos,
                        vel = vel,
                        orient = orient,
                        outageCountdown = outageCountdown,
                        onNavigate = onNavigate,
                        onOpenEngineering = { showEngineeringSheet = true }
                    )
                }
            }
        } else {
            // Phone Layout: Live Map as Visual Hero (~50% height)
            CanvasMapView(
                position = pos,
                mapMatchState = mapMatch,
                navigationMode = mode,
                showDetailedTrails = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .verticalScroll(rememberScrollState())
            ) {
                HomeControlsPanel(
                    viewModel = viewModel,
                    mode = mode,
                    pos = pos,
                    vel = vel,
                    orient = orient,
                    outageCountdown = outageCountdown,
                    onNavigate = onNavigate,
                    onOpenEngineering = { showEngineeringSheet = true }
                )
            }
        }
    }

    // Advanced Engineering Suite Bottom Sheet Modal
    if (showEngineeringSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEngineeringSheet = false },
            containerColor = SurfaceSlate,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            EngineeringToolsContent(onNavigate = { route ->
                showEngineeringSheet = false
                onNavigate(route)
            })
        }
    }
}

@Composable
private fun HomeControlsPanel(
    viewModel: NavigationViewModel,
    mode: NavigationMode,
    pos: com.intellinav.data.model.PositionEstimate,
    vel: com.intellinav.data.model.VelocityData,
    orient: com.intellinav.data.model.OrientationData,
    outageCountdown: Int,
    onNavigate: (String) -> Unit,
    onOpenEngineering: () -> Unit
) {
    // Primary Action CTA: Start Live Navigation
    Card(
        onClick = { onNavigate("live_navigation") },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2942)),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(ElectricCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DeepSpaceNavy,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "START LIVE NAVIGATION",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Continuous positioning with active map matching",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Live Primary Telemetry Dashboard (2x2 Grid)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "SPEED",
            value = String.format("%.1f", vel.forwardSpeedMs * 3.6f),
            unit = "km/h",
            icon = Icons.Default.Place,
            accentColor = ElectricCyan,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "HEADING",
            value = String.format("%.0f°", pos.headingDeg),
            unit = orient.headingSource,
            icon = Icons.Default.LocationOn,
            accentColor = if (orient.isHeadingReliable) MintGreen else AmberWarning,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        MetricCard(
            title = "DRIFT",
            value = String.format("%.1f", pos.accumulatedDriftMeters),
            unit = "meters",
            icon = Icons.Default.Warning,
            accentColor = if (mode == NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE) AmberWarning else ElectricCyan,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "UNCERTAINTY",
            value = String.format("±%.1f", pos.uncertaintyMeters),
            unit = "meters",
            icon = Icons.Default.Place,
            accentColor = MintGreen,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Outage Simulation Trigger Box
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "GNSS OUTAGE SIMULATOR", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (outageCountdown > 0) {
                    Text(
                        text = "OUTAGE: ${outageCountdown}s",
                        color = AmberWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (mode == NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE) {
                Button(
                    onClick = { viewModel.restoreGnss() },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = DeepSpaceNavy, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESTORE GNSS SIGNAL", color = DeepSpaceNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.triggerOutage(30) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "SIMULATE GNSS OUTAGE (30s)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Advanced Engineering Suite Sheet Launcher Button
    Card(
        onClick = onOpenEngineering,
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Advanced Engineering Suite", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(text = "System Status, Calibration, Benchmark, AI Model", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EngineeringToolsContent(
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ADVANCED ENGINEERING SUITE",
            color = ElectricCyan,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Group 1: SYSTEM
        EngineeringCategoryHeader("SYSTEM")
        EngineeringToolRow("System Status", "Hardware sensors, rate & health check", "system_status", Icons.Default.CheckCircle, onNavigate)
        EngineeringToolRow("Live Telemetry", "100Hz IMU, GNSS & INS data audit", "telemetry", Icons.Default.Info, onNavigate)
        EngineeringToolRow("In-Vehicle Calibration", "Vehicle mounting alignment matrix", "calibration", Icons.Default.Build, onNavigate)

        Spacer(modifier = Modifier.height(16.dp))

        // Group 2: NAVIGATION ENGINE
        EngineeringCategoryHeader("NAVIGATION ENGINE")
        EngineeringToolRow("Map Matching Engine", "Road segment centerline snapping", "map_matching", Icons.Default.Place, onNavigate)
        EngineeringToolRow("GNSS Outage Demo", "10s / 30s / 60s / 120s outage simulation", "outage_demo", Icons.Default.Warning, onNavigate)

        Spacer(modifier = Modifier.height(16.dp))

        // Group 3: RESEARCH & VALIDATION
        EngineeringCategoryHeader("RESEARCH & VALIDATION")
        EngineeringToolRow("Dataset Replay", "IO-VNBD driving log playback", "replay", Icons.Default.PlayArrow, onNavigate)
        EngineeringToolRow("Benchmark Evaluation", "Ground-truth RMSE & drift rate metrics", "benchmark", Icons.Default.Star, onNavigate)
        EngineeringToolRow("AI Model & Dataset", "Edge feature extraction & speed model", "ai_dataset", Icons.Default.Share, onNavigate)
        EngineeringToolRow("Edge Engine Architecture", "Smartphone vs 200Hz FOG architecture", "edge_engine", Icons.Default.Build, onNavigate)

        Spacer(modifier = Modifier.height(16.dp))

        // Group 4: HISTORY & PROJECT INFO
        EngineeringCategoryHeader("HISTORY & ABOUT")
        EngineeringToolRow("Route History", "Recorded trip session logs", "history", Icons.Default.List, onNavigate)
        EngineeringToolRow("About & Project Info", "Architecture & SIH26168 Details", "about", Icons.Default.Info, onNavigate)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EngineeringCategoryHeader(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun EngineeringToolRow(
    title: String,
    subtitle: String,
    route: String,
    icon: ImageVector,
    onNavigate: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onNavigate(route) }
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF1E293B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
    }
}
