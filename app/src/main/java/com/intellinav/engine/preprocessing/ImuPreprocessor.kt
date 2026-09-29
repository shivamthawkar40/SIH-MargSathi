package com.intellinav.engine.preprocessing

import com.intellinav.data.model.FilteredSensorData
import com.intellinav.data.model.RawSensorData
import kotlin.math.abs
import kotlin.math.sqrt

class ImuPreprocessor {

    private val alphaAcc = 0.25f // Low pass filter factor
    private var filteredAccX = 0f
    private var filteredAccY = 0f
    private var filteredAccZ = 9.81f

    private val GRAVITY_STANDARD = 9.81f
    private var vibrationVariance = 0f

    fun preprocess(raw: RawSensorData): FilteredSensorData {
        // Apply Low-Pass Filter (EMA) to smooth high-frequency noise
        filteredAccX = filteredAccX + alphaAcc * (raw.accX - filteredAccX)
        filteredAccY = filteredAccY + alphaAcc * (raw.accY - filteredAccY)
        filteredAccZ = filteredAccZ + alphaAcc * (raw.accZ - filteredAccZ)

        val magnitude = sqrt(filteredAccX * filteredAccX + filteredAccY * filteredAccY + filteredAccZ * filteredAccZ)

        // Calculate vibration disturbance index based on magnitude deviation from 1g
        val instantVibration = abs(raw.accMagnitude - GRAVITY_STANDARD)
        vibrationVariance = 0.9f * vibrationVariance + 0.1f * instantVibration

        return FilteredSensorData(
            accX = filteredAccX,
            accY = filteredAccY,
            accZ = filteredAccZ,
            accMagnitude = magnitude,
            vibrationIndex = vibrationVariance.coerceIn(0f, 5f),
            timestampNanos = raw.timestampNanos
        )
    }

    /**
     * Subtracts gravity vector from filtered acceleration, returning linear vehicle dynamics.
     */
    fun separateGravity(filtered: FilteredSensorData, pitchDeg: Float, rollDeg: Float): FloatArray {
        val pitchRad = Math.toRadians(pitchDeg.toDouble())
        val rollRad = Math.toRadians(rollDeg.toDouble())

        // Estimated gravity components in phone frame
        val gx = -GRAVITY_STANDARD * Math.sin(pitchRad).toFloat()
        val gy = GRAVITY_STANDARD * Math.sin(rollRad).toFloat() * Math.cos(pitchRad).toFloat()
        val gz = GRAVITY_STANDARD * Math.cos(rollRad).toFloat() * Math.cos(pitchRad).toFloat()

        val linearAx = filtered.accX - gx
        val linearAy = filtered.accY - gy
        val linearAz = filtered.accZ - gz

        return floatArrayOf(linearAx, linearAy, linearAz)
    }
}
