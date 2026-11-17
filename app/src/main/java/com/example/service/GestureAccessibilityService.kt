package com.example.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.data.ActionType
import com.example.data.GestureRepository
import com.example.data.GestureSettings
import com.example.data.GestureTriggerType
import com.example.engine.ExecutionResult
import com.example.engine.GestureActionExecutor
import com.example.engine.MultiTouchGestureAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot

class GestureAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var windowManager: WindowManager
    private lateinit var repository: GestureRepository
    private lateinit var executor: GestureActionExecutor

    private var currentSettings = GestureSettings()
    private var leftEdgeView: View? = null
    private var rightEdgeView: View? = null
    private var floatingOrbView: View? = null
    private var floatingOrbParams: WindowManager.LayoutParams? = null
    private var gesturePadView: GesturePadOverlayView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isRunning.value = true

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = GestureRepository.getInstance(applicationContext)
        executor = GestureActionExecutor(applicationContext)

        serviceScope.launch {
            repository.ensureDefaultsSeeded()
        }

        serviceScope.launch {
            repository.settingsFlow.collect { settings ->
                currentSettings = settings
                syncOverlays()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Global actions and overlay triggers do not inspect app content
    }

    override fun onInterrupt() {
        hideGesturePadOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isRunning.value = false
        removeAllOverlays()
        serviceScope.cancel()
    }

    fun performSystemAction(action: ActionType): Boolean {
        return when (action) {
            ActionType.SYSTEM_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            ActionType.SYSTEM_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            ActionType.SYSTEM_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            ActionType.SYSTEM_NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            ActionType.SYSTEM_QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            ActionType.SYSTEM_POWER_DIALOG -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            ActionType.SYSTEM_SPLIT_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                } else false
            }
            ActionType.SYSTEM_LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                } else false
            }
            ActionType.SYSTEM_SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                } else false
            }
            else -> false
        }
    }

    fun showGesturePadOverlay() {
        if (gesturePadView != null) return
        val pad = GesturePadOverlayView(
            context = this,
            multiTapWindowMs = currentSettings.multiTapWindowMs.toLong(),
            onDismiss = { hideGesturePadOverlay() },
            onGesture = { trigger ->
                hideGesturePadOverlay()
                triggerGesture(trigger)
            }
        )
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        runCatching {
            windowManager.addView(pad, params)
            gesturePadView = pad
        }
    }

    fun hideGesturePadOverlay() {
        gesturePadView?.let { view ->
            runCatching { windowManager.removeView(view) }
            gesturePadView = null
        }
    }

    private fun triggerGesture(trigger: GestureTriggerType) {
        if (!currentSettings.masterEnabled) return
        serviceScope.launch {
            val mapping = repository.getEnabledGesture(trigger)
            if (mapping == null) {
                Toast.makeText(
                    applicationContext,
                    "الإيماءة (${trigger.titleAr}) غير مفعّلة",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            repository.recordTrigger(mapping.id)
            val result = executor.execute(mapping, currentSettings.hapticFeedback)
            if (result is ExecutionResult.Error || result is ExecutionResult.NeedsAccessibility) {
                Toast.makeText(applicationContext, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncOverlays() {
        removeEdgeOverlays()
        removeFloatingOrb()

        if (!currentSettings.masterEnabled) {
            hideGesturePadOverlay()
            return
        }

        if (currentSettings.edgeZonesEnabled) {
            addEdgeOverlays()
        }
        if (currentSettings.floatingOrbEnabled) {
            addFloatingOrb()
        }
    }

    private fun removeAllOverlays() {
        removeEdgeOverlays()
        removeFloatingOrb()
        hideGesturePadOverlay()
    }

    private fun removeEdgeOverlays() {
        leftEdgeView?.let { runCatching { windowManager.removeView(it) } }
        rightEdgeView?.let { runCatching { windowManager.removeView(it) } }
        leftEdgeView = null
        rightEdgeView = null
    }

    private fun removeFloatingOrb() {
        floatingOrbView?.let { runCatching { windowManager.removeView(it) } }
        floatingOrbView = null
        floatingOrbParams = null
    }

    private fun addEdgeOverlays() {
        val density = resources.displayMetrics.density
        val widthPx = (currentSettings.edgeWidthDp * density).toInt().coerceAtLeast(16)
        val heightPx = (resources.displayMetrics.heightPixels * 0.42f).toInt()

        val leftView = EdgeHandleView(this, isLeft = true) { isHold ->
            triggerGesture(
                if (isHold) GestureTriggerType.EDGE_LEFT_HOLD else GestureTriggerType.EDGE_LEFT_SWIPE
            )
        }
        val leftParams = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }

        val rightView = EdgeHandleView(this, isLeft = false) { isHold ->
            triggerGesture(
                if (isHold) GestureTriggerType.EDGE_RIGHT_HOLD else GestureTriggerType.EDGE_RIGHT_SWIPE
            )
        }
        val rightParams = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        runCatching {
            windowManager.addView(leftView, leftParams)
            leftEdgeView = leftView
            windowManager.addView(rightView, rightParams)
            rightEdgeView = rightView
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addFloatingOrb() {
        val density = resources.displayMetrics.density
        val sizePx = (48f * density).toInt()
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (10f * density).toInt()
            y = (resources.displayMetrics.heightPixels * 0.34f).toInt()
        }

        val orb = FloatingOrbView(this, currentSettings.orbOpacity)
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var moved = false

        orb.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    if (hypot(dx, dy) > 8f * density) {
                        moved = true
                        params.x = (initialX - dx.toInt()).coerceAtLeast(0)
                        params.y = (initialY + dy.toInt()).coerceAtLeast(0)
                        runCatching { windowManager.updateViewLayout(orb, params) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        showGesturePadOverlay()
                    }
                    true
                }
                else -> false
            }
        }

        runCatching {
            windowManager.addView(orb, params)
            floatingOrbView = orb
            floatingOrbParams = params
        }
    }

    @SuppressLint("ViewConstructor")
    private inner class EdgeHandleView(
        context: Context,
        private val isLeft: Boolean,
        private val onTriggered: (isHold: Boolean) -> Unit
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(95, 0, 229, 255)
            style = Paint.Style.FILL
        }
        private var downX = 0f
        private var downY = 0f
        private var triggered = false
        private var holdJob: Job? = null
        private val density = resources.displayMetrics.density

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val barW = 3.5f * density
            val left = if (isLeft) 0f else width - barW
            val rect = RectF(left, height * 0.28f, left + barW, height * 0.72f)
            canvas.drawRoundRect(rect, barW, barW, paint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    triggered = false
                    holdJob?.cancel()
                    holdJob = serviceScope.launch {
                        delay(480L)
                        if (!triggered) {
                            triggered = true
                            onTriggered(true)
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!triggered) {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        val inward = if (isLeft) dx > 34f * density else dx < -34f * density
                        if (inward && abs(dx) > abs(dy)) {
                            triggered = true
                            holdJob?.cancel()
                            onTriggered(false)
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    holdJob?.cancel()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }

    @SuppressLint("ViewConstructor")
    private class FloatingOrbView(
        context: Context,
        private val orbOpacity: Float
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((orbOpacity * 225).toInt(), 11, 18, 36)
            style = Paint.Style.FILL
        }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((orbOpacity * 245).toInt(), 0, 229, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * density
        }
        private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((orbOpacity * 255).toInt(), 0, 229, 255)
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val r = (width / 2f) - 3f * density
            canvas.drawCircle(cx, cy, r, bgPaint)
            canvas.drawCircle(cx, cy, r - 1f * density, ringPaint)
            canvas.drawCircle(cx, cy, r * 0.42f, ringPaint)
            canvas.drawCircle(cx, cy, 4f * density, corePaint)
        }
    }

    @SuppressLint("ViewConstructor")
    private inner class GesturePadOverlayView(
        context: Context,
        multiTapWindowMs: Long,
        private val onDismiss: () -> Unit,
        onGesture: (GestureTriggerType) -> Unit
    ) : View(context) {
        private val density = resources.displayMetrics.density
        private val analyzer = MultiTouchGestureAnalyzer(
            scope = serviceScope,
            density = density,
            multiTapWindowMs = multiTapWindowMs,
            onGestureDetected = onGesture
        )

        private val activePointers = mutableMapOf<Int, Pair<Float, Float>>()
        private val trailPath = Path()
        private val closeRect = RectF()

        private val bgPaint = Paint().apply {
            color = Color.argb(196, 7, 11, 20)
        }
        private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 229, 255)
            style = Paint.Style.STROKE
            strokeWidth = 5f * density
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 0, 229, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 18f * density
        }
        private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 148, 163, 184)
            textAlign = Paint.Align.CENTER
            textSize = 13f * density
        }
        private val closeBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 29, 42, 69)
            style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            canvas.drawText(
                "لوحة الإيماءات الحرة",
                width / 2f,
                height * 0.16f,
                textPaint
            )
            canvas.drawText(
                "نفّذ أي إيماءة (مثلاً ضغطتين بـ 3 أصابع أو سحب أو رسم O / V / L / Z)",
                width / 2f,
                height * 0.21f,
                subTextPaint
            )

            canvas.drawPath(trailPath, trailPaint)

            for ((_, pos) in activePointers) {
                canvas.drawCircle(pos.first, pos.second, 34f * density, ringPaint)
                canvas.drawCircle(pos.first, pos.second, 12f * density, ringPaint)
            }

            // Bottom close pill
            val btnW = 140f * density
            val btnH = 48f * density
            val left = (width - btnW) / 2f
            val top = height * 0.85f
            closeRect.set(left, top, left + btnW, top + btnH)
            canvas.drawRoundRect(closeRect, 24f * density, 24f * density, closeBtnPaint)
            canvas.drawText("إغلاق", width / 2f, top + btnH * 0.63f, textPaint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val action = event.actionMasked
            val idx = event.actionIndex

            if (action == MotionEvent.ACTION_DOWN && event.pointerCount == 1) {
                if (closeRect.contains(event.x, event.y)) {
                    onDismiss()
                    return true
                }
                trailPath.reset()
                trailPath.moveTo(event.x, event.y)
            }

            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val id = event.getPointerId(idx)
                    val x = event.getX(idx)
                    val y = event.getY(idx)
                    activePointers[id] = x to y
                    analyzer.onPointerDown(id, x, y)
                }
                MotionEvent.ACTION_MOVE -> {
                    for (i in 0 until event.pointerCount) {
                        val id = event.getPointerId(i)
                        val x = event.getX(i)
                        val y = event.getY(i)
                        activePointers[id] = x to y
                        analyzer.onPointerMove(id, x, y)
                        if (event.pointerCount == 1) {
                            trailPath.lineTo(x, y)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val id = event.getPointerId(idx)
                    val x = event.getX(idx)
                    val y = event.getY(idx)
                    activePointers.remove(id)
                    analyzer.onPointerUp(id, x, y)
                }
                MotionEvent.ACTION_CANCEL -> {
                    activePointers.clear()
                    analyzer.onCancel()
                }
            }
            invalidate()
            return true
        }
    }

    companion object {
        @Volatile
        var instance: GestureAccessibilityService? = null
            private set

        private val _isRunning = MutableStateFlow(false)
        val isRunningFlow: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}
