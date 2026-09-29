package com.intellinav.data.model

enum class UnitSystem { METRIC, IMPERIAL }
enum class MapStyle { DARK, LIGHT, SATELLITE }

data class AppSettings(
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val mapStyle: MapStyle = MapStyle.DARK,
    val highFrequencySensors: Boolean = true,
    val nhcEnabled: Boolean = true,
    val mapMatchingEnabled: Boolean = true,
    val aiAssistedSpeed: Boolean = true,
    val simulatedOutageDurationSec: Int = 30
)
