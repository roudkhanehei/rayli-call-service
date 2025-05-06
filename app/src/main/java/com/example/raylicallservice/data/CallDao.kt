package com.example.raylicallservice.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE callId = :callId")
    suspend fun getCallById(callId: String): CallEntity?

    @Query("DELETE FROM calls")
    suspend fun deleteAllCalls()
} 