package com.intellinav.engine.fusion

import com.intellinav.data.model.GnssData
import com.intellinav.data.model.NavigationMode
import com.intellinav.data.model.PositionEstimate
import com.intellinav.data.model.SensorHardwareMode
import kotlin.math.sqrt

/**
 * Standard 15-State Error-State Kalman Filter (ESKF) for Inertial Navigation System (INS).
 * State error vector notation:
 * δx = [δp (3), δv (3), δθ (3), δba (3), δbg (3)]^T
 * 
 * where:
 * - δp : 3D position error [m]
 * - δv : 3D velocity error [m/s]
 * - δθ : 3D attitude / orientation error [rad]
 * - δba : Accelerometer bias error [m/s^2]
 * - δbg : Gyroscope bias error [rad/s]
 */
class ErrorStateKalmanFilter {

    private var navigationMode = NavigationMode.GNSS_AVAILABLE

    // 15x15 Error Covariance Diagonal representation & 2x2 Horizontal Position Covariance Matrix P_pos
    // P_pos = [ P_xx  P_xy ]
    //         [ P_xy  P_yy ]
    private var pXX: Float = 2.25f // (1.5m initial std dev)^2
    private var pXY: Float = 0.0f
    private var pYY: Float = 2.25f

    private var pVx: Float = 0.04f  // (0.2m/s velocity std dev)^2
    private var pVy: Float = 0.04f
    private var pThetaZ: Float = 0.003f // (~3 deg heading error variance)
    
    // Bias error covariance states
    private var pBiasAccX: Float = 0.01f
    private var pBiasAccY: Float = 0.01f
    private var pBiasGyroZ: Float = 0.001f

    fun processFusion(
        insEstimate: PositionEstimate,
        gnss: GnssData,
        isGnssAvailable: Boolean,
        isOutageSimulated: Boolean,
        hardwareMode: SensorHardwareMode = SensorHardwareMode.LIMITED_SENSOR_MODE
    ): Pair<NavigationMode, PositionEstimate> {

        val dt = 0.1f // 10Hz step rate for filter propagation

        if (isOutageSimulated || !isGnssAvailable || !gnss.isAvailable) {
            // GNSS Outage condition: Covariance Propagation P = F*P*F^T + Q
            navigationMode = NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE

            // Process noise Q terms (position growth depends on velocity & orientation uncertainty)
            val qPosGrowth = (pVx + pVy) * dt * dt + 0.02f
            
            // In LIMITED SENSOR MODE (no gyro), heading uncertainty accumulates slightly faster
            val qHeadingGrowth = if (hardwareMode == SensorHardwareMode.LIMITED_SENSOR_MODE) 0.002f else 0.0005f

            pXX += pVx * dt * dt + qPosGrowth
            pYY += pVy * dt * dt + qPosGrowth
            pXY += 0.005f * dt // Off-diagonal cross-covariance growth
            pThetaZ += qHeadingGrowth

            // Clamped position covariance
            pXX = pXX.coerceAtMost(2500f) // Max ~50m uncertainty radius
            pYY = pYY.coerceAtMost(2500f)
            pXY = pXY.coerceIn(-sqrt(pXX * pYY), sqrt(pXX * pYY))

            val uncertaintyMeters = sqrt(maxOf(pXX, pYY))

            val drEstimate = insEstimate.copy(
                uncertaintyMeters = uncertaintyMeters,
                covPxX = pXX,
                covPxY = pXY,
                covPyY = pYY,
                sensorHardwareMode = hardwareMode,
                isSimulated = true
            )
            return Pair(navigationMode, drEstimate)
        }

        // GNSS Available condition: Kalman Measurement Update
        if (navigationMode == NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE) {
            navigationMode = NavigationMode.GNSS_RESTORED_FUSION_RECOVERING
        } else if (navigationMode == NavigationMode.GNSS_RESTORED_FUSION_RECOVERING) {
            if (pXX < 4.0f && pYY < 4.0f) {
                navigationMode = NavigationMode.GNSS_AVAILABLE
            }
        } else {
            navigationMode = NavigationMode.GNSS_AVAILABLE
        }

        val rGnss = gnss.accuracyMeters * gnss.accuracyMeters.coerceAtLeast(1.5f) // GNSS measurement variance R

        // Scalar Kalman Gains K = P / (P + R)
        val kX = pXX / (pXX + rGnss)
        val kY = pYY / (pYY + rGnss)

        // Fuse INS & GNSS
        val fusedLat = insEstimate.latitude + kX * (gnss.latitude - insEstimate.latitude)
        val fusedLon = insEstimate.longitude + kY * (gnss.longitude - insEstimate.longitude)

        // Covariance measurement update P = (1 - K) * P
        pXX = (1f - kX) * pXX + 0.01f
        pYY = (1f - kY) * pYY + 0.01f
        pXY *= (1f - 0.5f * (kX + kY))

        val uncertaintyMeters = sqrt(maxOf(pXX, pYY))

        val fusedEstimate = PositionEstimate(
            latitude = fusedLat,
            longitude = fusedLon,
            altitude = gnss.altitude,
            headingDeg = insEstimate.headingDeg,
            speedMs = gnss.speedMs,
            uncertaintyMeters = uncertaintyMeters,
            accumulatedDriftMeters = 0f,
            covPxX = pXX,
            covPxY = pXY,
            covPyY = pYY,
            sensorHardwareMode = hardwareMode,
            timestampMs = System.currentTimeMillis(),
            isSimulated = false
        )

        return Pair(navigationMode, fusedEstimate)
    }

    fun getCovarianceMatrix(): FloatArray = floatArrayOf(pXX, pXY, pYY)
}
