package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.SyncedAuditRecord
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.SyncVisualState
import com.example.ui.components.pressScaleEffect
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess

/**
 * Barre de recherche capsule moderne et animée avec compteur de résultats
 */
@Composable
fun AnimatedSurveySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    matchCount: Int,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val infiniteTransition = rememberInfiniteTransition(label = "search_pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isFocused || query.isNotBlank()) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 450f),
        label = "search_icon_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused || query.isNotBlank()) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(220),
        label = "search_border_color"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused || query.isNotBlank()) 1.5.dp else 1.dp,
        animationSpec = tween(220),
        label = "search_border_width"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFocused) 3.dp else 1.dp,
        shadowElevation = if (isFocused) 4.dp else 1.dp,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(38.dp)
            ) {
                if (isFocused || query.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .scale(pulseScale)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = if (query.isNotBlank() || isFocused) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Rechercher",
                        tint = if (isFocused || query.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .size(18.dp)
                            .scale(iconScale)
                    )
                }
            }

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Rechercher code, nom, région ou date...",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused }
                    .testTag("search_enquete_field")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (matchCount > 0) {
                            StatusSuccess.copy(alpha = 0.14f)
                        } else {
                            StatusError.copy(alpha = 0.14f)
                        }
                    ) {
                        Text(
                            text = if (matchCount > 1) "$matchCount fiches" else "$matchCount fiche",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (matchCount > 0) StatusSuccess else StatusError,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .pressScaleEffect(0.85f)
                            .testTag("clear_search_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Effacer la recherche",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sélecteur horizontal moderne par Questionnaire
 */
@Composable
fun QuestionnaireFilterSelectorRow(
    questionnaires: List<QuestionnaireEntity>,
    selectedQuestionnaireId: Long?,
    totalCount: Int,
    countsByQuestionnaire: Map<Long, Int>,
    onSelectQuestionnaire: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Questionnaires",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (selectedQuestionnaireId != null) {
                Surface(
                    onClick = { onSelectQuestionnaire(null) },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Voir tout",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp)
        ) {
            item {
                val isSelected = selectedQuestionnaireId == null
                Surface(
                    onClick = { onSelectQuestionnaire(null) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier.pressScaleEffect(0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tous",
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = "$totalCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            items(questionnaires, key = { it.idQuestionnaire }) { q ->
                val isSelected = selectedQuestionnaireId == q.idQuestionnaire
                val count = countsByQuestionnaire[q.idQuestionnaire] ?: 0

                Surface(
                    onClick = {
                        onSelectQuestionnaire(if (isSelected) null else q.idQuestionnaire)
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier.pressScaleEffect(0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = q.titre,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            }
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * En-tête structurée de section par Questionnaire avec résumé et action rapide
 */
@Composable
fun QuestionnaireSectionHeader(
    questionnaireTitle: String,
    questionnaireId: Long,
    responseCount: Int,
    syncedCount: Int,
    pendingCount: Int,
    onFillNewForThisQuestionnaire: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryTheme = resolveQuestionnaireCategoryIcon(questionnaireTitle)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(categoryTheme.second.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryTheme.first,
                    contentDescription = null,
                    tint = categoryTheme.second,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = questionnaireTitle,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$responseCount ${if (responseCount > 1) "fiches" else "fiche"}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (syncedCount > 0) {
                        Text(
                            text = "• $syncedCount synchro",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusSuccess
                        )
                    }
                    if (pendingCount > 0) {
                        Text(
                            text = "• $pendingCount local",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusPending
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onFillNewForThisQuestionnaire,
                shape = RoundedCornerShape(11.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .pressScaleEffect(0.93f)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Remplir",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Remplir",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun resolveQuestionnaireCategoryIcon(title: String): Pair<ImageVector, Color> {
    val lower = title.lowercase()
    return when {
        lower.contains("agri") || lower.contains("cultur") || lower.contains("prod") ->
            Icons.Default.Eco to Color(0xFF16A34A)
        lower.contains("demo") || lower.contains("popul") || lower.contains("menag") || lower.contains("loge") ->
            Icons.Default.Groups to Color(0xFF7C3AED)
        lower.contains("educ") || lower.contains("ecol") || lower.contains("forma") ->
            Icons.Default.AutoStories to Color(0xFFD97706)
        lower.contains("eau") || lower.contains("sant") || lower.contains("assain") ->
            Icons.Default.WaterDrop to Color(0xFF0284C7)
        else ->
            Icons.AutoMirrored.Filled.Assignment to Color(0xFF0F3E76)
    }
}

/**
 * Carte d'enquête terrain professionnelle avec données réelles (Répondant, Code, Lieu, Date, État Sync)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnqueteItemCard(
    enquete: SyncedAuditRecord,
    indexOnPage: Int = 0,
    isHighlighted: Boolean = false,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    isSyncing: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onViewDetail: () -> Unit,
    onSync: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val syncVisual = SyncVisualState.fromCode(enquete.syncStatus)
    val displayDate = formatSurveyDate(enquete.dateEnquete)

    val cleanCode = remember(enquete.codeUnique, enquete.idEnquete) {
        enquete.codeUnique
            .replace(Regex("(?i)^Fiche\\s*N°?\\s*"), "")
            .replace(Regex("(?i)^Fiche\\s*"), "")
            .trim()
            .ifBlank { "ENQ-${enquete.idEnquete}" }
    }

    val cleanRespondentName = remember(enquete.nomComplet, cleanCode, enquete.codeUnique) {
        val raw = (enquete.nomComplet ?: "")
            .replace(cleanCode, "", ignoreCase = true)
            .replace(enquete.codeUnique, "", ignoreCase = true)
            .replace(Regex("(?i)^Fiche\\s*N°?\\s*"), "")
            .replace(Regex("(?i)^Fiche\\s*"), "")
            .replace(Regex("(?i)ENQ-\\d+-\\d+"), "")
            .replace(Regex("(?i)ENQ-\\d+"), "")
            .trim()
        if (raw.isNotBlank() &&
            !raw.equals("personne enquêtée", ignoreCase = true) &&
            !raw.equals("respondant", ignoreCase = true) &&
            !raw.startsWith("ENQ-", ignoreCase = true)
        ) raw else null
    }

    val primaryTitle = cleanRespondentName ?: cleanCode
    val initials = remember(primaryTitle, cleanRespondentName) {
        if (cleanRespondentName != null) {
            cleanRespondentName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString("")
                .ifBlank { "EQ" }
        } else null
    }

    val locationLabel = remember(enquete.commune, enquete.region) {
        listOfNotNull(
            enquete.commune?.takeIf { it.isNotBlank() },
            enquete.region?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
    }

    val isCompleted = enquete.statut.equals("TERMINE", ignoreCase = true) ||
        enquete.statut.equals("TERMINEE", ignoreCase = true) ||
        enquete.statut.equals("VALIDE", ignoreCase = true) ||
        enquete.statut.equals("VALIDEE", ignoreCase = true)

    val cardBg by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            isHighlighted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(180),
        label = "card_bg"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(180),
        label = "card_border"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "card_sync_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScaleEffect(0.985f)
            .testTag("enquete_item_card_${enquete.idEnquete}"),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        shadowElevation = if (isSelected) 4.dp else 1.5.dp,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(18.dp))
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelect() else onViewDetail()
                    },
                    onLongClick = onLongClick
                )
        ) {
            // Left Vertical Sync Accent Strip
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(syncVisual.accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
            ) {
                // Top Row: Avatar/Checkbox + Title/Code + Sync Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(
                                    2.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sélectionné",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(syncVisual.accentColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (initials != null) {
                                Text(
                                    text = initials,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = syncVisual.accentColor
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = syncVisual.accentColor,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = primaryTitle,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = cleanCode,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCompleted) {
                                    StatusSuccess.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                }
                            ) {
                                Text(
                                    text = if (isCompleted) "Terminée" else "En cours",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) StatusSuccess else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SyncStatusBadge(
                        status = enquete.syncStatus,
                        isSyncing = isSyncing,
                        isCompact = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Row: Date + Geographic Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = displayDate,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (locationLabel.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.5.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = locationLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    thickness = 0.8.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Card Footer: Questionnaire tag + Quick Action Cue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = enquete.questionnaireTitre?.takeIf { it.isNotBlank() } ?: "Fiche #${enquete.idEnquete}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (enquete.syncStatus != "SYNCHRONISEE" && !isSelectionMode) {
                            Surface(
                                onClick = onSync,
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier.pressScaleEffect(0.92f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Synchroniser",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .then(if (isSyncing) Modifier.rotate(spinAngle) else Modifier)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sync",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Détails",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Voir les détails",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * État vide soigné (soit aucune fiche enregistrée, soit aucun résultat pour le filtre actif)
 */
@Composable
fun SurveyEmptyStateCard(
    isFilteredEmpty: Boolean,
    questionnaireTitle: String? = null,
    onResetFilters: () -> Unit = {},
    onStartNewSurvey: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFilteredEmpty && questionnaireTitle == null) {
                Image(
                    painter = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                    contentDescription = "Enquêteur menant une enquête sur le terrain",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                )
            } else {
                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFilteredEmpty) Icons.Default.FilterListOff else Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isFilteredEmpty -> "Aucun résultat trouvé"
                        questionnaireTitle != null -> "Aucune réponse pour ce questionnaire"
                        else -> "Aucune fiche d'enquête enregistrée"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        isFilteredEmpty -> "Aucune fiche ne correspond à votre recherche ou au filtre de synchronisation actif."
                        questionnaireTitle != null -> "Aucune fiche n'a encore été enregistrée pour « $questionnaireTitle »."
                        else -> "Prêt pour la collecte terrain ? Démarrez une nouvelle enquête pour enregistrer votre première fiche."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (isFilteredEmpty) {
                    OutlinedButton(
                        onClick = onResetFilters,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.pressScaleEffect(0.95f)
                    ) {
                        Icon(Icons.Default.FilterListOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Réinitialiser les filtres", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStartNewSurvey,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.pressScaleEffect(0.95f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (questionnaireTitle != null) "Remplir ce questionnaire" else "Nouvelle enquête",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pagination intelligente et adaptative avec fenêtre dynamique autour de la page courante
 */
@Composable
fun SurveyPaginationSection(
    startIndex: Int,
    endIndex: Int,
    totalCount: Int,
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Affichage de $startIndex à $endIndex sur $totalCount fiches",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            val pagesToShow = remember(currentPage, totalPages) {
                buildPaginationTokens(currentPage, totalPages)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaginationSquareButton(
                    onClick = { if (currentPage > 1) onPageSelected(currentPage - 1) },
                    enabled = currentPage > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Précédent",
                        tint = if (currentPage > 1) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                pagesToShow.forEach { pageToken ->
                    if (pageToken == null) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "…",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        PaginationSquareNumberButton(
                            numberText = "$pageToken",
                            isActive = currentPage == pageToken,
                            onClick = { onPageSelected(pageToken) }
                        )
                    }
                }

                PaginationSquareButton(
                    onClick = { if (currentPage < totalPages) onPageSelected(currentPage + 1) },
                    enabled = currentPage < totalPages
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Suivant",
                        tint = if (currentPage < totalPages) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun buildPaginationTokens(currentPage: Int, totalPages: Int): List<Int?> {
    if (totalPages <= 5) {
        return (1..totalPages).toList()
    }
    val tokens = mutableListOf<Int?>()
    tokens.add(1)
    if (currentPage > 3) {
        tokens.add(null)
    }
    val start = maxOf(2, currentPage - 1)
    val end = minOf(totalPages - 1, currentPage + 1)
    for (p in start..end) {
        tokens.add(p)
    }
    if (currentPage < totalPages - 2) {
        tokens.add(null)
    }
    tokens.add(totalPages)
    return tokens
}

@Composable
fun PaginationSquareButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun PaginationSquareNumberButton(
    numberText: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = numberText,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun formatSurveyDate(rawDate: String): String {
    if (rawDate.isBlank()) return "—"
    return try {
        if (rawDate.contains("-")) {
            val parts = rawDate.split("-")
            if (parts.size >= 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2].take(2)
                "$day/$month/$year"
            } else rawDate
        } else rawDate
    } catch (_: Exception) {
        rawDate
    }
}
