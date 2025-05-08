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

    @Delete
    suspend fun deleteCall(call: CallEntity)

    @Query("DELETE FROM calls")
    suspend fun deleteAllCalls()

    @Query("SELECT * FROM calls WHERE timestamp >= :startDate AND timestamp < :endDate ORDER BY timestamp DESC")
    suspend fun getCallsBetweenDates(startDate: Date, endDate: Date): List<CallEntity>
} 