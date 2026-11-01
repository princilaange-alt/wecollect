package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Photo
// Removed alias import
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import android.graphics.Bitmap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.R
import com.example.data.model.AnnonceFichierEntity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.SurveyViewModel
import com.example.ui.components.FacebookPullRefreshContainer
import com.example.ui.components.pressScaleEffect
import com.example.ui.components.AttachmentDisplay
import com.example.ui.components.AttachmentItemDisplay
import com.example.ui.components.FullPdfViewerDialog
import com.example.ui.components.AnnouncementPhotoViewerDialog
import com.example.ui.components.EnqueteurAvatar
import com.example.ui.components.OnlineStatusBadge
import com.example.util.AnnouncementMediaHelper
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Announcement item data model avec support de fichiers images, PDFs et médias visuels
 */
data class AnnouncementItem(
    val id: String,
    val title: String,
    val timeAgo: String,
    val fileName: String,
    val description: String = "",
    val isQuestionnaire: Boolean = false,
    val questionnaireId: Long? = null,
    val idAnnonce: String? = null,
    val priority: String = "NORMALE",
    val authorName: String? = null,
    val authorRole: String? = null,
    val isRead: Boolean = true,
    val targetType: String = "TOUS",
    val targetValue: String? = null,
    val files: List<AnnonceFichierEntity> = emptyList(),
    val imageResId: Int? = null,
    val fileMimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val previewUrl: String? = null
)

/**
 * Résout fidèlement les URLs de stockage Supabase, Base64 ou fichiers locaux sans inventer d'images
 */
fun resolveStorageOrFileUrl(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()
    return when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("content://", ignoreCase = true) || trimmed.startsWith("file://", ignoreCase = true) || trimmed.startsWith("data:", ignoreCase = true) -> trimmed
        trimmed.startsWith("/9j/") -> "data:image/jpeg;base64,$trimmed"
        trimmed.startsWith("iVBOR") -> "data:image/png;base64,$trimmed"
        trimmed.startsWith("UklGR") -> "data:image/webp;base64,$trimmed"
        trimmed.startsWith("JVBERi") -> "data:application/pdf;base64,$trimmed"
        trimmed.startsWith("/storage/v1/") -> "https://hcqhrzdskyzdqzdrhisc.supabase.co$trimmed"
        trimmed.startsWith("storage/v1/") -> "https://hcqhrzdskyzdqzdrhisc.supabase.co/$trimmed"
        trimmed.startsWith("/api/annonces/") -> trimmed
        trimmed.startsWith("/") && java.io.File(trimmed).exists() -> "file://$trimmed"
        (!trimmed.startsWith("/") && trimmed.length > 80 && !trimmed.contains(" ") && !trimmed.contains("http")) -> "data:image/jpeg;base64,$trimmed"
        else -> {
            val clean = trimmed.trimStart('/')
            if (clean.startsWith("object/public/")) {
                "https://hcqhrzdskyzdqzdrhisc.supabase.co/storage/v1/$clean"
            } else if (clean.startsWith("public/")) {
                "https://hcqhrzdskyzdqzdrhisc.supabase.co/storage/v1/object/$clean"
            } else if (clean.startsWith("annonces/")) {
                "https://hcqhrzdskyzdqzdrhisc.supabase.co/storage/v1/object/public/$clean"
            } else {
                "https://hcqhrzdskyzdqzdrhisc.supabase.co/storage/v1/object/public/annonces/$clean"
            }
        }
    }
}

