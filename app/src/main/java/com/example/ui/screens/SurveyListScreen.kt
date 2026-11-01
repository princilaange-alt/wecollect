package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.FloatingActionButton
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.EnqueteDetail
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.SyncedAuditRecord
import com.example.data.util.MediaUtils
import com.example.ui.SurveyViewModel
import com.example.ui.components.FacebookPullRefreshContainer
import com.example.ui.components.FacebookShimmerCrossfade
import com.example.ui.components.FacebookSurveyListSkeleton
import com.example.ui.components.OnlineStatusBadge
import com.example.ui.components.SyncStatusBadge
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.components.SyncStatusLegendRow
import com.example.ui.components.SyncVisualState
import com.example.ui.components.TopSyncTransferAnimation
import com.example.ui.theme.StatusPending
import com.example.ui.components.pressScaleEffect

@Composable
fun SurveyListScreen(
    viewModel: SurveyViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val enquetes by viewModel.filteredEnquetes.collectAsState()
    val allEnquetes by viewModel.allEnquetes.collectAsState()
    val allEnquetesAudit by viewModel.allEnquetesAudit.collectAsState()
    val allQuestionnaires by viewModel.allQuestionnaires.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val selectedDetail by viewModel.selectedEnqueteDetail.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isPullingForm by viewModel.isPullingForm.collectAsState()
    val isRefreshingData by viewModel.isRefreshingData.collectAsState()
    val isConnectedAndOnline by viewModel.isConnectedAndOnline.collectAsState()
    val globalSyncState by viewModel.globalSyncState.collectAsState()

    val isCurrentlyLoading = isRefreshingData || isPullingForm || isSyncing

    // Filter by Questionnaire state ("Samy atao mitokana ny reponse par questionnaire")
    var selectedQuestionnaireId by remember { mutableStateOf<Long?>(null) }

    // Consolidated list of questionnaires
    val questionnairesList = remember(allQuestionnaires, allEnquetesAudit) {
        val qMap = LinkedHashMap<Long, QuestionnaireEntity>()
        allQuestionnaires.forEach { q ->
            qMap[q.idQuestionnaire] = q
        }
        allEnquetesAudit.forEach { e ->
            if (!qMap.containsKey(e.idQuestionnaire)) {
                qMap[e.idQuestionnaire] = QuestionnaireEntity(
                    idQuestionnaire = e.idQuestionnaire,
                    titre = e.questionnaireTitre?.takeIf { it.isNotBlank() } ?: "Questionnaire #${e.idQuestionnaire}"
                )
            }
        }
        qMap.values.toList()
    }

    // Counts per questionnaire based on current enquetes list
    val enquetesByQuestionnaireCount = remember(enquetes) {
        enquetes.groupingBy { it.idQuestionnaire }.eachCount()
    }

    // Filter by questionnaire
    val questionnaireFilteredEnquetes = remember(enquetes, selectedQuestionnaireId) {
        if (selectedQuestionnaireId == null) {
            enquetes
        } else {
            enquetes.filter { it.idQuestionnaire == selectedQuestionnaireId }
        }
    }
    
    // Multi-selection state (like Messaging / WhatsApp / Gmail)
    var selectedEnqueteIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedEnqueteIds.isNotEmpty()
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var enqueteToDelete by remember { mutableStateOf<SyncedAuditRecord?>(null) }

    // Pagination state
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 10
    val totalCount = questionnaireFilteredEnquetes.size
    val totalPages = maxOf(1, (totalCount + pageSize - 1) / pageSize)

    LaunchedEffect(totalCount, searchQuery, statusFilter, selectedQuestionnaireId) {
        if (currentPage > totalPages) {
            currentPage = 1
        }
    }

    val startIndex = if (totalCount == 0) 0 else (currentPage - 1) * pageSize + 1
    val endIndex = minOf(currentPage * pageSize, totalCount)
    val paginatedEnquetes = remember(questionnaireFilteredEnquetes, currentPage) {
        questionnaireFilteredEnquetes.drop((currentPage - 1) * pageSize).take(pageSize)
    }

    // Group the paginated results by questionnaire for distinct sections
    val groupedEnquetes = remember(paginatedEnquetes) {
        paginatedEnquetes.groupBy { it.idQuestionnaire }
    }

    val pendingCount = allEnquetes.count { it.syncStatus == "EN_ATTENTE" }
    val syncedCount = allEnquetes.count { it.syncStatus == "SYNCHRONISEE" }
    val errorCount = allEnquetes.count { it.syncStatus == "ERREUR" }

    BackHandler(enabled = selectedQuestionnaireId != null || isSelectionMode || selectedDetail != null || enqueteToDelete != null || showBulkDeleteDialog) {
        when {
            selectedDetail != null -> viewModel.closeEnqueteDetail()
            isSelectionMode -> selectedEnqueteIds = emptySet()
            selectedQuestionnaireId != null -> selectedQuestionnaireId = null
            enqueteToDelete != null -> enqueteToDelete = null
            showBulkDeleteDialog -> showBulkDeleteDialog = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
        // TOP APP BAR: Standard mode vs Selection mode
        Surface(
            color = if (isSelectionMode) Color(0xFF0A2E5C) else Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    // Close selection mode
                    IconButton(
                        onClick = { selectedEnqueteIds = emptySet() },
                        modifier = Modifier
                            .pressScaleEffect(0.85f)
                            .testTag("close_selection_mode")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer la sélection",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${selectedEnqueteIds.size} sélectionnée(s)",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )

                    // Select All / Deselect All Button
                    IconButton(
                        onClick = {
                            val allIds = enquetes.map { it.idEnquete }.toSet()
                            selectedEnqueteIds = if (selectedEnqueteIds.containsAll(allIds)) {
                                emptySet()
                            } else {
                                allIds
                            }
                        },
                        modifier = Modifier.pressScaleEffect(0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "Tout sélectionner",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Delete Selected Button
                    IconButton(
                        onClick = { showBulkDeleteDialog = true },
                        modifier = Modifier
                            .pressScaleEffect(0.85f)
                            .testTag("bulk_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer les éléments sélectionnés",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    // Normal Header
                    // Menu Drawer Button on Left
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .pressScaleEffect(0.85f)
                            .testTag("survey_list_drawer_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu navigation",
                            tint = Color(0xFF0F2942),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Mes enquêtes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F2942),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )

                    // Indicateur d'état réseau en haut à droite (En ligne / Hors-ligne)
                    OnlineStatusBadge(
                        isConnectedAndOnline = isConnectedAndOnline,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
        }

        // Top animated sync & transfer banner
        TopSyncTransferAnimation(
            isSyncing = isSyncing,
            isPullingForm = isPullingForm
        )

        FacebookPullRefreshContainer(
            isRefreshing = isCurrentlyLoading,
            onRefresh = { viewModel.refreshAllData() },
            modifier = Modifier.fillMaxSize()
        ) {
            FacebookShimmerCrossfade(
                isLoading = isCurrentlyLoading,
                skeletonContent = { FacebookSurveyListSkeleton() },
                actualContent = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Animated Search Bar
                AnimatedSurveySearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    matchCount = questionnaireFilteredEnquetes.size,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Indicative Sync Status Legend & Filter Row
            item {
                SyncStatusLegendRow(
                    pendingCount = pendingCount,
                    syncedCount = syncedCount,
                    errorCount = errorCount,
                    selectedFilter = statusFilter,
                    onFilterSelected = { viewModel.setStatusFilter(it) }
                )
            }

            // Questionnaire Filter Chips Row (Samy atao mitokana ny reponse par questionnaire)
            if (questionnairesList.isNotEmpty()) {
                item(key = "questionnaire_selector_chips") {
                    QuestionnaireFilterSelectorRow(
                        questionnaires = questionnairesList,
                        selectedQuestionnaireId = selectedQuestionnaireId,
                        totalCount = enquetes.size,
                        countsByQuestionnaire = enquetesByQuestionnaireCount,
                        onSelectQuestionnaire = { qId ->
                            selectedQuestionnaireId = qId
                            currentPage = 1
                        }
                    )
                }
            }

            // Empty state: either globally empty or empty for selected questionnaire
            if (enquetes.isEmpty()) {
                item(key = "empty_global") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                contentDescription = "Enquêteur menant une enquête sur le terrain",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aucune fiche d'enquête enregistrée",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Prêt pour la collecte ? Choisissez un questionnaire ci-dessus pour démarrer vos enquêtes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else if (questionnaireFilteredEnquetes.isEmpty()) {
                // Filtered by a specific questionnaire that has 0 responses
                item(key = "empty_questionnaire_filter") {
                    val selQ = questionnairesList.find { it.idQuestionnaire == selectedQuestionnaireId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Aucune réponse pour ce questionnaire",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Aucune fiche n'a encore été enregistrée pour « ${selQ?.titre ?: "ce questionnaire"} ».",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (selectedQuestionnaireId != null) {
                                        viewModel.selectQuestionnaire(selectedQuestionnaireId!!)
                                    }
                                    viewModel.startNewSurvey()
                                    viewModel.setTab(2)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remplir ce questionnaire")
                            }
                        }
                    }
                }
            } else {
                // Display responses clearly grouped and separated per questionnaire
                groupedEnquetes.forEach { (qId, enquetesInGroup) ->
                    // Distinct Section Header for this Questionnaire
                    item(key = "q_section_header_$qId") {
                        val qTitle = enquetesInGroup.firstOrNull()?.questionnaireTitre
                            ?: questionnairesList.find { it.idQuestionnaire == qId }?.titre
                            ?: "Questionnaire #$qId"
                        val qSyncedCount = enquetesInGroup.count { it.syncStatus == "SYNCHRONISEE" }
                        val qPendingCount = enquetesInGroup.count { it.syncStatus == "EN_ATTENTE" }

                        QuestionnaireSectionHeader(
                            questionnaireTitle = qTitle,
                            questionnaireId = qId,
                            responseCount = enquetesByQuestionnaireCount[qId] ?: enquetesInGroup.size,
                            syncedCount = qSyncedCount,
                            pendingCount = qPendingCount,
                            onFillNewForThisQuestionnaire = {
                                viewModel.selectQuestionnaire(qId)
                                viewModel.startNewSurvey()
                                viewModel.setTab(2)
                            }
                        )
                    }

                    // Individual survey cards under this questionnaire section
                    itemsIndexed(
                        items = enquetesInGroup,
                        key = { _, enquete -> enquete.idEnquete }
                    ) { index, enquete ->
                        val isSelected = selectedEnqueteIds.contains(enquete.idEnquete)

                        EnqueteItemCard(
                            enquete = enquete,
                            indexOnPage = index,
                            isHighlighted = false,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            isSyncing = isSyncing,
                            onToggleSelect = {
                                selectedEnqueteIds = if (isSelected) {
                                    selectedEnqueteIds - enquete.idEnquete
                                } else {
                                    selectedEnqueteIds + enquete.idEnquete
                                }
                            },
                            onLongClick = {
                                selectedEnqueteIds = selectedEnqueteIds + enquete.idEnquete
                            },
                            onViewDetail = { viewModel.viewEnqueteDetail(enquete.idEnquete) },
                            onSync = { viewModel.syncEnqueteById(enquete.idEnquete) },
                            onDelete = { enqueteToDelete = enquete }
                        )
                    }
                }

                // Pagination footer - automatically displayed ONLY if there are more than 10 items
                if (totalCount > 10) {
                    item(key = "pagination_footer") {
                        SurveyPaginationSection(
                            startIndex = startIndex,
                            endIndex = endIndex,
                            totalCount = totalCount,
                            currentPage = currentPage,
                            totalPages = totalPages,
                            onPageSelected = { page -> currentPage = page }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
        )
    }
    }
    }

    // Single Deletion Confirmation Dialog
    enqueteToDelete?.let { targetEnquete ->
        AlertDialog(
            onDismissRequest = { enqueteToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Supprimer cette fiche ?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Code : ${targetEnquete.codeUnique.replace(Regex("(?i)^Fiche\\s*N°?\\s*"), "").trim().ifBlank { targetEnquete.codeUnique }}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cette fiche est stockée en toute sécurité dans l'appareil. Elle ne s'efface jamais automatiquement (que vous soyez hors-ligne ou en ligne). Seule votre décision manuelle peut la supprimer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Confirmez-vous la suppression définitive ?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEnquete(targetEnquete.idEnquete)
                        enqueteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Oui, supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { enqueteToDelete = null }) {
                    Text("X Annuler")
                }
            }
        )
    }

    // Bulk Multi-Deletion Confirmation Dialog
    if (showBulkDeleteDialog && selectedEnqueteIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = StatusError,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Supprimer les ${selectedEnqueteIds.size} fiches ?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "Voulez-vous vraiment supprimer définitivement les ${selectedEnqueteIds.size} fiches sélectionnées de la mémoire locale ?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cette action est irréversible.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusError
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMultipleEnquetes(selectedEnqueteIds)
                        selectedEnqueteIds = emptySet()
                        showBulkDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Oui, supprimer (${selectedEnqueteIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("X Annuler")
                }
            }
        )
    }

    // Detail Modal Dialog
    selectedDetail?.let { detail ->
        EnqueteDetailDialog(
            detail = detail,
            payloadPreview = viewModel.syncService.getPayloadPreview(detail),
            onDismiss = { viewModel.closeEnqueteDetail() },
            onSync = {
                viewModel.syncEnqueteById(detail.enquete.idEnquete)
                viewModel.closeEnqueteDetail()
            },
            onDelete = {
                viewModel.closeEnqueteDetail()
                enqueteToDelete = allEnquetesAudit.firstOrNull { it.idEnquete == detail.enquete.idEnquete }
                    ?: SyncedAuditRecord(
                        idEnquete = detail.enquete.idEnquete,
                        codeUnique = detail.codeUnique,
                        idQuestionnaire = detail.enquete.idQuestionnaire,
                        idEnqueteur = detail.enquete.idEnqueteur,
                        dateEnquete = detail.enquete.dateEnquete,
                        statut = detail.enquete.statut,
                        syncStatus = detail.enquete.syncStatus,
                        createdAt = detail.enquete.createdAt,
                        updatedAt = detail.enquete.updatedAt
                    )
            }
        )
    }
}

/**
 * AnimatedSurveySearchBar
 * Barre de recherche avec style souligné épuré (underlined), sans bordure de fond,
 * avec ligne indicatrice animée, icône interactive et badge de résultats.
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

    // Animation radar pulse pour l'icône de recherche quand elle est active ou a du texte
    val infiniteTransition = rememberInfiniteTransition(label = "search_pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Rotation et échelle fluide pour l'icône de recherche
    val iconRotation by animateFloatAsState(
        targetValue = if (query.isNotBlank()) 90f else if (isFocused) 15f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "search_icon_rotation"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isFocused || query.isNotBlank()) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "search_icon_scale"
    )

    val underlineColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isFocused || query.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        animationSpec = tween(250),
        label = "search_underline_color"
    )
    val underlineHeight by animateDpAsState(
        targetValue = if (isFocused || query.isNotBlank()) 2.dp else 1.dp,
        animationSpec = tween(250),
        label = "search_underline_height"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Icône de recherche animée avec halo radar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .padding(2.dp)
            ) {
                if (isFocused || query.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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
                            color = if (query.isNotBlank() || isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Rechercher",
                        tint = if (isFocused || query.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(iconRotation)
                            .scale(iconScale)
                    )
                }
            }

            // Champ de texte souligné sans boîte de fond
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Rechercher par code, nom ou date...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
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

            // Badge de compteur et Bouton effacer
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
                        shape = RoundedCornerShape(8.dp),
                        color = if (matchCount > 0) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (matchCount > 0) "$matchCount" else "0",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (matchCount > 0) Color(0xFF059669) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                            .size(36.dp)
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
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ligne de soulignement (Underline)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(underlineHeight)
                .background(underlineColor, RoundedCornerShape(1.dp))
        )
    }
}

/**
 * Sélecteur horizontal par Questionnaire (Samy atao mitokana ny reponse par questionnaire)
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
            .padding(vertical = 4.dp)
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
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Questionnaires",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
            }
            if (selectedQuestionnaireId != null) {
                Text(
                    text = "Voir tout",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .clickable { onSelectQuestionnaire(null) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp)
        ) {
            // "Tous" chip
            item {
                val isSelected = selectedQuestionnaireId == null
                Surface(
                    onClick = { onSelectQuestionnaire(null) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                    shadowElevation = 0.dp,
                    modifier = Modifier.pressScaleEffect(0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tous",
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "$totalCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Individual Questionnaire chips
            items(questionnaires, key = { it.idQuestionnaire }) { q ->
                val isSelected = selectedQuestionnaireId == q.idQuestionnaire
                val count = countsByQuestionnaire[q.idQuestionnaire] ?: 0

                Surface(
                    onClick = {
                        onSelectQuestionnaire(if (isSelected) null else q.idQuestionnaire)
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                    shadowElevation = 0.dp,
                    modifier = Modifier.pressScaleEffect(0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = q.titre,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF0284C7),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * En-tête de section par Questionnaire sans fond ni bordure
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sleek vertical accent indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF0284C7))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = questionnaireTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F2942),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$responseCount ${if (responseCount > 1) "réponses" else "réponse"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0284C7)
                )
                if (syncedCount > 0) {
                    Text(
                        text = " • $syncedCount synchro",
                        fontSize = 11.5.sp,
                        color = Color(0xFF16A34A)
                    )
                }
                if (pendingCount > 0) {
                    Text(
                        text = " • $pendingCount en attente",
                        fontSize = 11.5.sp,
                        color = Color(0xFFD97706)
                    )
                }
            }
        }
        // Quick button to start a new survey directly for this questionnaire
        Surface(
            onClick = onFillNewForThisQuestionnaire,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFE0F2FE),
            modifier = Modifier.pressScaleEffect(0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Remplir",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Remplir",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )
            }
        }
    }
}

data class SurveyVisualTheme(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val statusText: String,
    val statusBgColor: Color,
    val statusTextColor: Color
)

fun resolveSurveyVisualTheme(enquete: SyncedAuditRecord, indexOnPage: Int): SurveyVisualTheme {
    val qTitle = enquete.questionnaireTitre?.trim() ?: ""
    val titleLower = (qTitle.ifBlank { enquete.codeUnique }).lowercase()

    val isTerminated = enquete.statut.equals("TERMINE", ignoreCase = true) ||
            enquete.statut.equals("VALIDE", ignoreCase = true) ||
            enquete.syncStatus.equals("SYNCHRONISE", ignoreCase = true)

    val isError = enquete.syncStatus.equals("ERREUR", ignoreCase = true)

    val statusText = when {
        isError -> "Erreur"
        isTerminated -> "Terminée"
        else -> "En cours"
    }

    val (statusBg, statusFg) = when {
        isError -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        isTerminated -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
        else -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
    }

    return when {
        titleLower.contains("agri") || titleLower.contains("cultur") || titleLower.contains("prod") || (indexOnPage % 4 == 0) -> {
            SurveyVisualTheme(
                title = qTitle.ifBlank { "Enquête agricole 2025" },
                subtitle = "Agriculture • Production",
                icon = Icons.Default.Eco,
                iconBgColor = Color(0xFF22C55E),
                statusText = statusText,
                statusBgColor = statusBg,
                statusTextColor = statusFg
            )
        }
        titleLower.contains("demo") || titleLower.contains("popul") || titleLower.contains("menag") || titleLower.contains("loge") || (indexOnPage % 4 == 1) -> {
            SurveyVisualTheme(
                title = qTitle.ifBlank { "Enquête démographique" },
                subtitle = "Population • Logement",
                icon = Icons.Default.Groups,
                iconBgColor = Color(0xFFA855F7),
                statusText = statusText,
                statusBgColor = statusBg,
                statusTextColor = statusFg
            )
        }
        titleLower.contains("educ") || titleLower.contains("ecol") || titleLower.contains("forma") || (indexOnPage % 4 == 2) -> {
            SurveyVisualTheme(
                title = qTitle.ifBlank { "Éducation 2025" },
                subtitle = "Accès à l'éducation",
                icon = Icons.Default.AutoStories,
                iconBgColor = Color(0xFFF59E0B),
                statusText = statusText,
                statusBgColor = statusBg,
                statusTextColor = statusFg
            )
        }
        else -> {
            SurveyVisualTheme(
                title = qTitle.ifBlank { "Eau et assainissement" },
                subtitle = "Santé • Environnement",
                icon = Icons.Default.WaterDrop,
                iconBgColor = Color(0xFF0284C7),
                statusText = statusText,
                statusBgColor = statusBg,
                statusTextColor = statusFg
            )
        }
    }
}

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
    val visual = resolveSurveyVisualTheme(enquete, indexOnPage)
    val displayDate = formatSurveyDate(enquete.dateEnquete)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
            .pressScaleEffect(0.99f)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect() else onViewDetail()
                },
                onLongClick = onLongClick
            )
            .testTag("enquete_item_card_${enquete.idEnquete}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox en mode sélection
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(
                            2.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Sélectionné",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Sleek vertical colored accent indicator (Miavaka tsara tsy misy icon na card border)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(visual.statusTextColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info: Title, Subtitle, and Date
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = visual.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = visual.subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "Créée le $displayDate",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF94A3B8)
                )
            }

            // Right side: Top status pill badge + bottom chevron
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                // Status Pill Badge (En cours / Terminée)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(visual.statusBgColor)
                        .padding(horizontal = 9.dp, vertical = 3.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = visual.statusText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = visual.statusTextColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Voir les détails",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Clean subtle hairline separator between items
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp, end = 4.dp),
            color = Color(0xFFF1F5F9),
            thickness = 1.dp
        )
    }
}

@Composable
fun SurveyPaginationSection(
    startIndex: Int,
    endIndex: Int,
    totalCount: Int,
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp)
    ) {
        Text(
            text = "Affichage de $startIndex à $endIndex sur $totalCount",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(bottom = 12.dp)
        )

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
                    tint = if (currentPage > 1) Color(0xFF334155) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp)
                )
            }

            PaginationSquareNumberButton(
                numberText = "1",
                isActive = currentPage == 1,
                onClick = { onPageSelected(1) }
            )

            if (totalPages >= 2) {
                PaginationSquareNumberButton(
                    numberText = "2",
                    isActive = currentPage == 2,
                    onClick = { onPageSelected(2) }
                )
            }

            if (totalPages >= 3) {
                PaginationSquareNumberButton(
                    numberText = "3",
                    isActive = currentPage == 3,
                    onClick = { onPageSelected(3) }
                )
            }

            if (totalPages > 4) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }

                PaginationSquareNumberButton(
                    numberText = "$totalPages",
                    isActive = currentPage == totalPages,
                    onClick = { onPageSelected(totalPages) }
                )
            }

            PaginationSquareButton(
                onClick = { if (currentPage < totalPages) onPageSelected(currentPage + 1) },
                enabled = currentPage < totalPages
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Suivant",
                    tint = if (currentPage < totalPages) Color(0xFF334155) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
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
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        color = Color.White,
        modifier = Modifier.size(34.dp)
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
        shape = RoundedCornerShape(8.dp),
        border = if (isActive) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        color = if (isActive) Color(0xFF2563EB) else Color.White,
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = numberText,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) Color.White else Color(0xFF334155)
            )
        }
    }
}

fun formatSurveyDate(rawDate: String): String {
    if (rawDate.isBlank()) return "17/09/2026"
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
    } catch (e: Exception) {
        rawDate
    }
}

@Composable
fun EnqueteDetailDialog(
    detail: EnqueteDetail,
    payloadPreview: String = "",
    onDismiss: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Supprimer cette enquête ?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "L'enquête ${detail.codeUnique} sera définitivement supprimée de la base de données locale.",
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Supprimer définitivement", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    val cleanCode = detail.codeUnique
        .replace(Regex("(?i)^Fiche\\s*N°?\\s*"), "")
        .replace(Regex("(?i)^Fiche\\s*"), "")
        .trim()
        .ifBlank { "Enquête #${detail.enquete.idEnquete}" }

    val rawNom = detail.respondentNom
        .replace(cleanCode, "", ignoreCase = true)
        .replace(detail.codeUnique, "", ignoreCase = true)
        .replace(Regex("(?i)^Fiche\\s*N°?\\s*"), "")
        .replace(Regex("(?i)^Fiche\\s*"), "")
        .replace(Regex("(?i)ENQ-\\d+-\\d+"), "")
        .replace(Regex("(?i)ENQ-\\d+"), "")
        .trim()

    val qTitre = detail.questionnaireTitre.ifBlank { "Questionnaire #${detail.enquete.idQuestionnaire}" }
    val dateEnq = detail.enquete.dateEnquete
    val enqueteur = detail.enqueteurNom.ifBlank { "Non renseigné" }
    val isSynced = detail.enquete.syncStatus == "SYNCHRONISEE"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(0.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. Header Row (Titre "Détails de la personne" + "X" close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Détails de la personne",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F2942)
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Profile Header (Avatar + Name + Code)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFDBEAFE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (rawNom.isNotBlank() && rawNom.lowercase() != "personne enquêtée" && rawNom.lowercase() != "respondant" && !rawNom.startsWith("ENQ-", ignoreCase = true)) {
                            Text(
                                text = rawNom,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F2942)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Code unique
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Code unique : ",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = cleanCode,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Scrollable details list (Recorded database responses)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Questionnaire Metadata
                    item {
                        DetailRowItem(
                            icon = Icons.AutoMirrored.Filled.Article,
                            label = "Questionnaire",
                            value = qTitre
                        )
                    }
                    item {
                        DetailRowItem(
                            icon = Icons.Default.CalendarToday,
                            label = "Date de l'enquête",
                            value = dateEnq
                        )
                    }
                    item {
                        DetailRowItem(
                            icon = Icons.Default.PersonOutline,
                            label = "Enquêteur",
                            value = enqueteur
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Section Title: Database Recorded Responses
                    item {
                        Text(
                            text = "Données enregistrées (${detail.reponses.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2942)
                        )
                    }

                    if (detail.reponses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucune réponse enregistrée dans la base pour cette enquête.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        items(detail.reponses.size) { idx ->
                            val rep = detail.reponses[idx]
                            val qText = rep.question.libelle.replace("*", "").trim()
                            val ansText = if (!rep.reponse.valeur.isNullOrBlank()) {
                                rep.reponse.valeur!!
                            } else {
                                rep.option?.libelle ?: "Non renseigné"
                            }
                            val icon = selectIconForQuestion(qText)

                            DetailRowItem(
                                icon = icon,
                                label = qText,
                                value = ansText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer", color = StatusError, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text("Fermer", color = Color(0xFF0F3E76), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    isPill: Boolean = false,
    pillBgColor: Color = Color(0xFFDCFCE7),
    pillTextColor: Color = Color(0xFF16A34A)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            fontSize = 13.5.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (isPill) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = pillBgColor
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pillTextColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 13.5.sp,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

fun selectIconForQuestion(questionLibelle: String): ImageVector {
    val q = questionLibelle.lowercase()
    return when {
        q.contains("cin") || q.contains("carte") || q.contains("identité") -> Icons.Default.Badge
        q.contains("date") || q.contains("naissance") || q.contains("dob") -> Icons.Default.CalendarToday
        q.contains("sexe") || q.contains("genre") -> Icons.Default.Person
        q.contains("téléphone") || q.contains("phone") || q.contains("contact") -> Icons.Default.Phone
        q.contains("adresse") || q.contains("quartier") -> Icons.Default.LocationOn
        q.contains("région") || q.contains("region") -> Icons.Default.Place
        q.contains("district") -> Icons.Default.Business
        q.contains("commune") -> Icons.Default.LocationCity
        q.contains("village") || q.contains("fokontany") -> Icons.Default.Home
        else -> Icons.AutoMirrored.Filled.Article
    }
}

@Composable
private fun QuestionAnswerCard(
    item: com.example.data.model.ReponseWithQuestion
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        // Question Libellé (Clean, professional view without Q01 or question type badges)
        Text(
            text = item.question.libelle.replace("*", "").trim(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

            val qType = item.question.type.uppercase()
            val qLabel = item.question.libelle.lowercase()
            val valStr = item.reponse.valeur ?: item.option?.libelle

            val isImg = (qType in listOf("IMAGE", "PHOTO", "PICTURE") || qType.contains("IMAGE") || qType.contains("PHOTO") || qLabel.contains("photo") || qLabel.contains("sary") || qLabel.contains("image"))
            val isAud = (qType in listOf("AUDIO", "VOCAL", "SON", "ENREGISTREMENT") || qType.contains("AUDIO") || qType.contains("VOCAL") || qType.contains("SON") || qLabel.contains("audio") || qLabel.contains("vocal") || qLabel.contains("enregistrement") || qLabel.contains("feo"))

            if (isImg && !valStr.isNullOrBlank()) {
                val context = LocalContext.current
                var showFullscreenPhoto by remember { mutableStateOf(false) }

                val imgModel: Any = remember(valStr) {
                    when {
                        valStr.startsWith("content://") -> android.net.Uri.parse(valStr)
                        valStr.startsWith("data:") || valStr.startsWith("http://") || valStr.startsWith("https://") -> valStr
                        else -> File(valStr)
                    }
                }

                val isCloud = valStr.startsWith("http://") || valStr.startsWith("https://")
                val isDataUri = valStr.startsWith("data:image/")

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showFullscreenPhoto = true }
                ) {
                    AsyncImage(
                        model = imgModel,
                        contentDescription = "Photo prise sur le terrain",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Zoom overlay pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Agrandir",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = null,
                            tint = Color(0xFF0F3E76),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCloud || isDataUri) "Photo distante" else File(valStr).name,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Synchronisation label removed as requested
                }
            }

                if (showFullscreenPhoto) {
                    Dialog(
                        onDismissRequest = { showFullscreenPhoto = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.95f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = imgModel,
                                    contentDescription = "Photo agrandie",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )

                                IconButton(
                                    onClick = { showFullscreenPhoto = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fermer",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isAud && !valStr.isNullOrBlank()) {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                var isPlaying by remember { mutableStateOf(false) }
                var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
                var playbackPositionMs by remember { mutableIntStateOf(0) }
                var totalDurationMs by remember { mutableIntStateOf(0) }
                var isReady by remember { mutableStateOf(false) }

                LaunchedEffect(valStr) {
                    val file = MediaUtils.resolveAudioFileForPlayback(context, valStr)
                    if (file != null && file.exists()) {
                        try {
                            val mmr = android.media.MediaMetadataRetriever()
                            mmr.setDataSource(file.absolutePath)
                            val durStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            totalDurationMs = durStr?.toIntOrNull() ?: 0
                            mmr.release()
                            isReady = true
                        } catch (_: Exception) {}
                    }
                }

                LaunchedEffect(isPlaying) {
                    if (isPlaying) {
                        while (isPlaying && mediaPlayer != null) {
                            try {
                                mediaPlayer?.let { player ->
                                    if (player.isPlaying) {
                                        playbackPositionMs = player.currentPosition
                                    }
                                }
                            } catch (_: Exception) {}
                            delay(200)
                        }
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        } catch (_: Exception) {}
                        mediaPlayer = null
                    }
                }

                fun togglePlayback() {
                    coroutineScope.launch {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false
                        } else {
                            if (mediaPlayer == null) {
                                val audioFile = MediaUtils.resolveAudioFileForPlayback(context, valStr)
                                if (audioFile != null && audioFile.exists()) {
                                    try {
                                        val player = MediaPlayer()
                                        player.setDataSource(audioFile.absolutePath)
                                        player.prepare()
                                        player.setOnCompletionListener {
                                            isPlaying = false
                                            playbackPositionMs = 0
                                        }
                                        mediaPlayer = player
                                        totalDurationMs = player.duration
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Erreur lecture audio : ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Fichier audio introuvable ou en cours de préparation", android.widget.Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                            }
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    }
                }

                val formatTime = { ms: Int ->
                    val sec = (ms / 1000) % 60
                    val min = (ms / (1000 * 60)) % 60
                    "%02d:%02d".format(min, sec)
                }

                val isCloudAudio = valStr.startsWith("http://") || valStr.startsWith("https://")
                val isDataAudio = valStr.startsWith("data:audio/")

                // Clean outlined Audio Player (No container background)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    IconButton(
                        onClick = { togglePlayback() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF0F3E76), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enregistrement vocal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCloudAudio || isDataAudio) "● Cloud" else "● Local",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCloudAudio || isDataAudio) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val progress = if (totalDurationMs > 0) (playbackPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF0F3E76),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(playbackPositionMs),
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (totalDurationMs > 0) formatTime(totalDurationMs) else "00:00",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val displayVal = item.reponse.valeur?.takeIf { it.isNotBlank() } ?: item.option?.libelle ?: "(Non renseigné)"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayVal,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
