package com.example.raylicallservice.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("your-endpoint")
    suspend fun postData(@Body data: PostData): Response<ApiResponse>
}

data class PostData(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApiResponse(
    val success: Boolean,
    val message: String
) 