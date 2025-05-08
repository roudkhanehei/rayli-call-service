package com.example.raylicallservice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val productId: Long = 0,
    val name: String,
    val description: String,
    val price: Double,
    val productUrl: String,
    val status: String // Active, Inactive, Out of Stock, etc.
) 