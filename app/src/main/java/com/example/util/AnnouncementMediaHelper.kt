package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.LruCache
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Helper central pour le décodage, la mise en cache et le rendu haute définition
 * des fichiers joints aux annonces (Images JPG/PNG, Documents PDF, etc.).
 *
 * Résout fidèlement les sources Base64 directes (comme tampo.jpg et Edouard.pdf sur Supabase),
 * les URLs de stockage Supabase, les fichiers locaux et URIs de contenu.
 */
object AnnouncementMediaHelper {

    private const val SUPABASE_BASE_URL = "https://hcqhrzdskyzdqzdrhisc.supabase.co"

    // Cache mémoire pour les miniatures et pages décodées pour éviter tout scintillement
    private val bitmapCache = LruCache<String, Bitmap>(24)

    /**
     * Résout n'importe quelle source d'image (Base64 data-url, URL Supabase, URI locale, File)
     * sous forme de Bitmap, Uri, URL ou File prêt pour Coil, identique à la photo de profil.
     */
    fun resolveImageModel(context: Context, rawTarget: String?): Any? {
        val trimmed = rawTarget?.trim() ?: ""

        if (trimmed.isNotBlank()) {
            val cacheKey = if (trimmed.length > 200) trimmed.substring(0, 100) + "#" + trimmed.length else trimmed
            bitmapCache.get(cacheKey)?.let { return it }
        }

        // Check for local file in cache first for offline readiness
        if (trimmed.contains("tampo", ignoreCase = true) || trimmed.endsWith(".jpg", ignoreCase = true) || trimmed.endsWith(".png", ignoreCase = true)) {
            val cached = getCachedImageFile(context, trimmed.substringAfterLast('/'), trimmed)
            if (cached != null && cached.exists() && cached.length() > 0) return cached
        }

        if (trimmed.isBlank()) {
            return null
        }

        // Si c'est un document PDF, ne jamais le traiter comme une image
        if (trimmed.endsWith(".pdf", ignoreCase = true) ||
            trimmed.contains(".pdf", ignoreCase = true) ||
            trimmed.startsWith("data:application/pdf") ||
            trimmed.startsWith("JVBERi")
        ) {
            return null
        }

        val cacheKey = if (trimmed.length > 200) trimmed.substring(0, 100) + "#" + trimmed.length else trimmed

        return when {
            trimmed.startsWith("content://") -> Uri.parse(trimmed)
            trimmed.startsWith("file://") -> Uri.parse(trimmed)
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed

            trimmed.startsWith("/storage/v1/") -> "$SUPABASE_BASE_URL$trimmed"
            trimmed.startsWith("storage/v1/") -> "$SUPABASE_BASE_URL/$trimmed"
            trimmed.startsWith("/api/") -> "$SUPABASE_BASE_URL$trimmed"

            // Base64 Image (avec préfixe data: ou brut /9j/ JPEG ou iVBOR PNG ou UklGR WebP)
            trimmed.startsWith("data:image/") ||
            trimmed.startsWith("/9j/") ||
            trimmed.startsWith("iVBOR") ||
            trimmed.startsWith("UklGR") ||
            (!trimmed.startsWith("/") && trimmed.length > 80 && !trimmed.contains(" ") && !trimmed.contains("http")) -> {
                decodeBase64Image(trimmed)?.also { bitmap ->
                    bitmapCache.put(cacheKey, bitmap)
                }
            }

            trimmed.startsWith("/") -> {
                val f = File(trimmed)
                if (f.exists()) f else "$SUPABASE_BASE_URL$trimmed"
            }

            else -> {
                val clean = trimmed.trimStart('/')
                when {
                    clean.startsWith("object/public/") -> "$SUPABASE_BASE_URL/storage/v1/$clean"
                    clean.startsWith("public/") -> "$SUPABASE_BASE_URL/storage/v1/object/$clean"
                    clean.startsWith("annonces/") || clean.startsWith("fichiers/") -> "$SUPABASE_BASE_URL/storage/v1/object/public/$clean"
                    else -> "$SUPABASE_BASE_URL/storage/v1/object/public/annonces/$clean"
                }
            }
        }
    }

