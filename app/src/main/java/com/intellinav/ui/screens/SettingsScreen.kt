package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.MapStyle
import com.intellinav.data.model.UnitSystem
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun SettingsScreen(
    viewModel: NavigationViewModel,
    onBack: () -> Unit
) {
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
            Text(text = "System Settings & Preferences", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Units Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "UNIT SYSTEM", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.unitSystem == UnitSystem.METRIC,
                                onClick = { viewModel.updateUnitSystem(UnitSystem.METRIC) },
                                label = { Text("Metric (km/h, m)") }
                            )
                            FilterChip(
                                selected = settings.unitSystem == UnitSystem.IMPERIAL,
                                onClick = { viewModel.updateUnitSystem(UnitSystem.IMPERIAL) },
                                label = { Text("Imperial (mph, ft)") }
                            )
                        }
                    }
                }
            }

            // Map Style Selector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "MAP STYLE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.mapStyle == MapStyle.DARK,
                                onClick = { viewModel.updateMapStyle(MapStyle.DARK) },
                                label = { Text("Deep Dark") }
                            )
                            FilterChip(
                                selected = settings.mapStyle == MapStyle.LIGHT,
                                onClick = { viewModel.updateMapStyle(MapStyle.LIGHT) },
                                label = { Text("Light") }
                            )
                            FilterChip(
                                selected = settings.mapStyle == MapStyle.SATELLITE,
                                onClick = { viewModel.updateMapStyle(MapStyle.SATELLITE) },
                                label = { Text("Satellite") }
                            )
                        }
                    }
                }
            }

            // Engine Toggles
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "ENGINE TOGGLES", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        SettingToggleRow("Non-Holonomic Constraints (NHC)", settings.nhcEnabled) { viewModel.toggleNhc(it) }
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingToggleRow("Map Matching Road Snap", settings.mapMatchingEnabled) { viewModel.toggleMapMatching(it) }
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingToggleRow("AI-Assisted Speed Estimation", settings.aiAssistedSpeed) { viewModel.toggleAiAssistedSpeed(it) }
                    }
                }
            }

            // Calibration Reset
            item {
                Button(
                    onClick = { viewModel.resetCalibration() },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralError),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "RESET IMU ALIGNMENT CALIBRATION", color = DeepSpaceNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = title, color = TextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MintGreen, checkedTrackColor = SurfaceSlate)
        )
    }
}
