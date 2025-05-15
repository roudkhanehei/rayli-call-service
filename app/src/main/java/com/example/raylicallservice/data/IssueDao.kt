package com.example.raylicallservice.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {
    @Query("SELECT * FROM issues ORDER BY issueOrder ASC")
    fun getAllIssues(): Flow<List<IssueEntity>>

    @Query("SELECT MAX(issueOrder) FROM issues")
    suspend fun getMaxOrder(): Int?

    @Query("SELECT * FROM issues WHERE issueID = :issueId")
    suspend fun getIssueById(issueId: String): IssueEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: IssueEntity)

    @Update
    suspend fun updateIssue(issue: IssueEntity)

    @Delete
    suspend fun deleteIssue(issue: IssueEntity)

    @Query("DELETE FROM issues")
    suspend fun deleteAllIssues()
}
