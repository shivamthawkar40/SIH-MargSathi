package com.intellinav.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.ui.theme.*

@Composable
fun AppDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    closeDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(0.dp),
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpaceNavy)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "INTELLINAV",
                color = ElectricCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp)
            )

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                DrawerItem("LIVE NAVIGATION", Icons.Default.Navigation, currentRoute == "live_navigation") { 
                    onNavigate("live_navigation"); closeDrawer() 
                }
                DrawerItem("DEAD RECKONING", Icons.Default.DirectionsCar, currentRoute == "live_navigation_telemetry") {
                    onNavigate("live_navigation_telemetry"); closeDrawer()
                }
                DrawerItem("SENSOR MONITOR", Icons.Default.Sensors, currentRoute == "telemetry") {
                    onNavigate("telemetry"); closeDrawer()
                }
                DrawerItem("AI ANALYSIS", Icons.Default.AutoGraph, currentRoute == "ai_dataset") {
                    onNavigate("ai_dataset"); closeDrawer()
                }
                DrawerItem("CALIBRATION", Icons.Default.CompassCalibration, currentRoute == "calibration") {
                    onNavigate("calibration"); closeDrawer()
                }
                DrawerItem("MAP MATCHING", Icons.Default.Route, currentRoute == "map_matching") {
                    onNavigate("map_matching"); closeDrawer()
                }
                DrawerItem("SENSOR FUSION", Icons.Default.MergeType, currentRoute == "sensor_fusion") {
                    onNavigate("sensor_fusion"); closeDrawer()
                }
                DrawerItem("TRIP ANALYTICS", Icons.Default.History, currentRoute == "history") {
                    onNavigate("history"); closeDrawer()
                }
                DrawerItem("OFFLINE MAPS", Icons.Default.Map, currentRoute == "offline_maps") {
                    onNavigate("offline_maps"); closeDrawer()
                }
                DrawerItem("SYSTEM STATUS", Icons.Default.HealthAndSafety, currentRoute == "system_status") {
                    onNavigate("system_status"); closeDrawer()
                }
                DrawerItem("SETTINGS", Icons.Default.Settings, currentRoute == "settings") {
                    onNavigate("settings"); closeDrawer()
                }
                DrawerItem("ABOUT", Icons.Default.Info, currentRoute == "about") {
                    onNavigate("about"); closeDrawer()
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            DrawerItem("SIGN OUT", Icons.Default.Logout, false) {
                onSignOut(); closeDrawer()
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp
            )
        },
        selected = isSelected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = ElectricCyan.copy(alpha = 0.1f),
            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            selectedIconColor = ElectricCyan,
            unselectedIconColor = TextSecondary,
            selectedTextColor = ElectricCyan,
            unselectedTextColor = TextSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
