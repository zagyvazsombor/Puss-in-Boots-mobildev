package com.example.shoeregistry

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity
data class ShoeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subBrand: String,
    val brand: String,
    val size: Int,
    val imageResId: Int,
    val color: String
    )