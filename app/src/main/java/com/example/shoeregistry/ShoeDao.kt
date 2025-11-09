package com.example.shoeregistry

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ShoeDao {
    @Delete
    suspend fun delete(shoe: ShoeEntity)
    @Query("SELECT * FROM ShoeEntity")
    suspend fun getAll(): List<ShoeEntity>

    @Insert
    suspend fun insertAll(shoes: List<ShoeEntity>): List<Long>

    @Update
    suspend fun updateShoe(shoe: ShoeEntity)

    @Query("DELETE FROM ShoeEntity")
    suspend fun deleteAll()

}