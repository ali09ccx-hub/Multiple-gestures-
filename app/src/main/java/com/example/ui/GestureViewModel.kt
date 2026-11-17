package com.example.ui

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionType
import com.example.data.GestureCategory
import com.example.data.GestureEntity
import com.example.data.GestureRepository
import com.example.data.GestureSettings
import com.example.data.GestureTriggerType
import com.example.data.InstalledAppInfo
import com.example.engine.ExecutionResult
import com.example.engine.GestureActionExecutor
import com.example.service.GestureAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GestureUiState(
    val allGestures: List<GestureEntity> = emptyList(),
    val filteredGestures: List<GestureEntity> = emptyList(),
    val selectedCategory: GestureCategory = GestureCategory.ALL,
    val searchQuery: String = "",
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingApps: Boolean = true,
    val settings: GestureSettings = GestureSettings(),
    val isAccessibilityEnabled: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val editingGesture: GestureEntity? = null,
    val lastLabTrigger: GestureTriggerType? = null,
    val executeOnLabTest: Boolean = false,
    val feedbackMessage: String? = null
) {
    val enabledCount: Int get() = allGestures.count { it.isEnabled }
    val totalCount: Int get() = allGestures.size
}

class GestureViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GestureRepository.getInstance(application)
    private val executor = GestureActionExecutor(application)

    private val selectedCategory = MutableStateFlow(GestureCategory.ALL)
    private val searchQuery = MutableStateFlow("")
    private val installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val isLoadingApps = MutableStateFlow(true)
    private val canDrawOverlays = MutableStateFlow(Settings.canDrawOverlays(application))
    private val editingGesture = MutableStateFlow<GestureEntity?>(null)
    private val lastLabTrigger = MutableStateFlow<GestureTriggerType?>(null)
    private val executeOnLabTest = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    private data class FilterState(
        val category: GestureCategory,
        val query: String,
        val apps: List<InstalledAppInfo>,
        val loadingApps: Boolean,
        val overlayAllowed: Boolean
    )

    private data class InteractionState(
        val editing: GestureEntity?,
        val lastTrigger: GestureTriggerType?,
        val executeLab: Boolean,
        val feedback: String?
    )

    private val filterFlow = combine(
        selectedCategory,
        searchQuery,
        installedApps,
        isLoadingApps,
        canDrawOverlays
    ) { cat, query, apps, loading, overlay ->
        FilterState(cat, query, apps, loading, overlay)
    }

    private val interactionFlow = combine(
        editingGesture,
        lastLabTrigger,
        executeOnLabTest,
        feedbackMessage
    ) { editing, lastTrigger, execLab, feedback ->
        InteractionState(editing, lastTrigger, execLab, feedback)
    }

    val uiState: StateFlow<GestureUiState> = combine(
        repository.gesturesFlow,
        repository.settingsFlow,
        GestureAccessibilityService.isRunningFlow,
        filterFlow,
        interactionFlow
    ) { gestures, settings, accRunning, filter, interaction ->
        val filtered = gestures.filter { item ->
            val catMatch = filter.category == GestureCategory.ALL || item.category == filter.category.name
            val q = filter.query.trim()
            val queryMatch = q.isEmpty() ||
                item.titleAr.contains(q, ignoreCase = true) ||
                item.targetLabel.contains(q, ignoreCase = true) ||
                item.badgeLabel.contains(q, ignoreCase = true)
            catMatch && queryMatch
        }
        GestureUiState(
            allGestures = gestures,
            filteredGestures = filtered,
            selectedCategory = filter.category,
            searchQuery = filter.query,
            installedApps = filter.apps,
            isLoadingApps = filter.loadingApps,
            settings = settings,
            isAccessibilityEnabled = accRunning,
            canDrawOverlays = filter.overlayAllowed,
            editingGesture = interaction.editing,
            lastLabTrigger = interaction.lastTrigger,
            executeOnLabTest = interaction.executeLab,
            feedbackMessage = interaction.feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GestureUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultsSeeded()
        }
        refreshInstalledApps()
    }

    fun refreshPermissions() {
        canDrawOverlays.value = Settings.canDrawOverlays(getApplication())
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            isLoadingApps.value = true
            installedApps.value = repository.loadInstalledApps()
            isLoadingApps.value = false
        }
    }

    fun selectCategory(category: GestureCategory) {
        selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun openEditor(gesture: GestureEntity) {
        editingGesture.value = gesture
    }

    fun closeEditor() {
        editingGesture.value = null
    }

    fun toggleGesture(id: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setGestureEnabled(id, enabled)
        }
    }

    fun setAllGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAllGesturesEnabled(enabled)
            feedbackMessage.value = if (enabled) "تم تفعيل جميع الإيماءات (24)" else "تم إيقاف جميع الإيماءات"
        }
    }

    fun assignApp(gestureId: String, appName: String, packageName: String, fallbackUrl: String? = null) {
        viewModelScope.launch {
            repository.assignAction(
                gestureId = gestureId,
                actionType = ActionType.OPEN_APP,
                targetPackage = packageName,
                targetLabel = appName,
                targetUrl = fallbackUrl
            )
            editingGesture.value = null
            feedbackMessage.value = "تم ربط الإيماءة بتطبيق $appName"
        }
    }

    fun assignShortcut(gestureId: String, actionType: ActionType) {
        viewModelScope.launch {
            repository.assignAction(
                gestureId = gestureId,
                actionType = actionType,
                targetPackage = null,
                targetLabel = actionType.titleAr,
                targetUrl = null
            )
            editingGesture.value = null
            feedbackMessage.value = "تم تعيين الاختصار: ${actionType.titleAr}"
        }
    }

    fun assignCustomUrl(gestureId: String, label: String, url: String) {
        viewModelScope.launch {
            val cleanUrl = url.trim()
            if (cleanUrl.isEmpty()) return@launch
            repository.assignAction(
                gestureId = gestureId,
                actionType = ActionType.OPEN_URL,
                targetPackage = null,
                targetLabel = label.ifBlank { cleanUrl },
                targetUrl = cleanUrl
            )
            editingGesture.value = null
            feedbackMessage.value = "تم ربط الإيماءة بالرابط"
        }
    }

    fun testGestureNow(gesture: GestureEntity) {
        viewModelScope.launch {
            repository.recordTrigger(gesture.id)
            val result = executor.execute(gesture, uiState.value.settings.hapticFeedback)
            feedbackMessage.value = result.message
        }
    }

    fun setExecuteOnLabTest(enabled: Boolean) {
        executeOnLabTest.value = enabled
    }

    fun onLabGestureDetected(trigger: GestureTriggerType) {
        lastLabTrigger.value = trigger
        val matched = uiState.value.allGestures.firstOrNull { it.id == trigger.name }
        if (executeOnLabTest.value && matched != null && matched.isEnabled) {
            testGestureNow(matched)
        } else if (matched != null) {
            val status = if (matched.isEnabled) matched.targetLabel else "معطّلة حالياً"
            feedbackMessage.value = "تم رصد: ${trigger.titleAr} ← $status"
        }
    }

    fun openOverlayPadFromApp() {
        val service = GestureAccessibilityService.instance
        if (service != null) {
            service.showGesturePadOverlay()
        } else {
            feedbackMessage.value = "فعّل خدمة إمكانية الوصول أولاً لتشغيل اللوحة العائمة فوق اللانشر"
        }
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    // Settings updates
    fun setMasterEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setMasterEnabled(enabled)
    }

    fun setFloatingOrbEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setFloatingOrbEnabled(enabled)
    }

    fun setEdgeZonesEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setEdgeZonesEnabled(enabled)
    }

    fun setEdgeWidthDp(width: Float) = viewModelScope.launch {
        repository.settingsStore.setEdgeWidthDp(width)
    }

    fun setOrbOpacity(opacity: Float) = viewModelScope.launch {
        repository.settingsStore.setOrbOpacity(opacity)
    }

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        repository.settingsStore.setHapticFeedback(enabled)
    }

    fun setMultiTapWindowMs(ms: Int) = viewModelScope.launch {
        repository.settingsStore.setMultiTapWindowMs(ms)
    }

    fun setDarkTheme(dark: Boolean) = viewModelScope.launch {
        repository.settingsStore.setDarkTheme(dark)
    }

    fun resetDefaults() = viewModelScope.launch {
        repository.resetDefaults()
        feedbackMessage.value = "تمت استعادة الإعدادات الافتراضية لجميع الإيماءات (24)"
    }
}
