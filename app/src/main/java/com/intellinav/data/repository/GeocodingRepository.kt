package com.intellinav.data.repository

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.intellinav.util.MapConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

data class SearchResult(
    val name: String,
    val location: LatLng
)

class GeocodingRepository(private val context: Context) {

    private val client = OkHttpClient()
    private val TAG = "INTELLINAV_SEARCH"

    suspend fun search(query: String): List<SearchResult> =
        withContext(Dispatchers.IO) {

            android.util.Log.d(TAG, "Starting geocoding request for query=$query")

            if (query.length < 3) {
                return@withContext emptyList()
            }

            val apiKey = MapConfig.API_KEY

            if (apiKey.isBlank()) {
                android.util.Log.e(TAG, "Geocoding API Key is blank! Returning empty.")
                throw Exception("Google Maps API Key is missing")
            }

            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedQuery&key=$apiKey"
                android.util.Log.d(TAG, "Geocoding URL = ${url.replace(apiKey, "API_KEY_HIDDEN")}")

                val request = Request.Builder().url(url).get().build()

                client.newCall(request).execute().use { response ->
                    android.util.Log.d(TAG, "HTTP status=${response.code}")

                    if (!response.isSuccessful) {
                        val err = response.body?.string()
                        android.util.Log.e(TAG, "Response failed: $err")
                        throw Exception("HTTP Error ${response.code}: $err")
                    }

                    val body = response.body?.string() ?: throw Exception("Empty response body")
                    val json = JSONObject(body)
                    val status = json.optString("status")

                    if (status != "OK" && status != "ZERO_RESULTS") {
                        val errMsg = json.optString("error_message")
                        android.util.Log.e(TAG, "Geocode API status != OK. Status: $status, ErrorMessage: $errMsg")
                        throw Exception("$status: $errMsg")
                    }

                    val results = json.optJSONArray("results") ?: return@use emptyList()
                    val list = mutableListOf<SearchResult>()
                    val count = minOf(results.length(), 5)

                    for (i in 0 until count) {
                        val result = results.getJSONObject(i)
                        val name = result.optString("formatted_address", "Unknown location")
                        val location = result.getJSONObject("geometry").getJSONObject("location")
                        val latitude = location.getDouble("lat")
                        val longitude = location.getDouble("lng")
                        list.add(SearchResult(name = name, location = LatLng(latitude, longitude)))
                    }

                    android.util.Log.d(TAG, "Parsed results count=${list.size}")
                    return@withContext list
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Google Geocoding failed: ${e.message}, falling back to Android Geocoder")
                
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    // For API >= 33 this is deprecated for synchronous calls, but backwards compatibility still allows it on most devices
                    val addresses = geocoder.getFromLocationName(query, 5)
                    
                    if (!addresses.isNullOrEmpty()) {
                        android.util.Log.d(TAG, "Android Geocoder found ${addresses.size} results")
                        val list = mutableListOf<SearchResult>()
                        for (address in addresses) {
                            val name = buildString {
                                address.featureName?.let { append(it) }
                                address.subLocality?.let { if (isNotEmpty()) append(", "); append(it) }
                                address.locality?.let { if (isNotEmpty()) append(", "); append(it) }
                                address.adminArea?.let { if (isNotEmpty()) append(", "); append(it) }
                            }.takeIf { it.isNotBlank() } ?: address.getAddressLine(0) ?: "Unknown location"
                            
                            list.add(SearchResult(name, LatLng(address.latitude, address.longitude)))
                        }
                        return@withContext list
                    } else {
                        android.util.Log.d(TAG, "Android Geocoder found 0 results")
                    }
                } catch (geocoderEx: Exception) {
                    android.util.Log.e(TAG, "Android Geocoder fallback also failed", geocoderEx)
                }

                throw e // If both fail, pass error to ViewModel
            }
        }

    private fun getMockResults(
        query: String
    ): List<SearchResult> {

        val results = listOf(

            SearchResult(
                "ISRO Headquarters, Bengaluru",
                LatLng(
                    12.9716,
                    77.5946
                )
            ),

            SearchResult(
                "Cubbon Park, Bengaluru",
                LatLng(
                    12.9779,
                    77.5952
                )
            ),

            SearchResult(
                "Bangalore Palace",
                LatLng(
                    13.0031,
                    77.5891
                )
            ),

            SearchResult(
                "Indiranagar, Bengaluru",
                LatLng(
                    12.9719,
                    77.6412
                )
            )
        )

        return results.filter {
            it.name.contains(
                query,
                ignoreCase = true
            )
        }
    }
}