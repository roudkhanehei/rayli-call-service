package com.example.raylicallservice.api

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header(ApiConstants.API_KEY_HEADER, ApiConstants.API_KEY)
            .build()
        return chain.proceed(newRequest)
    }
} 