package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GestureDao {
    @Query("SELECT * FROM gesture_mappings ORDER BY sortOrder ASC")
    fun observeAllGestures(): Flow<List<GestureEntity>>

    @Query("SELECT * FROM gesture_mappings ORDER BY sortOrder ASC")
    suspend fun getAllGesturesOnce(): List<GestureEntity>

    @Query("SELECT * FROM gesture_mappings WHERE id = :id LIMIT 1")
    suspend fun getGestureById(id: String): GestureEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(gestures: List<GestureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(gestures: List<GestureEntity>)

    @Update
    suspend fun updateGesture(gesture: GestureEntity)

    @Query("UPDATE gesture_mappings SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE gesture_mappings SET triggerCount = triggerCount + 1 WHERE id = :id")
    suspend fun incrementTriggerCount(id: String)

    @Query("UPDATE gesture_mappings SET isEnabled = :enabled")
    suspend fun setAllEnabled(enabled: Boolean)
}
