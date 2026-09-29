
package com.intellinav.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.intellinav.engine.DirectNavigationEngine
import com.intellinav.engine.NavSearchHelper
import com.intellinav.ui.components.RealMapView
import kotlinx.coroutines.launch

@Composable
fun MainNavigationScreen() {

    // -------------------------------------------------------------------------
    // CONTEXT
    // -------------------------------------------------------------------------

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val keyboardController =
        LocalSoftwareKeyboardController.current

    val focusManager =
        LocalFocusManager.current


    // -------------------------------------------------------------------------
    // NAVIGATION ENGINE
    // -------------------------------------------------------------------------

    val navEngine = remember {
        DirectNavigationEngine(context)
    }

    val searchHelper = remember {
        NavSearchHelper()
    }


    // -------------------------------------------------------------------------
    // LIVE POSITION
    // -------------------------------------------------------------------------

    val position by navEngine.position.collectAsState()


    // -------------------------------------------------------------------------
    // NAVIGATION MODE
    // -------------------------------------------------------------------------

    val navMode by navEngine.navMode.collectAsState()


    // -------------------------------------------------------------------------
    // ROUTE POINTS
    // -------------------------------------------------------------------------

    val routePoints by navEngine.routePoints.collectAsState()


    // -------------------------------------------------------------------------
    // SEARCH STATE
    // -------------------------------------------------------------------------

    var searchQuery by remember {
        mutableStateOf("")
    }

    var isSearching by remember {
        mutableStateOf(false)
    }

    var searchError by remember {
        mutableStateOf<String?>(null)
    }


    // -------------------------------------------------------------------------
    // SEARCH DESTINATION
    // -------------------------------------------------------------------------

    fun searchDestination() {

        val query = searchQuery.trim()

        if (query.isEmpty()) {
            searchError = "Enter a destination."
            return
        }

        if (isSearching) {
            return
        }

        scope.launch {

            isSearching = true
            searchError = null

            try {

                // -------------------------------------------------------------
                // CHECK CURRENT LOCATION
                // -------------------------------------------------------------

                val latitude = position.latitude
                val longitude = position.longitude

                if (
                    latitude.isNaN() ||
                    longitude.isNaN() ||
                    latitude == 0.0 ||
                    longitude == 0.0
                ) {

                    searchError =
                        "Waiting for current location..."

                    return@launch
                }


                // -------------------------------------------------------------
                // CURRENT POSITION
                // -------------------------------------------------------------

                val currentPosition = LatLng(
                    latitude,
                    longitude
                )


                // -------------------------------------------------------------
                // SEARCH DESTINATION
                // -------------------------------------------------------------

                val destination =
                    searchHelper.searchPlaceToLatLng(query)

                if (destination == null) {

                    searchError =
                        "Location not found."

                    return@launch
                }


                // -------------------------------------------------------------
                // GET DRIVING ROUTE
                // -------------------------------------------------------------

                val points =
                    searchHelper.getDrivingRoute(
                        currentPosition,
                        destination
                    )


                if (points.isEmpty()) {

                    searchError =
                        "Unable to calculate route."

                    return@launch
                }


                // -------------------------------------------------------------
                // SEND ROUTE TO NAVIGATION ENGINE
                // -------------------------------------------------------------

                navEngine.setTargetRoute(points)


                // -------------------------------------------------------------
                // HIDE KEYBOARD
                // -------------------------------------------------------------

                keyboardController?.hide()

                focusManager.clearFocus()

            } catch (e: Exception) {

                e.printStackTrace()

                searchError =
                    e.message ?: "Search failed."

            } finally {

                isSearching = false
            }
        }
    }


    // -------------------------------------------------------------------------
    // CLEANUP
    // -------------------------------------------------------------------------

    DisposableEffect(navEngine) {

        onDispose {
            navEngine.stop()
        }
    }


    // -------------------------------------------------------------------------
    // MAIN SCREEN
    // -------------------------------------------------------------------------

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // =====================================================================
        // MAP
        // =====================================================================

        RealMapView(
            position = position,

            /*
             * DirectNavigationEngine currently exposes routePoints as
             * List<LatLng>, while RealMapView expects RouteInfo?.
             *
             * Keep null here until RouteInfo is mapped properly.
             */
            route = null,

            navigationMode = navMode,

            isFollowingUser = true,

            modifier = Modifier.fillMaxSize()
        )


        // =====================================================================
        // SEARCH CARD
        // =====================================================================

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 40.dp)
                .align(Alignment.TopCenter),

            shape = RoundedCornerShape(22.dp),

            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {

                // =============================================================
                // CURRENT LOCATION
                // =============================================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.LocationOn,

                        contentDescription =
                            "Current location",

                        tint =
                            MaterialTheme.colorScheme.primary,

                        modifier =
                            Modifier.size(22.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Your location",

                            style =
                                MaterialTheme
                                    .typography
                                    .labelMedium,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )

                        Text(
                            text = "Current position",

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }


                // =============================================================
                // DIVIDER
                // =============================================================

                HorizontalDivider()


                // =============================================================
                // DESTINATION SEARCH
                // =============================================================

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            "Search destination",

                        tint =
                            Color.Gray,

                        modifier =
                            Modifier.padding(
                                start = 8.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )


                    TextField(
                        value = searchQuery,

                        onValueChange = { value ->

                            searchQuery = value

                            searchError = null
                        },

                        placeholder = {

                            Text(
                                text =
                                    "Where do you want to go?"
                            )
                        },

                        singleLine = true,

                        colors =
                            TextFieldDefaults.colors(

                                focusedContainerColor =
                                    Color.Transparent,

                                unfocusedContainerColor =
                                    Color.Transparent,

                                disabledContainerColor =
                                    Color.Transparent,

                                focusedIndicatorColor =
                                    Color.Transparent,

                                unfocusedIndicatorColor =
                                    Color.Transparent
                            ),

                        keyboardOptions =
                            KeyboardOptions(
                                imeAction =
                                    ImeAction.Search
                            ),

                        keyboardActions =
                            KeyboardActions(
                                onSearch = {
                                    searchDestination()
                                }
                            ),

                        modifier =
                            Modifier.weight(1f)
                    )


                    // =========================================================
                    // CLEAR BUTTON
                    // =========================================================

                    if (
                        searchQuery.isNotEmpty() &&
                        !isSearching
                    ) {

                        IconButton(
                            onClick = {

                                searchQuery = ""
                                searchError = null
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Close,

                                contentDescription =
                                    "Clear search"
                            )
                        }
                    }


                    // =========================================================
                    // SEARCH BUTTON / LOADING
                    // =========================================================

                    if (isSearching) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier
                                    .padding(end = 12.dp)
                                    .size(22.dp),

                            strokeWidth = 2.dp
                        )

                    } else {

                        IconButton(
                            onClick = {
                                searchDestination()
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Search,

                                contentDescription =
                                    "Search"
                            )
                        }
                    }
                }


                // =============================================================
                // ERROR
                // =============================================================

                searchError?.let { error ->

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text = error,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        modifier =
                            Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            )
                    )
                }
            }
        }


        // =====================================================================
        // ROUTE STATUS
        // =====================================================================

        if (routePoints.isNotEmpty()) {

            Card(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 24.dp),

                shape =
                    RoundedCornerShape(20.dp),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),

                    horizontalArrangement =
                        Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "Route ready",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )
                }
            }
        }
    }
}
