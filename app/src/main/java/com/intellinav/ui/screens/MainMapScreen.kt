package com.intellinav.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellinav.data.model.NavigationMode
import com.intellinav.data.model.VehicleType
import com.intellinav.ui.components.RealMapView
import com.intellinav.ui.navigation.AppDrawer
import com.intellinav.ui.theme.*
import com.intellinav.ui.viewmodel.AuthViewModel
import com.intellinav.ui.viewmodel.NavStatus
import com.intellinav.ui.viewmodel.NavigationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class SearchField {
    CURRENT_LOCATION,
    DESTINATION
}

@Composable
fun MainMapScreen(
    navViewModel: NavigationViewModel,
    authViewModel: AuthViewModel,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {

    val drawerState =
        rememberDrawerState(
            initialValue = DrawerValue.Closed
        )

    val scope =
        rememberCoroutineScope()

    // ================================================================
    // VIEWMODEL STATE
    // ================================================================

    val pos by
    navViewModel.positionEstimate.collectAsState()

    val mode by
    navViewModel.navigationMode.collectAsState()

    val vel by
    navViewModel.velocity.collectAsState()

    val route by
    navViewModel.currentRoute.collectAsState()

    val drTime by
    navViewModel.timeSinceGnssLoss.collectAsState()

    val searchResults by
    navViewModel.searchResults.collectAsState()

    val searchError by
    navViewModel.searchError.collectAsState()

    val navStatus by
    navViewModel.navStatus.collectAsState()

    val isFollowingUser by
    navViewModel.isFollowingUser.collectAsState()

    val selectedDestination by
    navViewModel.selectedDestination.collectAsState()

    val isSearching by
    navViewModel.isSearching.collectAsState()


    // ================================================================
    // SEARCH STATE
    // ================================================================

    /*
     * IMPORTANT:
     *
     * Empty string means:
     * "Use live location / show placeholder"
     *
     * We DO NOT store "Your location" here.
     */

    var currentLocationQuery by remember {
        mutableStateOf("")
    }

    var destinationQuery by remember {
        mutableStateOf("")
    }

    var activeSearchField by remember {
        mutableStateOf(
            SearchField.DESTINATION
        )
    }


    // ================================================================
    // FOCUS
    // ================================================================

    val currentLocationFocusRequester =
        remember {
            FocusRequester()
        }

    val destinationFocusRequester =
        remember {
            FocusRequester()
        }


    // ================================================================
    // ACTIVE QUERY
    // ================================================================

    val activeQuery =
        if (
            activeSearchField ==
            SearchField.CURRENT_LOCATION
        ) {
            currentLocationQuery
        } else {
            destinationQuery
        }


    // ================================================================
    // SEARCH DEBOUNCE
    // ================================================================
    //
    // Search is NOT called for every single keystroke immediately.
    //
    // Example:
    // H
    // Hb
    // Hb t
    // Hb to
    // Hb town
    //
    // Only after the user pauses typing for 350 ms
    // we send the request.
    //

    LaunchedEffect(
        navStatus,
        activeSearchField,
        activeQuery
    ) {

        if (
            navStatus !=
            NavStatus.SEARCHING
        ) {
            return@LaunchedEffect
        }

        val query =
            activeQuery.trim()

        if (query.length < 2) {

            navViewModel.clearSearchResults()

            return@LaunchedEffect
        }

        delay(350)

        navViewModel.searchLocation(
            query
        )
    }


    // ================================================================
    // DRAWER
    // ================================================================

    ModalNavigationDrawer(

        drawerState = drawerState,

        drawerContent = {

            AppDrawer(

                currentRoute =
                    "live_navigation",

                onNavigate =
                    onNavigate,

                onSignOut =
                    onSignOut,

                closeDrawer = {

                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        DeepSpaceNavy
                    )
        ) {


            // ========================================================
            // GOOGLE MAP
            // ========================================================

            val isRoutePreview = navStatus == NavStatus.PREVIEW || navStatus == NavStatus.SELECTING_VEHICLE || navStatus == NavStatus.READY

            RealMapView(

                position =
                    pos,

                route =
                    route,

                navigationMode =
                    mode,

                isFollowingUser =
                    isFollowingUser,
                    
                isRoutePreview = 
                    isRoutePreview,

                modifier =
                    Modifier.fillMaxSize(),

                onMapReady = {
                    // Map instance is internally handled
                    // by RealMapView.
                },

                onRecenter = {

                    navViewModel
                        .setFollowingUser(true)
                },

                onUserInitiatedMove = {

                    navViewModel
                        .setFollowingUser(false)
                }
            )

            // ========================================================
            // SEARCH OVERLAY (Placed here to be over the map)
            // ========================================================
            
            if (navStatus == NavStatus.SEARCHING) {
                SearchOverlay(
                    currentLocationQuery = currentLocationQuery,
                    destinationQuery = destinationQuery,
                    activeField = activeSearchField,
                    onCurrentLocationQueryChange = { query ->
                        currentLocationQuery = query
                        activeSearchField = SearchField.CURRENT_LOCATION
                        navViewModel.clearSearchResults()
                    },
                    onDestinationQueryChange = { query ->
                        destinationQuery = query
                        activeSearchField = SearchField.DESTINATION
                        navViewModel.clearSearchResults()
                    },
                    onCurrentLocationTap = {
                        activeSearchField = SearchField.CURRENT_LOCATION
                        navViewModel.clearSearchResults()
                    },
                    onDestinationTap = {
                        activeSearchField = SearchField.DESTINATION
                        navViewModel.clearSearchResults()
                    },
                    onUseCurrentLocation = {
                        currentLocationQuery = ""
                        navViewModel.setStartPosition(null)
                        navViewModel.clearSearchResults()
                        activeSearchField = SearchField.DESTINATION
                        scope.launch {
                            delay(100)
                            destinationFocusRequester.requestFocus()
                        }
                    },
                    onBack = {
                        currentLocationQuery = ""
                        destinationQuery = ""
                        navViewModel.clearSearchResults()
                        navViewModel.setNavStatus(NavStatus.IDLE)
                    },
                    results = searchResults,
                    searchError = searchError,
                    isSearching = isSearching,
                    onResultSelect = { result ->
                        if (activeSearchField == SearchField.CURRENT_LOCATION) {
                            currentLocationQuery = result.name
                            navViewModel.setStartPosition(result.location)
                            navViewModel.clearSearchResults()
                            activeSearchField = SearchField.DESTINATION
                            scope.launch {
                                delay(100)
                                destinationFocusRequester.requestFocus()
                            }
                        } else {
                            destinationQuery = result.name
                            navViewModel.clearSearchResults()
                            navViewModel.selectDestination(result)
                        }
                    },
                    currentLocationFocusRequester = currentLocationFocusRequester,
                    destinationFocusRequester = destinationFocusRequester
                )
            }


            // ========================================================
            // TOP UI
            // ========================================================

            when (navStatus) {


                // ====================================================
                // IDLE
                // ====================================================

                NavStatus.IDLE -> {

                    IdleSearchBar(

                        onSearchTap = {

                            activeSearchField =
                                SearchField.DESTINATION

                            navViewModel
                                .setNavStatus(
                                    NavStatus.SEARCHING
                                )
                        },

                        onMenuTap = {

                            scope.launch {
                                drawerState.open()
                            }
                        },

                        onProfileTap = {

                            onNavigate(
                                "profile"
                            )
                        }
                    )
                }


                // ====================================================
                // SEARCH
                // ====================================================

                NavStatus.SEARCHING -> {
                    // Handled above the 'when' block to ensure proper Z-index rendering over map
                }


                // ====================================================
                // VEHICLE / READY
                // ====================================================

                NavStatus.SELECTING_VEHICLE,
                NavStatus.READY -> {

                    TopDirectionsCard(

                        destinationName =
                            selectedDestination
                                ?.name
                                ?: "Destination",

                        onBack = {

                            navViewModel
                                .stopNavigation()
                        }
                    )
                }


                // ====================================================
                // PREVIEW
                // ====================================================

                NavStatus.PREVIEW -> {
                    // Route is displayed by RealMapView.
                }


                // ====================================================
                // NAVIGATING
                // ====================================================

                NavStatus.NAVIGATING -> {
                    // Navigation banner below.
                }
            }


            // ========================================================
            // NAVIGATION BANNER
            // ========================================================

            if (
                navStatus ==
                NavStatus.NAVIGATING
            ) {

                NavigationBanner(

                    instruction =
                        route
                            ?.instructions
                            ?.firstOrNull()
                            ?: "Continue towards ${
                                selectedDestination
                                    ?.name
                                    ?: "destination"
                            }"
                )
            }


            // ========================================================
            // NAVIGATION FLOATING BUTTONS
            // ========================================================

            if (
                navStatus ==
                NavStatus.NAVIGATING
            ) {

                Column(

                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .padding(
                                end = 16.dp,
                                bottom = 120.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            14.dp
                        )
                ) {

                    FloatingNavButton(
                        icon =
                            Icons.Default.CompassCalibration
                    )

                    FloatingNavButton(
                        icon =
                            Icons.Default.Search
                    )

                    FloatingNavButton(
                        icon =
                            Icons.Default.VolumeUp
                    )
                }
            }


            // ========================================================
            // BOTTOM PANEL
            // ========================================================

            Box(

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter
                        )
                        .padding(
                            16.dp
                        )
                        .widthIn(
                            max = 600.dp
                        )
            ) {

                when (navStatus) {


                    // ==================================================
                    // ROUTE PREVIEW
                    // ==================================================

                    NavStatus.PREVIEW -> {

                        RoutePreviewCard(

                            destinationName =
                                selectedDestination
                                    ?.name
                                    ?: "Unknown",

                            durationMin =
                                route?.durationMin,

                            distanceKm =
                                route?.distanceKm,

                            onDirectionsTap = {

                                navViewModel
                                    .proceedToVehicleSelection()
                            },

                            onBackTap = {

                                navViewModel
                                    .stopNavigation()
                            }
                        )
                    }


                    // ==================================================
                    // VEHICLE
                    // ==================================================

                    NavStatus.SELECTING_VEHICLE -> {

                        VehicleSelectionCard(

                            onSelect = {
                                    type ->

                                navViewModel
                                    .selectVehicle(
                                        type
                                    )
                            },

                            onCancel = {

                                navViewModel
                                    .setNavStatus(
                                        NavStatus.PREVIEW
                                    )
                            }
                        )
                    }


                    // ==================================================
                    // READY
                    // ==================================================

                    NavStatus.READY -> {

                        NavigationReadyCard(

                            destinationName =
                                selectedDestination
                                    ?.name
                                    ?: "Unknown",

                            durationMin =
                                route?.durationMin,

                            distanceKm =
                                route?.distanceKm,

                            onStartTap = {

                                navViewModel
                                    .startNavigation()
                            },

                            onBackTap = {

                                navViewModel
                                    .setNavStatus(
                                        NavStatus.SELECTING_VEHICLE
                                    )
                            }
                        )
                    }


                    // ==================================================
                    // ACTIVE NAVIGATION
                    // ==================================================

                    NavStatus.NAVIGATING -> {

                        ActiveNavCard(

                            durationMin =
                                route?.durationMin,

                            distanceKm =
                                route?.distanceKm,

                            mode =
                                mode,

                            drTime =
                                drTime,

                            speedMs =
                                vel.forwardSpeedMs,

                            headingDeg =
                                pos.headingDeg,

                            onExitTap = {

                                navViewModel
                                    .stopNavigation()
                            }
                        )
                    }


                    // ==================================================
                    // IDLE TELEMETRY
                    // ==================================================

                    NavStatus.IDLE -> {

                        TelemetryCard(

                            mode =
                                mode,

                            speedMs =
                                vel.forwardSpeedMs,

                            headingDeg =
                                pos.headingDeg
                        )
                    }

                    else -> Unit
                }
            }


            // ========================================================
            // RECENTER
            // ========================================================

            if (
                !isFollowingUser &&
                navStatus !=
                NavStatus.SEARCHING
            ) {

                FloatingActionButton(

                    onClick = {

                        navViewModel
                            .setFollowingUser(
                                true
                            )
                    },

                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomEnd
                            )
                            .padding(
                                bottom = 180.dp,
                                end = 16.dp
                            ),

                    containerColor =
                        SurfaceSlate.copy(
                            alpha = 0.95f
                        ),

                    contentColor =
                        ElectricCyan
                ) {

                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription =
                            "Re-center"
                    )
                }
            }
        }
    }
}


