package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GestureTriggerType
import com.example.engine.MultiTouchGestureAnalyzer
import com.example.ui.GestureUiState

@Composable
fun GestureLabScreen(
    state: GestureUiState,
    onToggleExecuteOnTest: (Boolean) -> Unit,
    onGestureDetected: (GestureTriggerType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    val analyzer = remember(density) {
        MultiTouchGestureAnalyzer(
            scope = scope,
            density = density,
            multiTapWindowMs = state.settings.multiTapWindowMs.toLong(),
            onGestureDetected = onGestureDetected
        )
    }

    LaunchedEffect(state.settings.multiTapWindowMs) {
        analyzer.updateMultiTapWindow(state.settings.multiTapWindowMs.toLong())
    }

    val activePointers = remember { mutableStateMapOf<Long, Offset>() }
    val trailPoints = remember { mutableStateListOf<Offset>() }
    var activeFingerCount by remember { mutableIntStateOf(0) }

    val cyan = MaterialTheme.colorScheme.primary
    val sapphire = MaterialTheme.colorScheme.secondary
    val coral = MaterialTheme.colorScheme.tertiary

    val lastMatchedEntity = remember(state.lastLabTrigger, state.allGestures) {
        state.lastLabTrigger?.let { trigger ->
            state.allGestures.firstOrNull { it.id == trigger.name }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header & Live Execution Toggle
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Gesture,
                    contentDescription = null,
                    tint = cyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مختبر الإيماءات التفاعلي",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "تنفيذ الإجراء فعلياً عند الاختبار",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.executeOnLabTest,
                    onCheckedChange = onToggleExecuteOnTest,
                    modifier = Modifier.testTag("lab_execute_switch")
                )
            }
        }

        // Multi-Touch Kinetic Canvas
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF070C18),
            border = BorderStroke(1.5.dp, cyan.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("gesture_lab_canvas")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(analyzer) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val now = System.currentTimeMillis()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        if (activePointers.isEmpty()) {
                                            trailPoints.clear()
                                        }
                                        event.changes.forEach { change ->
                                            if (change.pressed && !activePointers.containsKey(change.id.value)) {
                                                activePointers[change.id.value] = change.position
                                                analyzer.onPointerDown(
                                                    pointerId = change.id.value.toInt(),
                                                    x = change.position.x,
                                                    y = change.position.y,
                                                    nowMs = now
                                                )
                                                if (activePointers.size == 1) {
                                                    trailPoints.add(change.position)
                                                }
                                            }
                                            change.consume()
                                        }
                                        activeFingerCount = activePointers.size
                                    }
                                    PointerEventType.Move -> {
                                        event.changes.forEach { change ->
                                            if (change.pressed) {
                                                activePointers[change.id.value] = change.position
                                                analyzer.onPointerMove(
                                                    pointerId = change.id.value.toInt(),
                                                    x = change.position.x,
                                                    y = change.position.y
                                                )
                                                if (activePointers.size == 1) {
                                                    trailPoints.add(change.position)
                                                }
                                            }
                                            change.consume()
                                        }
                                    }
                                    PointerEventType.Release -> {
                                        event.changes.forEach { change ->
                                            if (!change.pressed && activePointers.containsKey(change.id.value)) {
                                                activePointers.remove(change.id.value)
                                                analyzer.onPointerUp(
                                                    pointerId = change.id.value.toInt(),
                                                    x = change.position.x,
                                                    y = change.position.y,
                                                    nowMs = now
                                                )
                                            }
                                            change.consume()
                                        }
                                        activeFingerCount = activePointers.size
                                    }
                                    else -> {
                                        if (event.changes.none { it.pressed }) {
                                            activePointers.clear()
                                            activeFingerCount = 0
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Kinetic Canvas Drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Subtle tech grid
                    val step = 48.dp.toPx()
                    var x = step
                    while (x < size.width) {
                        drawLine(
                            color = Color(0x16FFFFFF),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = step
                    while (y < size.height) {
                        drawLine(
                            color = Color(0x16FFFFFF),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }

                    // Single-finger laser path
                    if (trailPoints.size > 1) {
                        val path = Path().apply {
                            moveTo(trailPoints.first().x, trailPoints.first().y)
                            for (i in 1 until trailPoints.size) {
                                lineTo(trailPoints[i].x, trailPoints[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(listOf(cyan, sapphire, coral)),
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    // Multi-touch concentric finger ripples
                    activePointers.values.forEach { pos ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(cyan.copy(alpha = 0.45f), Color.Transparent),
                                center = pos,
                                radius = 64.dp.toPx()
                            ),
                            radius = 64.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = cyan,
                            radius = 34.dp.toPx(),
                            center = pos,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = pos
                        )
                    }
                }

                // Top HUD inside canvas
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xAA162036)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = cyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "أصابع نشطة: $activeFingerCount",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = lastMatchedEntity != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        lastMatchedEntity?.let { matched ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cyan.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, cyan)
                            ) {
                                Text(
                                    text = "${matched.badgeLabel}  ${matched.titleAr} ← ${matched.targetLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Center prompt when idle
                if (activeFingerCount == 0 && trailPoints.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "المس هنا لتجربة الإيماءات",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "جرّب الضغط بـ 3 أصابع مرتين، أو السحب بـ 3 أصابع، أو رسم دائرة O / حرف V / L / Z",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Bottom quick gesture cheat-sheet pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(14.dp)
                ) {
                    listOf("3×2 يوتيوب", "3×1 كلاود", "3↓ لقطة شاشة", "O دائرة").forEach { hint ->
                        AssistChip(
                            onClick = {},
                            label = { Text(hint, color = Color.White) },
                            modifier = Modifier.background(Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
