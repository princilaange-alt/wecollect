package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Utility for handling, compressing, and synchronizing photos and audio recordings
 * for field surveys to Supabase (Supabase Storage & Base64 Data URI).
 */
object MediaUtils {

    const val MAX_IMAGE_DIMENSION = 1280
    const val JPEG_COMPRESSION_QUALITY = 82

    /**
     * Checks whether the value represents a media asset (Image or Audio)
     */
    fun isMediaValue(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val v = value.trim()
        return v.startsWith("data:image/") ||
                v.startsWith("data:audio/") ||
                v.startsWith("http://") ||
                v.startsWith("https://") ||
                v.startsWith("content://") ||
                v.startsWith("/data/") ||
                isImageFile(v) ||
                isAudioFile(v)
    }

    /**
     * Checks whether the value represents an image
     */
    fun isImage(value: String?, questionType: String? = null, questionLabel: String? = null): Boolean {
        if (value.isNullOrBlank()) return false
        val v = value.trim()
        if (v.startsWith("data:image/")) return true
        if (isImageFile(v)) return true

        val qType = questionType?.trim()?.uppercase() ?: ""
        val qLabel = questionLabel?.trim()?.lowercase() ?: ""

        val typeMatches = qType in listOf("IMAGE", "PHOTO", "PICTURE", "CAMERA") ||
                qType.contains("IMAGE") || qType.contains("PHOTO") ||
                (qType == "TEXTE" && (qLabel.contains("photo") || qLabel.contains("sary") || qLabel.contains("image")))

        return typeMatches && (v.startsWith("data:image/") || v.startsWith("http") || File(v).exists())
    }

    /**
     * Checks whether the value represents an audio recording
     */
    fun isAudio(value: String?, questionType: String? = null, questionLabel: String? = null): Boolean {
        if (value.isNullOrBlank()) return false
        val v = value.trim()
        if (v.startsWith("data:audio/")) return true
        if (isAudioFile(v)) return true

        val qType = questionType?.trim()?.uppercase() ?: ""
        val qLabel = questionLabel?.trim()?.lowercase() ?: ""

        val typeMatches = qType in listOf("AUDIO", "VOCAL", "SON", "VOICE", "ENREGISTREMENT", "MICRO") ||
                qType.contains("AUDIO") || qType.contains("VOCAL") || qType.contains("SON") ||
                (qType == "TEXTE" && (qLabel.contains("audio") || qLabel.contains("vocal") || qLabel.contains("enregistrement") || qLabel.contains("feo") || qLabel.contains("peo")))

        return typeMatches && (v.startsWith("data:audio/") || v.startsWith("http") || File(v).exists())
    }

    fun isImageFile(pathOrName: String): Boolean {
        val lower = pathOrName.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
    }

    fun isAudioFile(pathOrName: String): Boolean {
        val lower = pathOrName.lowercase()
        return lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.endsWith(".aac") || lower.endsWith(".wav") || lower.endsWith(".3gp")
    }

    /**
     * Optimizes, auto-rotates (EXIF), scales down and saves image to internal storage.
     */
    suspend fun processAndSaveImage(
        context: Context,
        sourceUri: Uri,
        questionId: Long
    ): File? = withContext(Dispatchers.IO) {
        try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri) ?: return@withContext null

            // Read dimensions first
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            var inSampleSize = 1
            if (originalHeight > MAX_IMAGE_DIMENSION || originalWidth > MAX_IMAGE_DIMENSION) {
                val halfHeight = originalHeight / 2
                val halfWidth = originalWidth / 2
                while ((halfHeight / inSampleSize) >= MAX_IMAGE_DIMENSION || (halfWidth / inSampleSize) >= MAX_IMAGE_DIMENSION) {
                    inSampleSize *= 2
                }
            }

            // Decode scaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            inputStream = context.contentResolver.openInputStream(sourceUri)
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (sampledBitmap == null) return@withContext null

