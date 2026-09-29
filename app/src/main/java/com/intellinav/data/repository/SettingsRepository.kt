package com.intellinav.data.repository

import com.intellinav.data.model.AppSettings
import com.intellinav.data.model.MapStyle
import com.intellinav.data.model.UnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun updateUnitSystem(unit: UnitSystem) {
        _settings.value = _settings.value.copy(unitSystem = unit)
    }

    fun updateMapStyle(style: MapStyle) {
        _settings.value = _settings.value.copy(mapStyle = style)
    }

    fun toggleNhc(enabled: Boolean) {
        _settings.value = _settings.value.copy(nhcEnabled = enabled)
    }

    fun toggleMapMatching(enabled: Boolean) {
        _settings.value = _settings.value.copy(mapMatchingEnabled = enabled)
    }

    fun toggleAiAssistedSpeed(enabled: Boolean) {
        _settings.value = _settings.value.copy(aiAssistedSpeed = enabled)
    }

    fun setOutageDuration(seconds: Int) {
        _settings.value = _settings.value.copy(simulatedOutageDurationSec = seconds)
    }
}
