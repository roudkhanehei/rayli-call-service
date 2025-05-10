package com.example.raylicallservice.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("wp-json/rayli-call-manager/v1/receive-call-data")
    suspend fun postCallData(@Body data: CallData): Response<ApiResponse>
}

data class CallData(
    val call_id: String?,
    val caller_number: String?,
    val call_duration: Long,
    val call_status: String?,
    val call_date: Long = System.currentTimeMillis(),
    val customer_name: String? = null,
    val products_id: String? = null,
    val description: String? = null,
    val organization: String? = null,
    val customer_id: Long,
    val simcart: String? = null,
    val sim_number: String? = null,
    val sim_slot: String? = null,
    val issue: String? = null,
    val additional_data: String? = null
)

data class ApiResponse(
    val success: Boolean,
    val message: String
) 