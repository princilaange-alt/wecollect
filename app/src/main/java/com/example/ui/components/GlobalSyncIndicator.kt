package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GlobalSyncState

/**
 * Famantarana ny satan'ny fampitoviana angona (Global Synchronization Indicator) :
 * - Synchronisé : Maitso (CloudDone)
 * - Synchronisation en cours : Manga miaraka amin'ny animation mihodina (Sync)
 * - Hors ligne : Volondavenona / Amber (CloudOff)
 * - Erreur : Mena (ErrorOutline)
 */
@Composable
fun GlobalSyncIndicator(
    state: GlobalSyncState,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    showText: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    // Rotation animation rehefa Synchronisation en cours
    val infiniteTransition = rememberInfiniteTransition(label = "syncRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncSpin"
    )

    val (accentColor, containerColor, borderColor, icon) = when (state) {
        GlobalSyncState.SYNCHRONISE -> SyncQuadruple(
            Color(0xFF16A34A),
            Color(0xFFDCFCE7),
            Color(0xFF86EFAC),
            Icons.Default.CloudDone
        )
        GlobalSyncState.EN_COURS -> SyncQuadruple(
            Color(0xFF2563EB),
            Color(0xFFDBEAFE),
            Color(0xFF93C5FD),
            Icons.Default.Sync
        )
        GlobalSyncState.HORS_LIGNE -> SyncQuadruple(
            Color(0xFF64748B),
            Color(0xFFF1F5F9),
            Color(0xFFCBD5E1),
            Icons.Default.CloudOff
        )
        GlobalSyncState.ERREUR -> SyncQuadruple(
            Color(0xFFDC2626),
            Color(0xFFFEE2E2),
            Color(0xFFFCA5A5),
            Icons.Default.ErrorOutline
        )
    }

    Surface(
        modifier = modifier
            .testTag("global_sync_indicator_${state.code.lowercase()}")
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onClick() }
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isCompact) 8.dp else 10.dp,
                vertical = if (isCompact) 4.dp else 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Icon miaraka amin'ny animation raha EN_COURS
            Icon(
                imageVector = icon,
                contentDescription = state.label,
                tint = accentColor,
                modifier = Modifier
                    .size(if (isCompact) 14.dp else 16.dp)
                    .then(
                        if (state == GlobalSyncState.EN_COURS) Modifier.rotate(rotation) else Modifier
                    )
            )

            if (showText) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = state.label,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    letterSpacing = (-0.1).sp
                )
            }
        }
    }
}

private data class SyncQuadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
