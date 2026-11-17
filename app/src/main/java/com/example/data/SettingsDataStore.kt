package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "gesture_settings")

data class GestureSettings(
    val masterEnabled: Boolean = true,
    val floatingOrbEnabled: Boolean = true,
    val edgeZonesEnabled: Boolean = true,
    val edgeWidthDp: Float = 20f,
    val orbOpacity: Float = 0.88f,
    val hapticFeedback: Boolean = true,
    val multiTapWindowMs: Int = 380,
    val darkTheme: Boolean = true
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val FLOATING_ORB = booleanPreferencesKey("floating_orb_enabled")
        val EDGE_ZONES = booleanPreferencesKey("edge_zones_enabled")
        val EDGE_WIDTH_DP = floatPreferencesKey("edge_width_dp")
        val ORB_OPACITY = floatPreferencesKey("orb_opacity")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val MULTI_TAP_WINDOW = intPreferencesKey("multi_tap_window_ms")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val settingsFlow: Flow<GestureSettings> = context.settingsDataStore.data.map { prefs ->
        GestureSettings(
            masterEnabled = prefs[Keys.MASTER_ENABLED] ?: true,
            floatingOrbEnabled = prefs[Keys.FLOATING_ORB] ?: true,
            edgeZonesEnabled = prefs[Keys.EDGE_ZONES] ?: true,
            edgeWidthDp = prefs[Keys.EDGE_WIDTH_DP] ?: 20f,
            orbOpacity = prefs[Keys.ORB_OPACITY] ?: 0.88f,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            multiTapWindowMs = prefs[Keys.MULTI_TAP_WINDOW] ?: 380,
            darkTheme = prefs[Keys.DARK_THEME] ?: true
        )
    }

    suspend fun setMasterEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MASTER_ENABLED] = enabled }
    }

    suspend fun setFloatingOrbEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.FLOATING_ORB] = enabled }
    }

    suspend fun setEdgeZonesEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.EDGE_ZONES] = enabled }
    }

    suspend fun setEdgeWidthDp(widthDp: Float) {
        context.settingsDataStore.edit { it[Keys.EDGE_WIDTH_DP] = widthDp.coerceIn(10f, 36f) }
    }

    suspend fun setOrbOpacity(opacity: Float) {
        context.settingsDataStore.edit { it[Keys.ORB_OPACITY] = opacity.coerceIn(0.25f, 1.0f) }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setMultiTapWindowMs(windowMs: Int) {
        context.settingsDataStore.edit { it[Keys.MULTI_TAP_WINDOW] = windowMs.coerceIn(220, 650) }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_THEME] = dark }
    }
}