// ========================================================================
// TOP DIRECTIONS CARD
// ========================================================================

@Composable
fun TopDirectionsCard(
    destinationName: String,
    onBack: () -> Unit
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription =
                        "Back",
                    tint =
                        Color.Black
                )
            }


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    Color(
                                        0xFF1A73E8
                                    )
                                )
                    )

                    Spacer(
                        Modifier.width(12.dp)
                    )

                    Text(
                        text =
                            if (
                                destinationName
                                    .isBlank()
                            ) {
                                "Your location"
                            } else {
                                "Your location"
                            },
                        color =
                            Color.Black,
                        fontSize =
                            16.sp
                    )
                }


                HorizontalDivider(
                    Modifier.padding(
                        start = 22.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                    color =
                        Color.LightGray
                            .copy(
                                alpha = 0.5f
                            )
                )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Place,
                        contentDescription =
                            "Destination",
                        tint =
                            Color(
                                0xFFD93025
                            ),
                        modifier =
                            Modifier.size(
                                20.dp
                            )
                    )

                    Spacer(
                        Modifier.width(12.dp)
                    )

                    Text(
                        text =
                            destinationName,
                        color =
                            Color.Black,
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            IconButton(
                onClick = {}
            ) {

                Icon(
                    Icons.Default.SwapVert,
                    contentDescription =
                        "Swap",
                    tint =
                        Color.DarkGray
                )
            }
        }
    }
}


