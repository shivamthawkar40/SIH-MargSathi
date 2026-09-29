package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.VehicleType
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.AuthViewModel
import com.intellinav.ui.viewmodel.NavigationViewModel
import androidx.compose.runtime.*

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    navViewModel: NavigationViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val systemHealth by navViewModel.systemHealth.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var editedName by remember(userProfile) { mutableStateOf(userProfile.name) }
    var editedVehicleType by remember(userProfile) { mutableStateOf(userProfile.vehicleType) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceNavy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "USER PROFILE",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            TextButton(onClick = { 
                if (isEditMode) {
                    authViewModel.updateProfile(editedName, editedVehicleType)
                }
                isEditMode = !isEditMode 
            }) {
                Text(
                    text = if (isEditMode) "SAVE" else "EDIT",
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Profile Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(SurfaceSlate),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isEditMode) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricCyan
                    )
                )
            } else {
                Text(
                    text = userProfile.name,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userProfile.email,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "NAVIGATION PROFILE",
            color = ElectricCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isEditMode) {
            Text(text = "Vehicle Type", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            VehicleType.values().forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = editedVehicleType == type,
                        onClick = { editedVehicleType = type },
                        colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                    )
                    Text(text = type.label, color = TextPrimary, modifier = Modifier.padding(start = 8.dp))
                }
            }
        } else {
            ProfileInfoCard(
                label = "Vehicle Type",
                value = userProfile.vehicleType.label,
                icon = Icons.Default.DirectionsCar
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "SYSTEM & PERMISSIONS",
            color = ElectricCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ProfileInfoCard(
            label = "Location Access",
            value = if (systemHealth.gnssStatus == com.intellinav.data.model.SensorAvailabilityStatus.AVAILABLE) "GRANTED" else "LIMITED",
            icon = Icons.Default.LocationOn
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ProfileInfoCard(
            label = "IMU Status",
            value = systemHealth.sensorHardwareMode.label,
            icon = Icons.Default.Sensors
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralError.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoralError)
        ) {
            Text("SIGN OUT", color = CoralError, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileInfoCard(
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceSlate),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
