package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SyncVisualState(
    val code: String,
    val title: String,
    val fullTitle: String,
    val description: String,
    val primaryIcon: ImageVector,
    val secondaryIcon: ImageVector,
    val accentColor: Color,
    val containerColor: Color,
    val borderColor: Color
) {
    PENDING(
        code = "EN_ATTENTE",
        title = "Local uniquement",
        fullTitle = "Stockée en local",
        description = "Stockée hors-ligne dans SQLite Room",
        primaryIcon = Icons.Default.CloudQueue,
        secondaryIcon = Icons.Default.Schedule,
        accentColor = Color(0xFFD97706), // Amber-700
        containerColor = Color(0xFFFEF3C7), // Amber-100
        borderColor = Color(0xFFFCD34D) // Amber-300
    ),
    SYNCED(
        code = "SYNCHRONISEE",
        title = "Synchronisée",
        fullTitle = "Synchronisée",
        description = "Transmise avec succès",
        primaryIcon = Icons.Default.CloudDone,
        secondaryIcon = Icons.Default.CheckCircle,
        accentColor = Color(0xFF16A34A), // Emerald-700
        containerColor = Color.Transparent, // No background for synchronisée
        borderColor = Color.Transparent // No border for synchronisée
    ),
    ERROR(
        code = "ERREUR",
        title = "Erreur",
        fullTitle = "Échec sync",
        description = "Échec réseau - stockée en local",
        primaryIcon = Icons.Default.CloudOff,
        secondaryIcon = Icons.Default.ErrorOutline,
        accentColor = Color(0xFFDC2626), // Red-600
        containerColor = Color(0xFFFEE2E2), // Red-100
        borderColor = Color(0xFFFCA5A5) // Red-300
    );

    companion object {
        fun fromCode(code: String): SyncVisualState {
            return when (code.trim().uppercase()) {
                "SYNCHRONISEE" -> SYNCED
                "ERREUR" -> ERROR
                else -> PENDING
            }
        }
    }
}

/**
 * Pastille indicative claire et élégante d'état de synchronisation avec icône de nuage et pastille de couleur
 */
@Composable
fun SyncStatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    isSyncing: Boolean = false,
    isCompact: Boolean = false,
    showSubtext: Boolean = false
) {
    val visualState = SyncVisualState.fromCode(status)

    // Pulsing animation for pending state
    val infiniteTransition = rememberInfiniteTransition(label = "badge_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    val isActuallySyncing = isSyncing && visualState != SyncVisualState.SYNCED
    val isSynced = visualState == SyncVisualState.SYNCED

    val accentColor = if (isActuallySyncing) Color(0xFF2563EB) else visualState.accentColor
    val containerColor = if (isActuallySyncing) Color(0xFFEFF6FF) else (if (isSynced) Color.Transparent else visualState.containerColor)
    val borderColor = if (isActuallySyncing) Color(0xFF93C5FD) else (if (isSynced) Color.Transparent else visualState.borderColor)
    val displayIcon = if (isActuallySyncing) Icons.Default.Sync else visualState.primaryIcon
    val displayText = if (isActuallySyncing) {
        if (isCompact) "En cours" else "Synchronisation..."
    } else {
        if (isCompact) visualState.title else visualState.fullTitle
    }

    Surface(
        modifier = modifier
            .testTag("sync_status_badge_${if (isActuallySyncing) "syncing" else visualState.code.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = if (isSynced && !isActuallySyncing) null else BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isSynced && !isActuallySyncing) 2.dp else (if (isCompact) 8.dp else 10.dp),
                vertical = if (isSynced && !isActuallySyncing) 2.dp else (if (isCompact) 3.5.dp else 5.dp)
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pastille ronde de couleur vive
            Box(
                modifier = Modifier
                    .size(if (isCompact) 6.dp else 7.dp)
                    .alpha(if (visualState == SyncVisualState.PENDING || isActuallySyncing) pulseAlpha else 1f)
                    .background(accentColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))

            // Icône
            Icon(
                imageVector = displayIcon,
                contentDescription = displayText,
                tint = accentColor,
                modifier = Modifier
                    .size(if (isCompact) 13.dp else 15.dp)
                    .then(if (isActuallySyncing) Modifier.rotate(spinAngle) else Modifier)
            )

            Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 5.dp))

            Text(
                text = displayText,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCompact) 11.sp else 12.sp,
                lineHeight = if (isCompact) 13.sp else 15.sp
            )
        }
    }
}

/**
 * Barre latérale colorée pour repérer immédiatement l'état sur une carte
 */
@Composable
fun SyncStatusAccentStrip(
    status: String,
    modifier: Modifier = Modifier
) {
    val visualState = SyncVisualState.fromCode(status)
    Box(
        modifier = modifier
            .width(5.dp)
            .background(
                color = visualState.accentColor,
                shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
            )
    )
}

/**
 * Bannière explicative récapitulative des états de synchronisation
 */
@Composable
fun SyncStatusLegendRow(
    pendingCount: Int,
    syncedCount: Int,
    errorCount: Int,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "États de Synchronisation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Filtrer la liste",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // En attente
            SyncLegendChip(
                state = SyncVisualState.PENDING,
                count = pendingCount,
                isSelected = selectedFilter == "EN_ATTENTE",
                onClick = {
                    onFilterSelected(if (selectedFilter == "EN_ATTENTE") "TOUS" else "EN_ATTENTE")
                },
                modifier = Modifier.weight(1f)
            )

            // Synchronisé
            SyncLegendChip(
                state = SyncVisualState.SYNCED,
                count = syncedCount,
                isSelected = selectedFilter == "SYNCHRONISEE",
                onClick = {
                    onFilterSelected(if (selectedFilter == "SYNCHRONISEE") "TOUS" else "SYNCHRONISEE")
                },
                modifier = Modifier.weight(1f)
            )

            // Erreur
            SyncLegendChip(
                state = SyncVisualState.ERROR,
                count = errorCount,
                isSelected = selectedFilter == "ERREUR",
                onClick = {
                    onFilterSelected(if (selectedFilter == "ERREUR") "TOUS" else "ERREUR")
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SyncLegendChip(
    state: SyncVisualState,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) state.accentColor.copy(alpha = 0.12f) else Color.Transparent

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        modifier = modifier
            .pressScaleEffect(0.92f)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("legend_chip_${state.code.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.primaryIcon,
                    contentDescription = null,
                    tint = state.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$count",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = state.accentColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) state.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