// ========================================================================
// IDLE SEARCH BAR
// ========================================================================

@Composable
fun IdleSearchBar(
    onSearchTap: () -> Unit,
    onMenuTap: () -> Unit,
    onProfileTap: () -> Unit
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,

                    // Slightly lower than before.
                    top = 58.dp,

                    bottom = 16.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        // ==============================================================
        // HAMBURGER
        // ==============================================================

        IconButton(

            onClick =
                onMenuTap,

            modifier =
                Modifier
                    .size(48.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        SurfaceSlate.copy(
                            alpha = 0.95f
                        )
                    )
        ) {

            Icon(
                Icons.Default.Menu,
                contentDescription =
                    "Menu",
                tint =
                    ElectricCyan
            )
        }


        Spacer(
            Modifier.width(12.dp)
        )


        // ==============================================================
        // SEARCH BAR
        // ==============================================================

        Card(

            modifier =
                Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clickable(
                        onClick =
                            onSearchTap
                    ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        SurfaceSlate.copy(
                            alpha = 0.96f
                        )
                ),

            shape =
                RoundedCornerShape(
                    28.dp
                ),

            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 7.dp
                )
        ) {

            Row(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 17.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.Search,
                    contentDescription =
                        null,
                    tint =
                        TextSecondary
                )

                Spacer(
                    Modifier.width(12.dp)
                )

                Text(
                    text =
                        "Search here",
                    color =
                        TextSecondary,
                    fontSize =
                        17.sp
                )

                Spacer(
                    Modifier.weight(1f)
                )

                Icon(
                    Icons.Default.Mic,
                    contentDescription =
                        null,
                    tint =
                        TextSecondary
                )
            }
        }


        Spacer(
            Modifier.width(12.dp)
        )


        // ==============================================================
        // PROFILE
        // ==============================================================

        IconButton(

            onClick =
                onProfileTap,

            modifier =
                Modifier
                    .size(48.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        SurfaceSlate.copy(
                            alpha = 0.95f
                        )
                    )
        ) {

            Icon(
                Icons.Default.Person,
                contentDescription =
                    "Profile",
                tint =
                    TextPrimary
            )
        }
    }
}


