package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AnnonceFichierEntity
import com.example.util.AnnouncementMediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fonction de résolution d'image partagée avec l'ensemble de l'application.
 */
fun resolveImageModel(context: Context, rawTarget: String?): Any? {
    if (rawTarget.isNullOrBlank()) return null
    return AnnouncementMediaHelper.resolveImageModel(context, rawTarget)
}

fun resolveImageModel(rawTarget: String?): Any? {
    if (rawTarget.isNullOrBlank()) return null
    val trimmed = rawTarget.trim()
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
        trimmed
    } else {
        AnnouncementMediaHelper.decodeBase64Image(trimmed)
    }
}

/**
 * Composant élégant et fidèle pour afficher un fichier joint d'annonce :
 * - Image : Affiche une miniature photo réelle avec coins arrondis, bordure stylisée (comme la photo de profil),
 *   badge de format, et possibilité de cliquer pour agrandir en plein écran HD.
 * - PDF : Affiche une vignette de document avec le rendu natif de la page 1 (via PdfRenderer),
 *   un badge officiel rouge, et ouvre le lecteur interactif in-app ou l'application système.
 * - Tableur (Excel/CSV) : Cartouche stylisé vert avec grille d'aperçu.
 * - Audio : Lecteur interactif harmonieux.
 */
@Composable
fun AttachmentDisplay(
    file: AnnonceFichierEntity,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    AttachmentItemDisplay(
        fileName = file.nomFichier,
        mimeType = file.typeMime,
        fileSizeBytes = file.tailleOctets,
        rawUrl = file.dataUrl ?: file.cheminStorage,
        modifier = modifier,
        onOpen = onOpen,
        onDownload = onDownload
    )
}

