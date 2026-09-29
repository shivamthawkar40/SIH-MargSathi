package com.intellinav.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.intellinav.data.model.NavigationMode
import com.intellinav.data.model.PositionEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DirectNavigationEngine(
    private val context: Context
) : SensorEventListener {

    // =========================================================
    // LOCATION
    // =========================================================

    private val fusedLocation =
        LocationServices.getFusedLocationProviderClient(context)

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var locationCallback: LocationCallback? = null

    // =========================================================
    // POSITION
    // =========================================================

    private val _position =
        MutableStateFlow(
            PositionEstimate(
                latitude = 0.0,
                longitude = 0.0,
                altitude = 0.0,
                speedMs = 0f,
                headingDeg = 0f
            )
        )

    val position = _position.asStateFlow()

    // =========================================================
    // NAVIGATION MODE
    // =========================================================

    private val _navMode =
        MutableStateFlow(
            NavigationMode.GNSS_AVAILABLE
        )

    val navMode = _navMode.asStateFlow()

    // =========================================================
    // ROUTE
    // =========================================================

    private val _routePoints =
        MutableStateFlow<List<LatLng>>(emptyList())

    val routePoints = _routePoints.asStateFlow()

    @Volatile
    private var currentRouteIdx = 0

    // =========================================================
    // GNSS STATE
    // =========================================================

    @Volatile
    private var lastGnssReceived = 0L

    /*
     * Don't declare GNSS lost after one missed update.
     *
     * 3 seconds gives the location provider some tolerance
     * against normal Android scheduling delays.
     */
    private val gnssTimeoutMs = 3000L

    @Volatile
    private var lastGnssPosition: LatLng? = null

    @Volatile
    private var lastGnssAltitude = 0.0

    @Volatile
    private var lastGnssAccuracy = Float.MAX_VALUE

    // =========================================================
    // MOTION STATE
    // =========================================================

    @Volatile
    private var currentSpeedMps = 0f

    @Volatile
    private var headingDeg = 0f

    /*
     * Last reliable heading.
     *
     * Important:
     * When vehicle is stationary, we DO NOT randomly change
     * heading.
     */
    @Volatile
    private var lastReliableHeadingDeg = 0f

    @Volatile
    private var isMoving = false

    private val stationarySpeedThreshold = 0.7f

    // =========================================================
    // DR STATE
    // =========================================================

    @Volatile
    private var deadReckoningStarted = false

    @Volatile
    private var drLastPosition: LatLng? = null

    @Volatile
    private var drLastTimestamp = 0L

    /*
     * Prevents artificial acceleration from creating large
     * movement when the vehicle is stationary.
     */
    @Volatile
    private var accelerationMotionScore = 0f

    // =========================================================
    // 10 Hz LOOP
    // =========================================================

    private val scheduler =
        Executors.newSingleThreadScheduledExecutor()

    // =========================================================
    // INITIALIZATION
    // =========================================================

    init {
        setupSensors()
        startGnss()
        start10HzNavigationLoop()
    }

    // =========================================================
    // ROUTE
    // =========================================================

    fun setTargetRoute(points: List<LatLng>) {

        _routePoints.value = points

        /*
         * Don't blindly start at route[0].
         *
         * Find the route point nearest to the CURRENT position.
         */
        val current = _position.value

        currentRouteIdx =
            if (points.isEmpty()) {
                0
            } else {
                findNearestRouteIndex(
                    LatLng(
                        current.latitude,
                        current.longitude
                    ),
                    points
                )
            }
    }

    // =========================================================
    // SENSOR SETUP
    // =========================================================

    private fun setupSensors() {

        val accelerometer =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

        val gyroscope =
            sensorManager.getDefaultSensor(
                Sensor.TYPE_GYROSCOPE
            )

        accelerometer?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        gyroscope?.let {

            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    // =========================================================
    // GNSS
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun startGnss() {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            _navMode.value =
                NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE

            return
        }

        val request =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                500L
            )
                .setMinUpdateIntervalMillis(250L)
                .setWaitForAccurateLocation(false)
                .setMaxUpdateDelayMillis(1000L)
                .build()

        val callback =
            object : LocationCallback() {

                override fun onLocationResult(
                    result: LocationResult
                ) {

                    val location =
                        result.lastLocation ?: return

                    /*
                     * Ignore obviously bad fixes.
                     */
                    if (
                        !location.hasAccuracy() ||
                        location.accuracy > 35f
                    ) {
                        return
                    }

                    val now =
                        System.currentTimeMillis()

                    lastGnssReceived = now

                    lastGnssPosition =
                        LatLng(
                            location.latitude,
                            location.longitude
                        )

                    lastGnssAltitude =
                        location.altitude

                    lastGnssAccuracy =
                        location.accuracy

                    /*
                     * GNSS has returned.
                     */
                    val wasDeadReckoning =
                        deadReckoningStarted

                    if (wasDeadReckoning) {

                        /*
                         * Don't instantly teleport the DR position.
                         *
                         * Blend it toward GNSS over subsequent
                         * navigation ticks.
                         */
                        correctDeadReckoningToGnss(
                            location.latitude,
                            location.longitude
                        )

                        deadReckoningStarted = false
                    }

                    _navMode.value =
                        NavigationMode.GNSS_AVAILABLE

                    // -------------------------------------------------
                    // SPEED
                    // -------------------------------------------------

                    if (
                        location.hasSpeed() &&
                        location.speed >= 0f &&
                        location.speed.isFinite()
                    ) {

                        val gpsSpeed =
                            location.speed

                        /*
                         * Ignore tiny GNSS speed noise.
                         */
                        currentSpeedMps =
                            if (gpsSpeed < 0.5f) {
                                0f
                            } else {
                                gpsSpeed.coerceIn(
                                    0f,
                                    60f
                                )
                            }
                    }

                    isMoving =
                        currentSpeedMps >
                                stationarySpeedThreshold

                    // -------------------------------------------------
                    // HEADING
                    // -------------------------------------------------

                    if (
                        location.hasBearing() &&
                        isMoving
                    ) {

                        val gpsBearing =
                            normalizeBearing(
                                location.bearing.toDouble()
                            )

                        /*
                         * Smooth bearing changes.
                         */
                        headingDeg =
                            smoothBearing(
                                headingDeg,
                                gpsBearing.toFloat(),
                                0.35f
                            )

                        lastReliableHeadingDeg =
                            headingDeg
                    } else {

                        /*
                         * Stationary:
                         * keep previous heading.
                         */
                        headingDeg =
                            lastReliableHeadingDeg
                    }

                    // -------------------------------------------------
                    // POSITION
                    // -------------------------------------------------

                    if (!deadReckoningStarted) {

                        _position.value =
                            PositionEstimate(
                                latitude =
                                    location.latitude,

                                longitude =
                                    location.longitude,

                                altitude =
                                    location.altitude,

                                speedMs =
                                    currentSpeedMps,

                                headingDeg =
                                    headingDeg
                            )
                    }
                }
            }

        locationCallback = callback

        try {

            fusedLocation.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )

        } catch (_: SecurityException) {

            _navMode.value =
                NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE
        }
    }

    // =========================================================
    // 10 Hz NAVIGATION LOOP
    // =========================================================

    private fun start10HzNavigationLoop() {

        scheduler.scheduleAtFixedRate(
            {

                val now =
                    System.currentTimeMillis()

                val gnssLost =
                    lastGnssReceived == 0L ||
                            (
                                    now -
                                            lastGnssReceived
                                    ) >
                            gnssTimeoutMs

                if (gnssLost) {

                    if (!deadReckoningStarted) {
                        startDeadReckoning()
                    }

                    _navMode.value =
                        NavigationMode
                            .GNSS_LOST_DEAD_RECKONING_ACTIVE

                    updateDeadReckoning()

                } else {

                    /*
                     * GNSS is healthy.
                     *
                     * Don't let the direct DR loop overwrite
                     * the real GNSS position.
                     */
                    if (!deadReckoningStarted) {

                        _navMode.value =
                            NavigationMode.GNSS_AVAILABLE
                    }
                }

            },
            100L,
            100L,
            TimeUnit.MILLISECONDS
        )
    }

    // =========================================================
    // START DR
    // =========================================================

    private fun startDeadReckoning() {

        val current =
            _position.value

        drLastPosition =
            LatLng(
                current.latitude,
                current.longitude
            )

        drLastTimestamp =
            System.currentTimeMillis()

        /*
         * Find route point closest to the REAL current position.
         */
        val route =
            _routePoints.value

        if (route.isNotEmpty()) {

            currentRouteIdx =
                findNearestRouteIndex(
                    drLastPosition!!,
                    route
                )
        }

        deadReckoningStarted = true
    }

    // =========================================================
    // DEAD RECKONING UPDATE
    // =========================================================

    private fun updateDeadReckoning() {

        val now =
            System.currentTimeMillis()

        val previous =
            drLastPosition
                ?: LatLng(
                    _position.value.latitude,
                    _position.value.longitude
                )

        var dt =
            if (drLastTimestamp == 0L) {
                0.1
            } else {
                (now - drLastTimestamp)
                    .toDouble() / 1000.0
            }

        /*
         * Protect against scheduling delays.
         */
        dt =
            dt.coerceIn(
                0.05,
                0.25
            )

        drLastTimestamp = now

        /*
         * If vehicle is stationary, NEVER move.
         */
        if (
            currentSpeedMps <
            stationarySpeedThreshold
        ) {

            drLastPosition = previous

            _position.value =
                PositionEstimate(
                    latitude =
                        previous.latitude,

                    longitude =
                        previous.longitude,

                    altitude =
                        _position.value.altitude,

                    speedMs = 0f,

                    headingDeg =
                        lastReliableHeadingDeg
                )

            return
        }

        // ---------------------------------------------------------
        // HEADING
        // ---------------------------------------------------------

        var drHeading =
            lastReliableHeadingDeg

        val route =
            _routePoints.value

        /*
         * Use route geometry only when the vehicle is actually
         * moving.
         */
        if (
            route.size >= 2 &&
            currentRouteIdx <
            route.size - 1
        ) {

            val routePoint =
                route[currentRouteIdx]

            /*
             * Only use route heading if we are reasonably close
             * to that route segment.
             */
            val distanceToRoute =
                distanceBetween(
                    previous,
                    routePoint
                )

            if (distanceToRoute < 40.0) {

                val nextPoint =
                    route[currentRouteIdx + 1]

                val routeBearing =
                    calculateBearing(
                        routePoint,
                        nextPoint
                    )

                drHeading =
                    smoothBearing(
                        drHeading,
                        routeBearing.toFloat(),
                        0.20f
                    )

                lastReliableHeadingDeg =
                    drHeading
            }
        }

        headingDeg = drHeading

        // ---------------------------------------------------------
        // DISTANCE
        // ---------------------------------------------------------

        val stepDistance =
            (
                    currentSpeedMps *
                            dt
                    ).coerceIn(
                    0.0,
                    25.0
                )

        /*
         * If there is essentially no movement,
         * don't modify position.
         */
        if (stepDistance < 0.01) {

            drLastPosition = previous

            return
        }

        val projected =
            extrapolatePoint(
                start = previous,
                meters = stepDistance,
                bearing = drHeading.toDouble()
            )

        drLastPosition =
            projected

        // ---------------------------------------------------------
        // ROUTE PROGRESS
        // ---------------------------------------------------------

        if (
            route.size >= 2 &&
            currentRouteIdx <
            route.size - 1
        ) {

            val next =
                route[currentRouteIdx + 1]

            if (
                distanceBetween(
                    projected,
                    next
                ) < 8.0
            ) {

                currentRouteIdx =
                    min(
                        currentRouteIdx + 1,
                        route.lastIndex
                    )
            }
        }

        // ---------------------------------------------------------
        // POSITION OUTPUT
        // ---------------------------------------------------------

        _position.value =
            PositionEstimate(
                latitude =
                    projected.latitude,

                longitude =
                    projected.longitude,

                altitude =
                    _position.value.altitude,

                speedMs =
                    currentSpeedMps,

                headingDeg =
                    drHeading
            )
    }

    // =========================================================
    // GNSS RECOVERY
    // =========================================================

    private fun correctDeadReckoningToGnss(
        latitude: Double,
        longitude: Double
    ) {

        val current =
            _position.value

        val dr =
            LatLng(
                current.latitude,
                current.longitude
            )

        val gnss =
            LatLng(
                latitude,
                longitude
            )

        val error =
            distanceBetween(
                dr,
                gnss
            )

        /*
         * Very small difference:
         * just accept GNSS.
         */
        if (error < 5.0) {

            _position.value =
                PositionEstimate(
                    latitude = latitude,
                    longitude = longitude,
                    altitude = lastGnssAltitude,
                    speedMs = currentSpeedMps,
                    headingDeg = headingDeg
                )

            drLastPosition = gnss

            return
        }

        /*
         * For a larger difference, move only part-way toward
         * GNSS. This prevents a visual teleport.
         */
        val correctionFactor =
            when {
                error < 15.0 -> 0.65
                error < 50.0 -> 0.45
                else -> 0.25
            }

        val corrected =
            LatLng(
                current.latitude +
                        (
                                latitude -
                                        current.latitude
                                ) *
                        correctionFactor,

                current.longitude +
                        (
                                longitude -
                                        current.longitude
                                ) *
                        correctionFactor
            )

        _position.value =
            PositionEstimate(
                latitude =
                    corrected.latitude,

                longitude =
                    corrected.longitude,

                altitude =
                    lastGnssAltitude,

                speedMs =
                    currentSpeedMps,

                headingDeg =
                    headingDeg
            )

        drLastPosition =
            corrected
    }

    // =========================================================
    // SENSOR EVENTS
    // =========================================================

    override fun onSensorChanged(
        event: SensorEvent?
    ) {

        event ?: return

        when (event.sensor.type) {

            Sensor.TYPE_ACCELEROMETER -> {

                val x =
                    event.values[0]

                val y =
                    event.values[1]

                val z =
                    event.values[2]

                val magnitude =
                    sqrt(
                        (
                                x * x +
                                        y * y +
                                        z * z
                                ).toDouble()
                    ).toFloat()

                val netAcceleration =
                    abs(
                        magnitude - 9.81f
                    )

                /*
                 * This is ONLY a motion detector.
                 *
                 * We deliberately do NOT integrate this directly
                 * into speed because that caused unrealistic
                 * movement in the previous implementation.
                 */
                accelerationMotionScore =
                    accelerationMotionScore * 0.9f +
                            netAcceleration * 0.1f
            }

            Sensor.TYPE_GYROSCOPE -> {

                /*
                 * Gyroscope data is received here.
                 *
                 * The main ESKF/AI navigation architecture
                 * remains responsible for high-quality inertial
                 * estimation.
                 */
            }
        }
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int
    ) {
        // No action required.
    }

    // =========================================================
    // FIND NEAREST ROUTE POINT
    // =========================================================

    private fun findNearestRouteIndex(
        position: LatLng,
        route: List<LatLng>
    ): Int {

        if (route.isEmpty()) {
            return 0
        }

        var nearestIndex = 0
        var nearestDistance = Double.MAX_VALUE

        route.forEachIndexed { index, point ->

            val distance =
                distanceBetween(
                    position,
                    point
                )

            if (distance < nearestDistance) {

                nearestDistance =
                    distance

                nearestIndex =
                    index
            }
        }

        return nearestIndex
    }

    // =========================================================
    // BEARING
    // =========================================================

    private fun calculateBearing(
        start: LatLng,
        end: LatLng
    ): Double {

        val lat1 =
            Math.toRadians(
                start.latitude
            )

        val lon1 =
            Math.toRadians(
                start.longitude
            )

        val lat2 =
            Math.toRadians(
                end.latitude
            )

        val lon2 =
            Math.toRadians(
                end.longitude
            )

        val dLon =
            lon2 - lon1

        val y =
            sin(dLon) * cos(lat2)

        val x =
            cos(lat1) * sin(lat2) -
                    sin(lat1) *
                    cos(lat2) *
                    cos(dLon)

        return normalizeBearing(
            Math.toDegrees(
                atan2(y, x)
            )
        )
    }

    // =========================================================
    // BEARING NORMALIZATION
    // =========================================================

    private fun normalizeBearing(
        bearing: Double
    ): Double {

        var result =
            bearing % 360.0

        if (result < 0.0) {
            result += 360.0
        }

        return result
    }

    // =========================================================
    // SMOOTH BEARING
    // =========================================================

    private fun smoothBearing(
        current: Float,
        target: Float,
        factor: Float
    ): Float {

        val delta =
            (
                    (
                            target -
                                    current +
                                    540f
                            ) % 360f
                    ) - 180f

        return normalizeBearing(
            (
                    current +
                            delta * factor
                    ).toDouble()
        ).toFloat()
    }

    // =========================================================
    // EXTRAPOLATE POSITION
    // =========================================================

    private fun extrapolatePoint(
        start: LatLng,
        meters: Double,
        bearing: Double
    ): LatLng {

        val earthRadius =
            6378137.0

        val angularDistance =
            meters / earthRadius

        val bearingRadians =
            Math.toRadians(
                bearing
            )

        val lat1 =
            Math.toRadians(
                start.latitude
            )

        val lon1 =
            Math.toRadians(
                start.longitude
            )

        val lat2 =
            kotlin.math.asin(
                sin(lat1) *
                        cos(angularDistance) +
                        cos(lat1) *
                        sin(angularDistance) *
                        cos(bearingRadians)
            )

        val lon2 =
            lon1 +
                    atan2(
                        sin(bearingRadians) *
                                sin(angularDistance) *
                                cos(lat1),

                        cos(angularDistance) -
                                sin(lat1) *
                                sin(lat2)
                    )

        return LatLng(
            Math.toDegrees(lat2),
            Math.toDegrees(lon2)
        )
    }

    // =========================================================
    // DISTANCE
    // =========================================================

    private fun distanceBetween(
        p1: LatLng,
        p2: LatLng
    ): Double {

        val earthRadius =
            6378137.0

        val dLat =
            Math.toRadians(
                p2.latitude -
                        p1.latitude
            )

        val dLon =
            Math.toRadians(
                p2.longitude -
                        p1.longitude
            )

        val lat1 =
            Math.toRadians(
                p1.latitude
            )

        val lat2 =
            Math.toRadians(
                p2.latitude
            )

        val a =
            sin(dLat / 2.0).pow(2.0) +
                    cos(lat1) *
                    cos(lat2) *
                    sin(dLon / 2.0).pow(2.0)

        val safeA =
            a.coerceIn(
                0.0,
                1.0
            )

        return earthRadius *
                2.0 *
                atan2(
                    sqrt(safeA),
                    sqrt(1.0 - safeA)
                )
    }

    // =========================================================
    // STOP
    // =========================================================

    fun stop() {

        locationCallback?.let {

            try {
                fusedLocation
                    .removeLocationUpdates(it)
            } catch (_: Exception) {
            }
        }

        locationCallback = null

        sensorManager.unregisterListener(
            this
        )

        if (!scheduler.isShutdown) {
            scheduler.shutdownNow()
        }
    }
}