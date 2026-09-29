package com.intellinav.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.intellinav.data.model.VehicleType
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.AuthViewModel

@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }
    var selectedVehicleType by remember { mutableStateOf(VehicleType.CAR) }
    
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        step = 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                1 -> {
                    Text(
                        text = "VEHICLE TYPE",
                        color = ElectricCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select your vehicle category",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    VehicleType.values().forEach { type ->
                        VehicleOption(type.label, selectedVehicleType == type) { selectedVehicleType = type }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONTINUE", color = DeepSpaceNavy, fontWeight = FontWeight.Bold)
                    }
                }
                2 -> {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "LOCATION PERMISSION",
                        color = ElectricCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "INTELLINAV requires precise location access for seamless navigation.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Button(
                        onClick = {
                            val missing = locationPermissions.filter {
                                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                            }
                            if (missing.isNotEmpty()) {
                                permissionLauncher.launch(missing.toTypedArray())
                            } else {
                                step = 3
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GRANT PERMISSION", color = DeepSpaceNavy, fontWeight = FontWeight.Bold)
                    }
                }
                3 -> {
                    Text(
                        text = "SETUP COMPLETE",
                        color = MintGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "System is ready for navigation.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateProfile(viewModel.userProfile.value.name, selectedVehicleType)
                            viewModel.completeOnboarding()
                            onComplete()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("START NAVIGATION", color = DeepSpaceNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleOption(
    name: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    Surface(
        onClick = { onSelect(name) },
        color = if (isSelected) ElectricCyan.copy(alpha = 0.1f) else SurfaceSlate,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, ElectricCyan) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onSelect(name) },
                colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan, unselectedColor = TextMuted)
            )
            Text(
                text = name,
                color = if (isSelected) ElectricCyan else TextPrimary,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
