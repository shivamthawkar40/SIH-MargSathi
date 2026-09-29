package com.intellinav.engine.replay

import com.intellinav.data.model.GnssData
import com.intellinav.data.model.PositionEstimate
import com.intellinav.data.model.RawSensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

data class ReplayFrame(
    val timestampMs: Long,
    val rawSensor: RawSensorData,
    val gnssData: GnssData,
    val groundTruthLat: Double,
    val groundTruthLon: Double,
    val isGnssAvailableInFrame: Boolean
)

class ReplayEngine {

    private val sampleFrames = mutableListOf<ReplayFrame>()
    private var currentIndex = 0
    private var isPlaying = false
    private var speedMultiplier = 1.0f

    private val _currentFrame = MutableStateFlow<ReplayFrame?>(null)
    val currentFrame: StateFlow<ReplayFrame?> = _currentFrame.asStateFlow()

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    init {
        generateIovnbdSampleDataset()
    }

    private fun generateIovnbdSampleDataset() {
        sampleFrames.clear()
        val baseLat = 12.9716
        val baseLon = 77.5946
        val numSamples = 300 // 300 seconds of simulated IO-VNBD trajectory

        val EARTH_RADIUS = 6378137.0

        for (i in 0 until numSamples) {
            val tMs = i * 1000L
            val speedMs = 12.0f + (sin(i / 10.0) * 3.0).toFloat() // ~43 km/h
            val headingDeg = (45.0 + (sin(i / 25.0) * 15.0)).toFloat()
            val headingRad = Math.toRadians(headingDeg.toDouble())

            val distanceMeters = speedMs * (i * 1.0)
            val dNorth = distanceMeters * cos(headingRad)
            val dEast = distanceMeters * sin(headingRad)

            val gtLat = baseLat + (dNorth / EARTH_RADIUS) * (180.0 / Math.PI)
            val gtLon = baseLon + (dEast / (EARTH_RADIUS * cos(Math.toRadians(baseLat)))) * (180.0 / Math.PI)

            // Simulate GNSS outage between t = 60s and t = 180s (120 sec outage)
            val isGnssAvailable = !(i in 60..180)

            val noisyAccX = (sin(i * 0.5) * 0.4).toFloat()
            val noisyAccY = (cos(i * 0.5) * 0.2).toFloat()
            val noisyAccZ = 9.81f + (sin(i * 2.0) * 0.3).toFloat()

            val rawSensor = RawSensorData(
                accX = noisyAccX,
                accY = noisyAccY,
                accZ = noisyAccZ,
                accMagnitude = 9.81f,
                gyroX = 0.01f,
                gyroY = 0.01f,
                gyroZ = 0.05f,
                gyroMagnitude = 0.051f,
                magX = 22f,
                magY = -14f,
                magZ = 45f,
                magMagnitude = 52f,
                timestampNanos = tMs * 1_000_000L
            )

            val gnss = GnssData(
                latitude = gtLat + if (isGnssAvailable) 0.0 else 0.0005,
                longitude = gtLon + if (isGnssAvailable) 0.0 else 0.0005,
                altitude = 920.0,
                accuracyMeters = 2.5f,
                speedMs = speedMs,
                bearingDeg = headingDeg,
                satellitesUsed = if (isGnssAvailable) 12 else 0,
                timestampMs = tMs,
                isAvailable = isGnssAvailable
            )

            sampleFrames.add(
                ReplayFrame(
                    timestampMs = tMs,
                    rawSensor = rawSensor,
                    gnssData = gnss,
                    groundTruthLat = gtLat,
                    groundTruthLon = gtLon,
                    isGnssAvailableInFrame = isGnssAvailable
                )
            )
        }
        if (sampleFrames.isNotEmpty()) {
            _currentFrame.value = sampleFrames[0]
        }
    }

    fun play() {
        isPlaying = true
        _isPlayingState.value = true
    }

    fun pause() {
        isPlaying = false
        _isPlayingState.value = false
    }

    fun stop() {
        isPlaying = false
        _isPlayingState.value = false
        currentIndex = 0
        if (sampleFrames.isNotEmpty()) {
            _currentFrame.value = sampleFrames[0]
            _progress.value = 0f
        }
    }

    fun setSpeedMultiplier(mult: Float) {
        speedMultiplier = mult
    }

    fun seekTo(progressFraction: Float) {
        if (sampleFrames.isEmpty()) return
        currentIndex = (progressFraction * (sampleFrames.size - 1)).toInt().coerceIn(0, sampleFrames.size - 1)
        _currentFrame.value = sampleFrames[currentIndex]
        _progress.value = currentIndex.toFloat() / (sampleFrames.size - 1)
    }

    fun tickNextFrame(): ReplayFrame? {
        if (!isPlaying || sampleFrames.isEmpty()) return _currentFrame.value
        currentIndex++
        if (currentIndex >= sampleFrames.size) {
            currentIndex = 0 // Loop or stop
            isPlaying = false
            _isPlayingState.value = false
        }
        _progress.value = currentIndex.toFloat() / (sampleFrames.size - 1)
        val frame = sampleFrames[currentIndex]
        _currentFrame.value = frame
        return frame
    }

    fun getAllFrames(): List<ReplayFrame> = sampleFrames
    fun getTotalDurationSeconds(): Long = if (sampleFrames.isNotEmpty()) sampleFrames.last().timestampMs / 1000L else 0L
}
