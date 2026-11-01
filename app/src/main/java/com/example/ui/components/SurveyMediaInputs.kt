package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.util.MediaUtils
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Image Input Component:
 * Strictly NO text form.
 * Direct choice to pick from device memory / storage (Galerie / Fichiers)
 * or take picture directly with camera.
 */
@Composable
fun SurveyImageInput(
    questionId: Long,
    value: String,
    onImageChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showEnlargedPreview by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Direct Picker from device memory / gallery
    val memoryImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val processedFile = MediaUtils.processAndSaveImage(context, uri, questionId)
                    if (processedFile != null) {
                        onImageChanged(processedFile.absolutePath)
                    } else {
                        Toast.makeText(context, "Impossible de charger l'image", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erreur d'importation : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Full-resolution Camera capture launcher via FileProvider
    val fullCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            coroutineScope.launch {
                try {
                    val processedFile = MediaUtils.processAndSaveImage(context, tempCameraUri!!, questionId)
                    if (processedFile != null) {
                        onImageChanged(processedFile.absolutePath)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erreur photo : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fallback direct preview launcher
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                try {
                    val file = MediaUtils.saveBitmapCompressed(context, bitmap, questionId)
                    onImageChanged(file.absolutePath)
                } catch (e: Exception) {
                    Toast.makeText(context, "Erreur d'enregistrement : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchCamera() {
        try {
            val tempFile = File(context.cacheDir, "temp_cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            tempCameraUri = uri
            fullCameraLauncher.launch(uri)
        } catch (_: Exception) {
            cameraPreviewLauncher.launch(null)
        }
    }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "Autorisation Caméra requise pour prendre une photo", Toast.LENGTH_LONG).show()
        }
    }

    fun startPickFromMemory() {
        memoryImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun startDirectCamera() {
        val hasCamPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCamPermission) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("survey_image_input_$questionId")
    ) {
        if (value.isBlank()) {
            // Direct Choices (Tonga dia safidy mivantana tsy misy form text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Choice 1: Pick from device memory / storage
                FilledTonalButton(
                    onClick = { startPickFromMemory() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_pick_image_memory_$questionId"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importer depuis l'appareil", style = MaterialTheme.typography.labelMedium)
                }

                // Choice 2: Direct Camera
                OutlinedButton(
                    onClick = { startDirectCamera() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_camera_$questionId"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Prendre photo", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sélectionnez une image enregistrée sur l'appareil ou prenez une photo en direct.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            // Image already chosen
            val file = remember(value) { File(value) }
            val fileName = file.name
            val fileSizeKb = if (file.exists()) file.length() / 1024 else 0L

            val imgModel: Any = remember(value) {
                when {
                    value.startsWith("content://") -> Uri.parse(value)
                    value.startsWith("data:") || value.startsWith("http://") || value.startsWith("https://") -> value
                    else -> file
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showEnlargedPreview = true }
                ) {
                    AsyncImage(
                        model = imgModel,
                        contentDescription = "Sary voafidy",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )

                    // Overlay badge: click to enlarge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Agrandir",
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Agrandir",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Sary voaray (Mémoire)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusSuccess
                            )
                            Text(
                                text = if (fileSizeKb > 0) "$fileName ($fileSizeKb Ko)" else fileName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { startPickFromMemory() },
                            modifier = Modifier.testTag("btn_replace_photo_memory_$questionId")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mémoire", style = MaterialTheme.typography.labelMedium)
                        }

                        IconButton(
                            onClick = { onImageChanged("") },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_delete_photo_$questionId")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Fafana",
                                tint = StatusError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEnlargedPreview && value.isNotBlank()) {
        Dialog(onDismissRequest = { showEnlargedPreview = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val enlargedModel: Any = remember(value) {
                        when {
                            value.startsWith("content://") -> Uri.parse(value)
                            value.startsWith("data:") || value.startsWith("http://") || value.startsWith("https://") -> value
                            else -> File(value)
                        }
                    }
                    AsyncImage(
                        model = enlargedModel,
                        contentDescription = "Sary lehibe",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showEnlargedPreview = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Fermer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Audio Input Component:
 * Strictly NO text form.
 * Direct choice to pick an audio file directly from device memory / storage
 * or record directly with the microphone.
 */
@Composable
fun SurveyAudioInput(
    questionId: Long,
    value: String,
    onAudioChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Direct Picker from device memory / storage for Audio files
    val memoryAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val originalName = queryFileName(context, uri) ?: "audio_${System.currentTimeMillis()}.mp3"
                val ext = originalName.substringAfterLast('.', "mp3")
                val destFile = File(context.filesDir, "survey_audio_${questionId}_${System.currentTimeMillis()}.$ext")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                onAudioChanged(destFile.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur de sélection audio : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Recording State
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var activeAudioFile by remember { mutableStateOf<File?>(null) }

    // Playback State
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(0) }

    LaunchedEffect(value) {
        if (value.isNotBlank()) {
            coroutineScope.launch {
                val file = MediaUtils.resolveAudioFileForPlayback(context, value)
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
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSeconds++
            }
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
                mediaRecorder?.release()
            } catch (_: Exception) {}
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {}
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        activeAudioFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                onAudioChanged(file.absolutePath)
            }
        }
    }

    fun startRecording() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false

            val file = File(context.filesDir, "survey_audio_${questionId}_${System.currentTimeMillis()}.mp4")
            activeAudioFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
            Toast.makeText(context, "Erreur du micro : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(context, "Autorisation Micro requise pour l'enregistrement audio", Toast.LENGTH_LONG).show()
        }
    }

    fun requestStartRecording() {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            startRecording()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun togglePlayback() {
        if (value.isBlank()) return
        coroutineScope.launch {
            if (isPlaying) {
                mediaPlayer?.pause()
                isPlaying = false
            } else {
                if (mediaPlayer == null) {
                    val audioFile = MediaUtils.resolveAudioFileForPlayback(context, value)
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
                            Toast.makeText(context, "Erreur de lecture audio : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    } else {
                        Toast.makeText(context, "Fichier audio introuvable", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }
                mediaPlayer?.start()
                isPlaying = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("survey_audio_input_$questionId")
    ) {
        if (isRecording) {
            val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .scale(pulseScale)
                            .background(StatusError, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Enregistrement en cours...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusError
                        )
                        Text(
                            text = formatDurationSeconds(recordingDurationSeconds),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { stopRecording() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StatusError.copy(alpha = 0.15f),
                        contentColor = StatusError
                    ),
                    modifier = Modifier.testTag("btn_stop_audio_$questionId")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Arrêter",
                        tint = StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Arrêter", fontWeight = FontWeight.Bold)
                }
            }
        } else if (value.isBlank()) {
            // Direct Choices (Tonga dia safidy mivantana tsy misy form text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Choice 1: Direct pick audio from device memory
                FilledTonalButton(
                    onClick = { memoryAudioLauncher.launch("audio/*") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_pick_audio_memory_$questionId"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importer depuis l'appareil", style = MaterialTheme.typography.labelMedium)
                }

                // Choice 2: Direct record
                OutlinedButton(
                    onClick = { requestStartRecording() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_start_record_$questionId"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enregistrer (Micro)", style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sélectionnez un fichier audio de l'appareil ou effectuez un enregistrement au micro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            // Audio attached (picked from memory or recorded)
            val file = remember(value) { File(value) }
            val fileName = if (value.startsWith("http://") || value.startsWith("https://")) "Supabase Cloud Audio" else if (value.startsWith("data:")) "Supabase Data Audio" else file.name
            val progress = if (totalDurationMs > 0) {
                (playbackPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { togglePlayback() },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_play_audio_$questionId")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Lire",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enregistrement audio prêt",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusSuccess
                            )
                            Text(
                                text = if (totalDurationMs > 0) {
                                    "${formatMs(playbackPositionMs)} / ${formatMs(totalDurationMs)}"
                                } else {
                                    "00:00"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Re-pick from memory
                    IconButton(
                        onClick = {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            isPlaying = false
                            memoryAudioLauncher.launch("audio/*")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_repick_audio_memory_$questionId")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Sélectionner un autre fichier audio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            isPlaying = false
                            onAudioChanged("")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_delete_audio_$questionId")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Fafana",
                            tint = StatusError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = it.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Exception) {}
    }
    val path = uri.path
    val cut = path?.lastIndexOf('/') ?: -1
    return if (cut != -1) path?.substring(cut + 1) else null
}

private fun formatDurationSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
