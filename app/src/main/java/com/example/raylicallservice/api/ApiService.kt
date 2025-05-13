
       
        package com.example.raylicallservice.api

        import retrofit2.Response
        import retrofit2.http.Body
        import retrofit2.http.POST
        import retrofit2.http.Url
        
        interface ApiService {
            @POST
            suspend fun postCallData(@Url endpoint: String, @Body data: CallData): Response<ApiResponse>
        
        }
        
        data class CallData(
            val call_id: String?,
            val caller_number: String?,
            val call_duration: Long,
            val call_status: String?,
            val call_date: String?,
            val customer_name: String? = null,
            val products_id: String? = null,
            val description: String? = null,
            val organization: String? = null,
            val customer_id: Long,
            val simcart: String? = null,
            val sim_number: String? = null,
            val sim_slot: String? = null,
            val issue: String? = null,
            val additional_data: String? = null,
            val imei: String? = null,
            val call_direction: String? = null,
            val is_synced: Boolean = false
        ) 