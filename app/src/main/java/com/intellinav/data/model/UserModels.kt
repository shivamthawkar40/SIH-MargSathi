package com.intellinav.data.model

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val vehicleType: VehicleType = VehicleType.CAR,
    val isAuthenticated: Boolean = false,
    val hasCompletedOnboarding: Boolean = false
)

enum class VehicleType(val label: String) {
    BIKE("Bike"),
    CAR("Car"),
    AUTO("Auto / Three-Wheeler"),
    BUS("Bus"),
    TRUCK("Truck"),
    HEAVY_VEHICLE("Heavy Vehicle"),
    OTHER("Other")
}
