package com.example.raylicallservice.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header(ApiConstants.API_KEY_HEADER, ApiConstants.getApiKey(context))
            .build()
        return chain.proceed(newRequest)
    }
} 