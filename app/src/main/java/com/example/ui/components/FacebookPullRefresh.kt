package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * FacebookPullRefreshContainer
 * 
 * Container interactif avec geste "Pull-to-Refresh" et indicateur circulaire flottant
 * identique à Facebook sur Android (White circle with Facebook Blue spinner & smooth spring physics).
 */
@Composable
fun FacebookPullRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    pullThreshold: Dp = 65.dp,
    indicatorRestingOffset: Dp = 54.dp,
    indicatorColor: Color = Color(0xFF1877F2), // Facebook Blue
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val pullThresholdPx = with(density) { pullThreshold.toPx() }
    val indicatorRestingOffsetPx = with(density) { indicatorRestingOffset.toPx() }

    val pullDistance = remember { Animatable(0f) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }

    // Synchronisation de la position de l'indicateur selon l'état de chargement
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullDistance.animateTo(
                targetValue = indicatorRestingOffsetPx,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            )
        } else {
            hasTriggeredHaptic = false
            pullDistance.animateTo(
                targetValue = 0f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        }
    }

    fun handleRelease() {
        if (pullDistance.value >= pullThresholdPx && !isRefreshing) {
            onRefresh()
            scope.launch {
                pullDistance.animateTo(
                    targetValue = indicatorRestingOffsetPx,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
                )
            }
        } else if (!isRefreshing) {
            scope.launch {
                pullDistance.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f)
                )
            }
        }
    }

    val nestedScrollConnection = remember(isRefreshing, pullThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Rehefa misintona miakatra nefa mbola eo ambonin'ny aotra ny pullDistance
                if (source == NestedScrollSource.UserInput && available.y < 0 && pullDistance.value > 0f) {
                    val newOffset = (pullDistance.value + available.y).coerceAtLeast(0f)
                    val consumed = pullDistance.value - newOffset
                    scope.launch { pullDistance.snapTo(newOffset) }
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Rehefa misintona midina eo an-tampon'ny lisitra (scroll at top)
                if (source == NestedScrollSource.UserInput && available.y > 0 && !isRefreshing) {
                    val dragFactor = 0.5f
                    val newOffset = (pullDistance.value + available.y * dragFactor).coerceAtMost(pullThresholdPx * 1.6f)

                    if (newOffset >= pullThresholdPx && !hasTriggeredHaptic) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        hasTriggeredHaptic = true
                    } else if (newOffset < pullThresholdPx && hasTriggeredHaptic) {
                        hasTriggeredHaptic = false
                    }

                    scope.launch { pullDistance.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                handleRelease()
                return androidx.compose.ui.unit.Velocity.Zero
            }

            override suspend fun onPostFling(consumed: androidx.compose.ui.unit.Velocity, available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                handleRelease()
                return super.onPostFling(consumed, available)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(isRefreshing) {
                detectVerticalDragGestures(
                    onDragEnd = { handleRelease() },
                    onDragCancel = { handleRelease() },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragAmount > 0 && !isRefreshing) {
                            change.consume()
                            val dragFactor = 0.45f
                            val newOffset = (pullDistance.value + dragAmount * dragFactor).coerceAtMost(pullThresholdPx * 1.6f)
                            if (newOffset >= pullThresholdPx && !hasTriggeredHaptic) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                hasTriggeredHaptic = true
                            }
                            scope.launch { pullDistance.snapTo(newOffset) }
                        } else if (dragAmount < 0 && pullDistance.value > 0f) {
                            change.consume()
                            val newOffset = (pullDistance.value + dragAmount).coerceAtLeast(0f)
                            scope.launch { pullDistance.snapTo(newOffset) }
                        }
                    }
                )
            }
    ) {
        // Votoatin'ny efijery
        content()

        // Facebook Refresh Indicator (Floating Circular Pill)
        val currentOffset = pullDistance.value
        val progress = (currentOffset / pullThresholdPx).coerceIn(0f, 1f)
        val isVisible = currentOffset > 2f || isRefreshing

        if (isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(100f)
                    .offset { IntOffset(0, currentOffset.roundToInt() - with(density) { 36.dp.roundToPx() }) }
                    .padding(top = 8.dp)
            ) {
                FacebookRefreshIndicator(
                    isRefreshing = isRefreshing,
                    pullProgress = progress,
                    indicatorColor = indicatorColor
                )
            }
        }
    }
}

/**
 * FacebookRefreshIndicator
 * Boribory fotsy manana aloka (Elevation) misy spinner manga Facebook na arc mihodina
 */
@Composable
fun FacebookRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier,
    indicatorColor: Color = Color(0xFF1877F2) // Facebook Blue
) {
    val scale = if (isRefreshing) 1f else (pullProgress * 1.15f).coerceIn(0.4f, 1f)

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        modifier = modifier
            .size(42.dp)
            .scale(scale)
            .testTag("facebook_refresh_indicator")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isRefreshing) {
                // Spinning active loader (Facebook Blue)
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = indicatorColor,
                    strokeWidth = 2.8.dp,
                    trackColor = indicatorColor.copy(alpha = 0.15f)
                )
            } else {
                // Arc mihodina manaraka ny fisintonana (Pull progress arc)
                val rotationAngle = pullProgress * 300f
                Canvas(
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotationAngle)
                ) {
                    val strokeWidth = 2.8.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val arcAngle = (pullProgress * 280f).coerceIn(10f, 280f)

                    drawArc(
                        color = indicatorColor,
                        startAngle = 0f,
                        sweepAngle = arcAngle,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
