package com.intellinav.engine.velocity

import com.intellinav.data.model.FilteredSensorData
import com.intellinav.data.model.GnssData
import com.intellinav.data.model.RawSensorData
import com.intellinav.data.model.VelocityData
import kotlin.math.abs
import kotlin.math.sqrt

class VelocityEstimator {

    private var estimatedForwardSpeedMs = 0f
    private var lastTimestampNanos: Long = 0L

    // Sliding window buffer for multi-frame stationary detection
    private val accWindow = mutableListOf<Float>()
    private val windowSize = 20 // 20 frames (~0.2s - 0.4s)

    fun estimateVelocity(
        raw: RawSensorData,
        filtered: FilteredSensorData,
        vehicleAcc: FloatArray,
        gnss: GnssData,
        isGnssAvailable: Boolean,
        isGyroAvailable: Boolean = false,
        aiPredictedSpeed: Float? = null,
        aiConfidence: Float = 0.8f
    ): VelocityData {
        val nowNanos = raw.timestampNanos
        val dt = if (lastTimestampNanos != 0L) (nowNanos - lastTimestampNanos) / 1e9f else 0.01f
        lastTimestampNanos = nowNanos

        // 1. Sliding Window Multi-Sample Stationary Detection using low-pass filtered magnitude
        val accMag = if (filtered.accMagnitude > 0f) filtered.accMagnitude else sqrt(filtered.accX * filtered.accX + filtered.accY * filtered.accY + filtered.accZ * filtered.accZ)
        val gyroMag = raw.gyroMagnitude

        accWindow.add(accMag)
        if (accWindow.size > windowSize) {
            accWindow.removeAt(0)
        }

        val accMean = accWindow.average().toFloat()
        var accVariance = 0f
        for (v in accWindow) {
            accVariance += (v - accMean) * (v - accMean)
        }
        if (accWindow.isNotEmpty()) accVariance /= accWindow.size

        val accDevFromGravity = abs(accMean - 9.81f)

        // Robust ZUPT criteria: requires low variance AND low gravity deviation
        val isStationary = if (isGyroAvailable) {
            accVariance < 0.04f && accDevFromGravity < 0.20f && gyroMag < 0.06f
        } else if (isGnssAvailable && gnss.isAvailable) {
            accVariance < 0.03f && accDevFromGravity < 0.15f && gnss.speedMs < 0.2f
        } else {
            // Strict check during outage without gyro
            accVariance < 0.025f && accDevFromGravity < 0.12f && abs(vehicleAcc[0]) < 0.08f
        }

        if (isStationary && accWindow.size >= windowSize) {
            // Priority 1: Zero Velocity Update (ZUPT) applied strictly upon confirmed stationary state
            // AI speed is IGNORED when stationary to prevent artificial velocity drift
            estimatedForwardSpeedMs = 0f
            return VelocityData(
                forwardSpeedMs = 0f,
                lateralSpeedMs = 0f,
                verticalSpeedMs = 0f,
                isZuptActive = true,
                isAiAssisted = false
            )
        }

        var isAiUsed = false
        if (isGnssAvailable && gnss.isAvailable && gnss.speedMs >= 0f) {
            // Anchor speed to valid GNSS fix
            estimatedForwardSpeedMs = gnss.speedMs
        } else {
            // Inertial Integration based on forward vehicle acceleration
            val forwardAcc = vehicleAcc[0] // +X is vehicle forward acceleration
            val netAcc = if (abs(forwardAcc) > 0.12f) forwardAcc else 0f
            val inertialSpeed = (estimatedForwardSpeedMs + netAcc * dt).coerceIn(0f, 50f)

            // Bayesian measurement fusion with AI speed prediction during dynamic movement
            if (aiPredictedSpeed != null && aiPredictedSpeed >= 0f) {
                estimatedForwardSpeedMs = fuseAiSpeed(inertialSpeed, aiPredictedSpeed, aiConfidence)
                isAiUsed = true
            } else {
                estimatedForwardSpeedMs = inertialSpeed
            }
        }

        return VelocityData(
            forwardSpeedMs = estimatedForwardSpeedMs,
            lateralSpeedMs = 0f, // Constrained by NHC (vy = 0)
            verticalSpeedMs = 0f, // Constrained by NHC (vz = 0)
            isZuptActive = false,
            isAiAssisted = isAiUsed
        )
    }

    /**
     * Bayesian inverse-variance measurement fusion between inertial speed and AI speed model.
     * Prevents blind scalar averaging by weighting by reciprocal variance noise models.
     */
    private fun fuseAiSpeed(inertialSpeed: Float, aiSpeed: Float, aiConfidence: Float): Float {
        val sigmaIns2 = 0.4f // Inertial integration noise variance
        val sigmaAi2 = (1.2f / aiConfidence.coerceAtLeast(0.1f)) // AI noise model
        val wIns = 1.0f / sigmaIns2
        val wAi = 1.0f / sigmaAi2
        return (wIns * inertialSpeed + wAi * aiSpeed) / (wIns + wAi)
    }
}
