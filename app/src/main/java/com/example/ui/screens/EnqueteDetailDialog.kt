package com.example.ui.screens

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.EnqueteDetail
import com.example.data.model.ReponseWithQuestion
import com.example.data.util.GpsLocationHelper
import com.example.data.util.MediaUtils
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.pressScaleEffect
import com.example.ui.theme.StatusError
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EnqueteDetailDialog(
    detail: EnqueteDetail,
    payloadPreview: String = "",
    onDismiss: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

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
            shape = RoundedCornerShape(20.dp)
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

    val hasRespondentName = rawNom.isNotBlank() &&
        !rawNom.equals("personne enquêtée", ignoreCase = true) &&
        !rawNom.equals("respondant", ignoreCase = true) &&
        !rawNom.startsWith("ENQ-", ignoreCase = true)

    val displayTitle = if (hasRespondentName) rawNom else cleanCode
    val qTitre = detail.questionnaireTitre.ifBlank { "Questionnaire #${detail.enquete.idQuestionnaire}" }
    val dateEnq = formatSurveyDate(detail.enquete.dateEnquete)
    val enqueteur = detail.enqueteurNom.ifBlank { "Non renseigné" }
    val isSynced = detail.enquete.syncStatus == "SYNCHRONISEE"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .widthIn(max = 620.dp)
                .fillMaxHeight(0.89f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Sleek Gradient Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0A2E5C),
                                    Color(0xFF0F3E76),
                                    Color(0xFF0284C7)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "FICHE D'ENQUÊTE",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val initials = displayTitle
                                .split(" ")
                                .filter { it.isNotBlank() }
                                .take(2)
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .joinToString("")
                                .ifBlank { "EQ" }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasRespondentName) {
                                    Text(
                                        text = initials,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(cleanCode))
                                            Toast.makeText(context, "Code copié : $cleanCode", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = cleanCode,
                                                fontSize = 11.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copier le code",
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    // Status & Metadata Summary Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Informations générales",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    SyncStatusBadge(
                                        status = detail.enquete.syncStatus,
                                        isCompact = true
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                DetailRowItem(
                                    icon = Icons.AutoMirrored.Filled.Article,
                                    label = "Questionnaire",
                                    value = qTitre
                                )
                                DetailRowItem(
                                    icon = Icons.Default.CalendarToday,
                                    label = "Date de l'enquête",
                                    value = dateEnq
                                )
                                DetailRowItem(
                                    icon = Icons.Default.PersonOutline,
                                    label = "Enquêteur",
                                    value = enqueteur
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Réponses enregistrées",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "${detail.reponses.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (detail.reponses.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Aucune réponse enregistrée dans la base pour cette enquête.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(detail.reponses.size) { idx ->
                            val rep = detail.reponses[idx]
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    QuestionAnswerCard(item = rep)
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }
                }

                // 3. Footer Action Bar
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.pressScaleEffect(0.95f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = StatusError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Supprimer", color = StatusError, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isSynced) {
                            FilledTonalButton(
                                onClick = onSync,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.pressScaleEffect(0.95f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Synchroniser", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.pressScaleEffect(0.95f)
                        ) {
                            Text("Fermer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

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
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.1f)
            )
        }
    }
}

fun selectIconForQuestion(questionLibelle: String): ImageVector {
    val q = questionLibelle.lowercase()
    return when {
        q.contains("gps") || q.contains("localisation") || q.contains("géolocalisation") || q.contains("coordonnées") || q.contains("toerana") -> Icons.Default.LocationOn
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
    item: ReponseWithQuestion
) {
    val qLabelClean = item.question.libelle.replace("*", "").trim()
    val qType = item.question.type.uppercase()
    val qLabel = item.question.libelle.lowercase()
    val valStr = item.reponse.valeur ?: item.option?.libelle

    val isImg = (qType in listOf("IMAGE", "PHOTO", "PICTURE") || qType.contains("IMAGE") || qType.contains("PHOTO") || qLabel.contains("photo") || qLabel.contains("sary") || qLabel.contains("image"))
    val isAud = (qType in listOf("AUDIO", "VOCAL", "SON", "ENREGISTREMENT") || qType.contains("AUDIO") || qType.contains("VOCAL") || qType.contains("SON") || qLabel.contains("audio") || qLabel.contains("vocal") || qLabel.contains("enregistrement") || qLabel.contains("feo"))
    val questionIcon = selectIconForQuestion(qLabelClean)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = questionIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = qLabelClean,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isImg && !valStr.isNullOrBlank()) {
            var showFullscreenPhoto by remember { mutableStateOf(false) }

            val imgModel: Any = remember(valStr) {
                when {
                    valStr.startsWith("content://") -> Uri.parse(valStr)
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
                        .clip(RoundedCornerShape(10.dp))
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

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Photo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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

            LaunchedEffect(valStr) {
                val file = MediaUtils.resolveAudioFileForPlayback(context, valStr)
                if (file != null && file.exists()) {
                    try {
                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(file.absolutePath)
                        val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        totalDurationMs = durStr?.toIntOrNull() ?: 0
                        mmr.release()
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
                                    Toast.makeText(context, "Erreur lecture audio : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                            } else {
                                Toast.makeText(context, "Fichier audio introuvable ou en cours de préparation", Toast.LENGTH_SHORT).show()
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                IconButton(
                    onClick = { togglePlayback() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
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
                        color = MaterialTheme.colorScheme.primary,
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
            val rawAnswer = item.reponse.valeur?.takeIf { it.isNotBlank() } ?: item.option?.libelle
            val gpsCoords = GpsLocationHelper.parseCoordinates(rawAnswer)
            val displayVal = when {
                gpsCoords != null -> "Lat: ${gpsCoords.latitude}, Lon: ${gpsCoords.longitude} (±${gpsCoords.accuracy}m)"
                !rawAnswer.isNullOrBlank() -> rawAnswer
                else -> "(Non renseigné)"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (gpsCoords != null) Icons.Default.LocationOn else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (rawAnswer.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color(0xFF10B981),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = displayVal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (rawAnswer.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
