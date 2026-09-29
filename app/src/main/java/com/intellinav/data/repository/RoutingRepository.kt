package com.intellinav.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.intellinav.data.model.VehicleType
import com.intellinav.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class RouteInfo(
    val points: List<LatLng> = emptyList(),
    val distanceKm: Float = 0f,
    val durationMin: Int = 0,
    val destinationName: String = "",
    val instructions: List<String> = emptyList()
)

class RoutingRepository {

    private val _currentRoute = MutableStateFlow<RouteInfo?>(null)
    val currentRoute: StateFlow<RouteInfo?> = _currentRoute.asStateFlow()

    private val client = OkHttpClient()

    private val TAG = "RoutingRepository"

    suspend fun getRoute(
        start: LatLng,
        end: LatLng,
        destinationName: String,
        vehicleType: VehicleType = VehicleType.CAR
    ) {
        withContext(Dispatchers.IO) {

            val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY

            if (apiKey.isEmpty()) {
                Log.e(TAG, "Google Maps API Key is empty.")
                return@withContext
            }

            val travelMode = when (vehicleType) {
                VehicleType.BIKE -> "TWO_WHEELER"
                else -> "DRIVE"
            }

            val jsonBody = JSONObject().apply {

                put(
                    "origin",
                    JSONObject().apply {
                        put(
                            "location",
                            JSONObject().apply {
                                put(
                                    "latLng",
                                    JSONObject().apply {
                                        put("latitude", start.latitude)
                                        put("longitude", start.longitude)
                                    }
                                )
                            }
                        )
                    }
                )

                put(
                    "destination",
                    JSONObject().apply {
                        put(
                            "location",
                            JSONObject().apply {
                                put(
                                    "latLng",
                                    JSONObject().apply {
                                        put("latitude", end.latitude)
                                        put("longitude", end.longitude)
                                    }
                                )
                            }
                        )
                    }
                )

                put("travelMode", travelMode)
                put("computeAlternativeRoutes", false)
                put("polylineQuality", "HIGH_QUALITY")
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            // We get 403 PERMISSION_DENIED "Requests from this Android client application <empty> are blocked."
            // Because making requests directly to Google Maps APIs from an Android device using OkHttp 
            // does not automatically send the SHA-1 App Signature like the native Maps SDK does.
            // When an API Key is Android-restricted, direct HTTP requests fail unless we provide the exact 
            // headers Google expects (X-Android-Package and X-Android-Cert).
            //
            // We'll calculate the SHA1 natively and attach it to the headers.
            val appContext = com.intellinav.MyApplication.appContext
            val packageName = appContext.packageName
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val packageInfo = appContext.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = appContext.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }
            
            val cert = signatures?.firstOrNull()?.toByteArray()
            val sha1 = if (cert != null) {
                val md = java.security.MessageDigest.getInstance("SHA1")
                val digest = md.digest(cert)
                digest.joinToString("") { String.format("%02X", it) }
            } else ""

            val request = Request.Builder()
                .url("https://routes.googleapis.com/directions/v2:computeRoutes")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader(
                    "X-Goog-FieldMask",
                    "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline"
                )
                .addHeader("X-Android-Package", packageName)
                .addHeader("X-Android-Cert", sha1)
                .post(requestBody)
                .build()

            try {

                Log.d("INTELLINAV_ROUTE", "origin=$start")
                Log.d("INTELLINAV_ROUTE", "destination=$end")
                Log.d("INTELLINAV_ROUTE", "Requesting Google Routes API")

                val response = client.newCall(request).execute()

                val responseCode = response.code

                Log.d("INTELLINAV_ROUTE", "HTTP=$responseCode")

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""

                    Log.e(
                        "INTELLINAV_ROUTE",
                        "Google Routes API ERROR: $responseCode $errorBody"
                    )

                    return@withContext
                }

                val body = response.body?.string() ?: ""

                val json = JSONObject(body)

                val routes = json.optJSONArray("routes")

                if (routes == null || routes.length() == 0) {
                    Log.e("INTELLINAV_ROUTE", "No routes returned by Google Routes API")
                    return@withContext
                }

                val route = routes.getJSONObject(0)

                val distanceMeters =
                    route.optDouble("distanceMeters", 0.0)

                val durationString =
                    route.optString("duration", "0s")

                val durationSeconds =
                    durationString
                        .removeSuffix("s")
                        .toDoubleOrNull() ?: 0.0

                val distanceKm =
                    (distanceMeters / 1000.0).toFloat()

                val durationMin =
                    (durationSeconds / 60.0).toInt()

                val encodedPolyline =
                    route
                        .optJSONObject("polyline")
                        ?.optString("encodedPolyline", "")
                        ?: ""

                val points = decodePolyline(encodedPolyline)

                if (points.isEmpty()) {
                    Log.e("INTELLINAV_ROUTE", "Google returned an empty route polyline")
                    return@withContext
                }

                Log.d(
                    "INTELLINAV_ROUTE",
                    "SUCCESS: ${points.size} points, " +
                            "$distanceKm km, " +
                            "$durationMin min"
                )

                _currentRoute.value = RouteInfo(
                    points = points,
                    distanceKm = distanceKm,
                    durationMin = durationMin,
                    destinationName = destinationName,
                    instructions = emptyList()
                )

            } catch (e: Exception) {

                Log.e(
                    "INTELLINAV_ROUTE",
                    "Google Routes API exception",
                    e
                )
            }
        }
    }

    /**
     * Decode Google's encoded polyline format.
     */
    private fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()

        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {

            var shift = 0
            var result = 0

            while (true) {

                val b = encoded[index++].code - 63

                result = result or ((b and 0x1f) shl shift)

                shift += 5

                if (b < 0x20) break
            }

            val deltaLat =
                if ((result and 1) != 0)
                    (result shr 1).inv()
                else
                    result shr 1

            lat += deltaLat

            shift = 0
            result = 0

            while (true) {

                val b = encoded[index++].code - 63

                result = result or ((b and 0x1f) shl shift)

                shift += 5

                if (b < 0x20) break
            }

            val deltaLng =
                if ((result and 1) != 0)
                    (result shr 1).inv()
                else
                    result shr 1

            lng += deltaLng

            poly.add(
                LatLng(
                    lat / 1E5,
                    lng / 1E5
                )
            )
        }

        return poly
    }

    fun clearRoute() {
        _currentRoute.value = null
    }
}