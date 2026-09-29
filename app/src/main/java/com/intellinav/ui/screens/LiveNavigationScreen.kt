package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.ui.components.RealMapView
import com.intellinav.ui.components.MetricCard
import com.intellinav.ui.components.NavigationBadge
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun LiveNavigationScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
    val mode by viewModel.navigationMode.collectAsState()
    val pos by viewModel.positionEstimate.collectAsState()
    val mapMatch by viewModel.mapMatchState.collectAsState()
    val gnss by viewModel.gnss.collectAsState()
    val vel by viewModel.velocity.collectAsState()
    val outageCountdown by viewModel.outageCountdown.collectAsState()

    val configuration = LocalConfiguration.current

    val isLandscapeOrTablet =
        configuration.screenWidthDp >= 600 ||
                configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(12.dp)
    ) {

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            NavigationBadge(
                mode = mode,
                outageCountdown = outageCountdown
            )
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (isLandscapeOrTablet) {

            // Tablet / Landscape
            Row(
                modifier = Modifier.fillMaxSize()
            ) {

                RealMapView(
                    position = pos,
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxHeight()
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    NavControlsPanel(
                        viewModel = viewModel,
                        pos = pos,
                        vel = vel,
                        gnss = gnss,
                        mapMatch = mapMatch
                    )
                }
            }

        } else {

            // Compact Phone
            RealMapView(
                position = pos,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                NavControlsPanel(
                    viewModel = viewModel,
                    pos = pos,
                    vel = vel,
                    gnss = gnss,
                    mapMatch = mapMatch
                )
            }
        }
    }
}

@Composable
private fun NavControlsPanel(
    viewModel: NavigationViewModel,
    pos: com.intellinav.data.model.PositionEstimate,
    vel: com.intellinav.data.model.VelocityData,
    gnss: com.intellinav.data.model.GnssData,
    mapMatch: com.intellinav.data.model.MapMatchState
) {

    // Quick Outage Test Controls
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = "TRIGGER GNSS OUTAGE DEMO",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                OutageButton(
                    label = "10s",
                    viewModel = viewModel,
                    sec = 10,
                    modifier = Modifier.weight(1f)
                )

                OutageButton(
                    label = "30s",
                    viewModel = viewModel,
                    sec = 30,
                    modifier = Modifier.weight(1f)
                )

                OutageButton(
                    label = "60s",
                    viewModel = viewModel,
                    sec = 60,
                    modifier = Modifier.weight(1f)
                )

                OutageButton(
                    label = "120s",
                    viewModel = viewModel,
                    sec = 120,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    // Telemetry Gauges
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        MetricCard(
            title = "Speed",
            value = String.format(
                "%.1f",
                vel.forwardSpeedMs * 3.6f
            ),
            unit = "km/h",
            icon = Icons.Default.Place,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Heading",
            value = String.format(
                "%.0f°",
                pos.headingDeg
            ),
            unit = "deg",
            icon = Icons.Default.LocationOn,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        MetricCard(
            title = "DR Drift",
            value = String.format(
                "%.1f",
                pos.accumulatedDriftMeters
            ),
            unit = "m",
            icon = Icons.Default.Warning,
            accentColor = AmberWarning,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Uncertainty",
            value = String.format(
                "%.1f",
                pos.uncertaintyMeters
            ),
            unit = "m",
            icon = Icons.Default.Place,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    // Road Snap Details
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CardSurface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = "MAP MATCHING & CONSTRAINTS",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Matched Road:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Text(
                    text = mapMatch.candidateRoadName,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "Centerline Distance:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Text(
                    text = String.format(
                        "%.2f m",
                        mapMatch.centerlineDistanceMeters
                    ),
                    color = MintGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "NHC Status:",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Text(
                    text = if (mapMatch.isNhcActive) {
                        "ACTIVE"
                    } else {
                        "INACTIVE"
                    },
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OutageButton(
    label: String,
    viewModel: NavigationViewModel,
    sec: Int,
    modifier: Modifier = Modifier
) {

    Button(
        onClick = {
            viewModel.triggerOutage(sec)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = AmberWarning
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(
            horizontal = 4.dp,
            vertical = 6.dp
        ),
        modifier = modifier
    ) {

        Text(
            text = label,
            color = DeepSpaceNavy,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}