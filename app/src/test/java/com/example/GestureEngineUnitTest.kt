package com.example

import com.example.data.GestureRepository
import com.example.data.GestureTriggerType
import com.example.engine.MultiTouchGestureAnalyzer
import com.example.engine.TouchPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class GestureEngineUnitTest {

    @Test
    fun defaultsContainMoreThanTwentyGesturesWithYouTubeAndClaude() {
        val defaults = GestureRepository.buildDefaultGestures()
        assertTrue(defaults.size >= 24)
        assertEquals(GestureTriggerType.entries.size, defaults.size)

        val g1 = defaults.first { it.id == GestureTriggerType.THREE_FINGER_DOUBLE_TAP.name }
        assertEquals("com.google.android.youtube", g1.targetPackage)

        val g2 = defaults.first { it.id == GestureTriggerType.THREE_FINGER_TAP.name }
        assertEquals("com.anthropic.claude", g2.targetPackage)
    }

    @Test
    fun detectsThreeFingerDoubleTap() = runTest {
        val detected = mutableListOf<GestureTriggerType>()
        val analyzer = MultiTouchGestureAnalyzer(
            scope = this,
            density = 1f,
            multiTapWindowMs = 380L,
            onGestureDetected = { detected.add(it) }
        )

        // First 3-finger tap
        analyzer.onPointerDown(0, 100f, 100f, nowMs = 0L)
        analyzer.onPointerDown(1, 200f, 100f, nowMs = 10L)
        analyzer.onPointerDown(2, 300f, 100f, nowMs = 20L)
        analyzer.onPointerUp(0, 102f, 102f, nowMs = 60L)
        analyzer.onPointerUp(1, 202f, 102f, nowMs = 65L)
        analyzer.onPointerUp(2, 302f, 102f, nowMs = 70L)

        // Second 3-finger tap within 380ms
        analyzer.onPointerDown(0, 105f, 105f, nowMs = 190L)
        analyzer.onPointerDown(1, 205f, 105f, nowMs = 195L)
        analyzer.onPointerDown(2, 305f, 105f, nowMs = 200L)
        analyzer.onPointerUp(0, 106f, 106f, nowMs = 240L)
        analyzer.onPointerUp(1, 206f, 106f, nowMs = 245L)
        analyzer.onPointerUp(2, 306f, 106f, nowMs = 250L)

        advanceTimeBy(500L)
        assertEquals(listOf(GestureTriggerType.THREE_FINGER_DOUBLE_TAP), detected)
    }

    @Test
    fun detectsThreeFingerSwipeDown() = runTest {
        val detected = mutableListOf<GestureTriggerType>()
        val analyzer = MultiTouchGestureAnalyzer(
            scope = this,
            density = 1f,
            onGestureDetected = { detected.add(it) }
        )

        analyzer.onPointerDown(0, 100f, 100f, nowMs = 0L)
        analyzer.onPointerDown(1, 200f, 100f, nowMs = 5L)
        analyzer.onPointerDown(2, 300f, 100f, nowMs = 10L)
        analyzer.onPointerMove(0, 100f, 260f)
        analyzer.onPointerMove(1, 200f, 260f)
        analyzer.onPointerMove(2, 300f, 260f)
        analyzer.onPointerUp(0, 100f, 260f, nowMs = 180L)
        analyzer.onPointerUp(1, 200f, 260f, nowMs = 185L)
        analyzer.onPointerUp(2, 300f, 260f, nowMs = 190L)

        assertEquals(listOf(GestureTriggerType.THREE_FINGER_SWIPE_DOWN), detected)
    }

    @Test
    fun detectsCircleDrawnShape() = runTest {
        val analyzer = MultiTouchGestureAnalyzer(
            scope = this,
            density = 1f,
            onGestureDetected = {}
        )
        val circlePoints = (0..24).map { i ->
            val angle = (2.0 * Math.PI * i / 24.0)
            TouchPoint(
                x = 200f + (80f * cos(angle)).toFloat(),
                y = 200f + (80f * sin(angle)).toFloat()
            )
        }
        assertEquals(GestureTriggerType.DRAW_CIRCLE, analyzer.recognizeDrawnShape(circlePoints))
    }
}
