package com.intellinav.engine.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.intellinav.data.model.GnssData
import com.intellinav.data.model.RawSensorData
import com.intellinav.data.model.SensorAvailabilityStatus
import com.intellinav.data.model.SensorHardwareMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class SensorCollector(context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _rawSensorFlow = MutableStateFlow(RawSensorData())
    val rawSensorFlow: StateFlow<RawSensorData> = _rawSensorFlow.asStateFlow()

    private val _gnssFlow = MutableStateFlow(GnssData())
    val gnssFlow: StateFlow<GnssData> = _gnssFlow.asStateFlow()

    private val _accStatus = MutableStateFlow(SensorAvailabilityStatus.UNAVAILABLE)
    val accStatus: StateFlow<SensorAvailabilityStatus> = _accStatus.asStateFlow()

    private val _gyroStatus = MutableStateFlow(SensorAvailabilityStatus.UNAVAILABLE)
    val gyroStatus: StateFlow<SensorAvailabilityStatus> = _gyroStatus.asStateFlow()

    private val _magStatus = MutableStateFlow(SensorAvailabilityStatus.UNAVAILABLE)
    val magStatus: StateFlow<SensorAvailabilityStatus> = _magStatus.asStateFlow()

    private val _gnssStatus = MutableStateFlow(SensorAvailabilityStatus.UNAVAILABLE)
    val gnssStatus: StateFlow<SensorAvailabilityStatus> = _gnssStatus.asStateFlow()

    private val _rotationVectorStatus = MutableStateFlow(SensorAvailabilityStatus.UNAVAILABLE)
    val rotationVectorStatus: StateFlow<SensorAvailabilityStatus> = _rotationVectorStatus.asStateFlow()

    private val _sensorHardwareMode = MutableStateFlow(SensorHardwareMode.LIMITED_SENSOR_MODE)
    val sensorHardwareMode: StateFlow<SensorHardwareMode> = _sensorHardwareMode.asStateFlow()

    private var currentAcc = FloatArray(3)
    private var currentGyro = FloatArray(3)
    private var currentMag = FloatArray(3)
    private var currentRotationVector = FloatArray(4)
    private var hasRotVecData = false

    private var isListening = false
    private var lastGnssTimestamp = 0L
    private val GNSS_TIMEOUT_MS = 5000L

    fun startListening() {
        if (isListening || sensorManager == null) return
        isListening = true

        val accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accSensor != null) {
            sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST)
            _accStatus.value = SensorAvailabilityStatus.AVAILABLE
        }

        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
            _gyroStatus.value = SensorAvailabilityStatus.AVAILABLE
        }

        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (magSensor != null) {
            sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_FASTEST)
            _magStatus.value = SensorAvailabilityStatus.AVAILABLE
        }

        val rotVecSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) 
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
        if (rotVecSensor != null) {
            sensorManager.registerListener(this, rotVecSensor, SensorManager.SENSOR_DELAY_FASTEST)
            _rotationVectorStatus.value = SensorAvailabilityStatus.AVAILABLE
        }

        val hasGyro = _gyroStatus.value == SensorAvailabilityStatus.AVAILABLE
        val hasMag = _magStatus.value == SensorAvailabilityStatus.AVAILABLE

        _sensorHardwareMode.value = when {
            hasGyro && hasMag -> SensorHardwareMode.FULL_9DOF_IMU
            hasGyro -> SensorHardwareMode.FULL_6DOF_IMU
            else -> SensorHardwareMode.LIMITED_SENSOR_MODE
        }

        startGnssListening()
    }

    @SuppressLint("MissingPermission")
    private fun startGnssListening() {
        val lm = locationManager ?: return
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
                _gnssStatus.value = SensorAvailabilityStatus.AVAILABLE
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
                _gnssStatus.value = SensorAvailabilityStatus.AVAILABLE
            }
        } catch (e: Exception) {
            _gnssStatus.value = SensorAvailabilityStatus.ERROR
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
    }

    fun checkGnssStaleness() {
        if (lastGnssTimestamp > 0 && (System.currentTimeMillis() - lastGnssTimestamp) > GNSS_TIMEOUT_MS) {
            if (_gnssFlow.value.isAvailable) {
                _gnssFlow.value = _gnssFlow.value.copy(isAvailable = false)
                _gnssStatus.value = SensorAvailabilityStatus.UNAVAILABLE
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val timestamp = System.nanoTime()

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, currentAcc, 0, 3)
            Sensor.TYPE_GYROSCOPE -> System.arraycopy(event.values, 0, currentGyro, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, currentMag, 0, 3)
            Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val len = event.values.size.coerceAtMost(4)
                currentRotationVector = FloatArray(len)
                System.arraycopy(event.values, 0, currentRotationVector, 0, len)
                hasRotVecData = true
            }
        }

        val accMag = sqrt(currentAcc[0] * currentAcc[0] + currentAcc[1] * currentAcc[1] + currentAcc[2] * currentAcc[2])
        val gyroMag = sqrt(currentGyro[0] * currentGyro[0] + currentGyro[1] * currentGyro[1] + currentGyro[2] * currentGyro[2])
        val magMag = sqrt(currentMag[0] * currentMag[0] + currentMag[1] * currentMag[1] + currentMag[2] * currentMag[2])

        _rawSensorFlow.value = RawSensorData(
            accX = currentAcc[0], accY = currentAcc[1], accZ = currentAcc[2], accMagnitude = accMag,
            gyroX = currentGyro[0], gyroY = currentGyro[1], gyroZ = currentGyro[2], gyroMagnitude = gyroMag,
            magX = currentMag[0], magY = currentMag[1], magZ = currentMag[2], magMagnitude = magMag,
            rotationVector = currentRotationVector, hasRotationVector = hasRotVecData, timestampNanos = timestamp
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(location: Location) {
        lastGnssTimestamp = System.currentTimeMillis()
        val sats = location.extras?.getInt("satellites", 8) ?: 8
        _gnssFlow.value = GnssData(
            latitude = location.latitude, longitude = location.longitude, altitude = location.altitude,
            accuracyMeters = location.accuracy, speedMs = location.speed, bearingDeg = location.bearing,
            satellitesUsed = sats, timestampMs = location.time, isAvailable = true
        )
        _gnssStatus.value = SensorAvailabilityStatus.AVAILABLE
    }

    override fun onProviderEnabled(provider: String) { _gnssStatus.value = SensorAvailabilityStatus.AVAILABLE }
    override fun onProviderDisabled(provider: String) { _gnssStatus.value = SensorAvailabilityStatus.DISABLED }
}
