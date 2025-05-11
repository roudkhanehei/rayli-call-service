package com.example.raylicallservice.api
import android.content.Context
import android.content.SharedPreferences

object ApiConstants {
    private const val PREFS_NAME = "ApiSettings"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_API_BASE_URL = "api_base_url"
    const val API_KEY_HEADER = "X-API-Key"

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun getApiBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_BASE_URL, "") ?: ""
    }
} 