            // Read EXIF orientation
            var orientationDegrees = 0
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { exifInput ->
                    val exif = ExifInterface(exifInput)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    orientationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (_: Exception) {}

            // Scale to final max dimension
            val finalWidth: Int
            val finalHeight: Int
            val currentWidth = sampledBitmap.width
            val currentHeight = sampledBitmap.height

            if (currentWidth > MAX_IMAGE_DIMENSION || currentHeight > MAX_IMAGE_DIMENSION) {
                if (currentWidth >= currentHeight) {
                    finalWidth = MAX_IMAGE_DIMENSION
                    finalHeight = (currentHeight * (MAX_IMAGE_DIMENSION.toFloat() / currentWidth)).toInt()
                } else {
                    finalHeight = MAX_IMAGE_DIMENSION
                    finalWidth = (currentWidth * (MAX_IMAGE_DIMENSION.toFloat() / currentHeight)).toInt()
                }
            } else {
                finalWidth = currentWidth
                finalHeight = currentHeight
            }

            val scaledBitmap = if (finalWidth != currentWidth || finalHeight != currentHeight) {
                Bitmap.createScaledBitmap(sampledBitmap, finalWidth, finalHeight, true)
            } else {
                sampledBitmap
            }

            val finalBitmap = if (orientationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(orientationDegrees.toFloat()) }
                Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)
            } else {
                scaledBitmap
            }

            val destFile = File(context.filesDir, "survey_img_${questionId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, out)
            }

            if (scaledBitmap != sampledBitmap && !scaledBitmap.isRecycled) scaledBitmap.recycle()
            if (finalBitmap != sampledBitmap && !finalBitmap.isRecycled) finalBitmap.recycle()
            if (!sampledBitmap.isRecycled) sampledBitmap.recycle()

            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compresses bitmap from camera preview or custom capture
     */
    suspend fun saveBitmapCompressed(
        context: Context,
        bitmap: Bitmap,
        questionId: Long
    ): File = withContext(Dispatchers.IO) {
        val destFile = File(context.filesDir, "survey_img_${questionId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, out)
        }
        destFile
    }

    /**
     * Encodes a file on disk to a standard Base64 Data URI.
     */
    suspend fun fileToBase64DataUri(file: File, mimeType: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) return@withContext null
            val bytes = file.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:$mimeType;base64,$base64"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a Base64 Data URI into a cached local file so that MediaPlayer
     * or standard image tools can access it via local File path.
     */
    suspend fun decodeBase64ToCacheFile(
        context: Context,
        dataUri: String,
        prefix: String,
        extension: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!dataUri.startsWith("data:")) return@withContext null
            val commaIndex = dataUri.indexOf(',')
            if (commaIndex == -1) return@withContext null

            val base64Data = dataUri.substring(commaIndex + 1)
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)

            val cacheDir = File(context.cacheDir, "survey_media_cache").apply { mkdirs() }
            val fileName = "${prefix}_${Math.abs(dataUri.hashCode())}.$extension"
            val targetFile = File(cacheDir, fileName)

            if (!targetFile.exists() || targetFile.length() != bytes.size.toLong()) {
                FileOutputStream(targetFile).use { it.write(bytes) }
            }

            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resolves an audio file ready for local MediaPlayer playback.
     * Handles local files, Base64 Data URI, or cached network audio.
     */
    suspend fun resolveAudioFileForPlayback(context: Context, audioValue: String): File? = withContext(Dispatchers.IO) {
        if (audioValue.isBlank()) return@withContext null

        if (audioValue.startsWith("data:audio/")) {
            val ext = when {
                audioValue.startsWith("data:audio/mp4") || audioValue.startsWith("data:audio/m4a") -> "m4a"
                audioValue.startsWith("data:audio/mp3") || audioValue.startsWith("data:audio/mpeg") -> "mp3"
                audioValue.startsWith("data:audio/aac") -> "aac"
                audioValue.startsWith("data:audio/wav") -> "wav"
                else -> "m4a"
            }
            return@withContext decodeBase64ToCacheFile(context, audioValue, "audio", ext)
        }

        val localFile = File(audioValue)
        if (localFile.exists() && localFile.length() > 0) {
            return@withContext localFile
        }

        null
    }

    /**
     * Attempts to upload binary media directly to Supabase Storage bucket.
     * Returns the public URL if successful, or null if storage bucket is not configured.
     */
    suspend fun uploadToSupabaseStorage(
        client: OkHttpClient,
        supabaseUrl: String,
        supabaseKey: String,
        bucketName: String,
        storagePath: String,
        file: File,
        mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) return@withContext null

            val cleanUrl = supabaseUrl.trimEnd('/')
            val uploadUrl = "$cleanUrl/storage/v1/object/$bucketName/$storagePath"

            val body = file.asRequestBody(mimeType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                // Public storage URL
                "$cleanUrl/storage/v1/object/public/$bucketName/$storagePath"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Prepares media for real Supabase synchronization.
     * Tries Supabase Storage first, and if unavailable/forbidden, falls back to
     * standard Base64 Data URI which stores directly in the PostgreSQL table `reponses`.
     */
    suspend fun prepareMediaForSupabaseSync(
        client: OkHttpClient,
        supabaseUrl: String,
        supabaseKey: String,
        codeUnique: String,
        questionId: Long,
        valeur: String
    ): String = withContext(Dispatchers.IO) {
        if (valeur.isBlank()) return@withContext valeur

        // If it's already a Supabase public URL or data URI, keep it
        if (valeur.startsWith("http://") || valeur.startsWith("https://") || valeur.startsWith("data:")) {
            return@withContext valeur
        }

        val localFile = File(valeur)
        if (!localFile.exists() || localFile.length() == 0L) {
            return@withContext valeur
        }

        val isImg = isImageFile(localFile.name) || localFile.name.contains("img") || localFile.name.contains("photo")
        val isAud = isAudioFile(localFile.name) || localFile.name.contains("audio") || localFile.name.contains("vocal")

        val mimeType = when {
            isImg && localFile.name.endsWith(".png", true) -> "image/png"
            isImg -> "image/jpeg"
            isAud && (localFile.name.endsWith(".mp3", true)) -> "audio/mpeg"
            isAud -> "audio/mp4"
            else -> "application/octet-stream"
        }

        val storageFolder = if (isImg) "photos" else "audio"
        val storagePath = "enquetes/${codeUnique}_q${questionId}_${localFile.name}"

        // 1. Try Supabase Storage (bucket "surveys")
        val storageUrl = uploadToSupabaseStorage(
            client = client,
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey,
            bucketName = "surveys",
            storagePath = "$storageFolder/$storagePath",
            file = localFile,
            mimeType = mimeType
        )

        if (storageUrl != null) {
            return@withContext storageUrl
        }

        // 2. High-reliability Fallback: Base64 Data URI in PostgreSQL TEXT
        val dataUri = fileToBase64DataUri(localFile, mimeType)
        if (dataUri != null) {
            return@withContext dataUri
        }

        valeur
    }
}
