package com.intellinav.engine

import com.google.android.gms.maps.model.LatLng
import com.intellinav.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

class NavSearchHelper {

    private val client = OkHttpClient()

    // ---------------------------------------------------------
    // 1. LOCATION TEXT -> COORDINATES
    // Google Geocoding API
    // ---------------------------------------------------------

    suspend fun searchPlaceToLatLng(
        query: String
    ): LatLng? = withContext(Dispatchers.IO) {

        if (query.isBlank()) {
            return@withContext null
        }

        val apiKey =
            BuildConfig.GOOGLE_MAPS_API_KEY

        if (apiKey.isBlank()) {
            return@withContext null
        }

        try {

            val encodedQuery =
                URLEncoder.encode(
                    query,
                    "UTF-8"
                )

            val url =
                "https://maps.googleapis.com/maps/api/geocode/json" +
                        "?address=$encodedQuery" +
                        "&key=$apiKey"

            val request =
                Request.Builder()
                    .url(url)
                    .get()
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {
                        return@use null
                    }

                    val body =
                        response.body?.string()
                            ?: return@use null

                    val root =
                        JSONObject(body)

                    if (
                        root.optString("status") !=
                        "OK"
                    ) {
                        return@use null
                    }

                    val results =
                        root.optJSONArray("results")
                            ?: return@use null

                    if (results.length() == 0) {
                        return@use null
                    }

                    val firstResult =
                        results.getJSONObject(0)

                    val geometry =
                        firstResult
                            .getJSONObject("geometry")

                    val location =
                        geometry
                            .getJSONObject("location")

                    val latitude =
                        location.getDouble("lat")

                    val longitude =
                        location.getDouble("lng")

                    LatLng(
                        latitude,
                        longitude
                    )
                }

        } catch (e: Exception) {

            e.printStackTrace()

            null
        }
    }

    // ---------------------------------------------------------
    // 2. START + DESTINATION -> ROAD ROUTE
    // Google Routes API
    // ---------------------------------------------------------

    suspend fun getDrivingRoute(
        start: LatLng,
        dest: LatLng
    ): List<LatLng> = withContext(Dispatchers.IO) {

        val apiKey =
            BuildConfig.GOOGLE_MAPS_API_KEY

        if (apiKey.isBlank()) {
            return@withContext emptyList()
        }

        try {

            val jsonBody =
                JSONObject().apply {

                    put(
                        "origin",
                        JSONObject().apply {

                            put(
                                "location",
                                JSONObject().apply {

                                    put(
                                        "latLng",
                                        JSONObject().apply {

                                            put(
                                                "latitude",
                                                start.latitude
                                            )

                                            put(
                                                "longitude",
                                                start.longitude
                                            )
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

                                            put(
                                                "latitude",
                                                dest.latitude
                                            )

                                            put(
                                                "longitude",
                                                dest.longitude
                                            )
                                        }
                                    )
                                }
                            )
                        }
                    )

                    put(
                        "travelMode",
                        "DRIVE"
                    )

                    put(
                        "computeAlternativeRoutes",
                        false
                    )

                    put(
                        "polylineQuality",
                        "HIGH_QUALITY"
                    )
                }

            val requestBody =
                jsonBody
                    .toString()
                    .toRequestBody(
                        "application/json"
                            .toMediaType()
                    )

            val request =
                Request.Builder()
                    .url(
                        "https://routes.googleapis.com/" +
                                "directions/v2:computeRoutes"
                    )
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .addHeader(
                        "X-Goog-Api-Key",
                        apiKey
                    )
                    .addHeader(
                        "X-Goog-FieldMask",
                        "routes.polyline.encodedPolyline"
                    )
                    .post(requestBody)
                    .build()

            client.newCall(request)
                .execute()
                .use { response ->

                    if (!response.isSuccessful) {
                        return@use emptyList()
                    }

                    val body =
                        response.body?.string()
                            ?: return@use emptyList()

                    val root =
                        JSONObject(body)

                    val routes =
                        root.optJSONArray("routes")
                            ?: return@use emptyList()

                    if (routes.length() == 0) {
                        return@use emptyList()
                    }

                    val route =
                        routes.getJSONObject(0)

                    val encodedPolyline =
                        route
                            .optJSONObject("polyline")
                            ?.optString(
                                "encodedPolyline",
                                ""
                            )
                            ?: ""

                    if (encodedPolyline.isBlank()) {
                        return@use emptyList()
                    }

                    decodePolyline(
                        encodedPolyline
                    )
                }

        } catch (e: Exception) {

            e.printStackTrace()

            emptyList()
        }
    }

    // ---------------------------------------------------------
    // GOOGLE ENCODED POLYLINE -> LATLNG
    // ---------------------------------------------------------

    private fun decodePolyline(
        encoded: String
    ): List<LatLng> {

        val points =
            ArrayList<LatLng>()

        var index = 0
        var latitude = 0
        var longitude = 0

        while (
            index < encoded.length
        ) {

            var shift = 0
            var result = 0

            while (true) {

                if (index >= encoded.length) {
                    return points
                }

                val byte =
                    encoded[index++].code - 63

                result =
                    result or
                            ((byte and 0x1F) shl shift)

                shift += 5

                if (byte < 0x20) {
                    break
                }
            }

            val deltaLatitude =
                if ((result and 1) != 0) {
                    (result shr 1).inv()
                } else {
                    result shr 1
                }

            latitude +=
                deltaLatitude

            shift = 0
            result = 0

            while (true) {

                if (index >= encoded.length) {
                    return points
                }

                val byte =
                    encoded[index++].code - 63

                result =
                    result or
                            ((byte and 0x1F) shl shift)

                shift += 5

                if (byte < 0x20) {
                    break
                }
            }

            val deltaLongitude =
                if ((result and 1) != 0) {
                    (result shr 1).inv()
                } else {
                    result shr 1
                }

            longitude +=
                deltaLongitude

            points.add(
                LatLng(
                    latitude / 100000.0,
                    longitude / 100000.0
                )
            )
        }

        return points
    }
}