package com.intellinav.data.repository

import android.content.Context
import com.intellinav.data.model.UserProfile
import com.intellinav.data.model.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserRepository(context: Context) {

    private val prefs = context.getSharedPreferences("intellinav_user_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val vehicleTypeName = prefs.getString("vehicle_type", VehicleType.CAR.name) ?: VehicleType.CAR.name
        val vehicleType = try { VehicleType.valueOf(vehicleTypeName) } catch (e: Exception) { VehicleType.CAR }
        
        return UserProfile(
            name = prefs.getString("user_name", "") ?: "",
            email = prefs.getString("user_email", "") ?: "",
            vehicleType = vehicleType,
            isAuthenticated = prefs.getBoolean("is_authenticated", false),
            hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", false)
        )
    }

    fun signUp(name: String, email: String, password: String): Boolean {
        // Simple local sign up
        prefs.edit().apply {
            putString("user_name", name)
            putString("user_email", email)
            putString("user_password", password)
            putBoolean("is_authenticated", true)
            apply()
        }
        _userProfile.value = loadProfile()
        return true
    }

    fun signIn(email: String, password: String): Boolean {
        val storedEmail = prefs.getString("user_email", "")
        val storedPassword = prefs.getString("user_password", "")
        
        if (email == storedEmail && password == storedPassword && email.isNotEmpty()) {
            prefs.edit().putBoolean("is_authenticated", true).apply()
            _userProfile.value = loadProfile()
            return true
        }
        return false
    }

    fun signOut() {
        prefs.edit().putBoolean("is_authenticated", false).apply()
        _userProfile.value = loadProfile()
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
        _userProfile.value = loadProfile()
    }
    
    fun updateProfile(name: String, vehicleType: VehicleType) {
        prefs.edit().apply {
            putString("user_name", name)
            putString("vehicle_type", vehicleType.name)
            apply()
        }
        _userProfile.value = loadProfile()
    }
}
