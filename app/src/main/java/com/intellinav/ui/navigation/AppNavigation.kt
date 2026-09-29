package com.intellinav.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.intellinav.ui.screens.*
import com.intellinav.ui.viewmodel.AuthViewModel
import com.intellinav.ui.viewmodel.NavigationViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navViewModel: NavigationViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val userProfile by authViewModel.userProfile.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onTimeout = {
                if (userProfile.isAuthenticated) {
                    if (userProfile.hasCompletedOnboarding) {
                        navController.navigate("live_navigation") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("profile_setup") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                } else {
                    navController.navigate("signin") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            })
        }

        composable("signin") {
            SignInScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate("signup") },
                onSignInSuccess = {
                    if (userProfile.hasCompletedOnboarding) {
                        navController.navigate("live_navigation") {
                            popUpTo("signin") { inclusive = true }
                        }
                    } else {
                        navController.navigate("profile_setup") {
                            popUpTo("signin") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("signup") {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToSignIn = { navController.navigate("signin") },
                onSignUpSuccess = {
                    navController.navigate("profile_setup") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        composable("profile_setup") {
            ProfileSetupScreen(
                viewModel = authViewModel,
                onComplete = {
                    navController.navigate("live_navigation") {
                        popUpTo("profile_setup") { inclusive = true }
                    }
                }
            )
        }

        composable("live_navigation") {
            MainMapScreen(
                navViewModel = navViewModel,
                authViewModel = authViewModel,
                onNavigate = { route -> navController.navigate(route) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("signin") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("profile") {
            ProfileScreen(
                authViewModel = authViewModel,
                navViewModel = navViewModel,
                onBack = { navController.popBackStack() },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate("signin") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("live_navigation_telemetry") { LiveNavigationScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }

        // Engineering Screens
        composable("system_status") { SystemStatusScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("telemetry") { TelemetryScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("outage_demo") { GnssOutageDemoScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("map_matching") { MapMatchingScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("replay") { ReplayScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("benchmark") { BenchmarkScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("calibration") { CalibrationScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("ai_dataset") { AiModelDatasetScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("edge_engine") { EdgeEngineScreen(onBack = { navController.popBackStack() }) }
        composable("history") { RouteHistoryScreen(onBack = { navController.popBackStack() }) }
        composable("settings") { SettingsScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
        composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
        composable("offline_maps") { OfflineMapsScreen(onBack = { navController.popBackStack() }) }
        composable("sensor_fusion") { SensorFusionScreen(viewModel = navViewModel, onBack = { navController.popBackStack() }) }
    }
}
