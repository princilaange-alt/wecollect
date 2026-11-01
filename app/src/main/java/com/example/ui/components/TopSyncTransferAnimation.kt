package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * TopSyncTransferAnimation
 * Barre de progression ultra-mince avec effet Shimmer lumineux style Facebook
 * Fixée directement sous les Top Headers (Synchronisation, Annonces, Mes enquêtes, etc.)
 */
@Composable
fun TopSyncTransferAnimation(
    isSyncing: Boolean,
    isPullingForm: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var isSuccessState by remember { mutableStateOf(false) }
    val progressAnim = remember { Animatable(0f) }
    val isActive = isSyncing || isPullingForm

    // Fanarahana marina ny fanombohana sy ny fiafaran'ny asa (Sync na Pull)
    LaunchedEffect(isActive) {
        if (isActive) {
            isSuccessState = false
            isVisible = true
            progressAnim.snapTo(0.15f)

            // Miakatra haingana ho 0.40f avy hatrany
            progressAnim.animateTo(
                targetValue = 0.40f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )

            // Mivoatra miandalana hatramin'ny 0.88f mandra-pahavitan'ny asa
            progressAnim.animateTo(
                targetValue = 0.88f,
                animationSpec = tween(2500, easing = LinearEasing)
            )
        } else {
            if (isVisible) {
                // Rehefa vita ny asa (isActive = false) : ankafizo 100% maitso avy hatrany
                isSuccessState = true
                progressAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
                delay(350)
                // Manjavona avy eo
                isVisible = false
                isSuccessState = false
                progressAnim.snapTo(0f)
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(200)),
        exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(250)),
        modifier = modifier.fillMaxWidth()
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "fbProgressShimmer")

        // Tsipika mamirapiratra mandeha (Shimmer sweep 0f -> 1f)
        val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerOffset"
        )

        // Shimmer secondaire ho an'ny halalin'ny fahazavana (pulse)
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .background(Color(0x261877F2))
                .testTag("top_sync_transfer_banner")
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val currentProgressWidth = totalWidthPx * progressAnim.value

            // 1. Bara fototra miandalana (Filling Progress Bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressAnim.value)
                    .fillMaxHeight()
                    .background(
                        if (isSuccessState) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF059669),
                                    Color(0xFF10B981),
                                    Color(0xFF34D399)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1877F2), // Bleu Facebook
                                    Color(0xFF00A3FF), // Cyan vibrant
                                    Color(0xFF38BDF8)  // Bleu ciel
                                )
                            )
                        }
                    )
            )

            // 2. Tsipika mamirapiratra mandeha toy ny Facebook (Shimmer Glowing Beam)
            if (!isSuccessState) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .drawWithContent {
                            drawContent()

                            val shimmerWidth = totalWidthPx * 0.45f
                            val startX = (totalWidthPx * shimmerOffset) - (shimmerWidth / 2f)
                            val endX = startX + shimmerWidth

                            // Shimmer brush mamirapiratra toy ny laser/facebook loading
                            val shimmerBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x3338BDF8),
                                    Color.White.copy(alpha = 0.85f * pulseAlpha),
                                    Color(0xFF67E8F9).copy(alpha = 0.90f),
                                    Color.White.copy(alpha = 0.85f * pulseAlpha),
                                    Color(0x3338BDF8),
                                    Color.Transparent
                                ),
                                start = Offset(startX, 0f),
                                end = Offset(endX, 0f)
                            )

                            drawRect(
                                brush = shimmerBrush,
                                size = this.size
                            )
                        }
                )
            }
        }
    }
}
