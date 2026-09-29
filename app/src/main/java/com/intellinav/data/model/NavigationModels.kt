package com.intellinav.data.model

enum class NavigationMode(val label: String, val badgeColorHex: Long) {
    GNSS_AVAILABLE("GNSS AVAILABLE", 0xFF00E5FF), // Blue/Cyan
    GNSS_LOST_DEAD_RECKONING_ACTIVE("DEAD RECKONING ACTIVE", 0xFF00E676), // Green
    GNSS_RESTORED_FUSION_RECOVERING("GNSS RESTORED — FUSION RECOVERING", 0xFFFF9100), // Amber
    UNAVAILABLE("SYSTEM UNAVAILABLE", 0xFFFF5252),
    DEMO_REPLAY("DEMO / REPLAY MODE", 0xFFB388FF)
}

enum class SensorAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    DISABLED,
    ERROR
}

enum class CalibrationStatus {
    NOT_CALIBRATED,
    CALIBRATING,
    CALIBRATED
}

enum class OutageDuration(val seconds: Int) {
    SEC_10(10),
    SEC_30(30),
    SEC_60(60),
    SEC_120(120)
}

data class RawSensorData(
    val accX: Float = 0f,
    val accY: Float = 0f,
    val accZ: Float = 0f,
    val accMagnitude: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val gyroMagnitude: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val magMagnitude: Float = 0f,
    val rotationVector: FloatArray = floatArrayOf(),
    val hasRotationVector: Boolean = false,
    val timestampNanos: Long = 0L
)

data class FilteredSensorData(
    val accX: Float = 0f,
    val accY: Float = 0f,
    val accZ: Float = 0f,
    val accMagnitude: Float = 0f,
    val vibrationIndex: Float = 0f,
    val timestampNanos: Long = 0L
)

enum class HeadingConfidence(val label: String) {
    HIGH("HIGH (9-DOF IMU Fusion)"),
    MEDIUM("MEDIUM (GNSS Course)"),
    LIMITED_UNAVAILABLE("LIMITED / UNAVAILABLE (NO IMU)")
}

data class OrientationData(
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val yawDeg: Float = 0f,
    val headingDeg: Float = 0f,
    val vehicleHeadingDeg: Float = 0f,
    val alignmentConfidence: Float = 0f,
    val headingConfidence: HeadingConfidence = HeadingConfidence.LIMITED_UNAVAILABLE,
    val headingSource: String = "Sensor Unavailable / Held",
    val isHeadingReliable: Boolean = false,
    val status: CalibrationStatus = CalibrationStatus.NOT_CALIBRATED
)

data class VelocityData(
    val forwardSpeedMs: Float = 0f,
    val lateralSpeedMs: Float = 0f,
    val verticalSpeedMs: Float = 0f,
    val isZuptActive: Boolean = false,
    val isAiAssisted: Boolean = false
)

data class GnssData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val speedMs: Float = 0f,
    val bearingDeg: Float = 0f,
    val satellitesUsed: Int = 0,
    val timestampMs: Long = 0L,
    val isAvailable: Boolean = false
)

enum class SensorHardwareMode(val label: String, val badgeColorHex: Long) {
    FULL_9DOF_IMU("FULL 9-DOF IMU MODE", 0xFF00E676),
    FULL_6DOF_IMU("FULL 6-DOF IMU MODE", 0xFF00E5FF),
    LIMITED_SENSOR_MODE("LIMITED SENSOR MODE (NO GYRO/MAG)", 0xFFFF9100)
}

data class PositionEstimate(
    val latitude: Double = 12.9716, // Default Bangalore / ISRO HQ demo area
    val longitude: Double = 77.5946,
    val altitude: Double = 920.0,
    val headingDeg: Float = 0f,
    val speedMs: Float = 0f,
    val uncertaintyMeters: Float = 2.5f,
    val accumulatedDriftMeters: Float = 0f,
    val covPxX: Float = 4.0f,
    val covPxY: Float = 0.0f,
    val covPyY: Float = 4.0f,
    val sensorHardwareMode: SensorHardwareMode = SensorHardwareMode.LIMITED_SENSOR_MODE,
    val timestampMs: Long = 0L,
    val isSimulated: Boolean = false,
    val isReplay: Boolean = false
)

data class MapMatchState(
    val candidateRoadName: String = "Main Arterial Road",
    val matchedLatitude: Double = 12.9716,
    val matchedLongitude: Double = 77.5946,
    val matchedHeadingDeg: Float = 0f,
    val centerlineDistanceMeters: Float = 0.4f,
    val matchConfidence: Float = 0.95f,
    val isNhcActive: Boolean = true,
    val roadType: String = "Primary Highway"
)

data class SystemHealth(
    val sensorRateHz: Float = 100f,
    val uiRateHz: Float = 10f,
    val latencyMs: Long = 8L,
    val sensorHardwareMode: SensorHardwareMode = SensorHardwareMode.LIMITED_SENSOR_MODE,
    val accStatus: SensorAvailabilityStatus = SensorAvailabilityStatus.AVAILABLE,
    val gyroStatus: SensorAvailabilityStatus = SensorAvailabilityStatus.AVAILABLE,
    val magStatus: SensorAvailabilityStatus = SensorAvailabilityStatus.AVAILABLE,
    val gnssStatus: SensorAvailabilityStatus = SensorAvailabilityStatus.AVAILABLE,
    val calibrationState: CalibrationStatus = CalibrationStatus.NOT_CALIBRATED,
    val drReady: Boolean = true,
    val mapMatchActive: Boolean = true,
    val nhcActive: Boolean = true,
    val aiPipelineStatus: String = "Prototype inference pipeline — trained model integration pending"
)

data class BenchmarkMetrics(
    val datasetName: String = "IO-VNBD Ground Vehicle",
    val totalDistanceMeters: Float = 0f,
    val durationSeconds: Long = 0L,
    val rmsePositionErrorMeters: Float = 0f,
    val maxPositionErrorMeters: Float = 0f,
    val avgSpeedErrorMs: Float = 0f,
    val driftRatePercent: Float = 0f,
    val outageDurationSec: Int = 0,
    val hasGroundTruth: Boolean = false,
    val note: String = ""
)

data class RouteSession(
    val id: String,
    val timestampFormatted: String,
    val durationFormatted: String,
    val distanceKm: Float,
    val avgSpeedKmh: Float,
    val gnssOutageDurationSec: Int,
    val maxDriftMeters: Float,
    val modeSummary: String
)
