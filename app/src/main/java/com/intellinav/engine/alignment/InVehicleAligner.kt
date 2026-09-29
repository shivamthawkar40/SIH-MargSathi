package com.intellinav.engine.alignment

import android.hardware.SensorManager
import com.intellinav.data.model.CalibrationStatus
import com.intellinav.data.model.OrientationData
import com.intellinav.data.model.RawSensorData
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class InVehicleAligner {

    private var status = CalibrationStatus.NOT_CALIBRATED
    private var pitchBias = 0f
    private var rollBias = 0f
    private var yawOffset = 0f
    private var calibrationSamples = 0
    private var sumPitch = 0f
    private var sumRoll = 0f
    private var sumYaw = 0f

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startCalibration() {
        status = CalibrationStatus.CALIBRATING
        calibrationSamples = 0
        sumPitch = 0f
        sumRoll = 0f
        sumYaw = 0f
    }

    fun resetCalibration() {
        status = CalibrationStatus.NOT_CALIBRATED
        pitchBias = 0f
        rollBias = 0f
        yawOffset = 0f
        calibrationSamples = 0
    }

    private var lastKnownValidHeading: Float = 0f

    fun processSensors(
        raw: RawSensorData,
        gnss: com.intellinav.data.model.GnssData = com.intellinav.data.model.GnssData(),
        isGyroAvailable: Boolean = false,
        isMagAvailable: Boolean = false
    ): OrientationData {
        val gravity = floatArrayOf(raw.accX, raw.accY, raw.accZ)
        val geomagnetic = floatArrayOf(raw.magX, raw.magY, raw.magZ)

        var pitch = 0f
        var roll = 0f
        var yaw = 0f
        var headingSource = "Unknown"
        var headingConfidence = com.intellinav.data.model.HeadingConfidence.LIMITED_UNAVAILABLE
        var isHeadingReliable = false

        // 1. First priority: Android System Fused Rotation Vector Sensor
        var success = false
        if (raw.hasRotationVector && raw.rotationVector.isNotEmpty()) {
            try {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, raw.rotationVector)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                headingSource = "Android Rotation Vector Sensor"
                headingConfidence = com.intellinav.data.model.HeadingConfidence.HIGH
                isHeadingReliable = true
                success = true
            } catch (e: Exception) {
                success = false
            }
        }

        // 2. Second priority: Magnetometer + Accelerometer Rotation Matrix Fusion
        if (!success && isMagAvailable) {
            success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                headingSource = if (isGyroAvailable) "9-DOF IMU Sensor Fusion" else "Magnetometer / Acc Fusion"
                headingConfidence = if (isGyroAvailable) com.intellinav.data.model.HeadingConfidence.HIGH else com.intellinav.data.model.HeadingConfidence.MEDIUM
                isHeadingReliable = true
            }
        }

        // 3. Fallback: Accelerometer tilt for Pitch/Roll, and GNSS bearing while moving
        if (!success) {
            pitch = Math.toDegrees(atan2(-raw.accX.toDouble(), sqrt((raw.accY * raw.accY + raw.accZ * raw.accZ).toDouble()))).toFloat()
            roll = Math.toDegrees(atan2(raw.accY.toDouble(), raw.accZ.toDouble())).toFloat()

            if (gnss.isAvailable && gnss.speedMs >= 0.5f && gnss.bearingDeg > 0f) {
                yaw = gnss.bearingDeg
                lastKnownValidHeading = gnss.bearingDeg
                headingSource = "GNSS Course (Moving)"
                headingConfidence = com.intellinav.data.model.HeadingConfidence.MEDIUM
                isHeadingReliable = true
            } else if (lastKnownValidHeading > 0f) {
                yaw = lastKnownValidHeading
                headingSource = "Last Known GNSS Course (Held)"
                headingConfidence = com.intellinav.data.model.HeadingConfidence.LIMITED_UNAVAILABLE
                isHeadingReliable = false
            } else {
                yaw = 0f
                headingSource = "Gyro/Mag Missing — Yaw Unavailable"
                headingConfidence = com.intellinav.data.model.HeadingConfidence.LIMITED_UNAVAILABLE
                isHeadingReliable = false
            }
        }

        if (status == CalibrationStatus.CALIBRATING) {
            sumPitch += pitch
            sumRoll += roll
            sumYaw += yaw
            calibrationSamples++

            if (calibrationSamples >= 50) {
                pitchBias = sumPitch / calibrationSamples
                rollBias = sumRoll / calibrationSamples
                yawOffset = sumYaw / calibrationSamples
                status = CalibrationStatus.CALIBRATED
            }
        }

        val alignedPitch = pitch - pitchBias
        val alignedRoll = roll - rollBias
        val rawHeading = (yaw - yawOffset + 360f) % 360f

        val confidence = when {
            status == CalibrationStatus.CALIBRATED -> 0.98f
            status == CalibrationStatus.CALIBRATING -> (calibrationSamples / 50f).coerceIn(0.1f, 0.9f)
            isMagAvailable -> 0.75f
            gnss.isAvailable && gnss.speedMs >= 0.5f -> 0.65f
            else -> 0.20f
        }

        return OrientationData(
            pitchDeg = alignedPitch,
            rollDeg = alignedRoll,
            yawDeg = yaw,
            headingDeg = rawHeading,
            vehicleHeadingDeg = rawHeading,
            alignmentConfidence = confidence,
            headingConfidence = headingConfidence,
            headingSource = headingSource,
            isHeadingReliable = isHeadingReliable,
            status = status
        )
    }

    /**
     * Transforms phone body acceleration (ax, ay, az) into vehicle reference frame
     * where +X is forward vehicle motion, +Y is lateral right, +Z is vertical up.
     */
    fun transformToVehicleFrame(acc: FloatArray, orientation: OrientationData): FloatArray {
        val pitchRad = Math.toRadians(orientation.pitchDeg.toDouble())
        val rollRad = Math.toRadians(orientation.rollDeg.toDouble())

        // Rotation around Y (pitch) and X (roll)
        val cosP = cos(pitchRad).toFloat()
        val sinP = sin(pitchRad).toFloat()
        val cosR = cos(rollRad).toFloat()
        val sinR = sin(rollRad).toFloat()

        // Forward acceleration (X_veh)
        val axVeh = acc[0] * cosP + acc[2] * sinP
        // Lateral acceleration (Y_veh)
        val ayVeh = acc[1] * cosR - acc[2] * sinR
        // Vertical acceleration (Z_veh)
        val azVeh = -acc[0] * sinP + acc[1] * sinR + acc[2] * cosP * cosR

        return floatArrayOf(axVeh, ayVeh, azVeh)
    }
}
