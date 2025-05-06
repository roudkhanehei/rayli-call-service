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
    val callState: String
) 