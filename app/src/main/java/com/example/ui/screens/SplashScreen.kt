package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic, lively startup splash screen showcasing the "WeCollect" brand.
 * Features an energetic elastic entrance bounce, organic hovering wave,
 * lively tilt/sway, continuous light-sweep gleam, and ambient pulse ripples.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Entrance animation animatables
    val logoScale = remember { Animatable(0.40f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoTilt = remember { Animatable(-9f) }
    val auraScale = remember { Animatable(0.3f) }
    val auraAlpha = remember { Animatable(0f) }

    // Dynamic continuous looping transitions ("mihetsiketsika tsara")
    val infiniteTransition = rememberInfiniteTransition(label = "wecollectDynamicMotion")

    // 1. Lively vertical floating wave (bobbing motion)
    val floatY by infiniteTransition.animateFloat(
        initialValue = -9f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // 2. Playful rhythmic tilt/sway
    val swayAngle by infiniteTransition.animateFloat(
        initialValue = -3.2f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swayAngle"
    )

    // 3. Subtle breathing scale pulse
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    // 4. Luminous shine sweep passing across the logo letters
    val shimmerSweep by infiniteTransition.animateFloat(
        initialValue = -0.8f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerSweep"
    )

    // 5. Dynamic energy ripple ring 1
    val ripple1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple1"
    )

    // 6. Dynamic energy ripple ring 2
    val ripple2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple2"
    )

    // 7. Ambient particle float phase
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )

    LaunchedEffect(Unit) {
        // Step 1: High-energy entrance with spring bounce & tilt rebound
        launch {
            auraAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            auraScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 450, easing = FastOutLinearInEasing)
            )
        }
        launch {
            logoTilt.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // Hold long enough to showcase the dynamic motion and shine (~2.4s)
        delay(2400)

        // Seamlessly complete splash and transition to login
        onSplashFinished()
    }

    // Edge-to-edge system bars styling
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("splash_screen")
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished
            ),
        contentAlignment = Alignment.Center
    ) {
        // Dynamic expanding energy ripples behind the logo
        Canvas(
            modifier = Modifier
                .size(360.dp)
                .alpha(auraAlpha.value)
        ) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f

            // Ripple 1 (Warm signature orange)
            val r1 = ripple1 * maxRadius
            val a1 = (1f - ripple1) * 0.35f
            if (a1 > 0f) {
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = a1),
                    radius = r1,
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Ripple 2 (Signature deep electric blue)
            val r2 = ripple2 * maxRadius
            val a2 = (1f - ripple2) * 0.30f
            if (a2 > 0f) {
                drawCircle(
                    color = Color(0xFF0284C7).copy(alpha = a2),
                    radius = r2,
                    center = centerOffset,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Playful orbiting sparkle particles
            val particleColors = listOf(
                Color(0xFFF97316),
                Color(0xFF0284C7),
                Color(0xFFFB923C),
                Color(0xFF38BDF8)
            )
            val numParticles = 6
            for (i in 0 until numParticles) {
                val angle = particlePhase + (i * (2 * Math.PI / numParticles).toFloat())
                val orbitX = centerOffset.x + (125.dp.toPx() * cos(angle))
                val orbitY = centerOffset.y + (48.dp.toPx() * sin(angle * 1.5f))
                val particleAlpha = ((sin(angle * 2f) + 1f) / 2f) * 0.55f * auraAlpha.value
                val particleColor = particleColors[i % particleColors.size]

                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha),
                    radius = (2.5f + sin(angle * 3f)).dp.toPx(),
                    center = Offset(orbitX, orbitY)
                )
            }
        }

        // Ambient glowing radial aura centered behind WeCollect logo
        Box(
            modifier = Modifier
                .size(310.dp)
                .scale(auraScale.value * breatheScale)
                .alpha(auraAlpha.value * 0.90f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFF97316).copy(alpha = 0.20f), // Warm WeCollect orange
                            Color(0xFF0284C7).copy(alpha = 0.12f), // Deep WeCollect blue
                            Color(0xFF38BDF8).copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // The dynamic, animated WeCollect logo with bounce, sway, float, and shining light gleam
        Image(
            painter = painterResource(id = R.drawable.ic_wecollect_logo),
            contentDescription = "WeCollect",
            modifier = Modifier
                .width(270.dp)
                .height(82.dp)
                .graphicsLayer {
                    translationY = floatY
                    rotationZ = logoTilt.value + swayAngle
                    scaleX = logoScale.value * breatheScale
                    scaleY = logoScale.value * breatheScale
                    alpha = logoAlpha.value
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // Luminous diagonal shine bar sweeping dynamically across the letters
                    val sweepCenter = shimmerSweep * size.width
                    val shineWidth = size.width * 0.45f
                    val sweepBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        start = Offset(sweepCenter - shineWidth / 2f, 0f),
                        end = Offset(sweepCenter + shineWidth / 2f, size.height)
                    )
                    drawRect(
                        brush = sweepBrush,
                        blendMode = BlendMode.SrcAtop
                    )
                }
                .padding(horizontal = 16.dp)
                .testTag("wecollect_splash_logo"),
            contentScale = ContentScale.Fit
        )
    }
}
