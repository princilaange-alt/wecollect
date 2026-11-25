package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.SyncedAuditRecord
import com.example.ui.SurveyViewModel
import com.example.ui.components.FacebookPullRefreshContainer
import com.example.ui.components.FacebookShimmerCrossfade
import com.example.ui.components.FacebookSurveyListSkeleton
import com.example.ui.components.OnlineStatusBadge
import com.example.ui.components.SyncStatusLegendRow
import com.example.ui.components.TopSyncTransferAnimation
import com.example.ui.components.pressScaleEffect
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending

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

    val isCurrentlyLoading = isRefreshingData || isPullingForm || isSyncing

    var selectedQuestionnaireId by remember { mutableStateOf<Long?>(null) }

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

    val enquetesByQuestionnaireCount = remember(enquetes) {
        enquetes.groupingBy { it.idQuestionnaire }.eachCount()
    }

    val questionnaireFilteredEnquetes = remember(enquetes, selectedQuestionnaireId) {
        if (selectedQuestionnaireId == null) {
            enquetes
        } else {
            enquetes.filter { it.idQuestionnaire == selectedQuestionnaireId }
        }
    }

    var selectedEnqueteIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedEnqueteIds.isNotEmpty()
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var enqueteToDelete by remember { mutableStateOf<SyncedAuditRecord?>(null) }

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

    val groupedEnquetes = remember(paginatedEnquetes) {
        paginatedEnquetes.groupBy { it.idQuestionnaire }
    }

    val pendingCount = allEnquetes.count { it.syncStatus == "EN_ATTENTE" }
    val syncedCount = allEnquetes.count { it.syncStatus == "SYNCHRONISEE" }
    val errorCount = allEnquetes.count { it.syncStatus == "ERREUR" }
    val totalGlobalCount = allEnquetes.size

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
            // TOP APP BAR: Smooth transition between Normal mode & Multi-Selection mode
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(150))
                },
                label = "survey_list_top_bar"
            ) { selectionActive ->
                if (selectionActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedEnqueteIds = emptySet() },
                                modifier = Modifier
                                    .pressScaleEffect(0.85f)
                                    .testTag("close_selection_mode")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer la sélection",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "${selectedEnqueteIds.size} sélectionnée(s)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            )

                            IconButton(
                                onClick = {
                                    val allIds = questionnaireFilteredEnquetes.map { it.idEnquete }.toSet()
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
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = { showBulkDeleteDialog = true },
                                modifier = Modifier
                                    .pressScaleEffect(0.85f)
                                    .testTag("bulk_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer les éléments sélectionnés",
                                    tint = Color(0xFFFFB4AB),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onOpenDrawer,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            tonalElevation = 1.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .pressScaleEffect(0.9f)
                                .testTag("survey_list_drawer_menu_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu navigation",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mes enquêtes",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitleText = buildString {
                                append("$totalGlobalCount ${if (totalGlobalCount > 1) "fiches collectées" else "fiche collectée"}")
                                if (pendingCount > 0) {
                                    append(" • $pendingCount à envoyer")
                                }
                            }
                            Text(
                                text = subtitleText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (pendingCount > 0) StatusPending else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OnlineStatusBadge(
                            isConnectedAndOnline = isConnectedAndOnline,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }

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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 720.dp),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 4.dp,
                                    bottom = 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Search Bar
                                item(key = "search_bar") {
                                    AnimatedSurveySearchBar(
                                        query = searchQuery,
                                        onQueryChange = { viewModel.setSearchQuery(it) },
                                        matchCount = questionnaireFilteredEnquetes.size,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Sync KPI Filter Cards
                                item(key = "sync_kpi_legend") {
                                    SyncStatusLegendRow(
                                        pendingCount = pendingCount,
                                        syncedCount = syncedCount,
                                        errorCount = errorCount,
                                        selectedFilter = statusFilter,
                                        onFilterSelected = { viewModel.setStatusFilter(it) }
                                    )
                                }

                                // Quick Sync All Banner when pending items exist and online
                                if ((pendingCount > 0 || errorCount > 0) && isConnectedAndOnline && !isSyncing) {
                                    item(key = "quick_sync_banner") {
                                        val toSyncCount = pendingCount + errorCount
                                        Surface(
                                            onClick = { viewModel.syncAllPending() },
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .pressScaleEffect(0.98f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudUpload,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "$toSyncCount ${if (toSyncCount > 1) "fiches prêtes" else "fiche prête"} pour l'envoi",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.primary
                                                ) {
                                                    Text(
                                                        text = "Tout envoyer",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Questionnaire Filter Pills
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

                                // Empty States vs Survey Cards
                                if (allEnquetes.isEmpty()) {
                                    item(key = "empty_global") {
                                        SurveyEmptyStateCard(
                                            isFilteredEmpty = false,
                                            onStartNewSurvey = {
                                                viewModel.startNewSurvey()
                                                viewModel.setTab(2)
                                            }
                                        )
                                    }
                                } else if (enquetes.isEmpty()) {
                                    item(key = "empty_search_or_status") {
                                        SurveyEmptyStateCard(
                                            isFilteredEmpty = true,
                                            onResetFilters = {
                                                viewModel.setSearchQuery("")
                                                viewModel.setStatusFilter("TOUS")
                                                selectedQuestionnaireId = null
                                            },
                                            onStartNewSurvey = {
                                                viewModel.startNewSurvey()
                                                viewModel.setTab(2)
                                            }
                                        )
                                    }
                                } else if (questionnaireFilteredEnquetes.isEmpty()) {
                                    val selQ = questionnairesList.find { it.idQuestionnaire == selectedQuestionnaireId }
                                    item(key = "empty_questionnaire_filter") {
                                        SurveyEmptyStateCard(
                                            isFilteredEmpty = false,
                                            questionnaireTitle = selQ?.titre ?: "ce questionnaire",
                                            onStartNewSurvey = {
                                                selectedQuestionnaireId?.let { viewModel.selectQuestionnaire(it) }
                                                viewModel.startNewSurvey()
                                                viewModel.setTab(2)
                                            }
                                        )
                                    }
                                } else {
                                    groupedEnquetes.forEach { (qId, enquetesInGroup) ->
                                        item(key = "q_section_header_$qId") {
                                            val qTitle = enquetesInGroup.firstOrNull()?.questionnaireTitre
                                                ?: questionnairesList.find { it.idQuestionnaire == qId }?.titre
                                                ?: "Questionnaire #$qId"
                                            val allForQ = enquetes.filter { it.idQuestionnaire == qId }
                                            val qSyncedCount = allForQ.count { it.syncStatus == "SYNCHRONISEE" }
                                            val qPendingCount = allForQ.count { it.syncStatus == "EN_ATTENTE" }

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

                                    if (totalCount > pageSize) {
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
                        text = "Cette fiche est stockée en toute sécurité dans l'appareil. Elle ne s'efface jamais automatiquement. Seule votre décision manuelle peut la supprimer.",
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
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { enqueteToDelete = null }) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(22.dp)
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
                    modifier = Modifier.size(34.dp)
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
                    Text("Supprimer (${selectedEnqueteIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Annuler")
                }
            },
            shape = RoundedCornerShape(22.dp)
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
