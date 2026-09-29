package com.intellinav.data.repository

import android.content.Context
import com.intellinav.data.model.*
import com.intellinav.engine.ai.AiFeatures
import com.intellinav.engine.alignment.InVehicleAligner
import com.intellinav.engine.benchmark.BenchmarkEngine
import com.intellinav.engine.constraints.NonHolonomicConstraints
import com.intellinav.engine.fusion.ErrorStateKalmanFilter
import com.intellinav.engine.ins.DeadReckoningEngine
import com.intellinav.engine.mapmatching.MapMatchingEngine
import com.intellinav.engine.preprocessing.ImuPreprocessor
import com.intellinav.engine.ai.MLInferenceEngine
import com.intellinav.engine.replay.ReplayEngine
import com.intellinav.engine.sensors.SensorCollector
import com.intellinav.engine.velocity.VelocityEstimator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class NavigationRepository(context: Context) {
    private var lastMapUiUpdateTime = 0L
    private val UI_UPDATE_INTERVAL_MS = 100L // 10Hz target

    private val sensorCollector = SensorCollector(context)
    private val aligner = InVehicleAligner()
    private val preprocessor = ImuPreprocessor()
    private val velocityEstimator = VelocityEstimator()
    private val drEngine = DeadReckoningEngine()
    private val eskf = ErrorStateKalmanFilter()
    private val nhc = NonHolonomicConstraints()
    private val mapMatcher = MapMatchingEngine()
    private val mlEngine = MLInferenceEngine()
    val replayEngine = ReplayEngine()
    val benchmarkEngine = BenchmarkEngine()

    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // State Flows exposed to UI
    private val _rawSensor = MutableStateFlow(RawSensorData())
    val rawSensor: StateFlow<RawSensorData> = _rawSensor.asStateFlow()

    private val _filteredSensor = MutableStateFlow(FilteredSensorData())
    val filteredSensor: StateFlow<FilteredSensorData> = _filteredSensor.asStateFlow()

    private val _orientation = MutableStateFlow(OrientationData())
    val orientation: StateFlow<OrientationData> = _orientation.asStateFlow()

    private val _velocity = MutableStateFlow(VelocityData())
    val velocity: StateFlow<VelocityData> = _velocity.asStateFlow()

    private val _gnss = MutableStateFlow(GnssData())
    val gnss: StateFlow<GnssData> = _gnss.asStateFlow()

    private val _positionEstimate = MutableStateFlow(PositionEstimate())
    val positionEstimate: StateFlow<PositionEstimate> = _positionEstimate.asStateFlow()

    private val _mapMatchState = MutableStateFlow(MapMatchState())
    val mapMatchState: StateFlow<MapMatchState> = _mapMatchState.asStateFlow()

    private val _navigationMode = MutableStateFlow(NavigationMode.GNSS_AVAILABLE)
    val navigationMode: StateFlow<NavigationMode> = _navigationMode.asStateFlow()

    private val _systemHealth = MutableStateFlow(SystemHealth())
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()

    private val _timeSinceGnssLoss = MutableStateFlow(0L)
    val timeSinceGnssLoss: StateFlow<Long> = _timeSinceGnssLoss.asStateFlow()

    private var gnssLossTimestamp = 0L

    private val _aiFeatures = MutableStateFlow(AiFeatures())
    val aiFeatures: StateFlow<AiFeatures> = _aiFeatures.asStateFlow()

    private val _benchmarkMetrics = MutableStateFlow(BenchmarkMetrics())
    val benchmarkMetrics: StateFlow<BenchmarkMetrics> = _benchmarkMetrics.asStateFlow()

    // Outage Simulation control
    private var isSimulatingOutage = false
    private var outageTimerJob: Job? = null
    private var outageDurationSec = 30
    private var outageRemainingSec = 0
    private val _outageCountdown = MutableStateFlow(0)
    val outageCountdown: StateFlow<Int> = _outageCountdown.asStateFlow()

    private var isReplayMode = false

    fun startSystem() {
        sensorCollector.startListening()
        startProcessingLoop()

    }

    fun stopSystem() {
        sensorCollector.stopListening()
        repositoryScope.cancel()
    }

    private fun startProcessingLoop() {
        // High frequency sensor collection background pipeline
        repositoryScope.launch {
            sensorCollector.rawSensorFlow.collect { raw ->
                if (!isReplayMode) {
                    processLiveFrame(raw)
                }
            }
        }

        // Replay tick loop
        repositoryScope.launch {
            while (isActive) {
                delay(100L) // 10Hz tick for replay & presentation state updates
                sensorCollector.checkGnssStaleness()
                if (isReplayMode) {
                    val frame = replayEngine.tickNextFrame()
                    if (frame != null) {
                        processReplayFrame(frame)
                    }
                }
                updateSystemHealth()
            }
        }
    }

    private fun processLiveFrame(raw: RawSensorData) {
        // 1. Raw IMU Preprocessing & Filtering
        val filtered = preprocessor.preprocess(raw)
        val currentGnss = sensorCollector.gnssFlow.value
        val isGyroOk = sensorCollector.gyroStatus.value == SensorAvailabilityStatus.AVAILABLE
        val isMagOk = sensorCollector.magStatus.value == SensorAvailabilityStatus.AVAILABLE

        // 2. GNSS Availability Verification (Real signal + Outage check)
        val isGnssAvailable = !isSimulatingOutage && currentGnss.isAvailable

        // 3. Coordinate Alignment (Phone Frame -> Vehicle Body Frame)
        val orient = aligner.processSensors(
            raw = raw,
            gnss = currentGnss,
            isGyroAvailable = isGyroOk,
            isMagAvailable = isMagOk
        )
        val vehicleAcc = aligner.transformToVehicleFrame(floatArrayOf(filtered.accX, filtered.accY, filtered.accZ), orient)

        // 4. Kinematics Velocity Estimation & Non-Holonomic Constraints (NHC)
        val rawVel = velocityEstimator.estimateVelocity(
            raw = raw,
            filtered = filtered,
            vehicleAcc = vehicleAcc,
            gnss = currentGnss,
            isGnssAvailable = isGnssAvailable,
            isGyroAvailable = isGyroOk
        )
        val constrainedVel = nhc.applyConstraints(rawVel)

        // 5. Strapdown Dead Reckoning Propagation
        val insPos = drEngine.updatePosition(
            velocity = constrainedVel,
            orientation = orient,
            gnss = currentGnss,
            isGnssAvailable = isGnssAvailable,
            timestampNanos = raw.timestampNanos
        )

        // 6. Sensor Fusion (Error-State Kalman Filter)
        val hardwareMode = sensorCollector.sensorHardwareMode.value
        val (mode, fusedPos) = eskf.processFusion(
            insEstimate = insPos,
            gnss = currentGnss,
            isGnssAvailable = isGnssAvailable,
            isOutageSimulated = isSimulatingOutage,
            hardwareMode = hardwareMode
        )

        // 7. Map Matching Snap onto Road Geometry
        val mapMatched = mapMatcher.matchToRoadNetwork(fusedPos, nhc.isEnabled())

        // 8. AI-ML Inference Pipeline
        val inference = mlEngine.processSensorInference(raw)
        val aiFeat = mlEngine.aiFeatures.value

        // 9. State Updates (High-frequency state flows)
        _rawSensor.value = raw
        _filteredSensor.value = filtered
        _orientation.value = orient
        _velocity.value = constrainedVel
        _gnss.value = currentGnss
        _mapMatchState.value = mapMatched
        _aiFeatures.value = aiFeat

        // 10. 10Hz Rate Limiter for Map UI Stability (Prevents Thread Choke & Camera Freezing)
        val now = System.currentTimeMillis()
        if (now - lastMapUiUpdateTime >= UI_UPDATE_INTERVAL_MS) {
            lastMapUiUpdateTime = now
            _positionEstimate.value = if (mapMatched.matchConfidence > 0.5f && nhc.isEnabled()) {
                fusedPos.copy(latitude = mapMatched.matchedLatitude, longitude = mapMatched.matchedLongitude)
            } else {
                fusedPos
            }
        }

        // 11. Navigation Mode Transition Management
        if (!isGnssAvailable) {
            if (gnssLossTimestamp == 0L) {
                gnssLossTimestamp = System.currentTimeMillis()
            }
            _timeSinceGnssLoss.value = (System.currentTimeMillis() - gnssLossTimestamp) / 1000
            _navigationMode.value = NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE
        } else {
            gnssLossTimestamp = 0L
            _timeSinceGnssLoss.value = 0L
            _navigationMode.value = mode
        }
    }
    private fun processReplayFrame(frame: com.intellinav.engine.replay.ReplayFrame) {
        val filtered = preprocessor.preprocess(frame.rawSensor)
        val orient = aligner.processSensors(
            raw = frame.rawSensor,
            gnss = frame.gnssData,
            isGyroAvailable = true,
            isMagAvailable = true
        )
        val vehicleAcc = aligner.transformToVehicleFrame(floatArrayOf(filtered.accX, filtered.accY, filtered.accZ), orient)

        val rawVel = velocityEstimator.estimateVelocity(
            raw = frame.rawSensor,
            filtered = filtered,
            vehicleAcc = vehicleAcc,
            gnss = frame.gnssData,
            isGnssAvailable = frame.isGnssAvailableInFrame
        )
        val constrainedVel = nhc.applyConstraints(rawVel)

        val insPos = drEngine.updatePosition(
            velocity = constrainedVel,
            orientation = orient,
            gnss = frame.gnssData,
            isGnssAvailable = frame.isGnssAvailableInFrame,
            timestampNanos = frame.rawSensor.timestampNanos
        )

        val (mode, fusedPos) = eskf.processFusion(
            insEstimate = insPos,
            gnss = frame.gnssData,
            isGnssAvailable = frame.isGnssAvailableInFrame,
            isOutageSimulated = !frame.isGnssAvailableInFrame
        )

        val mapMatched = mapMatcher.matchToRoadNetwork(fusedPos, nhc.isEnabled())
        val inference = mlEngine.processSensorInference(frame.rawSensor)
        val aiFeat = mlEngine.aiFeatures.value

        _rawSensor.value = frame.rawSensor
        _filteredSensor.value = filtered
        _orientation.value = orient
        _velocity.value = constrainedVel
        _gnss.value = frame.gnssData
        _positionEstimate.value = fusedPos.copy(isReplay = true)
        _mapMatchState.value = mapMatched
        _navigationMode.value = mode
        _aiFeatures.value = aiFeat
    }

    fun triggerGnssOutage(durationSec: Int) {
        outageTimerJob?.cancel()
        isSimulatingOutage = true
        outageDurationSec = durationSec
        outageRemainingSec = durationSec
        _outageCountdown.value = outageRemainingSec
        _navigationMode.value = NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE

        outageTimerJob = repositoryScope.launch {
            while (outageRemainingSec > 0 && isSimulatingOutage) {
                delay(1000L)
                outageRemainingSec--
                _outageCountdown.value = outageRemainingSec
            }
            restoreGnss()
        }
    }

    fun restoreGnss() {
        outageTimerJob?.cancel()
        isSimulatingOutage = false
        _outageCountdown.value = 0
        _navigationMode.value = NavigationMode.GNSS_RESTORED_FUSION_RECOVERING

        repositoryScope.launch {
            val restoredPos = drEngine.performDriftCorrection(_gnss.value)
            _positionEstimate.value = restoredPos
            delay(3000L)
            if (!isSimulatingOutage) {
                _navigationMode.value = NavigationMode.GNSS_AVAILABLE
            }
        }
    }

    fun startCalibration() = aligner.startCalibration()
    fun resetCalibration() = aligner.resetCalibration()

    fun setReplayMode(enabled: Boolean) {
        isReplayMode = enabled
        if (enabled) {
            _navigationMode.value = NavigationMode.DEMO_REPLAY
        } else {
            replayEngine.stop()
            _navigationMode.value = NavigationMode.GNSS_AVAILABLE
        }
    }

    fun runBenchmark() {
        val frames = replayEngine.getAllFrames()
        _benchmarkMetrics.value = benchmarkEngine.computeMetricsFromReplay(frames)
    }

    private fun updateSystemHealth() {
        _systemHealth.value = SystemHealth(
            sensorRateHz = 60f,
            uiRateHz = 10f,
            latencyMs = 4L,
            sensorHardwareMode = sensorCollector.sensorHardwareMode.value,
            accStatus = sensorCollector.accStatus.value,
            gyroStatus = sensorCollector.gyroStatus.value,
            magStatus = sensorCollector.magStatus.value,
            gnssStatus = sensorCollector.gnssStatus.value,
            calibrationState = _orientation.value.status,
            drReady = true,
            mapMatchActive = mapMatcher.matchToRoadNetwork(_positionEstimate.value, nhc.isEnabled()).matchConfidence > 0.5f,
            nhcActive = nhc.isEnabled(),
            aiPipelineStatus = "Prototype inference pipeline — trained model integration pending"
        )
    }
}
