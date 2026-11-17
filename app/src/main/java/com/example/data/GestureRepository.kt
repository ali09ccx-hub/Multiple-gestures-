package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GestureRepository(
    private val context: Context,
    private val gestureDao: GestureDao,
    val settingsStore: SettingsDataStore
) {
    val gesturesFlow: Flow<List<GestureEntity>> = gestureDao.observeAllGestures()
    val settingsFlow: Flow<GestureSettings> = settingsStore.settingsFlow

    suspend fun ensureDefaultsSeeded() {
        val existing = gestureDao.getAllGesturesOnce()
        if (existing.size < GestureTriggerType.entries.size) {
            gestureDao.insertAllIgnore(buildDefaultGestures())
        }
    }

    suspend fun resetDefaults() {
        gestureDao.upsertAll(buildDefaultGestures())
    }

    suspend fun setGestureEnabled(id: String, enabled: Boolean) {
        gestureDao.setEnabled(id, enabled)
    }

    suspend fun setAllGesturesEnabled(enabled: Boolean) {
        gestureDao.setAllEnabled(enabled)
    }

    suspend fun assignAction(
        gestureId: String,
        actionType: ActionType,
        targetPackage: String?,
        targetLabel: String,
        targetUrl: String? = null
    ) {
        val current = gestureDao.getGestureById(gestureId) ?: return
        gestureDao.updateGesture(
            current.copy(
                actionType = actionType.name,
                targetPackage = targetPackage,
                targetLabel = targetLabel,
                targetUrl = targetUrl,
                isEnabled = true
            )
        )
    }

    suspend fun getEnabledGesture(trigger: GestureTriggerType): GestureEntity? {
        val entity = gestureDao.getGestureById(trigger.name) ?: return null
        return if (entity.isEnabled) entity else null
    }

    suspend fun recordTrigger(gestureId: String) {
        gestureDao.incrementTriggerCount(gestureId)
    }

    suspend fun loadInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val myPackage = context.packageName
        resolveInfos
            .asSequence()
            .filter { it.activityInfo.packageName != myPackage }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                InstalledAppInfo(
                    appName = info.loadLabel(pm)?.toString() ?: info.activityInfo.packageName,
                    packageName = info.activityInfo.packageName,
                    icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                    isInstalled = true
                )
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    companion object {
        @Volatile
        private var INSTANCE: GestureRepository? = null

        fun getInstance(context: Context): GestureRepository {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val db = GestureDatabase.getInstance(appContext)
                val repo = GestureRepository(
                    context = appContext,
                    gestureDao = db.gestureDao(),
                    settingsStore = SettingsDataStore(appContext)
                )
                INSTANCE = repo
                repo
            }
        }

        fun buildDefaultGestures(): List<GestureEntity> {
            fun entry(
                trigger: GestureTriggerType,
                enabled: Boolean,
                actionType: ActionType,
                pkg: String? = null,
                label: String,
                url: String? = null
            ) = GestureEntity(
                id = trigger.name,
                sortOrder = trigger.sortOrder,
                titleAr = trigger.titleAr,
                subtitleAr = trigger.subtitleAr,
                badgeLabel = trigger.badgeLabel,
                category = trigger.category.name,
                isEnabled = enabled,
                actionType = actionType.name,
                targetPackage = pkg,
                targetLabel = label,
                targetUrl = url
            )

            return listOf(
                entry(
                    GestureTriggerType.THREE_FINGER_DOUBLE_TAP,
                    true,
                    ActionType.OPEN_APP,
                    "com.google.android.youtube",
                    "YouTube",
                    "https://m.youtube.com"
                ),
                entry(
                    GestureTriggerType.THREE_FINGER_TAP,
                    true,
                    ActionType.OPEN_APP,
                    "com.anthropic.claude",
                    "Claude",
                    "https://claude.ai"
                ),
                entry(
                    GestureTriggerType.THREE_FINGER_HOLD,
                    true,
                    ActionType.SYSTEM_SCREENSHOT,
                    null,
                    ActionType.SYSTEM_SCREENSHOT.titleAr
                ),
                entry(
                    GestureTriggerType.TWO_FINGER_DOUBLE_TAP,
                    true,
                    ActionType.TOGGLE_FLASHLIGHT,
                    null,
                    ActionType.TOGGLE_FLASHLIGHT.titleAr
                ),
                entry(
                    GestureTriggerType.TWO_FINGER_HOLD,
                    true,
                    ActionType.SYSTEM_LOCK_SCREEN,
                    null,
                    ActionType.SYSTEM_LOCK_SCREEN.titleAr
                ),
                entry(
                    GestureTriggerType.FOUR_FINGER_TAP,
                    true,
                    ActionType.SYSTEM_RECENTS,
                    null,
                    ActionType.SYSTEM_RECENTS.titleAr
                ),
                entry(
                    GestureTriggerType.FOUR_FINGER_DOUBLE_TAP,
                    true,
                    ActionType.SYSTEM_SPLIT_SCREEN,
                    null,
                    ActionType.SYSTEM_SPLIT_SCREEN.titleAr
                ),
                entry(
                    GestureTriggerType.FIVE_FINGER_TAP,
                    true,
                    ActionType.SYSTEM_HOME,
                    null,
                    ActionType.SYSTEM_HOME.titleAr
                ),
                entry(
                    GestureTriggerType.THREE_FINGER_SWIPE_UP,
                    true,
                    ActionType.OPEN_APP,
                    "com.android.chrome",
                    "Chrome",
                    "https://www.google.com"
                ),
                entry(
                    GestureTriggerType.THREE_FINGER_SWIPE_DOWN,
                    true,
                    ActionType.SYSTEM_SCREENSHOT,
                    null,
                    ActionType.SYSTEM_SCREENSHOT.titleAr
                ),
                entry(
                    GestureTriggerType.THREE_FINGER_SWIPE_RIGHT,
                    true,
                    ActionType.OPEN_APP,
                    "com.whatsapp",
                    "WhatsApp",
                    "https://web.whatsapp.com"
                ),
                entry(
                    GestureTriggerType.THREE_FINGER_SWIPE_LEFT,
                    true,
                    ActionType.OPEN_APP,
                    "org.telegram.messenger",
                    "Telegram",
                    "https://web.telegram.org"
                ),
                entry(
                    GestureTriggerType.TWO_FINGER_SWIPE_UP,
                    true,
                    ActionType.SYSTEM_QUICK_SETTINGS,
                    null,
                    ActionType.SYSTEM_QUICK_SETTINGS.titleAr
                ),
                entry(
                    GestureTriggerType.TWO_FINGER_SWIPE_DOWN,
                    true,
                    ActionType.SYSTEM_NOTIFICATIONS,
                    null,
                    ActionType.SYSTEM_NOTIFICATIONS.titleAr
                ),
                entry(
                    GestureTriggerType.EDGE_LEFT_SWIPE,
                    true,
                    ActionType.SYSTEM_BACK,
                    null,
                    ActionType.SYSTEM_BACK.titleAr
                ),
                entry(
                    GestureTriggerType.EDGE_RIGHT_SWIPE,
                    true,
                    ActionType.OPEN_GESTURE_PAD,
                    null,
                    ActionType.OPEN_GESTURE_PAD.titleAr
                ),
                entry(
                    GestureTriggerType.EDGE_LEFT_HOLD,
                    true,
                    ActionType.TOGGLE_FLASHLIGHT,
                    null,
                    ActionType.TOGGLE_FLASHLIGHT.titleAr
                ),
                entry(
                    GestureTriggerType.EDGE_RIGHT_HOLD,
                    true,
                    ActionType.SYSTEM_POWER_DIALOG,
                    null,
                    ActionType.SYSTEM_POWER_DIALOG.titleAr
                ),
                entry(
                    GestureTriggerType.TWO_FINGER_PINCH_IN,
                    true,
                    ActionType.SYSTEM_LOCK_SCREEN,
                    null,
                    ActionType.SYSTEM_LOCK_SCREEN.titleAr
                ),
                entry(
                    GestureTriggerType.TWO_FINGER_PINCH_OUT,
                    true,
                    ActionType.OPEN_APP,
                    "com.android.settings",
                    "الإعدادات",
                    null
                ),
                entry(
                    GestureTriggerType.DRAW_CIRCLE,
                    true,
                    ActionType.OPEN_APP,
                    "com.google.android.youtube",
                    "YouTube",
                    "https://m.youtube.com"
                ),
                entry(
                    GestureTriggerType.DRAW_V,
                    true,
                    ActionType.TOGGLE_FLASHLIGHT,
                    null,
                    ActionType.TOGGLE_FLASHLIGHT.titleAr
                ),
                entry(
                    GestureTriggerType.DRAW_L,
                    true,
                    ActionType.OPEN_URL,
                    null,
                    "Claude Web",
                    "https://claude.ai"
                ),
                entry(
                    GestureTriggerType.DRAW_Z,
                    true,
                    ActionType.MEDIA_PLAY_PAUSE,
                    null,
                    ActionType.MEDIA_PLAY_PAUSE.titleAr
                )
            )
        }
    }
}
