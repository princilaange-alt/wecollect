package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SurveyViewModel
import com.example.ui.components.OnlineStatusBadge
import com.example.ui.components.FacebookPullRefreshContainer
import com.example.ui.components.FacebookShimmerCrossfade
import com.example.ui.components.FacebookSyncSkeleton
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.components.TopSyncTransferAnimation
import com.example.ui.components.pressScaleEffect

@Composable
fun SyncScreen(
    viewModel: SurveyViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    val isForceOffline by viewModel.isForceOffline.collectAsState()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()
    val isConnectedAndOnline by viewModel.isConnectedAndOnline.collectAsState()
    val globalSyncState by viewModel.globalSyncState.collectAsState()
    val allEnquetes by viewModel.allEnquetes.collectAsState()
    val selectedDetail by viewModel.selectedEnqueteDetail.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val connectionTestResult by viewModel.connectionTestResult.collectAsState()
    val isPullingForm by viewModel.isPullingForm.collectAsState()
    val isRefreshingData by viewModel.isRefreshingData.collectAsState()
    val pullFormStatus by viewModel.pullFormStatus.collectAsState()
    val allQuestionnaires by viewModel.allQuestionnaires.collectAsState()
    val lastRealtimeSyncTime by viewModel.lastRealtimeSyncTime.collectAsState()

    val isCurrentlyLoading = isRefreshingData || isPullingForm || isSyncing

    val totalCount = allEnquetes.size
    val syncedCount = allEnquetes.count { it.syncStatus == "SYNCHRONISEE" }
    val pendingCount = allEnquetes.count { it.syncStatus == "EN_ATTENTE" }
    val errorCount = allEnquetes.count { it.syncStatus == "ERREUR" }
    val inProgressCount = if (isSyncing) 1 else 0

    val syncPercentage = if (totalCount > 0) {
        ((syncedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
    } else 100

    val progressFloat = if (totalCount > 0) {
        syncedCount.toFloat() / totalCount.toFloat()
    } else 1f

    var showMenu by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var showTestDialog by remember { mutableStateOf(false) }

    var urlInput by remember { mutableStateOf(viewModel.syncService.supabaseUrl) }
    var keyInput by remember { mutableStateOf(viewModel.syncService.supabaseAnonKey) }

    val hasAnyOpenDialog = selectedDetail != null || showMenu || showConfigDialog || showLogsDialog || showTestDialog

    BackHandler(enabled = hasAnyOpenDialog) {
        when {
            selectedDetail != null -> viewModel.closeEnqueteDetail()
            showConfigDialog -> showConfigDialog = false
            showLogsDialog -> showLogsDialog = false
            showTestDialog -> showTestDialog = false
            showMenu -> showMenu = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // --- 1. Top Bar ---
        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Menu Drawer Button on Left
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .pressScaleEffect(0.85f)
                        .testTag("sync_drawer_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu navigation",
                        tint = Color(0xFF0F2942),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title
                Text(
                    text = "Synchronisation",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F2942),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                )

                // Indicateur d'état réseau en haut à droite (En ligne / Hors-ligne)
                OnlineStatusBadge(
                    isConnectedAndOnline = isConnectedAndOnline,
                    modifier = Modifier.padding(end = 4.dp)
                )

                // 3-dots Menu on the Right
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .pressScaleEffect(0.85f)
                            .testTag("sync_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options de synchronisation",
                            tint = Color(0xFF0F2942),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Tester connexion Supabase") },
                            leadingIcon = { Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color(0xFF0F3E76)) },
                            onClick = {
                                showMenu = false
                                viewModel.testSupabaseConnection()
                                showTestDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Synchroniser manuellement") },
                            leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF0F3E76)) },
                            onClick = {
                                showMenu = false
                                viewModel.syncAllPending()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Actualiser questionnaire (Pull)") },
                            leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF0F3E76)) },
                            onClick = {
                                showMenu = false
                                viewModel.pullFormsFromSupabase()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Configuration Supabase") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF0F3E76)) },
                            onClick = {
                                showMenu = false
                                showConfigDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Voir journal des logs") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF0F3E76)) },
                            onClick = {
                                showMenu = false
                                showLogsDialog = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Effacer les logs") },
                            leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, tint = StatusError) },
                            onClick = {
                                showMenu = false
                                viewModel.clearSyncLogs()
                            }
                        )
                    }
                }
            }
        }

        // Animated top transfer banner during sync or pull
        TopSyncTransferAnimation(
            isSyncing = isSyncing,
            isPullingForm = isPullingForm
        )

        // --- 2. Scrollable Body ---
        FacebookPullRefreshContainer(
            isRefreshing = isCurrentlyLoading,
            onRefresh = { viewModel.refreshAllData() },
            modifier = Modifier.fillMaxSize()
        ) {
            FacebookShimmerCrossfade(
                isLoading = isCurrentlyLoading,
                skeletonContent = { FacebookSyncSkeleton() },
                actualContent = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
            // Main Center Status Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_main_card"),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Cloud Icon inside Pale Blue Rounded Container
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = Color(0xFF0F4A8A),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Cloud Sync",
                                        tint = Color(0xFF0F4A8A),
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Title
                        val titleText = when {
                            isSyncing -> "Synchronisation en cours..."
                            pendingCount > 0 -> "Synchronisation en attente"
                            else -> "Synchronisation terminée"
                        }
                        Text(
                            text = titleText,
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Subtitle
                        val subtitleText = when {
                            isSyncing -> "Ne quittez pas cette page pendant l'opération."
                            pendingCount > 0 -> "$pendingCount enquête(s) en attente de téléversement."
                            else -> "Toutes vos données sont sécurisées dans le cloud."
                        }
                        Text(
                            text = subtitleText,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Progress Bar (Sleek Blue Rounded)
                        LinearProgressIndicator(
                            progress = { progressFloat },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF1E70E8),
                            trackColor = Color(0xFFE2E8F0)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress details row (e.g., 18 / 25 enquêtes   72%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$syncedCount / $totalCount enquêtes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$syncPercentage%",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E70E8)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4 Status Counters Columns: Synchronisées, En cours, En attente, Erreur
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 1. Synchronisées (Green)
                            SyncStatColumn(
                                dotColor = Color(0xFF10B981),
                                label = "Synchronisées",
                                value = "$syncedCount",
                                modifier = Modifier.weight(1f)
                            )

                            // 2. En cours (Blue)
                            SyncStatColumn(
                                dotColor = Color(0xFF3B82F6),
                                label = "En cours",
                                value = "$inProgressCount",
                                modifier = Modifier.weight(1f)
                            )

                            // 3. En attente (Orange)
                            SyncStatColumn(
                                dotColor = Color(0xFFF59E0B),
                                label = "En attente",
                                value = "$pendingCount",
                                modifier = Modifier.weight(1f)
                            )

                            // 4. Erreur (Red)
                            SyncStatColumn(
                                dotColor = Color(0xFFEF4444),
                                label = "Erreur",
                                value = "$errorCount",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Outlined Action Button: Arrêter / Lancer la synchronisation
                        OutlinedButton(
                            onClick = {
                                if (isSyncing) {
                                    // Stop / Cancel indication
                                    viewModel.syncService.addLog("Arrêt demandé par l'enquêteur.", false)
                                } else {
                                    viewModel.syncAllPending()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScaleEffect(0.96f)
                                .height(46.dp)
                                .testTag("btn_sync_action"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSyncing) StatusError.copy(alpha = 0.15f) else Color(0xFF0F4A8A).copy(alpha = 0.12f),
                                contentColor = if (isSyncing) StatusError else Color(0xFF0F4A8A)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.Stop else Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isSyncing) StatusError else Color(0xFF0F4A8A)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSyncing) "Arrêter" else if (pendingCount > 0) "Lancer la synchronisation" else "Forcer la synchronisation",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSyncing) StatusError else Color(0xFF0F4A8A)
                                )
                            }
                        }
                    }
                }
            }
                    }
                }
            )
        }
    }

    // --- Dialogs ---

    // 1. Connection Test Dialog
    if (showTestDialog) {
        AlertDialog(
            onDismissRequest = { showTestDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = Color(0xFF0F3E76)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test de connexion Supabase", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    if (isTestingConnection) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Vérification de l'endpoint Supabase...")
                        }
                    } else if (connectionTestResult != null) {
                        val (isOk, msg) = connectionTestResult!!
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isOk) StatusSuccess else StatusError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 13.5.sp,
                                color = if (isOk) StatusSuccess else StatusError
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTestDialog = false }) {
                    Text("Fermer")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 2. Supabase Configuration Dialog
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF0F3E76))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configuration Supabase", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Point de terminaison REST :", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Supabase URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Supabase Anon Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSupabaseConfig(urlInput.trim(), keyInput.trim())
                        showConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3E76))
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("X Annuler")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3. Logs Dialog
    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF0F3E76))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Journaux de synchronisation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    if (syncLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Aucun journal pour le moment", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(syncLogs) { _, log ->
                                Text(
                                    text = "• ${log.message}",
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("Fermer")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 4. Survey Detail Dialog
    selectedDetail?.let { detail ->
        EnqueteDetailDialog(
            detail = detail,
            payloadPreview = viewModel.syncService.getPayloadPreview(detail),
            onDismiss = { viewModel.closeEnqueteDetail() },
            onSync = { viewModel.syncEnqueteById(detail.enquete.idEnquete) },
            onDelete = {
                viewModel.deleteEnquete(detail.enquete.idEnquete)
                viewModel.closeEnqueteDetail()
            }
        )
    }
}

@Composable
private fun SyncStatColumn(
    dotColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
