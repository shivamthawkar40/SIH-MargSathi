package com.intellinav.util

import com.intellinav.BuildConfig

object MapConfig {

    val API_KEY: String = BuildConfig.GOOGLE_MAPS_API_KEY

    fun isApiKeyConfigured(): Boolean {
        return API_KEY.isNotBlank()
    }
}