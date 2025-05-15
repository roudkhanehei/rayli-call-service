package com.example.raylicallservice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "issues")
data class IssueEntity(
    @PrimaryKey
    val issueID: String,
    val issueName: String,
    val issueCode: String,
    val issueStatus: String,
    val issueOrder: Int
)
