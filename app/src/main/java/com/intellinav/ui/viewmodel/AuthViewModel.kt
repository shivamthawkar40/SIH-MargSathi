package com.intellinav.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.intellinav.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val userRepository = UserRepository(application)
    val userProfile: StateFlow<com.intellinav.data.model.UserProfile> = userRepository.userProfile
    
    fun signUp(name: String, email: String, password: String): Boolean {
        return userRepository.signUp(name, email, password)
    }
    
    fun signIn(email: String, password: String): Boolean {
        return userRepository.signIn(email, password)
    }
    
    fun signOut() {
        userRepository.signOut()
    }
    
    fun completeOnboarding() {
        userRepository.completeOnboarding()
    }
    
    fun updateProfile(name: String, vehicleType: com.intellinav.data.model.VehicleType) {
        userRepository.updateProfile(name, vehicleType)
    }
}
