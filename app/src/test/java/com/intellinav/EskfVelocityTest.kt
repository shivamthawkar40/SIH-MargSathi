package com.intellinav

import com.intellinav.data.model.FilteredSensorData
import com.intellinav.data.model.GnssData
import com.intellinav.data.model.PositionEstimate
import com.intellinav.data.model.RawSensorData
import com.intellinav.data.model.SensorHardwareMode
import com.intellinav.engine.fusion.ErrorStateKalmanFilter
import com.intellinav.engine.velocity.VelocityEstimator
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class EskfVelocityTest {

    @Test
    fun testEskfOutageCovariancePropagation() {
        val eskf = ErrorStateKalmanFilter()
        val pos = PositionEstimate(latitude = 12.9716, longitude = 77.5946)
        val gnss = GnssData(isAvailable = false)

        // Initial GNSS unavailable step (Outage)
        val (mode, estimate) = eskf.processFusion(
            insEstimate = pos,
            gnss = gnss,
            isGnssAvailable = false,
            isOutageSimulated = true,
            hardwareMode = SensorHardwareMode.LIMITED_SENSOR_MODE
        )

        assertEquals("GNSS LOST — DEAD RECKONING ACTIVE", mode.label)
        assertTrue("Covariance Pxx should grow during outage", estimate.covPxX > 2.25f)
        assertTrue("Covariance Pyy should grow during outage", estimate.covPyY > 2.25f)
        assertEquals(SensorHardwareMode.LIMITED_SENSOR_MODE, estimate.sensorHardwareMode)
    }

    @Test
    fun testZuptStationaryVelocityConstraint() {
        val velocityEstimator = VelocityEstimator()
        // Simulate stationary sensor values (low acc variance, zero gyro)
        val raw = RawSensorData(accX = 0f, accY = 0f, accZ = 9.81f, accMagnitude = 9.81f, gyroMagnitude = 0.01f)
        val filtered = FilteredSensorData(accX = 0f, accY = 0f, accZ = 9.81f, accMagnitude = 9.81f)
        val gnss = GnssData(isAvailable = false)

        var result = velocityEstimator.estimateVelocity(
            raw = raw,
            filtered = filtered,
            vehicleAcc = floatArrayOf(0f, 0f, 0f),
            gnss = gnss,
            isGnssAvailable = false,
            isGyroAvailable = true,
            aiPredictedSpeed = 15.0f // Should be ignored during ZUPT stationary
        )

        // Fill window to confirm stationary ZUPT trigger
        for (i in 1..25) {
            result = velocityEstimator.estimateVelocity(
                raw = raw,
                filtered = filtered,
                vehicleAcc = floatArrayOf(0f, 0f, 0f),
                gnss = gnss,
                isGnssAvailable = false,
                isGyroAvailable = true,
                aiPredictedSpeed = 15.0f
            )
        }

        assertTrue("ZUPT should be active when stationary", result.isZuptActive)
        assertEquals("Velocity must strictly equal 0 when ZUPT is active", 0f, result.forwardSpeedMs, 0.001f)
        assertFalse("AI assistance should be disabled during ZUPT stationary", result.isAiAssisted)
    }
}
