package com.intellinav.ui.components

import android.os.Bundle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.intellinav.ui.components.MapUtils
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

import com.intellinav.data.model.NavigationMode
import com.intellinav.data.model.PositionEstimate
import com.intellinav.data.repository.RouteInfo

import com.intellinav.ui.theme.ElectricCyan
import com.intellinav.ui.theme.SurfaceSlate


@Composable
fun RealMapView(
    position: PositionEstimate,
    route: RouteInfo? = null,
    navigationMode: NavigationMode =
        NavigationMode.GNSS_AVAILABLE,
    isFollowingUser: Boolean = true,
    isRoutePreview: Boolean = false,
    modifier: Modifier = Modifier,
    onMapReady: (GoogleMap) -> Unit = {},
    onRecenter: () -> Unit = {},
    onUserInitiatedMove: () -> Unit = {}
) {

    val context = LocalContext.current

    val lifecycleOwner =
        LocalLifecycleOwner.current


    /*
     * =========================================================
     * GOOGLE MAP STATE
     * =========================================================
     */

    var mapInstance by
    remember {
        mutableStateOf<GoogleMap?>(null)
    }

    var isMapLoading by
    remember {
        mutableStateOf(true)
    }


    /*
     * =========================================================
     * MAP OBJECTS
     * =========================================================
     */

    /*
     * This is the actual Google Maps location marker.
     *
     * OLD:
     * triangle Polyline
     *
     * NEW:
     * Google Maps style location marker
     */
    var vehicleMarker by
    remember {
        mutableStateOf<Marker?>(null)
    }

    var routePolyline by
    remember {
        mutableStateOf<Polyline?>(null)
    }

    var destinationMarker by
    remember {
        mutableStateOf<Marker?>(null)
    }


    /*
     * =========================================================
     * MAPVIEW
     * =========================================================
     */

    val mapView =
        remember(context) {
            MapView(context)
        }


    /*
     * =========================================================
     * MAPVIEW LIFECYCLE
     * =========================================================
     */

    val lifecycleObserver =
        remember(mapView) {

            LifecycleEventObserver { _, event ->

                when (event) {

                    Lifecycle.Event.ON_CREATE -> {
                        mapView.onCreate(Bundle())
                    }

                    Lifecycle.Event.ON_START -> {
                        mapView.onStart()
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        mapView.onResume()
                    }

                    Lifecycle.Event.ON_PAUSE -> {
                        mapView.onPause()
                    }

                    Lifecycle.Event.ON_STOP -> {
                        mapView.onStop()
                    }

                    Lifecycle.Event.ON_DESTROY -> {
                        mapView.onDestroy()
                    }

                    else -> Unit
                }
            }
        }


    DisposableEffect(
        lifecycleOwner,
        mapView
    ) {

        lifecycleOwner.lifecycle.addObserver(
            lifecycleObserver
        )

        onDispose {

            lifecycleOwner.lifecycle.removeObserver(
                lifecycleObserver
            )

            try {
                mapView.onPause()
            } catch (_: Exception) {
            }

            try {
                mapView.onStop()
            } catch (_: Exception) {
            }
        }
    }


    /*
     * =========================================================
     * MAIN MAP CONTAINER
     * =========================================================
     */

    Box(
        modifier =
            modifier.fillMaxSize()
    ) {


        /*
         * =====================================================
         * GOOGLE MAP
         * =====================================================
         */

        AndroidView(

            factory = {

                mapView.apply {

                    getMapAsync { googleMap ->

                        mapInstance =
                            googleMap

                        isMapLoading =
                            false


                        /*
                         * -------------------------------------------------
                         * GOOGLE MAP UI
                         * -------------------------------------------------
                         */

                        googleMap.uiSettings.apply {

                            isZoomGesturesEnabled =
                                true

                            isScrollGesturesEnabled =
                                true

                            isRotateGesturesEnabled =
                                true

                            isTiltGesturesEnabled =
                                true

                            isZoomControlsEnabled =
                                false

                            isCompassEnabled =
                                true

                            /*
                             * We use our own recenter button.
                             */
                            isMyLocationButtonEnabled =
                                false
                        }


                        /*
                         * -------------------------------------------------
                         * USER CAMERA GESTURE
                         * -------------------------------------------------
                         */

                        googleMap.setOnCameraMoveStartedListener { reason ->

                            if (
                                reason ==
                                GoogleMap
                                    .OnCameraMoveStartedListener
                                    .REASON_GESTURE
                            ) {

                                onUserInitiatedMove()
                            }
                        }


                        /*
                         * -------------------------------------------------
                         * INITIAL CAMERA
                         * -------------------------------------------------
                         */

                        val initialPosition =
                            LatLng(
                                position.latitude,
                                position.longitude
                            )

                        googleMap.moveCamera(

                            CameraUpdateFactory
                                .newCameraPosition(

                                    CameraPosition.Builder()

                                        .target(
                                            initialPosition
                                        )

                                        .zoom(
                                            17.5f
                                        )

                                        .bearing(
                                            position.headingDeg
                                        )

                                        .tilt(
                                            0f
                                        )

                                        .build()
                                )
                        )


                        onMapReady(
                            googleMap
                        )
                    }
                }
            },

            modifier =
                Modifier.fillMaxSize()
        )


        /*
         * =====================================================
         * MAP LOADING
         * =====================================================
         */

        if (isMapLoading) {

            CircularProgressIndicator(

                modifier =
                    Modifier.align(
                        Alignment.Center
                    ),

                color =
                    ElectricCyan
            )
        }

        /*
         * =====================================================
         * ZOOM + RECENTER BUTTONS
         * =====================================================
         */

        Column(

            modifier =
                Modifier
                    .align(
                        Alignment.CenterEnd
                    )
                    .padding(
                        end = 16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {


            /*
             * ZOOM IN
             */

            MapControlButton(
                icon =
                    Icons.Default.Add
            ) {

                mapInstance?.animateCamera(
                    CameraUpdateFactory.zoomIn()
                )
            }


            /*
             * ZOOM OUT
             */

            MapControlButton(
                icon =
                    Icons.Default.Remove
            ) {

                mapInstance?.animateCamera(
                    CameraUpdateFactory.zoomOut()
                )
            }


            /*
             * RECENTER
             */

            MapControlButton(
                icon =
                    Icons.Default.MyLocation
            ) {

                onRecenter()

                val current =
                    LatLng(
                        position.latitude,
                        position.longitude
                    )

                mapInstance?.animateCamera(

                    CameraUpdateFactory
                        .newCameraPosition(

                            CameraPosition.Builder()

                                .target(
                                    current
                                )

                                .zoom(
                                    18f
                                )

                                .bearing(
                                    position.headingDeg
                                )

                                .tilt(
                                    0f
                                )

                                .build()
                        ),

                    300,

                    null
                )
            }
        }
    }


    /*
     * =========================================================
     * LIVE MAP UPDATE
     * =========================================================
     *
     * Position can update frequently.
     *
     * We update:
     *
     * 1. Vehicle marker
     * 2. Camera
     * 3. Route
     * 4. Destination marker
     *
     * The user stays on the route while moving.
     */

    LaunchedEffect(

        position,
        route,
        navigationMode,
        isFollowingUser,
        isRoutePreview,
        mapInstance

    ) {

        val googleMap =
            mapInstance
                ?: return@LaunchedEffect


        updateGoogleMapState(

            map =
                googleMap,

            position =
                position,

            route =
                route,

            navigationMode =
                navigationMode,

            isFollowingUser =
                isFollowingUser,
                
            isRoutePreview =
                isRoutePreview,

            currentVehicleMarker =
                vehicleMarker,

            currentRoutePolyline =
                routePolyline,

            currentDestinationMarker =
                destinationMarker

        ).let { objects ->

            vehicleMarker =
                objects.vehicleMarker

            routePolyline =
                objects.routePolyline

            destinationMarker =
                objects.destinationMarker
        }
    }
}


/*
 * =============================================================
 * MAP OBJECTS
 * =============================================================
 */

private data class MapObjects(

    val vehicleMarker:
    Marker?,

    val routePolyline:
    Polyline?,

    val destinationMarker:
    Marker?
)


/*
 * =============================================================
 * MAP CONTROL BUTTON
 * =============================================================
 */

@Composable
private fun MapControlButton(

    icon:
    androidx.compose.ui.graphics.vector
    .ImageVector,

    onClick:
        () -> Unit
) {

    Surface(

        onClick =
            onClick,

        color =
            SurfaceSlate.copy(
                alpha = 0.9f
            ),

        shape =
            CircleShape,

        shadowElevation =
            4.dp,

        modifier =
            Modifier.size(
                44.dp
            )
    ) {

        Box(

            contentAlignment =
                Alignment.Center
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    ElectricCyan,

                modifier =
                    Modifier.size(
                        24.dp
                    )
            )
        }
    }
}


/*
 * =============================================================
 * UPDATE GOOGLE MAP
 * =============================================================
 */

private fun updateGoogleMapState(

    map:
    GoogleMap,

    position:
    PositionEstimate,

    route:
    RouteInfo?,

    navigationMode:
    NavigationMode,

    isFollowingUser:
    Boolean,
    
    isRoutePreview:
    Boolean,

    currentVehicleMarker:
    Marker?,

    currentRoutePolyline:
    Polyline?,

    currentDestinationMarker:
    Marker?

):
        MapObjects {


    /*
     * =========================================================
     * CURRENT USER LOCATION
     * =========================================================
     */

    val target =
        LatLng(

            position.latitude,

            position.longitude
        )


    /*
     * =========================================================
     * CAMERA FOLLOW
     * =========================================================
     *
     * When user moves:
     *
     * camera follows user.
     *
     * When user manually drags map:
     *
     * isFollowingUser becomes false
     * through onUserInitiatedMove().
     */

    if (isFollowingUser) {
        
        if (isRoutePreview && route != null && route.points.isNotEmpty()) {
            val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            boundsBuilder.include(target)
            for (p in route.points) {
                boundsBuilder.include(p)
            }
            try {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 180)
                )
            } catch (e: Exception) {
                // Ignore if layout hasn't completed
            }
        } else {
            map.animateCamera(

                CameraUpdateFactory
                    .newCameraPosition(

                        CameraPosition.Builder()

                            .target(
                                target
                            )

                            .zoom(
                                18f
                            )

                            /*
                             * Follow heading.
                             */
                            .bearing(
                                position.headingDeg
                            )

                            /*
                             * Keep map flat.
                             */
                            .tilt(
                                0f
                            )

                            .build()
                    ),

                120,

                null
            )
        }
    }


    /*
     * =========================================================
     * ROUTE
     * =========================================================
     */

    val newRoutePolyline:
            Polyline?

    val newDestinationMarker:
            Marker?


    if (route == null) {


        /*
         * No route.
         */

        currentRoutePolyline?.remove()

        newRoutePolyline =
            null


        currentDestinationMarker?.remove()

        newDestinationMarker =
            null


    } else {


        /*
         * =====================================================
         * KEEP ROUTE ON MAP
         * =====================================================
         */

        if (
            route.points.isEmpty()
        ) {

            currentRoutePolyline?.remove()

            newRoutePolyline =
                null

        } else {

            if (
                currentRoutePolyline == null
            ) {

                /*
                 * Create route once.
                 */

                newRoutePolyline =
                    map.addPolyline(

                        PolylineOptions()

                            .addAll(
                                route.points
                            )

                            .color(
                                ElectricCyan.toArgb()
                            )

                            .width(
                                8f
                            )

                            .geodesic(
                                false
                            )

                            .zIndex(
                                5f
                            )
                    )

            } else {

                /*
                 * Reuse existing route.
                 *
                 * This avoids removing and
                 * recreating the route every
                 * time the vehicle moves.
                 */

                currentRoutePolyline.points =
                    route.points

                currentRoutePolyline.color =
                    ElectricCyan.toArgb()

                currentRoutePolyline.width =
                    8f

                currentRoutePolyline.zIndex =
                    5f

                newRoutePolyline =
                    currentRoutePolyline
            }
        }


        /*
         * =====================================================
         * DESTINATION MARKER
         * =====================================================
         */

        if (
            route.points.isNotEmpty()
        ) {

            val destination =
                route.points.last()


            if (
                currentDestinationMarker == null
            ) {

                newDestinationMarker =
                    map.addMarker(

                        MarkerOptions()

                            .position(
                                destination
                            )

                            .title(
                                route.destinationName
                            )

                            .icon(

                                BitmapDescriptorFactory
                                    .defaultMarker(
                                        BitmapDescriptorFactory
                                            .HUE_RED
                                    )
                            )

                            .zIndex(
                                10f
                            )
                    )

            } else {

                newDestinationMarker =
                    currentDestinationMarker

                newDestinationMarker.position =
                    destination

                newDestinationMarker.title =
                    route.destinationName
            }

        } else {

            currentDestinationMarker?.remove()

            newDestinationMarker =
                null
        }
    }


    /*
     * =========================================================
     * USER LOCATION MARKER
     * =========================================================
     *
     * THIS REPLACES THE TRIANGLE.
     *
     * It is the normal Google Maps marker.
     *
     * GNSS  -> BLUE
     * DR    -> GREEN
     * FUSION-> YELLOW
     */

    val markerColorInt = when (navigationMode) {

        NavigationMode.GNSS_AVAILABLE ->
            com.intellinav.ui.theme.ElectricCyan.toArgb()

        NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE ->
            android.graphics.Color.parseColor("#FF5252")

        NavigationMode.GNSS_RESTORED_FUSION_RECOVERING ->
            com.intellinav.ui.theme.AmberWarning.toArgb()

        else ->
            com.intellinav.ui.theme.ElectricCyan.toArgb()
    }


    val markerIcon =
        BitmapDescriptorFactory.fromBitmap(
            MapUtils.createNavigationArrowBitmap(
                markerColorInt
            )
        )


    /*
     * =========================================================
     * MARKER HUE
     * =========================================================
     *
     * FIX:
     * markerHue was missing.
     *
     * Google Maps defaultMarker() expects Float.
     */

    val markerHue: Float = when (navigationMode) {

        NavigationMode.GNSS_AVAILABLE ->
            BitmapDescriptorFactory.HUE_AZURE

        NavigationMode.GNSS_LOST_DEAD_RECKONING_ACTIVE ->
            BitmapDescriptorFactory.HUE_GREEN

        NavigationMode.GNSS_RESTORED_FUSION_RECOVERING ->
            BitmapDescriptorFactory.HUE_YELLOW

        else ->
            BitmapDescriptorFactory.HUE_AZURE
    }


    /*
     * =========================================================
     * VEHICLE MARKER
     * =========================================================
     */

    val newVehicleMarker:
            Marker


    if (
        currentVehicleMarker == null
    ) {

        /*
         * First marker creation.
         */

        newVehicleMarker =

            map.addMarker(

                MarkerOptions()

                    .position(
                        target
                    )

                    .title(
                        "INTELLINAV Vehicle"
                    )

                    /*
                     * Flat marker allows
                     * rotation according
                     * to heading.
                     */

                    .flat(
                        true
                    )

                    .anchor(
                        0.5f,
                        0.5f
                    )

                    .rotation(
                        position.headingDeg
                    )

                    .icon(

                        BitmapDescriptorFactory
                            .defaultMarker(
                                markerHue
                            )
                    )

                    .zIndex(
                        20f
                    )

            )!!


    } else {

        /*
         * Existing marker.
         */

        newVehicleMarker =
            currentVehicleMarker


        /*
         * =====================================================
         * LIVE LOCATION UPDATE
         * =====================================================
         */

        newVehicleMarker.position =
            target


        /*
         * =====================================================
         * LIVE HEADING UPDATE
         * =====================================================
         */

        newVehicleMarker.rotation =
            position.headingDeg


        /*
         * =====================================================
         * GNSS / DR COLOR UPDATE
         * =====================================================
         */

        newVehicleMarker.setIcon(

            BitmapDescriptorFactory
                .defaultMarker(
                    markerHue
                )
        )


        newVehicleMarker.isFlat =
            true
    }


    /*
     * =========================================================
     * RETURN UPDATED MAP OBJECTS
     * =========================================================
     */

    return MapObjects(

        vehicleMarker =
            newVehicleMarker,

        routePolyline =
            newRoutePolyline,

        destinationMarker =
            newDestinationMarker
    )
}