/**
 * HomeScreen representing the "Annonces" view matching IMG20261014092623.jpg:
 * - Clean Top App Bar with Hamburger menu icon & "Annonces" title
 * - Outlined Search box with "Rechercher..."
 * - Cards with time ago, blue document icon, title, and gray attachment box with download icon & teal link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SurveyViewModel,
    onOpenDrawer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val allQuestionnaires by viewModel.allQuestionnaires.collectAsState()
    val allEnquetes by viewModel.allEnquetes.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val detailedAnnonces by viewModel.detailedAnnonces.collectAsState()
    val isConnectedAndOnline by viewModel.isConnectedAndOnline.collectAsState()
    val isRefreshingData by viewModel.isRefreshingData.collectAsState()
    val isPullingForm by viewModel.isPullingForm.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val statusChangeNotification by viewModel.statusChangeNotification.collectAsState()

    val isCurrentlyLoading = isRefreshingData || isPullingForm || isSyncing

    var selectedAnnouncementToPreview by remember { mutableStateOf<AnnouncementItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var photoToEnlarge by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pdfToView by remember { mutableStateOf<Triple<String, Long, String?>?>(null) }

    // Built-in announcements matching the screenshot reference + active surveys and realtime annonces from Room/Supabase
    // Annonces issues exclusivement de la base de données (SQLite Room / Supabase)
    val announcementsList = remember(detailedAnnonces) {
        val list = mutableListOf<AnnouncementItem>()

        detailedAnnonces.forEach { da ->
            val a = da.annonce

            val pdfFile = da.fichiers.firstOrNull { f ->
                f.typeMime == "application/pdf" || f.nomFichier.endsWith(".pdf", true) ||
                (f.dataUrl != null && (f.dataUrl.startsWith("data:application/pdf") || f.dataUrl.startsWith("JVBERi")))
            }
            val jpgFile = da.fichiers.firstOrNull { f ->
                f.nomFichier.endsWith(".jpg", true) ||
                f.nomFichier.endsWith(".jpeg", true) ||
                (f.dataUrl != null && (f.dataUrl.startsWith("data:image/jpeg") || f.dataUrl.startsWith("/9j/")))
            }
            val imgFile = jpgFile ?: da.fichiers.firstOrNull { f ->
                f.typeMime?.startsWith("image/") == true ||
                f.nomFichier.endsWith(".png", true) ||
                f.nomFichier.endsWith(".webp", true) ||
                (f.dataUrl != null && (f.dataUrl.startsWith("data:image/") || f.dataUrl.startsWith("iVBOR") || f.dataUrl.startsWith("UklGR")))
            }
            val primaryFile = pdfFile ?: imgFile ?: da.fichiers.firstOrNull()

            val isPdf = pdfFile != null || a.typeAnnonce.equals("FICHIER", true) ||
                    da.primaryFileName.endsWith(".pdf", true) ||
                    primaryFile?.nomFichier?.endsWith(".pdf", true) == true ||
                    primaryFile?.typeMime == "application/pdf" ||
                    (primaryFile?.dataUrl != null && (primaryFile.dataUrl.startsWith("data:application/pdf") || primaryFile.dataUrl.startsWith("JVBERi")))

            val isImg = !isPdf && (
                    imgFile != null || a.typeAnnonce.equals("IMAGE", true) ||
                    da.primaryFileName.endsWith(".jpg", true) ||
                    da.primaryFileName.endsWith(".jpeg", true) ||
                    da.primaryFileName.endsWith(".png", true) ||
                    da.primaryFileName.endsWith(".webp", true) ||
                    (primaryFile != null && (
                        primaryFile.nomFichier.endsWith(".jpg", true) ||
                        primaryFile.nomFichier.endsWith(".jpeg", true) ||
                        primaryFile.nomFichier.endsWith(".png", true) ||
                        primaryFile.typeMime?.startsWith("image/") == true
                    ))
            )
            val isAud = !isPdf && !isImg && (
                    primaryFile?.typeMime?.startsWith("audio/") == true ||
                    da.primaryFileName.endsWith(".mp3", true) ||
                    da.primaryFileName.endsWith(".wav", true) ||
                    da.primaryFileName.endsWith(".m4a", true)
            )

            val mime = primaryFile?.typeMime ?: when {
                isPdf -> "application/pdf"
                isImg -> "image/jpeg"
                isAud -> "audio/mpeg"
                else -> "application/octet-stream"
            }
            val size = if (primaryFile != null && primaryFile.tailleOctets > 0L) {
                primaryFile.tailleOctets
            } else if (isImg) 2840000L else if (isPdf) 1450000L else 45000L

            val rawUrl = primaryFile?.dataUrl?.takeIf { it.isNotBlank() } ?: primaryFile?.cheminStorage
            val effectiveFileName = primaryFile?.nomFichier ?: da.primaryFileName

            list.add(
                AnnouncementItem(
                    id = "a_${a.idAnnonce}",
                    title = a.titre,
                    timeAgo = da.timeAgo,
                    fileName = effectiveFileName,
                    description = a.contenu ?: "",
                    idAnnonce = a.idAnnonce,
                    priority = a.priorite,
                    authorName = a.authorName,
                    authorRole = a.authorRole,
                    isRead = da.isRead,
                    targetType = a.cibleType,
                    targetValue = a.cibleValeur,
                    files = da.fichiers,
                    imageResId = if (isImg && rawUrl.isNullOrBlank()) {
                        if (a.idAnnonce.contains("guide") || da.primaryFileName.contains("guide")) {
                            R.drawable.login_hero_banner_1789034051081
                        } else {
                            R.drawable.field_survey_interview_1789034896483
                        }
                    } else null,
                    fileMimeType = mime,
                    fileSizeBytes = size,
                    previewUrl = rawUrl
                )
            )
        }

        list
    }

    val filteredAnnouncements = remember(searchQuery, announcementsList) {
        if (searchQuery.isBlank()) {
            announcementsList
        } else {
            val q = searchQuery.trim().lowercase()
            announcementsList.filter {
                it.title.lowercase().contains(q) ||
                it.fileName.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                (it.authorName?.lowercase()?.contains(q) == true) ||
                it.priority.lowercase().contains(q)
            }
        }
    }

    // Pre-rendu en arrière-plan des PDF pour que la 1ère page apparaisse immédiatement ("tonga de hiseho")
    LaunchedEffect(detailedAnnonces) {
        withContext(Dispatchers.IO) {
            detailedAnnonces.forEach { da ->
                da.fichiers.forEach { f ->
                    val isPdfFile = f.typeMime == "application/pdf" || f.nomFichier.endsWith(".pdf", true)
                    if (isPdfFile) {
                        val rawSrc = f.dataUrl?.takeIf { it.isNotBlank() } ?: f.cheminStorage
                        if (!rawSrc.isNullOrBlank()) {
                            AnnouncementMediaHelper.renderPdfFirstPage(context, f.nomFichier, rawSrc)
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = photoToEnlarge != null || pdfToView != null || selectedAnnouncementToPreview != null || showCreateDialog) {
        when {
            photoToEnlarge != null -> photoToEnlarge = null
            pdfToView != null -> pdfToView = null
            selectedAnnouncementToPreview != null -> selectedAnnouncementToPreview = null
            showCreateDialog -> showCreateDialog = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        FacebookPullRefreshContainer(
            isRefreshing = isCurrentlyLoading,
            onRefresh = { viewModel.refreshAllData() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
            // Clean Top App Bar matching IMG20261014092623.jpg
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu tiroir",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Annonces",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.2).sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Famantarana tambajotra (En ligne / Hors-ligne)
                    OnlineStatusBadge(
                        isConnectedAndOnline = isConnectedAndOnline,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            // Search Bar matching IMG20261014092623.jpg
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Rechercher",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Rechercher...",
                                    color = Color(0xFF64748B),
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("annonces_search_input")
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Effacer",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Status notification banner
            AnimatedVisibility(
                visible = statusChangeNotification != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                statusChangeNotification?.let { (msg, isPos) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPos) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        border = null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPos) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isPos) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    color = if (isPos) Color(0xFF14532D) else Color(0xFF991B1B),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissStatusChangeNotification() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = if (isPos) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Announcement Feed List matching IMG20261014092623.jpg
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(filteredAnnouncements, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { totalDistance -> totalDistance * 0.85f },
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                if (item.idAnnonce != null) {
                                    viewModel.deleteAnnonce(item.idAnnonce)
                                }
                                Toast.makeText(context, "Annonce supprimée", Toast.LENGTH_SHORT).show()
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {}
                    ) {
                        AnnouncementCard(
                            item = item,
                            onOpen = {
                                if (item.isQuestionnaire && item.questionnaireId != null) {
                                    viewModel.selectQuestionnaire(item.questionnaireId)
                                    viewModel.setTab(2) // Jump directly to SurveyFormScreen
                                } else {
                                    if (item.idAnnonce != null) {
                                        viewModel.markAnnonceAsRead(item.idAnnonce)
                                    }
                                    val isFilePdf = item.fileMimeType == "application/pdf" || item.fileName.endsWith(".pdf", true) || item.files.any { it.nomFichier.endsWith(".pdf", true) || it.typeMime == "application/pdf" }
                                    val imgFile = item.files.firstOrNull {
                                        it.nomFichier.endsWith(".jpg", true) ||
                                        it.nomFichier.endsWith(".jpeg", true) ||
                                        it.nomFichier.endsWith(".png", true) ||
                                        it.nomFichier.endsWith(".webp", true) ||
                                        it.typeMime?.startsWith("image/") == true ||
                                        (it.dataUrl != null && (it.dataUrl.startsWith("data:image/") || it.dataUrl.startsWith("/9j/") || it.dataUrl.startsWith("iVBOR")))
                                    }
                                    val isFileImg = imgFile != null ||
                                            item.fileName.endsWith(".jpg", true) ||
                                            item.fileName.endsWith(".jpeg", true) ||
                                            item.fileName.endsWith(".png", true) ||
                                            item.fileName.endsWith(".webp", true) ||
                                            item.fileMimeType?.startsWith("image/") == true
                                    val imgRaw = imgFile?.let { it.dataUrl?.takeIf { d -> d.isNotBlank() } ?: it.cheminStorage } ?: item.previewUrl
                                    val imgName = imgFile?.nomFichier ?: item.fileName

                                    val pdfRaw = item.previewUrl ?: item.files.firstOrNull { it.nomFichier.endsWith(".pdf", true) || it.typeMime == "application/pdf" }?.let { it.dataUrl ?: it.cheminStorage }

                                    if (isFilePdf) {
                                        pdfToView = Triple(item.fileName, item.fileSizeBytes, pdfRaw)
                                    } else if (isFileImg && !imgRaw.isNullOrBlank()) {
                                        photoToEnlarge = Pair(imgName, imgRaw)
                                    } else {
                                        selectedAnnouncementToPreview = item
                                    }
                                }
                            },
                            onDownload = {
                                val isFilePdf = item.fileMimeType == "application/pdf" || item.fileName.endsWith(".pdf", true) || item.files.any { it.nomFichier.endsWith(".pdf", true) || it.typeMime == "application/pdf" }
                                val imgFile = item.files.firstOrNull {
                                    it.nomFichier.endsWith(".jpg", true) ||
                                    it.nomFichier.endsWith(".jpeg", true) ||
                                    it.nomFichier.endsWith(".png", true) ||
                                    it.nomFichier.endsWith(".webp", true) ||
                                    it.typeMime?.startsWith("image/") == true
                                }
                                val isFileImg = imgFile != null ||
                                        item.fileName.endsWith(".jpg", true) ||
                                        item.fileName.endsWith(".jpeg", true) ||
                                        item.fileName.endsWith(".png", true) ||
                                        item.fileMimeType?.startsWith("image/") == true
                                val imgRaw = imgFile?.let { it.dataUrl?.takeIf { d -> d.isNotBlank() } ?: it.cheminStorage } ?: item.previewUrl
                                val imgName = imgFile?.nomFichier ?: item.fileName

                                if (isFilePdf) {
                                    val opened = AnnouncementMediaHelper.openPdfWithSystemViewer(context, item.fileName, item.previewUrl)
                                    if (!opened) {
                                        Toast.makeText(context, "Fichier PDF « ${item.fileName} » prêt.", Toast.LENGTH_SHORT).show()
                                    }
                                } else if (isFileImg) {
                                    AnnouncementMediaHelper.openImageWithSystemViewer(context, imgName, imgRaw)
                                } else {
                                    val opened = AnnouncementMediaHelper.openPdfWithSystemViewer(context, item.fileName, item.previewUrl)
                                    if (!opened) {
                                        Toast.makeText(context, "Fichier « ${item.fileName} » prêt.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                if (filteredAnnouncements.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Aucune annonce trouvée pour « $searchQuery »" else "Aucune annonce enregistrée dans la base de données",
                                fontSize = 14.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

    // Full screen photo viewer dialog
    photoToEnlarge?.let { (fName, rawUrl) ->
        AnnouncementPhotoViewerDialog(
            fileName = fName,
            fileSizeBytes = 0L,
            rawUrl = rawUrl,
            onDismiss = { photoToEnlarge = null }
        )
    }

    // Full screen PDF viewer dialog
    pdfToView?.let { (fName, size, rawUrl) ->
        FullPdfViewerDialog(
            fileName = fName,
            fileSizeBytes = size,
            rawUrl = rawUrl,
            onDismiss = { pdfToView = null }
        )
    }

    // Detail preview dialog when clicking on an announcement
    selectedAnnouncementToPreview?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedAnnouncementToPreview = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge priorité
                        val (prioBg, prioFg) = when (item.priority.uppercase()) {
                            "URGENTE" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
                            "IMPORTANTE" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
                            else -> Color(0xFFE0F2FE) to Color(0xFF0288D1)
                        }
                        Text(
                            text = item.priority.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = prioFg
                        )

                        Text(
                            text = item.timeAgo,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!item.authorName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.authorName,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            text = {
                Column {
                    val isImg = item.imageResId != null ||
                            !item.previewUrl.isNullOrBlank() ||
                            item.fileMimeType?.startsWith("image/") == true ||
                            item.fileName.endsWith(".jpg", true) ||
                            item.fileName.endsWith(".jpeg", true) ||
                            item.fileName.endsWith(".png", true) ||
                            item.fileName.endsWith(".webp", true)

                    val isPdf = item.fileMimeType == "application/pdf" ||
                            item.fileName.endsWith(".pdf", true) ||
                            item.files.any { it.nomFichier.endsWith(".pdf", true) || it.typeMime == "application/pdf" }

                    if (isImg) {
                        val rawImgTarget = remember(item.previewUrl, item.files) {
                            item.previewUrl ?: item.files.firstOrNull { f ->
                                f.typeMime?.startsWith("image/") == true ||
                                f.nomFichier.endsWith(".jpg", true) ||
                                f.nomFichier.endsWith(".png", true)
                            }?.let { it.dataUrl ?: it.cheminStorage }
                        }
                        val resolvedImg = remember(rawImgTarget) {
                            AnnouncementMediaHelper.resolveImageModel(context, rawImgTarget)
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFBAE6FD)),
                            color = Color(0xFF0F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clickable {
                                    if (!rawImgTarget.isNullOrBlank()) {
                                        photoToEnlarge = Pair(item.fileName, rawImgTarget)
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (resolvedImg != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(resolvedImg)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = item.title,
                                        placeholder = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                        error = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (item.imageResId != null) {
                                    Image(
                                        painter = painterResource(id = item.imageResId),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.75f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Agrandir", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    } else if (isPdf) {
                        val pdfRawTarget = remember(item.previewUrl, item.files) {
                            item.previewUrl ?: item.files.firstOrNull { f ->
                                f.typeMime == "application/pdf" || f.nomFichier.endsWith(".pdf", true)
                            }?.let { it.dataUrl ?: it.cheminStorage }
                        }
                        var previewPdfBmp by remember(pdfRawTarget) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(pdfRawTarget) {
                            if (!pdfRawTarget.isNullOrBlank()) {
                                withContext(Dispatchers.IO) {
                                    previewPdfBmp = AnnouncementMediaHelper.renderPdfFirstPage(context, item.fileName, pdfRawTarget)
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDA4AF)),
                            color = Color(0xFFFFF1F2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF",
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "DOCUMENT PDF OFFICIEL",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFBE123C)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Transparent
                                    ) {
                                        Text(
                                            text = "Document Validé",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF16A34A),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (previewPdfBmp != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clickable {
                                                pdfToView = Triple(item.fileName, item.fileSizeBytes, pdfRawTarget)
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                bitmap = previewPdfBmp!!.asImageBitmap(),
                                                contentDescription = item.fileName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color(0xFF1E293B).copy(alpha = 0.9f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Consulter le PDF", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = item.fileName,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Directive officielle, notice méthodologique et consignes sur le terrain du système wecollect.",
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                color = Color(0xFF475569)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Taille : ${formatFileSize(item.fileSizeBytes)} • Format Adobe Acrobat PDF",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            fontSize = 14.5.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "Fichiers joints :",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (item.files.isNotEmpty()) {
                        item.files.forEach { file ->
                            AttachmentDisplay(
                                file = file,
                                onOpen = {
                                    val isFilePdf = file.nomFichier.endsWith(".pdf", true) || file.typeMime == "application/pdf"
                                    val isFileImg = file.nomFichier.endsWith(".jpg", true) || file.nomFichier.endsWith(".png", true) || file.typeMime?.startsWith("image/") == true
                                    val raw = file.dataUrl ?: file.cheminStorage
                                    if (isFilePdf) {
                                        pdfToView = Triple(file.nomFichier, file.tailleOctets, raw)
                                    } else if (isFileImg && !raw.isNullOrBlank()) {
                                        photoToEnlarge = Pair(file.nomFichier, raw)
                                    } else {
                                        val opened = AnnouncementMediaHelper.openPdfWithSystemViewer(context, file.nomFichier, raw)
                                        if (!opened) {
                                            Toast.makeText(context, "Fichier « ${file.nomFichier} » prêt", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDownload = {
                                    val raw = file.dataUrl ?: file.cheminStorage
                                    AnnouncementMediaHelper.openPdfWithSystemViewer(context, file.nomFichier, raw)
                                }
                            )
                        }
                    } else if (item.fileName.isNotBlank()) {
                        AttachmentItemDisplay(
                            fileName = item.fileName,
                            mimeType = item.fileMimeType,
                            fileSizeBytes = item.fileSizeBytes,
                            rawUrl = item.previewUrl,
                            onOpen = {
                                val isFilePdf = item.fileName.endsWith(".pdf", true) || item.fileMimeType == "application/pdf"
                                val isFileImg = item.fileName.endsWith(".jpg", true) || item.fileName.endsWith(".png", true) || item.fileMimeType?.startsWith("image/") == true
                                if (isFilePdf) {
                                    pdfToView = Triple(item.fileName, item.fileSizeBytes, item.previewUrl)
                                } else if (isFileImg && !item.previewUrl.isNullOrBlank()) {
                                    photoToEnlarge = Pair(item.fileName, item.previewUrl)
                                } else {
                                    AnnouncementMediaHelper.openPdfWithSystemViewer(context, item.fileName, item.previewUrl)
                                }
                            },
                            onDownload = {
                                AnnouncementMediaHelper.openPdfWithSystemViewer(context, item.fileName, item.previewUrl)
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Dialogue pour créer et publier une annonce sur Supabase
    if (showCreateDialog) {
        var titre by remember { mutableStateOf("") }
        var contenu by remember { mutableStateOf("") }
        var priorite by remember { mutableStateOf("NORMALE") }
        var fileName by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showCreateDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Publier une annonce",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = titre,
                        onValueChange = { titre = it },
                        label = { Text("Titre de l'annonce") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = contenu,
                        onValueChange = { contenu = it },
                        label = { Text("Contenu du message") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        text = "Priorité :",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("NORMALE", "IMPORTANTE", "URGENTE").forEach { prio ->
                            val isSelected = priorite == prio
                            val prioColor = when (prio) {
                                "URGENTE" -> Color(0xFFEF4444)
                                "IMPORTANTE" -> Color(0xFFF59E0B)
                                else -> Color(0xFF0288D1)
                            }
                            Surface(
                                onClick = { priorite = prio },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) prioColor.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) prioColor else Color.Transparent)
                            ) {
                                Text(
                                    text = prio,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) prioColor else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Type de pièce jointe :",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("📄 PDF", "note_service_terrain.pdf", "FICHIER"),
                            Triple("📷 Image", "photo_terrain_enquete.jpg", "IMAGE"),
                            Triple("📊 Tableur", "echantillonnage.xlsx", "FICHIER"),
                            Triple("📝 Aucun", "", "TEXTE")
                        ).forEach { (label, defaultName, type) ->
                            val isSelected = (defaultName.isEmpty() && fileName.isEmpty()) || (defaultName.isNotEmpty() && fileName == defaultName)
                            Surface(
                                onClick = { fileName = defaultName },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0288D1).copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0288D1) else Color.Transparent)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF0288D1) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Nom du fichier joint (optionnel)") },
                        placeholder = { Text("ex: note_service_terrain.pdf ou photo.jpg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (titre.isBlank()) {
                            Toast.makeText(context, "Veuillez saisir un titre.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmitting = true
                        val deducedType = when {
                            fileName.endsWith(".jpg", true) || fileName.endsWith(".png", true) || fileName.endsWith(".jpeg", true) -> "IMAGE"
                            fileName.isNotBlank() -> "FICHIER"
                            else -> "TEXTE"
                        }
                        viewModel.publishAnnonce(
                            titre = titre.trim(),
                            contenu = contenu.trim(),
                            typeAnnonce = deducedType,
                            priorite = priorite,
                            cibleType = "TOUS",
                            fileName = fileName.trim().takeIf { it.isNotBlank() }
                        ) { success ->
                            isSubmitting = false
                            showCreateDialog = false
                            if (success) {
                                Toast.makeText(context, "Annonce publiée et synchronisée en direct avec la base de données !", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Annonce enregistrée localement (en attente de synchronisation).", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    enabled = !isSubmitting && titre.isNotBlank()
                ) {
                    Text(if (isSubmitting) "Publication..." else "Publier", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("X Annuler", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * Dégradé horizontal pastel doux conservé comme constante si besoin
 */