// ========================================================================
// SEARCH OVERLAY
// ========================================================================

@Composable
fun SearchOverlay(

    currentLocationQuery: String,

    destinationQuery: String,

    activeField: SearchField,


    onCurrentLocationQueryChange:
        (String) -> Unit,

    onDestinationQueryChange:
        (String) -> Unit,


    onCurrentLocationTap:
        () -> Unit,

    onDestinationTap:
        () -> Unit,

    onUseCurrentLocation:
        () -> Unit,


    onBack:
        () -> Unit,


    results:
    List<com.intellinav.data.repository.SearchResult>,
    
    searchError: String?,
    
    isSearching: Boolean,

    onResultSelect:
        (
        com.intellinav.data.repository.SearchResult
    ) -> Unit,


    currentLocationFocusRequester:
    FocusRequester,

    destinationFocusRequester:
    FocusRequester
) {

    val activeQuery =
        if (
            activeField ==
            SearchField.CURRENT_LOCATION
        ) {
            currentLocationQuery
        } else {
            destinationQuery
        }


    Surface(

        modifier =
            Modifier.fillMaxSize(),

        color =
            Color(
                0xFFF7F8FA
            )
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {


            // ==========================================================
            // HEADER
            // ==========================================================

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = 16.dp,
                            top = 18.dp,
                            bottom = 10.dp
                        ),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick =
                        onBack
                ) {

                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription =
                            "Back",
                        tint =
                            Color(
                                0xFF202124
                            ),
                        modifier =
                            Modifier.size(
                                28.dp
                            )
                    )
                }


                Text(
                    text =
                        "Plan your route",
                    color =
                        Color(
                            0xFF202124
                        ),
                    fontSize =
                        21.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }


            // ==========================================================
            // TWO INPUT BOXES
            // ==========================================================

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),

                shape =
                    RoundedCornerShape(
                        22.dp
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
            ) {

                Column(

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 9.dp
                        )
                ) {


                    // ==================================================
                    // CURRENT LOCATION
                    // ==================================================

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .size(12.dp)
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        Color(
                                            0xFF1A73E8
                                        )
                                    )
                        )


                        Spacer(
                            Modifier.width(16.dp)
                        )


                        BasicTextField(

                            value =
                                currentLocationQuery,

                            onValueChange =
                                onCurrentLocationQueryChange,

                            singleLine =
                                true,

                            textStyle =
                                androidx.compose.ui.text
                                    .TextStyle(
                                        color =
                                            Color(
                                                0xFF202124
                                            ),
                                        fontSize =
                                            17.sp
                                    ),

                            keyboardOptions =
                                KeyboardOptions(
                                    imeAction =
                                        ImeAction.Next
                                ),

                            keyboardActions =
                                KeyboardActions(

                                    onNext = {

                                        onDestinationTap()

                                        destinationFocusRequester
                                            .requestFocus()
                                    }
                                ),

                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .focusRequester(
                                        currentLocationFocusRequester
                                    )
                                    .onFocusChanged {

                                        if (
                                            it.isFocused
                                        ) {
                                            onCurrentLocationTap()
                                        }
                                    },

                            decorationBox =
                                { innerTextField ->

                                    Box(

                                        modifier =
                                            Modifier.fillMaxSize(),

                                        contentAlignment =
                                            Alignment.CenterStart
                                    ) {

                                        if (
                                            currentLocationQuery
                                                .isEmpty()
                                        ) {

                                            Text(
                                                text =
                                                    "Your location",
                                                color =
                                                    Color(
                                                        0xFF5F6368
                                                    ),
                                                fontSize =
                                                    17.sp
                                            )
                                        }

                                        innerTextField()
                                    }
                                }
                        )


                        // ==================================================
                        // LIVE LOCATION
                        // ==================================================

                        IconButton(

                            onClick =
                                onUseCurrentLocation
                        ) {

                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription =
                                    "Use live location",
                                tint =
                                    Color(
                                        0xFF1A73E8
                                    ),
                                modifier =
                                    Modifier.size(
                                        24.dp
                                    )
                            )
                        }
                    }


                    // ==================================================
                    // DIVIDER
                    // ==================================================

                    HorizontalDivider(

                        modifier =
                            Modifier.padding(
                                start = 28.dp
                            ),

                        color =
                            Color(
                                0xFFE0E0E0
                            )
                    )


                    // ==================================================
                    // DESTINATION
                    // ==================================================

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(62.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(

                            Icons.Default.Place,

                            contentDescription =
                                "Destination",

                            tint =
                                Color(
                                    0xFFD93025
                                ),

                            modifier =
                                Modifier.size(
                                    21.dp
                                )
                        )


                        Spacer(
                            Modifier.width(14.dp)
                        )


                        BasicTextField(

                            value =
                                destinationQuery,

                            onValueChange =
                                onDestinationQueryChange,

                            singleLine =
                                true,

                            textStyle =
                                androidx.compose.ui.text
                                    .TextStyle(
                                        color =
                                            Color(
                                                0xFF202124
                                            ),
                                        fontSize =
                                            17.sp
                                    ),

                            keyboardOptions =
                                KeyboardOptions(
                                    imeAction =
                                        ImeAction.Search
                                ),

                            keyboardActions =
                                KeyboardActions(

                                    onSearch = {

                                        if (
                                            destinationQuery
                                                .trim()
                                                .length >= 2
                                        ) {

                                            onDestinationQueryChange(
                                                destinationQuery
                                            )
                                        }
                                    }
                                ),

                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .focusRequester(
                                        destinationFocusRequester
                                    )
                                    .onFocusChanged {

                                        if (
                                            it.isFocused
                                        ) {
                                            onDestinationTap()
                                        }
                                    },

                            decorationBox =
                                { innerTextField ->

                                    Box(

                                        modifier =
                                            Modifier.fillMaxSize(),

                                        contentAlignment =
                                            Alignment.CenterStart
                                    ) {

                                        if (
                                            destinationQuery
                                                .isEmpty()
                                        ) {

                                            Text(
                                                text =
                                                    "Search destination",
                                                color =
                                                    Color(
                                                        0xFF5F6368
                                                    ),
                                                fontSize =
                                                    17.sp
                                            )
                                        }

                                        innerTextField()
                                    }
                                }
                        )


                        // ==================================================
                        // SWAP
                        // ==================================================

                        IconButton(

                            onClick = {

                                /*
                                 * Swap values.
                                 *
                                 * Note:
                                 * This UI callback only swaps text.
                                 * Route will be regenerated when a
                                 * destination is selected.
                                 */

                                val oldCurrent =
                                    currentLocationQuery

                                onCurrentLocationQueryChange(
                                    destinationQuery
                                )

                                onDestinationQueryChange(
                                    oldCurrent
                                )
                            }
                        ) {

                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription =
                                    "Swap locations",
                                tint =
                                    Color(
                                        0xFF5F6368
                                    )
                            )
                        }
                    }
                }
            }


            // ==========================================================
            // SUGGESTIONS HEADER
            // ==========================================================

            if (
                activeQuery.isNotBlank()
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 22.dp,
                                end = 20.dp,
                                top = 20.dp,
                                bottom = 8.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Search,
                        contentDescription =
                            null,
                        tint =
                            Color(
                                0xFF5F6368
                            ),
                        modifier =
                            Modifier.size(
                                21.dp
                            )
                    )

                    Spacer(
                        Modifier.width(10.dp)
                    )

                    Text(
                        text =
                            "Suggestions",
                        color =
                            Color(
                                0xFF5F6368
                            ),
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            // ==========================================================
            // RESULTS
            // ==========================================================

            LazyColumn(

                modifier =
                    Modifier.fillMaxSize(),

                contentPadding =
                    PaddingValues(
                        bottom = 30.dp
                    )
            ) {


                // ======================================================
                // LIVE LOCATION QUICK ACTION
                // ======================================================

                if (
                    activeField ==
                    SearchField.CURRENT_LOCATION
                ) {

                    item {

                        ListItem(

                            headlineContent = {

                                Text(
                                    text =
                                        "Use current location",
                                    color =
                                        Color(
                                            0xFF202124
                                        ),
                                    fontSize =
                                        16.sp,
                                    fontWeight =
                                        FontWeight.Medium
                                )
                            },

                            supportingContent = {

                                Text(
                                    text =
                                        "Use your phone's live location",
                                    color =
                                        Color(
                                            0xFF5F6368
                                        ),
                                    fontSize =
                                        13.sp
                                )
                            },

                            leadingContent = {

                                Box(

                                    modifier =
                                        Modifier
                                            .size(42.dp)
                                            .clip(
                                                CircleShape
                                            )
                                            .background(
                                                Color(
                                                    0xFFE8F0FE
                                                )
                                            ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription =
                                            null,
                                        tint =
                                            Color(
                                                0xFF1A73E8
                                            )
                                    )
                                }
                            },

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUseCurrentLocation()
                                    },

                            colors =
                                ListItemDefaults.colors(
                                    containerColor =
                                        Color.Transparent
                                )
                        )

                        HorizontalDivider(
                            color =
                                Color(
                                    0xFFE5E5E5
                                )
                        )
                    }
                }


                // ======================================================
                // SEARCH RESULTS
                // ======================================================

                items(
                    items = results,
                    key = {
                        "${it.name}_${it.location.latitude}_${it.location.longitude}"
                    }
                ) { result ->

                    ListItem(

                        headlineContent = {

                            Text(
                                text =
                                    result.name,
                                color =
                                    Color(
                                        0xFF202124
                                    ),
                                fontSize =
                                    16.sp,
                                fontWeight =
                                    FontWeight.Medium
                            )
                        },

                        supportingContent = {

                            Text(
                                text =
                                    "Tap to select this location",
                                color =
                                    Color(
                                        0xFF70757A
                                    ),
                                fontSize =
                                    13.sp
                            )
                        },

                        leadingContent = {

                            Box(

                                modifier =
                                    Modifier
                                        .size(42.dp)
                                        .clip(
                                            CircleShape
                                        )
                                        .background(
                                            Color(
                                                0xFFF1F3F4
                                            )
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    Icons.Default.Place,
                                    contentDescription =
                                        null,
                                    tint =
                                        Color(
                                            0xFF5F6368
                                        )
                                )
                            }
                        },

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    onResultSelect(
                                        result
                                    )
                                },

                        colors =
                            ListItemDefaults.colors(
                                containerColor =
                                    Color.Transparent
                            )
                    )

                    HorizontalDivider(
                        color =
                            Color(
                                0xFFE5E5E5
                            ),
                        thickness =
                            0.5.dp
                    )
                }


                // ======================================================
                // LOADING / NO RESULTS / ERROR
                // ======================================================

                if (isSearching) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 45.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 3.dp,
                                color = Color(0xFF1A73E8)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Searching places...",
                                color = Color(0xFF5F6368),
                                fontSize = 16.sp
                            )
                        }
                    }
                } else if (searchError != null && activeQuery.trim().length >= 2) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 45.dp, start = 20.dp, end = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD93025), modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Search Unavailable",
                                color = Color(0xFFD93025),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = searchError,
                                color = Color(0xFF5F6368),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else if (
                    activeQuery
                        .trim()
                        .length >= 2 &&
                    results.isEmpty()
                ) {

                    item {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 45.dp
                                    ),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Spacer(
                                Modifier.height(
                                    14.dp
                                )
                            )

                            Text(
                                text =
                                    "No places found",
                                color =
                                    Color(
                                        0xFF5F6368
                                    ),
                                fontSize =
                                    16.sp
                            )

                            Text(
                                text =
                                    "Finding places related to your search",
                                color =
                                    Color(
                                        0xFF9AA0A6
                                    ),
                                fontSize =
                                    13.sp
                            )
                        }
                    }
                }


                // ======================================================
                // EMPTY DESTINATION
                // ======================================================

                if (
                    activeQuery.isBlank() &&
                    activeField ==
                    SearchField.DESTINATION
                ) {

                    item {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 45.dp
                                    ),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Icon(
                                Icons.Default.Place,
                                contentDescription =
                                    null,
                                tint =
                                    Color(
                                        0xFF9AA0A6
                                    ),
                                modifier =
                                    Modifier.size(
                                        42.dp
                                    )
                            )

                            Spacer(
                                Modifier.height(
                                    12.dp
                                )
                            )

                            Text(
                                text =
                                    "Where do you want to go?",
                                color =
                                    Color(
                                        0xFF5F6368
                                    ),
                                fontSize =
                                    17.sp
                            )

                            Text(
                                text =
                                    "Search for a place or destination",
                                color =
                                    Color(
                                        0xFF9AA0A6
                                    ),
                                fontSize =
                                    13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


// ========================================================================
// ROUTE PREVIEW CARD
// ========================================================================

@Composable
fun RoutePreviewCard(

    destinationName: String,

    durationMin: Int?,

    distanceKm: Float?,

    onDirectionsTap: () -> Unit,

    onBackTap: () -> Unit
) {

    Card(

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        shape =
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 12.dp
            ),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            destinationName,
                        color =
                            Color.Black,
                        fontSize =
                            21.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Route preview",
                        color =
                            Color.Gray,
                        fontSize =
                            14.sp
                    )
                }


                IconButton(
                    onClick =
                        onBackTap
                ) {

                    Icon(
                        Icons.Default.Close,
                        contentDescription =
                            "Close",
                        tint =
                            Color.Gray
                    )
                }
            }


            Spacer(
                Modifier.height(18.dp)
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                InfoChip(
                    icon =
                        Icons.Default.Schedule,
                    text =
                        if (durationMin != null && durationMin > 0) "$durationMin min" else "-- min"
                )

                InfoChip(
                    icon =
                        Icons.Default.Route,
                    text =
                        if (distanceKm != null && distanceKm > 0f) {
                            String.format(
                                Locale.US,
                                "%.1f km",
                                distanceKm
                            )
                        } else {
                            "0.0 km"
                        }
                )
            }


            Spacer(
                Modifier.height(18.dp)
            )


            Row(

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Button(

                    onClick =
                        onDirectionsTap,

                    modifier =
                        Modifier
                            .weight(1f)
                            .height(50.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(
                                    0xFF1A73E8
                                )
                        ),

                    shape =
                        RoundedCornerShape(
                            25.dp
                        )
                ) {

                    Icon(
                        Icons.Default.Directions,
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Directions",
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                OutlinedButton(

                    onClick =
                        onDirectionsTap,

                    modifier =
                        Modifier
                            .weight(1f)
                            .height(50.dp),

                    shape =
                        RoundedCornerShape(
                            25.dp
                        )
                ) {

                    Icon(
                        Icons.Default.Navigation,
                        contentDescription =
                            null,
                        tint =
                            Color(
                                0xFF1A73E8
                            )
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Start",
                        color =
                            Color(
                                0xFF1A73E8
                            ),
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ========================================================================
// INFO CHIP
// ========================================================================

@Composable
private fun InfoChip(
    icon:
    androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {

    Surface(

        color =
            Color(
                0xFFF1F3F4
            ),

        shape =
            RoundedCornerShape(
                12.dp
            )
    ) {

        Row(

            modifier =
                Modifier.padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                icon,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(
                        17.dp
                    ),
                tint =
                    Color(
                        0xFF5F6368
                    )
            )

            Spacer(
                Modifier.width(6.dp)
            )

            Text(
                text =
                    text,
                color =
                    Color(
                        0xFF3C4043
                    ),
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.Medium
            )
        }
    }
}


// ========================================================================
// NAVIGATION READY CARD
// ========================================================================

@Composable
fun NavigationReadyCard(

    destinationName: String,

    durationMin: Int?,

    distanceKm: Float?,

    onStartTap: () -> Unit,

    onBackTap: () -> Unit
) {

    Card(

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        shape =
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 12.dp
            ),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(20.dp)
        ) {

            Text(
                text =
                    destinationName,
                color =
                    Color.Black,
                fontSize =
                    21.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                text =
                    "Navigation ready",
                color =
                    Color.Gray,
                fontSize =
                    14.sp
            )


            Spacer(
                Modifier.height(18.dp)
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                TravelModeItem(
                    icon =
                        Icons.Default.DirectionsCar,
                    label =
                        "${durationMin ?: "--"} min",
                    isSelected =
                        true
                )

                TravelModeItem(
                    icon =
                        Icons.Default.DirectionsBike,
                    label =
                        "${durationMin ?: "--"} min",
                    isSelected =
                        false
                )

                TravelModeItem(
                    icon =
                        Icons.Default.DirectionsWalk,
                    label =
                        "${durationMin ?: "--"} min",
                    isSelected =
                        false
                )
            }


            Spacer(
                Modifier.height(16.dp)
            )


            HorizontalDivider(
                color =
                    Color.LightGray.copy(
                        alpha = 0.5f
                    )
            )


            Spacer(
                Modifier.height(16.dp)
            )


            Text(

                text =
                    "${if (durationMin != null && durationMin > 0) durationMin else "--"} min  •  ${
                        String.format(
                            Locale.US,
                            "%.1f km",
                            distanceKm ?: 0f
                        )
                    }",

                color =
                    Color(
                        0xFF1E8E3E
                    ),

                fontSize =
                    22.sp,

                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(20.dp)
            )


            Row(

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Button(

                    onClick =
                        onStartTap,

                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(
                                    0xFF1A73E8
                                )
                        ),

                    shape =
                        RoundedCornerShape(
                            26.dp
                        )
                ) {

                    Icon(
                        Icons.Default.Navigation,
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text(
                        text =
                            "Start Navigation",
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            17.sp
                    )
                }


                IconButton(

                    onClick =
                        onBackTap,

                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(
                                CircleShape
                            )
                            .background(
                                Color(
                                    0xFFE8F0FE
                                )
                            )
                ) {

                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription =
                            "Back",
                        tint =
                            Color(
                                0xFF1A73E8
                            )
                    )
                }
            }
        }
    }
}


// ========================================================================
// TRAVEL MODE
// ========================================================================

@Composable
fun TravelModeItem(

    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    label: String,

    isSelected: Boolean
) {

    Column(

        horizontalAlignment =
            Alignment.CenterHorizontally,

        modifier =
            Modifier.width(80.dp)
    ) {

        Icon(

            icon,

            contentDescription =
                null,

            tint =
                if (isSelected) {
                    Color(
                        0xFF1A73E8
                    )
                } else {
                    Color.Gray
                }
        )


        Text(

            text =
                label,

            color =
                if (isSelected) {
                    Color(
                        0xFF1A73E8
                    )
                } else {
                    Color.Gray
                },

            fontSize =
                13.sp,

            fontWeight =
                if (isSelected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )


        if (isSelected) {

            Spacer(
                Modifier.height(4.dp)
            )

            Box(

                modifier =
                    Modifier
                        .height(3.dp)
                        .width(42.dp)
                        .background(
                            Color(
                                0xFF1A73E8
                            )
                        )
            )
        }
    }
}


// ========================================================================
// VEHICLE SELECTION
// ========================================================================

@Composable
fun VehicleSelectionCard(

    onSelect:
        (VehicleType) -> Unit,

    onCancel:
        () -> Unit
) {

    Card(

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceSlate.copy(
                        alpha = 0.97f
                    )
            ),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 10.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(
                    20.dp
                )
        ) {

            Text(
                text =
                    "Select Vehicle Type",
                color =
                    TextPrimary,
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                Modifier.height(18.dp)
            )


            Row(

                horizontalArrangement =
                    Arrangement.spacedBy(
                        20.dp
                    )
            ) {

                VehicleButton(

                    icon =
                        Icons.Default.DirectionsCar,

                    label =
                        "Car",

                    onClick = {

                        onSelect(
                            VehicleType.CAR
                        )
                    }
                )


                VehicleButton(

                    icon =
                        Icons.Default.DirectionsBike,

                    label =
                        "Bike",

                    onClick = {

                        onSelect(
                            VehicleType.BIKE
                        )
                    }
                )
            }


            Spacer(
                Modifier.height(14.dp)
            )


            TextButton(

                onClick =
                    onCancel,

                modifier =
                    Modifier.align(
                        Alignment.End
                    )
            ) {

                Text(
                    text =
                        "CANCEL",
                    color =
                        TextSecondary
                )
            }
        }
    }
}


