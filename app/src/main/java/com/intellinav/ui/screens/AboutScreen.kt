package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.ui.theme.*

@Composable
fun AboutScreen(
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
            Text(text = "About INTELLINAV", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, shape = RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "INTELLINAV", color = ElectricCyan, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Seamless Navigation Beyond GNSS", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Problem ID: SIH26168 • SIH 2026", color = MintGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Department of Space / ISRO", color = TextMuted, fontSize = 12.sp)
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
                        Text(text = "PROBLEM STATEMENT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "AI-ML Based Intelligent Dead Reckoning System for Seamless Navigation",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "INTELLINAV is a smartphone-based Dead Reckoning navigation system designed to estimate continuous ground vehicle position when GNSS signals become unavailable due to tunnels, urban canyons, or jamming.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
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
                        Text(text = "NAVIGATION PIPELINE ARCHITECTURE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sensors → Preprocessing → Alignment → Velocity Estimation → Dead Reckoning → Map Matching → Non-Holonomic Constraints → ESKF Fusion",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        )
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
                        Text(text = "HARDWARE & SENSOR ADAPTABILITY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        TechItem("• Gyroscope & Magnetometer:", "Dynamically detected via SensorManager")
                        TechItem("• Hardware Fallback:", "Uses Accelerometer tilt & GNSS bearing when IMU is absent")
                        TechItem("• Future Hardware Support:", "Fully architected for 9-DOF IMU & external FOG units")
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
                        Text(text = "PROTOTYPE STATUS & TRANSPARENCY", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        TechItem("• Dead Reckoning Pipeline:", "IMPLEMENTED (Active)")
                        TechItem("• Map Matching & NHC:", "IMPLEMENTED (Active)")
                        TechItem("• Error-State Kalman Fusion:", "IMPLEMENTED (Active)")
                        TechItem("• AI Speed Engine:", "PROTOTYPE (Feature extractor active, trained model pending)")
                        TechItem("• Dataset Replay:", "IMPLEMENTED (IO-VNBD Benchmark format)")
                    }
                }
            }
        }
    }
}

@Composable
private fun TechItem(label: String, valStr: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(text = "$label ", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = valStr, color = TextSecondary, fontSize = 12.sp)
    }
}

