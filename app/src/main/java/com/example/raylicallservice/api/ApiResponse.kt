package com.example.raylicallservice.api

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
) 