// ========================================================================
// VEHICLE BUTTON
// ========================================================================

@Composable
fun VehicleButton(

    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    label: String,

    onClick: () -> Unit
) {

    Column(

        modifier =
            Modifier
                .width(85.dp)
                .clickable(
                    onClick =
                        onClick
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(

            modifier =
                Modifier
                    .size(58.dp)
                    .clip(
                        CircleShape
                    )
                    .background(
                        CardSurface
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                icon,
                contentDescription =
                    null,
                tint =
                    ElectricCyan
            )
        }


        Text(

            text =
                label,

            color =
                TextPrimary,

            fontSize =
                13.sp,

            modifier =
                Modifier.padding(
                    top = 5.dp
                )
        )
    }
}


// ========================================================================
// NAVIGATION BANNER
// ========================================================================

@Composable
fun NavigationBanner(
    instruction: String
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color(
                        0xFF005D4B
                    )
            ),

        shape =
            RoundedCornerShape(
                14.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
    ) {

        Row(

            modifier =
                Modifier.padding(
                    16.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                Icons.Default.Navigation,

                contentDescription =
                    null,

                tint =
                    Color.White,

                modifier =
                    Modifier.size(
                        32.dp
                    )
            )


            Spacer(
                Modifier.width(14.dp)
            )


            Text(

                text =
                    instruction,

                color =
                    Color.White,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


// ========================================================================
// FLOATING NAV BUTTON
// ========================================================================

@Composable
fun FloatingNavButton(
    icon:
    androidx.compose.ui.graphics.vector.ImageVector
) {

    Surface(

        modifier =
            Modifier.size(
                46.dp
            ),

        shape =
            CircleShape,

        color =
            Color.White,

        shadowElevation =
            5.dp
    ) {

        Box(
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                icon,
                contentDescription =
                    null,
                tint =
                    Color.Black,
                modifier =
                    Modifier.size(
                        24.dp
                    )
            )
        }
    }
}


// ========================================================================
// ACTIVE NAVIGATION CARD
// ========================================================================

@Composable
fun ActiveNavCard(

    durationMin: Int?,

    distanceKm: Float?,

    mode: NavigationMode,

    drTime: Long,

    speedMs: Float,

    headingDeg: Float,

    onExitTap: () -> Unit
) {

    Card(

        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),

        shape =
            RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 12.dp
            ),

        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick =
                        onExitTap
                ) {

                    Icon(
                        Icons.Default.Close,
                        contentDescription =
                            "Exit",
                        tint =
                            Color.Black
                    )
                }


                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    val eta =
                        if (durationMin != null && durationMin > 0) formatEta(durationMin) else "ETA --"

                    Text(
                        text =
                            "${if (durationMin != null && durationMin > 0) durationMin else "--"} min",
                        color =
                            Color(
                                0xFF1E8E3E
                            ),
                        fontSize =
                            24.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            String.format(
                                Locale.US,
                                "%.1f km • %s",
                                distanceKm ?: 0f,
                                eta
                            ),
                        color =
                            Color.Gray,
                        fontSize =
                            15.sp
                    )
                }


                IconButton(
                    onClick = {}
                ) {

                    Icon(
                        Icons.Default.AltRoute,
                        contentDescription =
                            "Routes",
                        tint =
                            Color.Black
                    )
                }
            }


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            HorizontalDivider(
                color =
                    Color.LightGray.copy(
                        alpha = 0.5f
                    )
            )


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                val badgeColor =
                    Color(
                        mode.badgeColorHex
                    )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    badgeColor
                                )
                    )

                    Text(
                        text =
                            mode.label,
                        color =
                            Color.DarkGray,
                        fontSize =
                            12.sp,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            )
                    )
                }


                Text(

                    text =
                        String.format(
                            Locale.US,
                            "%.0f km/h • %.0f°",
                            speedMs * 3.6f,
                            headingDeg
                        ),

                    color =
                        Color.DarkGray,

                    fontSize =
                        12.sp
                )


                if (
                    mode ==
                    NavigationMode
                        .GNSS_LOST_DEAD_RECKONING_ACTIVE
                ) {

                    Text(
                        text =
                            "OUTAGE: ${drTime}s",
                        color =
                            Color.Red,
                        fontSize =
                            12.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ========================================================================
// TELEMETRY CARD
// ========================================================================

@Composable
fun TelemetryCard(

    mode: NavigationMode,

    speedMs: Float,

    headingDeg: Float
) {

    Card(

        colors =
            CardDefaults.cardColors(
                containerColor =
                    SurfaceSlate.copy(
                        alpha = 0.95f
                    )
            ),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                val badgeColor =
                    Color(
                        mode.badgeColorHex
                    )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    badgeColor
                                )
                    )

                    Text(
                        text =
                            mode.label,
                        color =
                            badgeColor,
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            )
                    )
                }
            }


            Spacer(
                Modifier.height(
                    12.dp
                )
            )


            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column {

                    Text(
                        text =
                            "CURRENT SPEED",
                        color =
                            TextMuted,
                        fontSize =
                            9.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Row(
                        verticalAlignment =
                            Alignment.Bottom
                    ) {

                        Text(
                            text =
                                String.format(
                                    Locale.US,
                                    "%.0f",
                                    speedMs * 3.6f
                                ),
                            color =
                                TextPrimary,
                            fontSize =
                                32.sp,
                            fontWeight =
                                FontWeight.Black
                        )

                        Text(
                            text =
                                " km/h",
                            color =
                                TextSecondary,
                            fontSize =
                                14.sp,
                            modifier =
                                Modifier.padding(
                                    bottom = 6.dp
                                )
                        )
                    }
                }


                VerticalDivider(

                    modifier =
                        Modifier
                            .height(40.dp)
                            .padding(
                                horizontal = 16.dp
                            ),

                    color =
                        CardBorder
                )


                Column(

                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            "HEADING",
                        color =
                            TextMuted,
                        fontSize =
                            9.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            String.format(
                                Locale.US,
                                "%.0f°",
                                headingDeg
                            ),
                        color =
                            ElectricCyan,
                        fontSize =
                            24.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}


// ========================================================================
// ETA
// ========================================================================

private fun formatEta(
    durationMin: Int
): String {

    val calendar =
        Calendar.getInstance()

    calendar.add(
        Calendar.MINUTE,
        durationMin
    )

    val sdf =
        SimpleDateFormat(
            "h:mm a",
            Locale.US
        )

    return "ETA ${sdf.format(calendar.time)}"
}
