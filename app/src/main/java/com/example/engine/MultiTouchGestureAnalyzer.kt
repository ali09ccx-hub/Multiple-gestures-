package com.example.engine

import com.example.data.GestureTriggerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class TouchPoint(val x: Float, val y: Float)

class MultiTouchGestureAnalyzer(
    private val scope: CoroutineScope,
    private val density: Float,
    private var multiTapWindowMs: Long = 380L,
    private val onGestureDetected: (GestureTriggerType) -> Unit
) {
    private val activeStartPoints = mutableMapOf<Int, TouchPoint>()
    private val lastKnownPoints = mutableMapOf<Int, TouchPoint>()
    private val activePointerIds = mutableSetOf<Int>()
    private val singleFingerTrail = mutableListOf<TouchPoint>()

    private var maxPointerCount = 0
    private var gestureStartTimeMs = 0L
    private var holdFiredThisStroke = false
    private var holdJob: Job? = null

    private var pendingTapJob: Job? = null
    private var pendingTapPointerCount = 0
    private var pendingTapTimestamp = 0L

    private val tapMoveThresholdPx = 24f * density
    private val swipeThresholdPx = 56f * density
    private val pinchThresholdPx = 60f * density
    private val drawMinLengthPx = 90f * density

    fun updateMultiTapWindow(windowMs: Long) {
        multiTapWindowMs = windowMs.coerceIn(220L, 650L)
    }

    fun onPointerDown(pointerId: Int, x: Float, y: Float, nowMs: Long = System.currentTimeMillis()) {
        if (activePointerIds.isEmpty()) {
            activeStartPoints.clear()
            lastKnownPoints.clear()
            singleFingerTrail.clear()
            maxPointerCount = 0
            gestureStartTimeMs = nowMs
            holdFiredThisStroke = false
        }

        val pt = TouchPoint(x, y)
        activeStartPoints[pointerId] = pt
        lastKnownPoints[pointerId] = pt
        activePointerIds.add(pointerId)

        if (activePointerIds.size > maxPointerCount) {
            maxPointerCount = activePointerIds.size
        }

        if (maxPointerCount == 1) {
            singleFingerTrail.add(pt)
        } else {
            singleFingerTrail.clear()
        }

        holdJob?.cancel()
        if (maxPointerCount in 2..3 && !holdFiredThisStroke) {
            val targetPointers = maxPointerCount
            holdJob = scope.launch {
                delay(520L)
                if (!holdFiredThisStroke &&
                    activePointerIds.size == targetPointers &&
                    maxMovementPx() < tapMoveThresholdPx
                ) {
                    holdFiredThisStroke = true
                    pendingTapJob?.cancel()
                    pendingTapPointerCount = 0
                    when (targetPointers) {
                        2 -> onGestureDetected(GestureTriggerType.TWO_FINGER_HOLD)
                        3 -> onGestureDetected(GestureTriggerType.THREE_FINGER_HOLD)
                    }
                }
            }
        }
    }

    fun onPointerMove(pointerId: Int, x: Float, y: Float) {
        if (!activePointerIds.contains(pointerId)) return
        val pt = TouchPoint(x, y)
        lastKnownPoints[pointerId] = pt
        if (maxPointerCount == 1 && singleFingerTrail.isNotEmpty()) {
            val last = singleFingerTrail.last()
            if (hypot(x - last.x, y - last.y) > 4f * density) {
                singleFingerTrail.add(pt)
            }
        }
        if (maxMovementPx() > tapMoveThresholdPx) {
            holdJob?.cancel()
        }
    }

    fun onPointerUp(pointerId: Int, x: Float, y: Float, nowMs: Long = System.currentTimeMillis()) {
        lastKnownPoints[pointerId] = TouchPoint(x, y)
        activePointerIds.remove(pointerId)

        if (activePointerIds.isEmpty()) {
            holdJob?.cancel()
            if (!holdFiredThisStroke) {
                evaluateCompletedStroke(nowMs)
            }
        }
    }

    fun onCancel() {
        holdJob?.cancel()
        activePointerIds.clear()
        activeStartPoints.clear()
        lastKnownPoints.clear()
        singleFingerTrail.clear()
        maxPointerCount = 0
    }

    private fun maxMovementPx(): Float {
        var maxDist = 0f
        for (id in activePointerIds) {
            val start = activeStartPoints[id] ?: continue
            val cur = lastKnownPoints[id] ?: continue
            val dist = hypot(cur.x - start.x, cur.y - start.y)
            if (dist > maxDist) maxDist = dist
        }
        return maxDist
    }

    private fun evaluateCompletedStroke(nowMs: Long) {
        val duration = nowMs - gestureStartTimeMs
        val ids = activeStartPoints.keys.toList()
        if (ids.isEmpty()) return

        if (maxPointerCount >= 5) {
            clearPendingTap()
            onGestureDetected(GestureTriggerType.FIVE_FINGER_TAP)
            return
        }

        var sumDx = 0f
        var sumDy = 0f
        var maxDisp = 0f
        var count = 0
        for (id in ids) {
            val start = activeStartPoints[id] ?: continue
            val end = lastKnownPoints[id] ?: continue
            val dx = end.x - start.x
            val dy = end.y - start.y
            val disp = hypot(dx, dy)
            sumDx += dx
            sumDy += dy
            if (disp > maxDisp) maxDisp = disp
            count++
        }
        if (count == 0) return

        val avgDx = sumDx / count
        val avgDy = sumDy / count

        // Check tap vs swipe/pinch/draw
        if (maxDisp <= tapMoveThresholdPx && duration <= 460L) {
            handleMultiFingerTap(maxPointerCount, nowMs)
            return
        }

        // Swipe or pinch
        when (maxPointerCount) {
            3 -> {
                clearPendingTap()
                val avgDist = hypot(avgDx, avgDy)
                if (avgDist >= swipeThresholdPx) {
                    if (abs(avgDy) >= abs(avgDx)) {
                        if (avgDy < 0) onGestureDetected(GestureTriggerType.THREE_FINGER_SWIPE_UP)
                        else onGestureDetected(GestureTriggerType.THREE_FINGER_SWIPE_DOWN)
                    } else {
                        if (avgDx > 0) onGestureDetected(GestureTriggerType.THREE_FINGER_SWIPE_RIGHT)
                        else onGestureDetected(GestureTriggerType.THREE_FINGER_SWIPE_LEFT)
                    }
                }
            }
            2 -> {
                clearPendingTap()
                if (ids.size >= 2) {
                    val s0 = activeStartPoints[ids[0]]!!
                    val s1 = activeStartPoints[ids[1]]!!
                    val e0 = lastKnownPoints[ids[0]]!!
                    val e1 = lastKnownPoints[ids[1]]!!
                    val startSpan = hypot(s1.x - s0.x, s1.y - s0.y)
                    val endSpan = hypot(e1.x - e0.x, e1.y - e0.y)
                    val spanDelta = endSpan - startSpan

                    if (spanDelta > pinchThresholdPx && endSpan / max(startSpan, 1f) > 1.35f) {
                        onGestureDetected(GestureTriggerType.TWO_FINGER_PINCH_OUT)
                        return
                    }
                    if (-spanDelta > pinchThresholdPx && startSpan / max(endSpan, 1f) > 1.35f) {
                        onGestureDetected(GestureTriggerType.TWO_FINGER_PINCH_IN)
                        return
                    }
                }
                if (abs(avgDy) >= swipeThresholdPx && abs(avgDy) > abs(avgDx) * 1.15f) {
                    if (avgDy < 0) onGestureDetected(GestureTriggerType.TWO_FINGER_SWIPE_UP)
                    else onGestureDetected(GestureTriggerType.TWO_FINGER_SWIPE_DOWN)
                }
            }
            1 -> {
                clearPendingTap()
                recognizeDrawnShape(singleFingerTrail)?.let(onGestureDetected)
            }
        }
    }

    private fun handleMultiFingerTap(fingerCount: Int, nowMs: Long) {
        if (fingerCount !in 2..4) return

        val isSecondTap = pendingTapPointerCount == fingerCount &&
            (nowMs - pendingTapTimestamp) <= multiTapWindowMs

        if (isSecondTap) {
            clearPendingTap()
            when (fingerCount) {
                2 -> onGestureDetected(GestureTriggerType.TWO_FINGER_DOUBLE_TAP)
                3 -> onGestureDetected(GestureTriggerType.THREE_FINGER_DOUBLE_TAP)
                4 -> onGestureDetected(GestureTriggerType.FOUR_FINGER_DOUBLE_TAP)
            }
            return
        }

        clearPendingTap()
        pendingTapPointerCount = fingerCount
        pendingTapTimestamp = nowMs
        pendingTapJob = scope.launch {
            delay(multiTapWindowMs)
            val savedCount = pendingTapPointerCount
            pendingTapPointerCount = 0
            when (savedCount) {
                3 -> onGestureDetected(GestureTriggerType.THREE_FINGER_TAP)
                4 -> onGestureDetected(GestureTriggerType.FOUR_FINGER_TAP)
            }
        }
    }

    private fun clearPendingTap() {
        pendingTapJob?.cancel()
        pendingTapJob = null
        pendingTapPointerCount = 0
    }

    internal fun recognizeDrawnShape(points: List<TouchPoint>): GestureTriggerType? {
        if (points.size < 8) return null
        var pathLen = 0f
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (i in points.indices) {
            val p = points[i]
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
            if (i > 0) {
                pathLen += hypot(p.x - points[i - 1].x, p.y - points[i - 1].y)
            }
        }

        if (pathLen < drawMinLengthPx) return null

        val width = max(maxX - minX, 1f)
        val height = max(maxY - minY, 1f)
        val diag = hypot(width, height)
        if (diag < 40f * density) return null

        val start = points.first()
        val end = points.last()
        val endToStartDist = hypot(end.x - start.x, end.y - start.y)

        // 1. Circle (O): closed path, balanced aspect ratio, sweeps around center
        val aspect = min(width, height) / max(width, height)
        if (endToStartDist < diag * 0.42f && aspect > 0.42f) {
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            var totalAngle = 0f
            for (i in 1 until points.size) {
                val a1 = atan2(points[i - 1].y - cy, points[i - 1].x - cx)
                val a2 = atan2(points[i].y - cy, points[i].x - cx)
                var delta = a2 - a1
                while (delta > Math.PI) delta -= (2 * Math.PI).toFloat()
                while (delta < -Math.PI) delta += (2 * Math.PI).toFloat()
                totalAngle += delta
            }
            if (abs(totalAngle) > 4.2f) {
                return GestureTriggerType.DRAW_CIRCLE
            }
        }

        // Open shapes: check V, L, Z
        // Find lowest point (maxY) index for V and L
        var maxYIdx = 0
        for (i in points.indices) {
            if (points[i].y > points[maxYIdx].y) maxYIdx = i
        }
        val midRatio = maxYIdx.toFloat() / (points.size - 1).coerceAtLeast(1)

        // 2. V shape: starts high, drops to lowest point around middle, rises back up
        if (midRatio in 0.25f..0.75f) {
            val vertex = points[maxYIdx]
            val leftDrop = vertex.y - start.y
            val rightRise = vertex.y - end.y
            if (leftDrop > height * 0.45f && rightRise > height * 0.45f && end.x != start.x) {
                return GestureTriggerType.DRAW_V
            }
        }

        // 3. L shape: first half moves strongly down, second half moves strongly right
        val midIdx = points.size / 2
        val midPt = points[midIdx]
        val leg1Dy = midPt.y - start.y
        val leg1Dx = midPt.x - start.x
        val leg2Dx = end.x - midPt.x
        val leg2Dy = end.y - midPt.y
        if (leg1Dy > height * 0.5f && abs(leg1Dy) > abs(leg1Dx) * 1.4f &&
            leg2Dx > width * 0.5f && abs(leg2Dx) > abs(leg2Dy) * 1.4f
        ) {
            return GestureTriggerType.DRAW_L
        }

        // 4. Z shape: top-left -> top-right -> bottom-left -> bottom-right
        val n = points.size
        val p0 = points[0]
        val p1 = points[(n * 0.33f).toInt().coerceIn(0, n - 1)]
        val p2 = points[(n * 0.67f).toInt().coerceIn(0, n - 1)]
        val p3 = points[n - 1]
        val seg1Right = (p1.x - p0.x) > width * 0.4f && abs(p1.y - p0.y) < height * 0.4f
        val seg2DiagLeftDown = (p2.x - p1.x) < -width * 0.35f && (p2.y - p1.y) > height * 0.35f
        val seg3Right = (p3.x - p2.x) > width * 0.4f && abs(p3.y - p2.y) < height * 0.4f
        if (seg1Right && seg2DiagLeftDown && seg3Right) {
            return GestureTriggerType.DRAW_Z
        }

        return null
    }
}
