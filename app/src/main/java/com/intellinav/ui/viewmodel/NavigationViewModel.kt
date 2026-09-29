package com.intellinav.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.intellinav.data.model.AppSettings
import com.intellinav.data.model.BenchmarkMetrics
import com.intellinav.data.model.FilteredSensorData
import com.intellinav.data.model.GnssData
import com.intellinav.data.model.MapMatchState
import com.intellinav.data.model.MapStyle
import com.intellinav.data.model.NavigationMode
import com.intellinav.data.model.OrientationData
import com.intellinav.data.model.PositionEstimate
import com.intellinav.data.model.RawSensorData
import com.intellinav.data.model.SystemHealth
import com.intellinav.data.model.UnitSystem
import com.intellinav.data.model.VehicleType
import com.intellinav.data.model.VelocityData
import com.intellinav.data.repository.GeocodingRepository
import com.intellinav.data.repository.NavigationRepository
import com.intellinav.data.repository.RoutingRepository
import com.intellinav.data.repository.SearchResult
import com.intellinav.data.repository.SettingsRepository
import com.intellinav.engine.ai.AiFeatures
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


enum class NavStatus {
    IDLE,
    SEARCHING,
    PREVIEW,
    SELECTING_VEHICLE,
    READY,
    NAVIGATING
}


class NavigationViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "INTELLINAV_NAV_VM"
        private const val SEARCH_DEBOUNCE_MS = 350L
    }

    // -------------------------------------------------------------------------
    // Repositories
    // -------------------------------------------------------------------------

    val repository = NavigationRepository(application)

    val settingsRepository = SettingsRepository()

    val routingRepository = RoutingRepository()

    private val geocodingRepository =
        GeocodingRepository(application)


    // -------------------------------------------------------------------------
    // Sensor / Navigation state
    // -------------------------------------------------------------------------

    val rawSensor: StateFlow<RawSensorData> =
        repository.rawSensor

    val filteredSensor: StateFlow<FilteredSensorData> =
        repository.filteredSensor

    val orientation: StateFlow<OrientationData> =
        repository.orientation

    val velocity: StateFlow<VelocityData> =
        repository.velocity

    val gnss: StateFlow<GnssData> =
        repository.gnss

    val positionEstimate: StateFlow<PositionEstimate> =
        repository.positionEstimate

    val mapMatchState: StateFlow<MapMatchState> =
        repository.mapMatchState

    val navigationMode: StateFlow<NavigationMode> =
        repository.navigationMode

    val systemHealth: StateFlow<SystemHealth> =
        repository.systemHealth

    val aiFeatures: StateFlow<AiFeatures> =
        repository.aiFeatures

    val benchmarkMetrics: StateFlow<BenchmarkMetrics> =
        repository.benchmarkMetrics

    val outageCountdown: StateFlow<Int> =
        repository.outageCountdown

    val timeSinceGnssLoss: StateFlow<Long> =
        repository.timeSinceGnssLoss


    // -------------------------------------------------------------------------
    // Route state
    // -------------------------------------------------------------------------

    val currentRoute:
            StateFlow<com.intellinav.data.repository.RouteInfo?> =
        routingRepository.currentRoute


    // -------------------------------------------------------------------------
    // Search state
    // -------------------------------------------------------------------------

    private val _searchResults =
        MutableStateFlow<List<SearchResult>>(emptyList())

    val searchResults: StateFlow<List<SearchResult>> =
        _searchResults.asStateFlow()


    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()


    private val _isSearching =
        MutableStateFlow(false)

    val isSearching: StateFlow<Boolean> =
        _isSearching.asStateFlow()


    private val _searchError =
        MutableStateFlow<String?>(null)

    val searchError: StateFlow<String?> =
        _searchError.asStateFlow()


    private var searchJob: Job? = null


    // -------------------------------------------------------------------------
    // Navigation status
    // -------------------------------------------------------------------------

    private val _navStatus =
        MutableStateFlow(NavStatus.IDLE)

    val navStatus: StateFlow<NavStatus> =
        _navStatus.asStateFlow()


    // -------------------------------------------------------------------------
    // Selected destination
    // -------------------------------------------------------------------------

    private val _selectedDestination =
        MutableStateFlow<SearchResult?>(null)

    val selectedDestination:
            StateFlow<SearchResult?> =
        _selectedDestination.asStateFlow()


    // -------------------------------------------------------------------------
    // User-following state
    // -------------------------------------------------------------------------

    private val _isFollowingUser =
        MutableStateFlow(true)

    val isFollowingUser:
            StateFlow<Boolean> =
        _isFollowingUser.asStateFlow()


    // -------------------------------------------------------------------------
    // Start position override
    //
    // Normally the app uses live positionEstimate.
    // This override is only used when explicitly set by the UI/demo.
    // -------------------------------------------------------------------------

    private val _startPositionOverride =
        MutableStateFlow<LatLng?>(null)

    val startPositionOverride:
            StateFlow<LatLng?> =
        _startPositionOverride.asStateFlow()


    // -------------------------------------------------------------------------
    // Settings
    // -------------------------------------------------------------------------

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings


    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    init {
        repository.startSystem()
    }


    // =========================================================================
    // POSITION
    // =========================================================================

    /**
     * Set an optional manual/demo starting position.
     *
     * Pass null to return to the real live positionEstimate.
     */
    fun setStartPosition(location: LatLng?) {
        _startPositionOverride.value = location

        Log.d(
            TAG,
            "Start position override = $location"
        )
    }


    /**
     * Clears manual starting position and returns
     * route calculations to the live position estimate.
     */
    fun clearStartPositionOverride() {
        _startPositionOverride.value = null

        Log.d(
            TAG,
            "Start position override cleared. Using live position."
        )
    }


    /**
     * Returns the best available current position.
     *
     * Priority:
     * 1. Explicit override
     * 2. Live PositionEstimate
     */
    private fun getCurrentStartPosition(): LatLng? {

        // Explicit override has priority.
        _startPositionOverride.value?.let {
            return it
        }

        val position = positionEstimate.value

        val latitude = position.latitude
        val longitude = position.longitude

        // Prevent accidental routing from 0,0.
        if (
            latitude.isNaN() ||
            longitude.isNaN() ||
            latitude == 0.0 ||
            longitude == 0.0
        ) {
            Log.w(
                TAG,
                "Live position is not valid yet: lat=$latitude lon=$longitude"
            )

            return null
        }

        return LatLng(
            latitude,
            longitude
        )
    }


    // =========================================================================
    // NAVIGATION STATUS
    // =========================================================================

    fun setNavStatus(status: NavStatus) {

        _navStatus.value = status

        if (status == NavStatus.NAVIGATING) {
            _isFollowingUser.value = true
        }

        Log.d(
            TAG,
            "Navigation status = $status"
        )
    }


    fun setFollowingUser(following: Boolean) {
        _isFollowingUser.value = following
    }


    // =========================================================================
    // SEARCH
    // =========================================================================

    /**
     * Update search text without immediately sending a request.
     *
     * UI can call this on every TextField change.
     */
    fun updateSearchQuery(query: String) {

        _searchQuery.value = query

        _searchError.value = null

        searchJob?.cancel()

        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty()) {

            _searchResults.value = emptyList()

            if (_navStatus.value == NavStatus.SEARCHING) {
                _navStatus.value = NavStatus.IDLE
            }

            return
        }

        searchJob = viewModelScope.launch {

            delay(SEARCH_DEBOUNCE_MS)

            searchLocationInternal(trimmedQuery)
        }
    }


    /**
     * Direct search function.
     *
     * Useful when user presses Search / Enter.
     */
    fun searchLocation(query: String) {

        _searchQuery.value = query

        searchJob?.cancel()

        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty()) {

            _searchResults.value = emptyList()
            _searchError.value = null
            _isSearching.value = false

            return
        }

        searchJob = viewModelScope.launch {

            searchLocationInternal(trimmedQuery)
        }
    }


    private suspend fun searchLocationInternal(
        query: String
    ) {

        Log.d(
            TAG,
            "Searching location: $query"
        )

        setNavStatus(NavStatus.SEARCHING)

        _isSearching.value = true
        _searchError.value = null

        try {

            val results =
                geocodingRepository.search(query)

            Log.d(
                TAG,
                "Search completed. Results = ${results.size}"
            )

            _searchResults.value = results

            if (results.isEmpty()) {

                _searchError.value =
                    "No locations found for \"$query\""
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Location search failed",
                e
            )

            _searchResults.value = emptyList()

            _searchError.value =
                e.message ?: "Unable to search location"

        } finally {

            _isSearching.value = false
        }
    }


    /**
     * Clears search suggestions.
     */
    fun clearSearchResults() {

        _searchResults.value = emptyList()
        _searchError.value = null
    }


    /**
     * Clears everything related to destination search.
     */
    fun clearSearch() {

        searchJob?.cancel()

        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _searchError.value = null
        _isSearching.value = false
    }


    // =========================================================================
    // DESTINATION
    // =========================================================================

    /**
     * Select a destination from search results.
     *
     * This moves the app into route preview.
     */
    fun selectDestination(
        result: SearchResult
    ) {

        Log.d(
            TAG,
            "Destination selected: ${result.name} @ ${result.location}"
        )

        _selectedDestination.value = result

        clearSearch()

        setNavStatus(NavStatus.PREVIEW)

        // Generate the initial preview route using CAR.
        requestRoute(
            destination = result.location,
            name = result.name,
            vehicleType = VehicleType.CAR
        )
    }


    /**
     * Clears selected destination and route.
     */
    fun clearDestination() {

        _selectedDestination.value = null

        clearRoute()

        setNavStatus(NavStatus.IDLE)
    }


    // =========================================================================
    // VEHICLE SELECTION
    // =========================================================================

    fun proceedToVehicleSelection() {

        if (_selectedDestination.value == null) {
            Log.w(
                TAG,
                "Cannot select vehicle: no destination selected"
            )

            return
        }

        setNavStatus(
            NavStatus.SELECTING_VEHICLE
        )
    }


    /**
     * Select vehicle and regenerate route using that vehicle.
     */
    fun selectVehicle(
        type: VehicleType
    ) {

        val destination =
            _selectedDestination.value

        if (destination == null) {

            Log.w(
                TAG,
                "Cannot select vehicle: destination is null"
            )

            return
        }

        Log.d(
            TAG,
            "Vehicle selected: $type"
        )

        // Generate route with selected vehicle.
        requestRoute(
            destination = destination.location,
            name = destination.name,
            vehicleType = type
        )

        setNavStatus(
            NavStatus.READY
        )
    }


    // =========================================================================
    // ROUTING
    // =========================================================================

    /**
     * Requests a route from current live/manual position
     * to selected destination.
     */
    fun requestRoute(
        destination: LatLng,
        name: String,
        vehicleType: VehicleType = VehicleType.CAR
    ) {

        viewModelScope.launch {

            val start =
                getCurrentStartPosition()

            if (start == null) {

                Log.w(
                    TAG,
                    "Cannot request route: current location unavailable"
                )

                _searchError.value =
                    "Waiting for your current location..."

                return@launch
            }

            Log.d(
                TAG,
                """
                Requesting route
                Start: $start
                Destination: $destination
                Name: $name
                Vehicle: $vehicleType
                """.trimIndent()
            )

            try {

                routingRepository.getRoute(
                    start,
                    destination,
                    name,
                    vehicleType
                )

                Log.d(
                    TAG,
                    "Route request completed"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Route request failed",
                    e
                )

                _searchError.value =
                    e.message ?: "Unable to calculate route"
            }
        }
    }


    fun clearRoute() {

        routingRepository.clearRoute()
    }


    // =========================================================================
    // NAVIGATION
    // =========================================================================

    /**
     * Starts actual navigation only when a destination
     * and route are available.
     */
    fun startNavigation() {

        val destination =
            _selectedDestination.value

        if (destination == null) {

            Log.w(
                TAG,
                "Cannot start navigation: destination missing"
            )

            return
        }

        if (currentRoute.value == null) {

            Log.w(
                TAG,
                "Cannot start navigation: route missing"
            )

            // Try requesting the route again.
            requestRoute(
                destination = destination.location,
                name = destination.name
            )

            return
        }

        Log.d(
            TAG,
            "Navigation started"
        )

        setNavStatus(
            NavStatus.NAVIGATING
        )
    }


    /**
     * Stops navigation and clears destination/route.
     */
    fun stopNavigation() {

        Log.d(
            TAG,
            "Navigation stopped"
        )

        setNavStatus(
            NavStatus.IDLE
        )

        _selectedDestination.value = null

        clearSearch()

        clearRoute()
    }


    // =========================================================================
    // GNSS OUTAGE / DEAD RECKONING
    // =========================================================================

    fun triggerOutage(seconds: Int) {

        repository.triggerGnssOutage(
            seconds
        )
    }


    fun restoreGnss() {

        repository.restoreGnss()
    }


    // =========================================================================
    // CALIBRATION
    // =========================================================================

    fun startCalibration() {

        repository.startCalibration()
    }


    fun resetCalibration() {

        repository.resetCalibration()
    }


    // =========================================================================
    // REPLAY
    // =========================================================================

    fun setReplayMode(enabled: Boolean) {

        repository.setReplayMode(
            enabled
        )
    }


    fun playReplay() {

        repository.replayEngine.play()
    }


    fun pauseReplay() {

        repository.replayEngine.pause()
    }


    fun stopReplay() {

        repository.replayEngine.stop()
    }


    fun seekReplay(
        fraction: Float
    ) {

        repository.replayEngine.seekTo(
            fraction
        )
    }


    fun setReplaySpeed(
        speed: Float
    ) {

        repository.replayEngine.setSpeedMultiplier(
            speed
        )
    }


    // =========================================================================
    // BENCHMARK
    // =========================================================================

    fun runBenchmark() {

        repository.runBenchmark()
    }


    // =========================================================================
    // SETTINGS
    // =========================================================================

    fun updateUnitSystem(
        unit: UnitSystem
    ) {

        settingsRepository.updateUnitSystem(
            unit
        )
    }


    fun updateMapStyle(
        style: MapStyle
    ) {

        settingsRepository.updateMapStyle(
            style
        )
    }


    fun toggleNhc(
        enabled: Boolean
    ) {

        settingsRepository.toggleNhc(
            enabled
        )
    }


    fun toggleMapMatching(
        enabled: Boolean
    ) {

        settingsRepository.toggleMapMatching(
            enabled
        )
    }


    fun toggleAiAssistedSpeed(
        enabled: Boolean
    ) {

        settingsRepository.toggleAiAssistedSpeed(
            enabled
        )
    }


    fun setOutageDurationPref(
        seconds: Int
    ) {

        settingsRepository.setOutageDuration(
            seconds
        )
    }


    // =========================================================================
    // CLEANUP
    // =========================================================================

    override fun onCleared() {

        searchJob?.cancel()

        repository.stopSystem()

        super.onCleared()
    }
}