val PastelCadreBrush = Brush.horizontalGradient(
    colors = listOf(
        Color.White,
        Color.White
    )
)

/**
 * Grille de points fins neutre (désactivée pour un fond propre)
 */
fun Modifier.drawDottedGrid(
    dotColor: Color = Color.Transparent,
    dotRadius: Float = 0f,
    stepDp: Dp = 16.dp
): Modifier = this

/**
 * Style complet de cadre pour les annonces :
 * Fond mat et bordure de couleur appliquée uniquement sur le côté gauche (left border).
 */
fun Modifier.pastelCadreStyle(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color(0xFFF1F5F9),
    leftBorderColor: Color = Color(0xFF0288D1),
    leftBorderWidth: Dp = 4.dp
): Modifier = this
    .clip(shape)
    .background(backgroundColor)
    .drawBehind {
        val widthPx = leftBorderWidth.toPx()
        drawRect(
            color = leftBorderColor,
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(widthPx, size.height)
        )
    }

/**
 * Announcement Card avec fond doux et reposant pour les yeux
 */
@Composable
fun AnnouncementCard(
    item: AnnouncementItem,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    // Fond adouci et mat (non éblouissant, pas trop blanc)
    val cardBackground = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val leftAccentColor = when (item.priority.uppercase()) {
        "URGENTE" -> Color(0xFFDC2626)
        "IMPORTANTE" -> Color(0xFFD97706)
        else -> Color(0xFF0288D1)
    }

    val isImage = (item.imageResId != null) ||
            (!item.previewUrl.isNullOrBlank() && (
                    item.fileMimeType?.startsWith("image/") == true ||
                    item.fileName.endsWith(".jpg", ignoreCase = true) ||
                    item.fileName.endsWith(".jpeg", ignoreCase = true) ||
                    item.fileName.endsWith(".png", ignoreCase = true) ||
                    item.fileName.endsWith(".webp", ignoreCase = true)
            )) ||
            item.fileName.endsWith(".jpg", ignoreCase = true) ||
            item.fileName.endsWith(".jpeg", ignoreCase = true) ||
            item.fileName.endsWith(".png", ignoreCase = true) ||
            item.fileName.endsWith(".webp", ignoreCase = true) ||
            item.fileMimeType?.startsWith("image/") == true ||
            item.files.any {
                it.nomFichier.endsWith(".jpg", true) ||
                it.nomFichier.endsWith(".jpeg", true) ||
                it.nomFichier.endsWith(".png", true) ||
                it.nomFichier.endsWith(".webp", true) ||
                it.typeMime?.startsWith("image/") == true ||
                (it.dataUrl != null && (it.dataUrl.startsWith("data:image/") || it.dataUrl.startsWith("/9j/") || it.dataUrl.startsWith("iVBOR")))
            }

    val isPdf = item.fileMimeType == "application/pdf" ||
            item.fileName.endsWith(".pdf", ignoreCase = true) ||
            item.files.any { it.nomFichier.endsWith(".pdf", true) || it.typeMime == "application/pdf" }

    val isAudio = item.fileMimeType?.startsWith("audio/") == true ||
            item.fileName.endsWith(".mp3", ignoreCase = true) ||
            item.fileName.endsWith(".wav", ignoreCase = true) ||
            item.fileName.endsWith(".m4a", ignoreCase = true)

    val isSpreadsheet = item.fileName.endsWith(".xls", ignoreCase = true) ||
            item.fileName.endsWith(".xlsx", ignoreCase = true) ||
            item.fileName.endsWith(".csv", ignoreCase = true) ||
            item.fileMimeType?.contains("excel", ignoreCase = true) == true

    val isQuestionnaire = item.isQuestionnaire ||
            item.fileName.endsWith(".gid", ignoreCase = true) ||
            item.fileMimeType == "application/x-survey"

    val hasAttachment = isImage || isPdf || isAudio || isSpreadsheet || isQuestionnaire || item.files.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .pressScaleEffect(0.985f)
            .pastelCadreStyle(
                shape = RoundedCornerShape(16.dp),
                backgroundColor = cardBackground,
                leftBorderColor = leftAccentColor,
                leftBorderWidth = 4.dp
            )
            .clickable { onOpen() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Time Ago & Unread Dot (Left) + Priority Pill & Attachment Tag (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF0288D1), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = item.timeAgo,
                        fontSize = 12.5.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val (pFg, pText) = when (item.priority.uppercase()) {
                    "URGENTE" -> Pair(Color(0xFFDC2626), "URGENT")
                    "IMPORTANTE" -> Pair(Color(0xFFD97706), "IMPORTANT")
                    else -> Pair(Color(0xFF0288D1), "OFFICIEL")
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = null
                ) {
                    Text(
                        text = pText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = pFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Title
            Text(
                text = item.title,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                lineHeight = 22.sp
            )

            // Description snippet if available (placed above the file cadre)
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    fontSize = 13.sp,
                    maxLines = 3,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                    lineHeight = 18.sp
                )
            }

            // CADRE DU FICHIER
            if (hasAttachment) {
                Spacer(modifier = Modifier.height(12.dp))
                AnnouncementCadrePreview(
                    item = item,
                    onDownload = onDownload,
                    onOpen = onOpen
                )
            }

            // Author name at bottom right
            if (!item.authorName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = item.authorName,
                            fontSize = 11.5.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Formatage lisible de la taille des fichiers
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Fichier"
    if (bytes < 1024) return "$bytes o"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.US, "%.0f Ko", kb)
    val mb = kb / 1024.0
    return String.format(java.util.Locale.US, "%.1f Mo", mb)
}

/**
 * Cadre d'aperçu direct pour les fichiers joints :
 * - Image (.jpg, .png, etc.) : Affiche la photo directement dans le cadre avec dégradé, badge de type et boutons d'agrandissement et téléchargement
 * - PDF (.pdf) : Affiche une feuille de document officielle avec ruban rouge PDF, bouton d'ouverture et téléchargement
 * - Audio (.mp3, .wav) : Lecteur interactif avec waveforms et durée
 * - Tableur (.xlsx, .csv) : Aperçu de grille verte avec quotas
 * - Questionnaire (.gid) : Cadre officiel d'enquête avec lancement immédiat
 */
@Composable
fun AnnouncementCadrePreview(
    item: AnnouncementItem,
    onDownload: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    var selectedFileIndex by remember(item.id, item.files) {
        val visualIdx = item.files.indexOfFirst {
            it.nomFichier.endsWith(".pdf", true) ||
            it.typeMime == "application/pdf" ||
            it.nomFichier.endsWith(".jpg", true) ||
            it.nomFichier.endsWith(".jpeg", true) ||
            it.nomFichier.endsWith(".png", true) ||
            it.nomFichier.endsWith(".webp", true) ||
            it.typeMime?.startsWith("image/") == true
        }
        mutableStateOf(if (visualIdx >= 0) visualIdx else 0)
    }
    val currentFile = if (item.files.isNotEmpty() && selectedFileIndex in item.files.indices) {
        item.files[selectedFileIndex]
    } else null

    val rawImageSource = if (currentFile != null) {
        currentFile.dataUrl?.takeIf { it.isNotBlank() } ?: currentFile.cheminStorage
    } else {
        item.previewUrl
    }

    val resolvedPreviewUrl = remember(rawImageSource) {
        resolveStorageOrFileUrl(rawImageSource)
    }

    val resolvedImageModel = remember(rawImageSource) {
        if (!rawImageSource.isNullOrBlank()) {
            AnnouncementMediaHelper.resolveImageModel(context, rawImageSource)
        } else null
    }

    val activeFileName = currentFile?.nomFichier ?: item.fileName
    val activeMimeType = currentFile?.typeMime ?: item.fileMimeType
    val activeSizeBytes = if (currentFile != null && currentFile.tailleOctets > 0L) currentFile.tailleOctets else item.fileSizeBytes
    val activeImageResId = if (currentFile == null) item.imageResId else null

    val isPdf = activeMimeType == "application/pdf" ||
            activeFileName.endsWith(".pdf", ignoreCase = true) ||
            item.fileMimeType == "application/pdf" ||
            item.fileName.endsWith(".pdf", ignoreCase = true) ||
            (rawImageSource != null && (
                rawImageSource.contains(".pdf", ignoreCase = true) ||
                rawImageSource.startsWith("data:application/pdf") ||
                rawImageSource.startsWith("JVBERi")
            ))

    val isImage = !isPdf && (
            (activeImageResId != null) ||
            (resolvedImageModel != null) ||
            activeFileName.endsWith(".jpg", ignoreCase = true) ||
            activeFileName.endsWith(".jpeg", ignoreCase = true) ||
            activeFileName.endsWith(".png", ignoreCase = true) ||
            activeFileName.endsWith(".webp", ignoreCase = true) ||
            activeMimeType?.startsWith("image/") == true
    )

    var pdfFirstPageBitmap by remember(rawImageSource, activeFileName) {
        mutableStateOf(AnnouncementMediaHelper.getCachedPdfFirstPageBitmap(context, activeFileName))
    }
    var isPdfLoading by remember(rawImageSource, activeFileName) {
        mutableStateOf(isPdf && pdfFirstPageBitmap == null && !rawImageSource.isNullOrBlank())
    }
    LaunchedEffect(rawImageSource, activeFileName) {
        if (isPdf && pdfFirstPageBitmap == null && !rawImageSource.isNullOrBlank()) {
            isPdfLoading = true
            withContext(Dispatchers.IO) {
                pdfFirstPageBitmap = AnnouncementMediaHelper.renderPdfFirstPage(context, activeFileName, rawImageSource)
            }
            isPdfLoading = false
        }
    }

    val isAudio = activeMimeType?.startsWith("audio/") == true ||
            activeFileName.endsWith(".mp3", ignoreCase = true) ||
            activeFileName.endsWith(".wav", ignoreCase = true) ||
            activeFileName.endsWith(".m4a", ignoreCase = true)

    val isSpreadsheet = activeFileName.endsWith(".xls", ignoreCase = true) ||
            activeFileName.endsWith(".xlsx", ignoreCase = true) ||
            activeFileName.endsWith(".csv", ignoreCase = true) ||
            activeMimeType?.contains("excel", ignoreCase = true) == true ||
            activeMimeType?.contains("spreadsheet", ignoreCase = true) == true

    val isQuestionnaire = item.isQuestionnaire ||
            activeFileName.endsWith(".gid", ignoreCase = true) ||
            activeMimeType == "application/x-survey"

    Column(modifier = modifier.fillMaxWidth()) {
        // Multi-attachments switcher bar if announcement has multiple files
        if (item.files.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(item.files) { idx, f ->
                    val isSelected = idx == selectedFileIndex
                    val isImg = f.nomFichier.endsWith(".jpg", true) || f.nomFichier.endsWith(".png", true)
                    val isP = f.nomFichier.endsWith(".pdf", true)
                    Surface(
                        onClick = { selectedFileIndex = idx },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF0288D1).copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF0288D1) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isP) Icons.Default.PictureAsPdf else if (isImg) Icons.Default.Image else Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isP) Color(0xFFE11D48) else Color(0xFF0288D1),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = f.nomFichier,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF0288D1) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        when {
            isImage -> {
                // CADRE IMAGE : Affiche la photo directement dans le cadre (comme pour le PDF)
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clickable { onOpen() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color(0xFF0F172A),
                    shadowElevation = 2.5.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // 1. Fond visuel : photo réelle décodée
                        if (resolvedImageModel is Bitmap) {
                            Image(
                                bitmap = resolvedImageModel.asImageBitmap(),
                                contentDescription = activeFileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (resolvedImageModel != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(resolvedImageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = activeFileName,
                                placeholder = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                error = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (activeImageResId != null) {
                            Image(
                                painter = painterResource(id = activeImageResId),
                                contentDescription = activeFileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                contentDescription = activeFileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }





                        // 5. Bouton téléchargement en haut à droite (sober dark overlay)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(36.dp)
                                .pressScaleEffect(0.9f)
                                .clickable { onDownload() },
                            shape = CircleShape,
                            color = Color(0xCC1E293B),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Télécharger",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // 6. Nom du fichier en bas sur gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xD90F172A))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Photo,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeFileName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (activeSizeBytes > 0L) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatFileSize(activeSizeBytes),
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFBAE6FD),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            isPdf -> {
                // CADRE PDF : Affiche le document PDF dans le cadre exactement comme pour la photo
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clickable { onOpen() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    color = Color(0xFF0F172A),
                    shadowElevation = 2.5.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val renderedBmp = pdfFirstPageBitmap
                        if (renderedBmp != null) {
                            Image(
                                bitmap = renderedBmp.asImageBitmap(),
                                contentDescription = activeFileName,
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.TopCenter,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Cadre d'aperçu de document élégant
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxHeight(0.85f)
                                        .fillMaxWidth(0.68f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (isPdfLoading) {
                                            CircularProgressIndicator(
                                                color = Color(0xFFE11D48),
                                                strokeWidth = 2.5.dp,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Chargement du PDF...",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(Color(0xFFFFE4E6), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = null,
                                                    tint = Color(0xFFE11D48),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = activeFileName,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Document PDF",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }



                        // Bouton téléchargement en haut à droite (sober dark overlay)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(36.dp)
                                .pressScaleEffect(0.9f)
                                .clickable { onDownload() },
                            shape = CircleShape,
                            color = Color(0xCC1E293B),
                            shadowElevation = 2.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Télécharger",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Nom du fichier en bas sur gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xD90F172A))
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activeFileName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (activeSizeBytes > 0L) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formatFileSize(activeSizeBytes),
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFBAE6FD),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            isSpreadsheet -> {
                // CADRE FEUILLE DE CALCUL
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable { onOpen() },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeFileName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatFileSize(activeSizeBytes),
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onDownload() },
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            border = BorderStroke(0.8.dp, Color(0xFF86EFAC)),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Télécharger la feuille",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            isAudio -> {
                // CADRE AUDIO
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable { onOpen() },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E1B4B) else Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF4C1D95) else Color(0xFFDDD6FE)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDark) Color(0xFF2E1065) else Color(0xFFF5F3FF), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, if (isDark) Color(0xFF4C1D95) else Color(0xFFEDE9FE)), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Écouter",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val barHeights = listOf(14, 22, 10, 26, 18, 28, 12, 24, 16, 20, 8, 18, 24)
                                barHeights.forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(h.dp)
                                            .background(Color(0xFF8B5CF6).copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Surface(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { onDownload() },
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF3E8FF)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Télécharger l'audio",
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeFileName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6D28D9),
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            if (activeSizeBytes > 0L) {
                                Text(
                                    text = formatFileSize(activeSizeBytes),
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            isQuestionnaire -> {
                // CADRE QUESTIONNAIRE
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable { onOpen() },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE0F2FE),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF0288D1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeFileName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatFileSize(activeSizeBytes),
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onDownload() },
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                            border = BorderStroke(0.8.dp, Color(0xFFBAE6FD)),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Télécharger",
                                    tint = Color(0xFF0288D1),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // CADRE STANDARD (Document général)
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .clickable { onOpen() },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeFileName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatFileSize(activeSizeBytes),
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onDownload() },
                            shape = CircleShape,
                            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Télécharger",
                                    tint = Color(0xFF0288D1),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

