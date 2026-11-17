package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GestureEntity::class], version = 1, exportSchema = false)
abstract class GestureDatabase : RoomDatabase() {
    abstract fun gestureDao(): GestureDao

    companion object {
        @Volatile
        private var INSTANCE: GestureDatabase? = null

        fun getInstance(context: Context): GestureDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GestureDatabase::class.java,
                    "gestures_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
