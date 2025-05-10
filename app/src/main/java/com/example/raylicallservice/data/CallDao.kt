package com.example.raylicallservice.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE callId = :callId")
    suspend fun getCallById(callId: String): CallEntity?

    @Query("SELECT * FROM calls WHERE phoneNumber = :phoneNumber ORDER BY timestamp DESC")
    fun getCallsByPhoneNumber(phoneNumber: String): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE strftime('%Y-%m-%d', datetime(timestamp/1000, 'unixepoch')) = strftime('%Y-%m-%d', datetime(:date/1000, 'unixepoch')) ORDER BY timestamp DESC")
    suspend fun getCallsForDay(date: Date): List<CallEntity>

    @Delete
    suspend fun deleteCall(call: CallEntity)

    @Query("DELETE FROM calls")
    suspend fun deleteAllCalls()

    @Query("SELECT * FROM calls WHERE timestamp >= :startDate AND timestamp < :endDate ORDER BY timestamp DESC")
    suspend fun getCallsBetweenDates(startDate: Date, endDate: Date): List<CallEntity>

   
} 