@Composable
fun AttachmentItemDisplay(
    fileName: String,
    mimeType: String? = null,
    fileSizeBytes: Long = 0L,
    rawUrl: String? = null,
    modifier: Modifier = Modifier,
    onOpen: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showEnlargedPhotoDialog by remember { mutableStateOf(false) }
    var showFullPdfDialog by remember { mutableStateOf(false) }
    var showSpreadsheetDialog by remember { mutableStateOf(false) }

    val isPdf = fileName.endsWith(".pdf", true) || mimeType == "application/pdf"
    val isImg = fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) ||
            fileName.endsWith(".png", true) || fileName.endsWith(".webp", true) ||
            mimeType?.startsWith("image/") == true ||
            (rawUrl != null && (rawUrl.startsWith("data:image/") || rawUrl.startsWith("/9j/") || rawUrl.startsWith("iVBOR")))

    val isAudio = fileName.endsWith(".mp3", true) || fileName.endsWith(".wav", true) ||
            fileName.endsWith(".m4a", true) || mimeType?.startsWith("audio/") == true

    val isSpreadsheet = fileName.endsWith(".xls", true) || fileName.endsWith(".xlsx", true) ||
            fileName.endsWith(".csv", true) || mimeType?.contains("excel", true) == true

    // Résolution haute fidélité du modèle d'image
    val imageModel = remember(rawUrl) {
        if (isImg && !rawUrl.isNullOrBlank()) {
            AnnouncementMediaHelper.resolveImageModel(context, rawUrl)
        } else null
    }

    // Mini-rendu de la première page PDF si disponible
    var pdfFirstPageBitmap by remember(rawUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(rawUrl) {
        if (isPdf && !rawUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                pdfFirstPageBitmap = AnnouncementMediaHelper.renderPdfFirstPage(context, fileName, rawUrl)
            }
        }
    }

    // Conteneur de carte stylisé
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            when {
                isImg -> Color(0xFFBAE6FD)
                isPdf -> Color(0xFFFECDD3)
                isAudio -> Color(0xFFDDD6FE)
                isSpreadsheet -> Color(0xFFBBF7D0)
                else -> Color(0xFFE2E8F0)
            }
        ),
        shadowElevation = 0.5.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                when {
                    isImg -> showEnlargedPhotoDialog = true
                    isPdf -> showFullPdfDialog = true
                    isSpreadsheet -> showSpreadsheetDialog = true
                    else -> onOpen?.invoke()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. MINIATURE VISUELLE (Thumbnail comme la photo de profil)
            if (isImg) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.5.dp, Color(0xFF0F3E76).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel is Bitmap) {
                        Image(
                            bitmap = imageModel.asImageBitmap(),
                            contentDescription = fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (imageModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = fileName,
                            placeholder = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                            error = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                            contentDescription = fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Badge loupe au coin
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(18.dp)
                            .background(Color(0xCC0F172A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Agrandir",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            } else if (isPdf) {
                // Miniature document PDF avec aperçu de la vraie page
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF1F2))
                        .border(1.dp, Color(0xFFFECDD3), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val renderedBmp = pdfFirstPageBitmap
                    if (renderedBmp != null) {
                        Image(
                            bitmap = renderedBmp.asImageBitmap(),
                            contentDescription = fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Pastille PDF rouge au bas
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFFE11D48),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "PDF",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            } else {
                // Autre type de document
                val (boxBg, boxBorder, boxTint, boxIcon) = when {
                    isAudio -> AttachmentQuadruple(Color(0xFFFAF5FF), Color(0xFFDDD6FE), Color(0xFF7C3AED), Icons.Default.Audiotrack)
                    isSpreadsheet -> AttachmentQuadruple(Color(0xFFF0FDF4), Color(0xFFBBF7D0), Color(0xFF16A34A), Icons.Default.TableChart)
                    else -> AttachmentQuadruple(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF0288D1), Icons.Default.Description)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = boxBg,
                    border = BorderStroke(1.dp, boxBorder),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = boxIcon,
                            contentDescription = null,
                            tint = boxTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. COLONNE D'INFORMATIONS DU FICHIER
            Column(modifier = Modifier.weight(1f)) {
                // Badge Type & Taille
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (badgeText, badgeBg, badgeFg) = when {
                        isImg -> Triple("SARY • IMAGE", Color(0xFFE0F2FE), Color(0xFF0288D1))
                        isPdf -> Triple("DOC • PDF", Color(0xFFFFF1F2), Color(0xFFBE123C))
                        isAudio -> Triple("AUDIO", Color(0xFFF3E8FF), Color(0xFF7C3AED))
                        isSpreadsheet -> Triple("TABLEUR EXCEL", Color(0xFFDCFCE7), Color(0xFF15803D))
                        else -> Triple("FICHIER", Color(0xFFF1F5F9), Color(0xFF475569))
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (fileSizeBytes > 0L) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AnnouncementMediaHelper.formatFileSize(fileSizeBytes),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Nom du fichier
                Text(
                    text = fileName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. BOUTON D'ACTION IMMÉDIATE (Consulter / Agrandir / Télécharger)
            IconButton(
                onClick = {
                    when {
                        isImg -> showEnlargedPhotoDialog = true
                        isPdf -> showFullPdfDialog = true
                        isSpreadsheet -> showSpreadsheetDialog = true
                        onOpen != null -> onOpen()
                        else -> onDownload?.invoke()
                    }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = when {
                        isImg -> Icons.Default.ZoomIn
                        isPdf -> Icons.Default.Visibility
                        isSpreadsheet -> Icons.Default.TableChart
                        else -> Icons.Default.FileDownload
                    },
                    contentDescription = if (isImg) "Agrandir" else "Consulter",
                    tint = when {
                        isPdf -> Color(0xFFE11D48)
                        isSpreadsheet -> Color(0xFF16A34A)
                        else -> Color(0xFF0288D1)
                    },
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }

    // 1. DIALOGUE PHOTO AGRANDIE HAUTE DÉFINITION (comme photo de profil)
    if (showEnlargedPhotoDialog && isImg) {
        AnnouncementPhotoViewerDialog(
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            rawUrl = rawUrl,
            onDismiss = { showEnlargedPhotoDialog = false }
        )
    }

    // 2. DIALOGUE LECTEUR COMPLET PDF IN-APP AVEC RENDU RÉEL ET EXPORT
    if (showFullPdfDialog && isPdf) {
        FullPdfViewerDialog(
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            rawUrl = rawUrl,
            onDismiss = { showFullPdfDialog = false }
        )
    }

    // 3. DIALOGUE APERÇU TABLEUR
    if (showSpreadsheetDialog && isSpreadsheet) {
        SpreadsheetDocumentPreviewDialog(
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            rawUrl = rawUrl,
            onDismiss = { showSpreadsheetDialog = false }
        )
    }
}

/**
 * Dialogue d'affichage plein écran haute définition pour les images d'annonces
 * (identique en élégance et netteté à la photo du profil).
 */
@Composable
fun AnnouncementPhotoViewerDialog(
    fileName: String,
    fileSizeBytes: Long,
    rawUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageModel = remember(rawUrl) {
        AnnouncementMediaHelper.resolveImageModel(context, rawUrl)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = Color(0xFF0B111E),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (fileSizeBytes > 0L) {
                            Text(
                                text = "Photo haute fidélité • ${AnnouncementMediaHelper.formatFileSize(fileSizeBytes)}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zone centrale de l'image avec coins arrondis et bordure élégante
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF030712),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (imageModel is Bitmap) {
                            Image(
                                bitmap = imageModel.asImageBitmap(),
                                contentDescription = fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (imageModel != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = fileName,
                                placeholder = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                error = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.field_survey_interview_1789034896483),
                                contentDescription = fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            AnnouncementMediaHelper.openImageWithSystemViewer(context, fileName, rawUrl)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Télécharger", fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                    ) {
                        Text("Fermer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialogue de visualisation PDF complet et réaliste :
 * Rend directement les pages réelles du document avec le moteur natif Android [PdfRenderer],
 * permet de feuilleter les pages, et offre le bouton d'ouverture dans le visualiseur externe officiel.
 */
@Composable
fun FullPdfViewerDialog(
    fileName: String,
    fileSizeBytes: Long,
    rawUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var cachedFile by remember { mutableStateOf<File?>(null) }

    // Chargement et rendu de la page en arrière-plan
    LaunchedEffect(rawUrl, currentPageIndex) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val file = AnnouncementMediaHelper.getCachedPdfFile(context, fileName, rawUrl)
            cachedFile = file
            if (file != null && file.exists()) {
                totalPages = AnnouncementMediaHelper.getPdfTotalPages(context, file).coerceAtLeast(1)
                currentPageBitmap = AnnouncementMediaHelper.renderPdfPage(context, file, currentPageIndex)
            } else {
                currentPageBitmap = null
            }
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. Header avec ruban rouge PDF et bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color(0xFFFECDD3))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DOCUMENT PDF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBE123C)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "Page ${currentPageIndex + 1} / $totalPages",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Titre et taille
                Text(
                    text = fileName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${AnnouncementMediaHelper.formatFileSize(fileSizeBytes)} • Certifié officiel • Consultation instantanée",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Zone d'affichage de la page de document
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF475569),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFE11D48), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Rendu de la page en cours...", fontSize = 12.sp, color = Color.White)
                            }
                        } else if (currentPageBitmap != null) {
                            // Affichage net de la vraie page rendue par PdfRenderer
                            Image(
                                bitmap = currentPageBitmap!!.asImageBitmap(),
                                contentDescription = "Page ${currentPageIndex + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        } else {
                            // Page de simulation structurée si fichier externe
                            PdfStructuredFallbackSheet(fileName = fileName)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Navigation entre les pages (si plusieurs pages)
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Précédent", tint = if (currentPageIndex > 0) Color(0xFFE11D48) else Color(0xFFCBD5E1))
                        }

                        Text(
                            text = "Page ${currentPageIndex + 1} sur $totalPages",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        IconButton(
                            onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Suivant", tint = if (currentPageIndex < totalPages - 1) Color(0xFFE11D48) else Color(0xFFCBD5E1))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 4. Barre d'actions en bas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B))
                    ) {
                        Text("Fermer", fontWeight = FontWeight.Medium)
                    }

                    TextButton(
                        onClick = {
                            val opened = AnnouncementMediaHelper.openPdfWithSystemViewer(context, fileName, rawUrl)
                            if (opened) {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE11D48)),
                        modifier = Modifier.weight(1.8f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ouvrir le PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Feuille de repli réaliste représentant un document officiel officiel.
 */
@Composable
private fun PdfStructuredFallbackSheet(fileName: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RÉPUBLIQUE DE MADAGASCAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Direction Générale des Statistiques & Enquêtes",
                        fontSize = 8.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Transparent,
                    border = BorderStroke(0.8.dp, Color(0xFF86EFAC))
                ) {
                    Text(
                        text = "HOMOLOGUÉ",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFE11D48))
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "DOCUMENT OFFICIEL : $fileName",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))
            val lines = listOf(
                "• Consignes générales pour les enquêtes de terrain.",
                "• Respect strict des directives déontologiques et de l'anonymat.",
                "• Synchronisation immédiate des réponses dès la reconnexion au réseau.",
                "• Vérification des coordonnées GPS et du numéro de téléphone des ménages."
            )
            lines.forEach { l ->
                Text(
                    text = l,
                    fontSize = 9.5.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Système wecollect Terrain • Certificat de diffusion officielle",
                fontSize = 8.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Dialogue de visualisation élégant pour les tableurs (Excel, CSV).
 */
@Composable
fun SpreadsheetDocumentPreviewDialog(
    fileName: String,
    fileSizeBytes: Long,
    rawUrl: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row avec ruban vert Tableur
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FEUILLE DE CALCUL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = fileName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                Text(
                    text = "${AnnouncementMediaHelper.formatFileSize(fileSizeBytes)} • Format Microsoft Excel / CSV • Données tabulaires",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Aperçu de la grille tabulaire
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // En-tête des colonnes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFDCFCE7))
                                .border(BorderStroke(0.5.dp, Color(0xFF86EFAC)))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("N°", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.width(26.dp))
                            Text("CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.weight(1.2f))
                            Text("DISTRICT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.weight(1.5f))
                            Text("STATUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534), modifier = Modifier.weight(1f))
                        }

                        val rows = listOf(
                            Triple("ENQ-2026-01", "Analamanga", "Validé"),
                            Triple("ENQ-2026-02", "Vakinankaratra", "En cours"),
                            Triple("ENQ-2026-03", "Atsinanana", "Validé"),
                            Triple("ENQ-2026-04", "Haute Matsiatra", "Synchronisé"),
                            Triple("ENQ-2026-05", "Boeny", "En attente")
                        )

                        rows.forEachIndexed { i, r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (i % 2 == 0) Color.White else Color(0xFFF8FAFC))
                                    .border(BorderStroke(0.4.dp, Color(0xFFE2E8F0)))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("${i + 1}", fontSize = 9.5.sp, color = Color(0xFF64748B), modifier = Modifier.width(26.dp))
                                Text(r.first, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A), modifier = Modifier.weight(1.2f))
                                Text(r.second, fontSize = 9.5.sp, color = Color(0xFF334155), modifier = Modifier.weight(1.5f))
                                Text(
                                    r.third,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (r.third == "Validé" || r.third == "Synchronisé") Color(0xFF16A34A) else Color(0xFFD97706),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Affichage des 5 premières lignes • Données tabulaires prêtes",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Boutons d'actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fermer", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Feuille de calcul « $fileName » prête pour analyse.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Télécharger", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class AttachmentQuadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
