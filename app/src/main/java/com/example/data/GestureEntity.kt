package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_mappings")
data class GestureEntity(
    @PrimaryKey val id: String,
    val sortOrder: Int,
    val titleAr: String,
    val subtitleAr: String,
    val badgeLabel: String,
    val category: String,
    val isEnabled: Boolean,
    val actionType: String,
    val targetPackage: String?,
    val targetLabel: String,
    val targetUrl: String? = null,
    val triggerCount: Int = 0
) {
    fun triggerTypeOrNull(): GestureTriggerType? =
        GestureTriggerType.entries.firstOrNull { it.name == id }

    fun actionTypeEnum(): ActionType =
        ActionType.entries.firstOrNull { it.name == actionType } ?: ActionType.OPEN_APP

    fun categoryEnum(): GestureCategory =
        GestureCategory.entries.firstOrNull { it.name == category } ?: GestureCategory.MULTI_TOUCH
}
