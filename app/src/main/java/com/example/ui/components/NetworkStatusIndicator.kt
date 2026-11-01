package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Famantarana sy animation ho an'ny tambajotra (Network Status Indicator):
 * - Endriny tsotra tahaka ny teo aloha : TSISY background, TSISY border.
 * - Rehefa en ligne : teboka maitso misy animation onja (pulse wave) sy soratra "En ligne".
 * - Rehefa hors-ligne : teboka mena tsotra sy soratra "Hors-ligne".
 */
@Composable
fun OnlineStatusBadge(
    isConnectedAndOnline: Boolean,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (isConnectedAndOnline) {
            // Animation onja malefaka ho an'ny teboka maitso rehefa En ligne
            val infiniteTransition = rememberInfiniteTransition(label = "onlinePulseAnim")
            val rippleScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 2.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleScale"
            )
            val rippleAlpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rippleAlpha"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(12.dp)
            ) {
                // Onja mivoaka (Ripple wave)
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(rippleScale)
                        .alpha(rippleAlpha)
                        .background(Color(0xFF34D399), CircleShape)
                )
                // Teboka afovoany (Core dot)
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color(0xFF34D399), CircleShape)
                )
            }
        } else {
            // Teboka mena tsotra rehefa Hors-ligne (tsy misy animation)
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Color(0xFFF87171), CircleShape)
            )
        }

        if (showText) {
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isConnectedAndOnline) "En ligne" else "Hors-ligne",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isConnectedAndOnline) Color(0xFF34D399) else Color(0xFFFCA5A5)
            )
        }
    }
}

/**
 * Famantarana feno mampiasa ny GlobalSyncState (Synchronisé / Synchronisation en cours / Hors ligne / Erreur)
 */
@Composable
fun OnlineStatusBadge(
    syncState: com.example.data.model.GlobalSyncState,
    modifier: Modifier = Modifier,
    showText: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    GlobalSyncIndicator(
        state = syncState,
        modifier = modifier,
        isCompact = true,
        showText = showText,
        onClick = onClick
    )
}
