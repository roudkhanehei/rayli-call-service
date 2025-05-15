package com.example.raylicallservice.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey
    val callId: String,
    val phoneNumber: String?,
    val timestamp: Date,
    val callState: String,
    val duration: Long = 0,
    val customerName: String? = null,
    val productsId: String? = null,
    val description: String? = null,
    val organization: String? = null,
    val customerId: String? = null,
    val simcart: String? = null,
    val sim_number: String? = null,
    val sim_slot: Int = -1,
    val imei: String? = null,
    val status: String? = null,
    val callDirection: String = "INCOMING", // Default to INCOMING for backward compatibility
    val issueId : Long = 0,
    val isSynced: Boolean = false
) 

