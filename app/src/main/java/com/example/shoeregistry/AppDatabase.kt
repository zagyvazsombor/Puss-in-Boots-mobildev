package com.example.shoeregistry

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ShoeEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoeDao(): ShoeDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shoe_db"
                ).build().also { instance = it }
            }
    }
}