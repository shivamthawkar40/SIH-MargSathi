package com.intellinav.engine.ins

import com.intellinav.data.model.GnssData
import com.intellinav.data.model.OrientationData
import com.intellinav.data.model.PositionEstimate
import com.intellinav.data.model.VelocityData
import kotlin.math.*

class DeadReckoningEngine {

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var currentAltitude: Double = 0.0
    private var currentSpeedMs: Float = 0f
    private var currentHeadingDeg: Float = 0f
    private var lastTimestampNanos: Long = 0L

    fun updatePosition(
        velocity: VelocityData,
        orientation: OrientationData,
        gnss: GnssData,
        isGnssAvailable: Boolean,
        timestampNanos: Long
    ): PositionEstimate {

        // GNSS available hone par live GNSS data se calibrate karein
        if (isGnssAvailable && gnss.isAvailable) {
            currentLat = gnss.latitude
            currentLon = gnss.longitude
            currentAltitude = gnss.altitude
            currentHeadingDeg = gnss.bearingDeg
            currentSpeedMs = gnss.speedMs
            lastTimestampNanos = timestampNanos

            return PositionEstimate(
                latitude = currentLat,
                longitude = currentLon,
                altitude = currentAltitude,
                speedMs = currentSpeedMs,
                headingDeg = currentHeadingDeg
            )
        }

        // GNSS outage / Dead Reckoning Mode
        if (lastTimestampNanos == 0L) {
            lastTimestampNanos = timestampNanos
            return PositionEstimate(
                latitude = currentLat,
                longitude = currentLon,
                altitude = currentAltitude,
                speedMs = currentSpeedMs,
                headingDeg = currentHeadingDeg
            )
        }

        val dt = (timestampNanos - lastTimestampNanos) / 1_000_000_000.0
        lastTimestampNanos = timestampNanos

        if (dt <= 0.0 || dt > 1.0) {
            return PositionEstimate(
                latitude = currentLat,
                longitude = currentLon,
                altitude = currentAltitude,
                speedMs = currentSpeedMs,
                headingDeg = currentHeadingDeg
            )
        }

        // Heading update from IMU orientation
        currentHeadingDeg = orientation.yawDeg

        // Forward Velocity update
        currentSpeedMs = if (velocity.forwardSpeedMs > 0.05f) {
            velocity.forwardSpeedMs
        } else {
            max(0f, currentSpeedMs - (0.5f * dt.toFloat()))
        }

        // Haversine/WGS84 forward coordinate projection
        val distanceTraveled = currentSpeedMs.toDouble() * dt
        val earthRadius = 6378137.0 // meters

        val headingRad = Math.toRadians(currentHeadingDeg.toDouble())
        val latRad = Math.toRadians(currentLat)
        val lonRad = Math.toRadians(currentLon)

        val newLatRad = asin(
            sin(latRad) * cos(distanceTraveled / earthRadius) +
                    cos(latRad) * sin(distanceTraveled / earthRadius) * cos(headingRad)
        )

        val newLonRad = lonRad + atan2(
            sin(headingRad) * sin(distanceTraveled / earthRadius) * cos(latRad),
            cos(distanceTraveled / earthRadius) - sin(latRad) * sin(newLatRad)
        )

        currentLat = Math.toDegrees(newLatRad)
        currentLon = Math.toDegrees(newLonRad)

        return PositionEstimate(
            latitude = currentLat,
            longitude = currentLon,
            altitude = currentAltitude,
            speedMs = currentSpeedMs,
            headingDeg = currentHeadingDeg
        )
    }

    fun performDriftCorrection(gnss: GnssData): PositionEstimate {
        currentLat = gnss.latitude
        currentLon = gnss.longitude
        currentAltitude = gnss.altitude
        currentSpeedMs = gnss.speedMs
        currentHeadingDeg = gnss.bearingDeg

        return PositionEstimate(
            latitude = currentLat,
            longitude = currentLon,
            altitude = currentAltitude,
            speedMs = currentSpeedMs,
            headingDeg = currentHeadingDeg
        )
    }

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val earthRadius = 6378137.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }
}