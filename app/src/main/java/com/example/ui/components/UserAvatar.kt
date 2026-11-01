package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Shield
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.UserEntity
import com.example.data.util.MediaUtils
import kotlinx.coroutines.launch
import java.io.File

/**
 * EnqueteurAvatar:
 * Mampiseho ny sarin'ny mpanao fanadihadiana (Enquêteur photo)
 * avy amin'ny Supabase (`photo_url` na `photo`) na avy eo an-toerana (Local storage / SQLite).
 */
@Composable
fun EnqueteurAvatar(
    photoUrl: String?,
    photo: String?,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    showBorder: Boolean = false,
    borderColor: Color = Color.White,
    borderWidth: Dp = 2.dp,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val rawTarget = remember(photoUrl, photo) {
        val target = photoUrl?.trim()?.takeIf { it.isNotBlank() }
            ?: photo?.trim()?.takeIf { it.isNotBlank() }
        target
    }

    val imgModel: Any = remember(rawTarget) {
        val target = rawTarget?.takeIf { it.isNotBlank() } ?: com.example.data.util.RealUserPhotos.USER_2_PHOTO
        when {
            target.startsWith("content://") -> Uri.parse(target)
            target.startsWith("file://") -> Uri.parse(target)
            target.startsWith("http://", ignoreCase = true) || target.startsWith("https://", ignoreCase = true) -> target
            target.startsWith("/storage/v1/") -> "https://hcqhrzdskyzdqzdrhisc.supabase.co$target"
            target.startsWith("storage/v1/") -> "https://hcqhrzdskyzdqzdrhisc.supabase.co/$target"
            target.startsWith("data:image/") || target.startsWith("/9j/") || target.startsWith("iVBOR") || (!target.startsWith("/") && target.length > 50) -> {
                com.example.util.AnnouncementMediaHelper.decodeBase64Image(target) ?: R.drawable.photo_enqueteur_reel
            }
            target.startsWith("/") && File(target).exists() -> File(target)
            else -> R.drawable.photo_enqueteur_reel
        }
    }

    val baseModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(if (showBorder) Modifier.border(borderWidth, borderColor, CircleShape) else Modifier)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .background(Color(0xFFE2E8F0))

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imgModel)
                .crossfade(true)
                .build(),
            contentDescription = "Photo réelle de l'enquêteur",
            placeholder = painterResource(id = R.drawable.photo_enqueteur_reel),
            error = painterResource(id = R.drawable.photo_enqueteur_reel),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * EditableEnqueteurAvatar:
 * Mampiseho ny sary sady mamela ny mpampiasa hanova na hampiditra ny sariny
 * izay mirakitra sy mandefa izany any amin'ny Supabase (tsanganana `photo` sy `photo_url`).
 */
@Composable
fun EditableEnqueteurAvatar(
    user: UserEntity?,
    isUpdating: Boolean = false,
    onPhotoUpdated: (String) -> Unit,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSavingLocally by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isSavingLocally = true
                val savedFile = MediaUtils.processAndSaveImage(context, uri, 9999L)
                isSavingLocally = false
                if (savedFile != null && savedFile.exists()) {
                    val base64 = try {
                        val bytes = savedFile.readBytes()
                        "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    } catch (_: Exception) {
                        savedFile.absolutePath
                    }
                    onPhotoUpdated(base64)
                } else {
                    onPhotoUpdated(uri.toString())
                }
            }
        }
    }

    val isSurveyor = !user?.role.equals("ADMIN", ignoreCase = true)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        EnqueteurAvatar(
            photoUrl = user?.photoUrl,
            photo = user?.photo,
            size = size,
            showBorder = true,
            borderColor = Color(0xFF0F3E76),
            borderWidth = 2.dp,
            onClick = {
                if (isSurveyor) {
                    Toast.makeText(context, "La photo de profil est officielle et ne peut pas être modifiée par l'enquêteur.", Toast.LENGTH_SHORT).show()
                } else if (!isUpdating && !isSavingLocally) {
                    photoPickerLauncher.launch("image/*")
                }
            }
        )

        // Status action badge at bottom-right (Shield/Lock for surveyors, Camera for Admin)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size((size.value * 0.38f).coerceAtLeast(26f).dp)
                .clip(CircleShape)
                .background(Color(0xFF0F3E76))
                .border(2.dp, Color.White, CircleShape)
                .clickable {
                    if (isSurveyor) {
                        Toast.makeText(context, "La photo de profil est officielle et ne peut pas être modifiée par l'enquêteur.", Toast.LENGTH_SHORT).show()
                    } else if (!isUpdating && !isSavingLocally) {
                        photoPickerLauncher.launch("image/*")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isUpdating || isSavingLocally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = Color.White,
                    strokeWidth = 1.5.dp
                )
            } else if (isSurveyor) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Photo officielle vérifiée",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.20f).coerceAtLeast(13f).dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Changer la photo de l'enquêteur",
                    tint = Color.White,
                    modifier = Modifier.size((size.value * 0.20f).coerceAtLeast(13f).dp)
                )
            }
        }
    }
}