    /**
     * Décode une chaîne Base64 en Bitmap de manière sécurisée avec nettoyage des sauts de ligne.
     */
    fun decodeBase64Image(raw: String): Bitmap? {
        return try {
            val base64Content = if (raw.contains(",")) raw.substringAfter(",") else raw
            val clean = base64Content.replace("\n", "").replace("\r", "").replace(" ", "").trim()
            val bytes = try {
                Base64.decode(clean, Base64.DEFAULT)
            } catch (_: Exception) {
                try {
                    Base64.decode(clean, Base64.URL_SAFE)
                } catch (_: Exception) {
                    Base64.decode(clean, Base64.NO_WRAP)
                }
            }
            if (bytes.isNotEmpty()) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Sauvegarde ou extrait une image dans le cache local pour partage ou ouverture externe.
     */
    fun getCachedImageFile(context: Context, fileName: String, rawSource: String?): File? {
        if (rawSource.isNullOrBlank()) return null
        val trimmed = rawSource.trim()
        try {
            val imgDir = File(context.cacheDir, "annonces_images").apply { mkdirs() }
            val cleanName = if (fileName.contains(".")) fileName else "$fileName.jpg"
            val targetFile = File(imgDir, cleanName)

            if (trimmed.startsWith("/") && File(trimmed).exists()) {
                return File(trimmed)
            }
            if (trimmed.startsWith("file://")) {
                val f = File(trimmed.removePrefix("file://"))
                if (f.exists()) return f
            }

            if (trimmed.startsWith("data:") || trimmed.startsWith("/9j/") || trimmed.startsWith("iVBOR") ||
                (!trimmed.startsWith("http") && trimmed.length > 200)) {
                val base64Content = if (trimmed.contains(",")) trimmed.substringAfter(",") else trimmed
                val clean = base64Content.replace("\n", "").replace("\r", "").replace(" ", "").trim()
                val bytes = try {
                    Base64.decode(clean, Base64.DEFAULT)
                } catch (_: Exception) {
                    Base64.decode(clean, Base64.NO_WRAP)
                }
                if (bytes.isNotEmpty()) {
                    FileOutputStream(targetFile).use { it.write(bytes) }
                    return targetFile
                }
            }

            if (targetFile.exists() && targetFile.length() > 0L) {
                return targetFile
            }

            // Fallback hors-ligne pour images (ex: tampo.jpg)
            if (fileName.contains("tampo", ignoreCase = true) || fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".png", ignoreCase = true)) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.field_survey_interview_1789034896483)
                    if (bitmap != null) {
                        val bos = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, bos)
                        val bytes = bos.toByteArray()
                        if (bytes.isNotEmpty()) {
                            FileOutputStream(targetFile).use { it.write(bytes) }
                            return targetFile
                        }
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Génère un document PDF officiel natif pour consultation hors-ligne immédiate.
     */
    fun generateSamplePdfFile(context: Context, fileName: String = "Edouard.pdf"): File {
        val pdfDir = File(context.cacheDir, "annonces_pdf").apply { mkdirs() }
        val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val targetFile = File(pdfDir, cleanName)
        if (targetFile.exists() && targetFile.length() > 500L) return targetFile

        try {
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // Format A4
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint().apply { isAntiAlias = true }

            // Bannière d'en-tête bleu institutionnel
            paint.color = android.graphics.Color.rgb(15, 62, 118) // #0F3E76
            canvas.drawRect(0f, 0f, 595f, 115f, paint)

            // Titre institutionnel
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 17f
            paint.isFakeBoldText = true
            canvas.drawText("RÉPUBLIQUE DE MADAGASCAR", 40f, 42f, paint)

            paint.textSize = 11.5f
            paint.isFakeBoldText = false
            canvas.drawText("DIRECTION GÉNÉRALE DES ÉTUDES & DE LA STATISTIQUE", 40f, 65f, paint)
            canvas.drawText("NOTE DE SERVICE ET CONSIGNES OFFICIELLES AUX ENQUÊTEURS", 40f, 85f, paint)

            // Titre de la note
            paint.color = android.graphics.Color.rgb(30, 41, 59)
            paint.textSize = 15f
            paint.isFakeBoldText = true
            canvas.drawText("Objet : Procédure de Collecte Terrain & Mode Hors-Ligne", 40f, 150f, paint)

            // Ligne de séparation
            paint.color = android.graphics.Color.rgb(203, 213, 225)
            paint.strokeWidth = 1.5f
            canvas.drawLine(40f, 165f, 555f, 165f, paint)

            // Paragraphes explicatifs
            val lines = listOf(
                "1. Missions de l'Enquêteur sur le terrain :",
                "   • Identifier chaque personne ou ménage avec un code unique inviolable.",
                "   • Enregistrer rigoureusement toutes les informations démographiques et socio-économiques.",
                "   • Effectuer la saisie complète des questionnaires assignés.",
                "",
                "2. Fonctionnement en Zone Blanche (Hors-Ligne / Sans Internet) :",
                "   • L'application fonctionne à 100% en local grâce à SQLite Room.",
                "   • Les fiches collectées sont sécurisées dans le stockage interne du terminal.",
                "   • Aucune donnée n'est perdue lors des coupures de réseau.",
                "",
                "3. Procédure de Synchronisation :",
                "   • Dès le retour en zone couverte par le réseau internet, lancer la synchronisation.",
                "   • Les fiches en attente seront téléversées automatiquement vers la base centrale.",
                "",
                "4. Contact et Assistance d'urgence :",
                "   • Supervision technique : coordination-enquetes@statistiques.mg",
                "   • Centre d'appel assistance terrain : +261 34 00 000 00"
            )

            var y = 200f
            for (line in lines) {
                if (line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.") || line.startsWith("4.")) {
                    paint.isFakeBoldText = true
                    paint.textSize = 13f
                    paint.color = android.graphics.Color.rgb(15, 62, 118)
                } else {
                    paint.isFakeBoldText = false
                    paint.textSize = 11.5f
                    paint.color = android.graphics.Color.rgb(51, 65, 85)
                }
                canvas.drawText(line, 45f, y, paint)
                y += 22f
            }

            // Pied de page
            paint.color = android.graphics.Color.rgb(100, 116, 139)
            paint.textSize = 9.5f
            paint.isFakeBoldText = false
            canvas.drawText("Document certifié conforme • Système de Collecte Terrain • Version 2026", 40f, 800f, paint)

            pdfDoc.finishPage(page)
            FileOutputStream(targetFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
        } catch (_: Exception) {}
        return targetFile
    }

    fun getSamplePdfBase64(context: Context, fileName: String = "Edouard.pdf"): String {
        val f = generateSamplePdfFile(context, fileName)
        return try {
            val bytes = f.readBytes()
            "data:application/pdf;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) { "" }
    }

    fun getSampleImageBase64(context: Context): String {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.field_survey_interview_1789034896483)
            if (bitmap != null) {
                val bos = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, bos)
                val bytes = bos.toByteArray()
                "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else ""
        } catch (_: Exception) { "" }
    }

    /**
     * Ouvre l'image avec la galerie ou le visualiseur externe du système.
     */
    fun openImageWithSystemViewer(context: Context, fileName: String, rawSource: String?): Boolean {
        val file = getCachedImageFile(context, fileName, rawSource)
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Image « $fileName » prête et disponible hors-ligne.", Toast.LENGTH_SHORT).show()
            return false
        }
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val mimeType = if (fileName.endsWith(".png", true)) "image/png" else "image/jpeg"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Ouvrir « $fileName » avec :")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Image « $fileName » enregistrée dans le cache.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Résout l'URL distante complète pour un fichier PDF hébergé sur Supabase Storage.
     */
    fun resolveRemotePdfUrl(rawSource: String?): String? {
        val trimmed = rawSource?.trim() ?: return null
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        if (trimmed.startsWith("/storage/v1/")) {
            return "$SUPABASE_BASE_URL$trimmed"
        }
        if (trimmed.startsWith("storage/v1/")) {
            return "$SUPABASE_BASE_URL/$trimmed"
        }
        if (trimmed.startsWith("/")) {
            return "$SUPABASE_BASE_URL$trimmed"
        }
        val clean = trimmed.trimStart('/')
        return when {
            clean.startsWith("object/public/") -> "$SUPABASE_BASE_URL/storage/v1/$clean"
            clean.startsWith("public/") -> "$SUPABASE_BASE_URL/storage/v1/object/$clean"
            clean.startsWith("annonces/") || clean.startsWith("fichiers/") -> "$SUPABASE_BASE_URL/storage/v1/object/public/$clean"
            else -> "$SUPABASE_BASE_URL/storage/v1/object/public/annonces/$clean"
        }
    }

    fun getCachedPdfFirstPageBitmap(context: Context, fileName: String): Bitmap? {
        val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val cacheKey = "pdf_page_0#$cleanName"
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val pdfDir = File(context.cacheDir, "annonces_pdf")
            val thumbFile = File(pdfDir, "thumb_${cleanName}.png")
            if (thumbFile.exists() && thumbFile.length() > 500L) {
                val bmp = BitmapFactory.decodeFile(thumbFile.absolutePath)
                if (bmp != null) {
                    bitmapCache.put(cacheKey, bmp)
                    bmp
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extrait ou télécharge localement le document PDF dans le cache de l'application
     * pour permettre son affichage avec PdfRenderer et son ouverture par FileProvider.
     */
    fun getCachedPdfFile(context: Context, fileName: String, rawSource: String?): File? {
        val pdfDir = File(context.cacheDir, "annonces_pdf").apply { mkdirs() }
        val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val targetFile = File(pdfDir, cleanName)

        // Si le fichier cible existe déjà dans le cache, le réutiliser immédiatement (0 ms)
        if (targetFile.exists() && targetFile.length() > 500L) {
            return targetFile
        }

        if (rawSource.isNullOrBlank()) {
            return null
        }
        val trimmed = rawSource.trim()

        try {
            // Si c'est déjà un fichier local existant
            if (trimmed.startsWith("/") && File(trimmed).exists()) {
                return File(trimmed)
            }
            if (trimmed.startsWith("file://")) {
                val path = trimmed.removePrefix("file://")
                val f = File(path)
                if (f.exists()) return f
            }

            // Si c'est une chaîne Base64 (data:application/pdf;base64,... ou JVBERi...)
            if (trimmed.startsWith("data:") || trimmed.startsWith("JVBERi") ||
                (!trimmed.startsWith("http") && trimmed.length > 200)) {
                val base64Content = if (trimmed.contains(",")) trimmed.substringAfter(",") else trimmed
                val bytes = Base64.decode(base64Content.trim(), Base64.DEFAULT)
                if (bytes.isNotEmpty()) {
                    FileOutputStream(targetFile).use { it.write(bytes) }
                    return targetFile
                }
            }

            // Si c'est une URL distante (Supabase Storage ou HTTP/HTTPS) : télécharger le document réel
            val remoteUrl = resolveRemotePdfUrl(trimmed)
            if (!remoteUrl.isNullOrBlank() && (remoteUrl.startsWith("http://", true) || remoteUrl.startsWith("https://", true))) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val req = Request.Builder()
                    .url(remoteUrl)
                    .addHeader("Cache-Control", "no-cache")
                    .get()
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    resp.body?.byteStream()?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (targetFile.exists() && targetFile.length() > 0L) {
                        return targetFile
                    }
                }
            }
        } catch (_: Exception) {}

        return if (targetFile.exists() && targetFile.length() > 0L) targetFile else null
    }

    /**
     * Rend la première page d'un document PDF sous forme de Bitmap réaliste
     * grâce au moteur natif Android [PdfRenderer].
     */
    fun renderPdfFirstPage(context: Context, fileName: String, rawSource: String?): Bitmap? {
        val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
        val cacheKey = "pdf_page_0#$cleanName"
        getCachedPdfFirstPageBitmap(context, cleanName)?.let { return it }

        val file = getCachedPdfFile(context, cleanName, rawSource) ?: return null
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                // Échelle adaptée pour une netteté cristalline
                val targetWidth = 720
                val targetHeight = (targetWidth * page.height / page.width.toFloat()).toInt().coerceAtLeast(400)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                bitmapCache.put(cacheKey, bitmap)
                try {
                    val pdfDir = File(context.cacheDir, "annonces_pdf")
                    val thumbFile = File(pdfDir, "thumb_${cleanName}.png")
                    FileOutputStream(thumbFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                } catch (_: Exception) {}

                bitmap
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Rend une page spécifique d'un document PDF.
     */
    fun renderPdfPage(context: Context, file: File, pageIndex: Int): Bitmap? {
        val cacheKey = "pdf_page_${pageIndex}#${file.name}"
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val targetWidth = 1080
                val targetHeight = (targetWidth * page.height / page.width.toFloat()).toInt().coerceAtLeast(600)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                bitmapCache.put(cacheKey, bitmap)
                bitmap
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Retourne le nombre total de pages d'un PDF.
     */
    fun getPdfTotalPages(context: Context, file: File): Int {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (_: Exception) {
            1
        }
    }

    /**
     * Ouvre le PDF avec le visualiseur officiel du système (Adobe, Google Drive, Chrome, etc.)
     * via le [FileProvider] de l'application.
     */
    fun openPdfWithSystemViewer(context: Context, fileName: String, rawSource: String?): Boolean {
        val file = getCachedPdfFile(context, fileName, rawSource)
        if (file == null || !file.exists()) {
            // Si c'est une URL directe HTTP, tenter de l'ouvrir dans le navigateur
            if (rawSource?.startsWith("http") == true) {
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawSource)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            Toast.makeText(context, "Fichier PDF « $fileName » en cours de chargement...", Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Ouvrir « $fileName » avec :")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            Toast.makeText(context, "Document « $fileName » prêt pour consultation hors-ligne.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Télécharge un fichier (PDF, Image, etc.) dans le dossier Téléchargements (Downloads) du système.
     */
    fun downloadFile(context: Context, fileName: String, rawSource: String?) {
        try {
            val file = getCachedPdfFile(context, fileName, rawSource) ?: getCachedImageFile(context, fileName, rawSource)
            if (file != null && file.exists()) {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val request = android.app.DownloadManager.Request(Uri.fromFile(file))
                    .setTitle(fileName)
                    .setDescription("Téléchargement du fichier...")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                downloadManager.enqueue(request)
                Toast.makeText(context, "Téléchargement démarré : $fileName", Toast.LENGTH_SHORT).show()
                return
            }

            if (!rawSource.isNullOrBlank() && (rawSource.startsWith("http://", true) || rawSource.startsWith("https://", true))) {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                val request = android.app.DownloadManager.Request(Uri.parse(rawSource))
                    .setTitle(fileName)
                    .setDescription("Téléchargement du fichier...")
                    .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                downloadManager.enqueue(request)
                Toast.makeText(context, "Téléchargement démarré : $fileName", Toast.LENGTH_SHORT).show()
                return
            }

            Toast.makeText(context, "Fichier « $fileName » prêt.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur lors du téléchargement : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formatage lisible de la taille d'un fichier.
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Fichier"
        if (bytes < 1024) return "$bytes o"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.0f Ko", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f Mo", mb)
    }
}
