package com.example.data.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

data class GpsCoordinates(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val capturedAt: String
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("latitude", latitude)
        obj.put("longitude", longitude)
        obj.put("accuracy", accuracy)
        obj.put("captured_at", capturedAt)
        return obj.toString(2)
    }
}

sealed class GpsResult {
    data class Success(val coordinates: GpsCoordinates) : GpsResult()
    data class Error(val errorType: GpsErrorType, val message: String) : GpsResult()
}

enum class GpsErrorType {
    PERMISSION_DENIED,
    GPS_DISABLED,
    POSITION_UNAVAILABLE,
    TIMEOUT,
    UNKNOWN
}

object GpsLocationHelper {

    /**
     * Détermine si une question doit être traitée comme question de type GPS
     */
    fun isGpsQuestion(type: String, libelle: String): Boolean {
        val normType = type.trim().uppercase()
        val normLibelle = libelle.trim().lowercase()

        if (normType in listOf("GPS", "GEOLOCALISATION", "LOCALISATION", "LOCATION", "COORDINATES", "GEO")) {
            return true
        }

        if (normType == "TEXTE" || normType == "STRING" || normType == "GPS") {
            if (normLibelle.contains("gps") ||
                normLibelle.contains("géolocalisation") ||
                normLibelle.contains("geolocalisation") ||
                normLibelle.contains("localisation") ||
                normLibelle.contains("coordonnées") ||
                normLibelle.contains("coordonnees") ||
                normLibelle.contains("latitude") ||
                normLibelle.contains("toerana")
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Analyse une chaîne JSON pour extraire les coordonnées GPS
     */
    fun parseCoordinates(jsonStr: String?): GpsCoordinates? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val trimmed = jsonStr.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                // Tentative d'analyse de format simple "lat, lon"
                if (trimmed.contains(",")) {
                    val parts = trimmed.split(",")
                    if (parts.size >= 2) {
                        val lat = parts[0].trim().toDoubleOrNull()
                        val lon = parts[1].trim().toDoubleOrNull()
                        if (lat != null && lon != null) {
                            return GpsCoordinates(
                                latitude = lat,
                                longitude = lon,
                                accuracy = 10.0,
                                capturedAt = formatIsoUtc(System.currentTimeMillis())
                            )
                        }
                    }
                }
                return null
            }
            val obj = JSONObject(trimmed)
            if (!obj.has("latitude") || !obj.has("longitude")) return null

            val lat = obj.getDouble("latitude")
            val lon = obj.getDouble("longitude")
            val acc = if (obj.has("accuracy")) obj.optDouble("accuracy", 0.0) else 0.0
            val capturedAt = if (obj.has("captured_at")) {
                obj.getString("captured_at")
            } else {
                formatIsoUtc(System.currentTimeMillis())
            }

            GpsCoordinates(
                latitude = lat,
                longitude = lon,
                accuracy = acc,
                capturedAt = capturedAt
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formate la date au standard ISO-8601 UTC (ex: 2026-11-03T10:30:00Z)
     */
    fun formatIsoUtc(timeMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timeMs))
    }

    /**
     * Vérifie si les autorisations de localisation sont accordées
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Vérifie si le GPS ou le fournisseur de localisation réseau est actif sur l'appareil
     */
    fun isLocationProviderEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gpsEnabled = try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val networkEnabled = try {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return gpsEnabled || networkEnabled
    }

    /**
     * Ouvre l'écran des paramètres de localisation Android
     */
    fun openLocationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Récupère la position GPS actuelle du téléphone avec gestion de timeout et double fallback
     * Fonctionne hors ligne à 100% (les satellites GPS ne nécessitent aucune connexion Internet).
     */
    suspend fun getCurrentLocation(context: Context): GpsResult = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) {
            return@withContext GpsResult.Error(
                GpsErrorType.PERMISSION_DENIED,
                "Autorisation de localisation refusée. Veuillez autoriser l'accès au GPS."
            )
        }

        if (!isLocationProviderEnabled(context)) {
            return@withContext GpsResult.Error(
                GpsErrorType.GPS_DISABLED,
                "Le GPS est désactivé. Veuillez activer la localisation sur votre téléphone."
            )
        }

        // Timeout global de 20 secondes
        val result = withTimeoutOrNull(20_000L) {
            // 1. Tenter d'abord FusedLocationProviderClient (Google Play Services)
            val fusedResult = fetchFromFusedLocation(context)
            if (fusedResult != null) return@withTimeoutOrNull fusedResult

            // 2. Fallback direct sur LocationManager natif d'Android (AOSP / offline complet)
            fetchFromAndroidLocationManager(context)
        }

        return@withContext result ?: GpsResult.Error(
            GpsErrorType.TIMEOUT,
            "Délai d'attente dépassé : position GPS introuvable. Veuillez vérifier que vous êtes à ciel ouvert et réessayer."
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFromFusedLocation(context: Context): GpsResult? {
        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    cts.cancel()
                }

                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc: Location? ->
                        if (loc != null) {
                            val coords = GpsCoordinates(
                                latitude = Math.round(loc.latitude * 10000.0) / 10000.0,
                                longitude = Math.round(loc.longitude * 10000.0) / 10000.0,
                                accuracy = Math.round((if (loc.hasAccuracy()) loc.accuracy.toDouble() else 10.0) * 10.0) / 10.0,
                                capturedAt = formatIsoUtc(if (loc.time > 0) loc.time else System.currentTimeMillis())
                            )
                            continuation.resume(GpsResult.Success(coords))
                        } else {
                            // Tenter la dernière position connue
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                                if (lastLoc != null) {
                                    val coords = GpsCoordinates(
                                        latitude = Math.round(lastLoc.latitude * 10000.0) / 10000.0,
                                        longitude = Math.round(lastLoc.longitude * 10000.0) / 10000.0,
                                        accuracy = Math.round((if (lastLoc.hasAccuracy()) lastLoc.accuracy.toDouble() else 15.0) * 10.0) / 10.0,
                                        capturedAt = formatIsoUtc(if (lastLoc.time > 0) lastLoc.time else System.currentTimeMillis())
                                    )
                                    continuation.resume(GpsResult.Success(coords))
                                } else {
                                    continuation.resume(null)
                                }
                            }.addOnFailureListener {
                                continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchFromAndroidLocationManager(context: Context): GpsResult? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        return suspendCancellableCoroutine { continuation ->
            // Vérifier d'abord s'il y a un lastKnownLocation récent
            val lastGps = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val lastNet = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val bestLast = when {
                lastGps != null && lastNet != null -> if (lastGps.time >= lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }

            // Si le dernier emplacement est récent (moins de 2 minutes), l'utiliser en fallback
            val isRecent = bestLast != null && (System.currentTimeMillis() - bestLast.time < 120_000L)

            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    try {
                        lm.removeUpdates(this)
                    } catch (_: Exception) {}
                    if (continuation.isActive) {
                        val coords = GpsCoordinates(
                            latitude = Math.round(loc.latitude * 10000.0) / 10000.0,
                            longitude = Math.round(loc.longitude * 10000.0) / 10000.0,
                            accuracy = Math.round((if (loc.hasAccuracy()) loc.accuracy.toDouble() else 10.0) * 10.0) / 10.0,
                            capturedAt = formatIsoUtc(if (loc.time > 0) loc.time else System.currentTimeMillis())
                        )
                        continuation.resume(GpsResult.Success(coords))
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            continuation.invokeOnCancellation {
                try {
                    lm.removeUpdates(listener)
                } catch (_: Exception) {}
            }

            var requestSent = false
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try {
                    lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
                    requestSent = true
                } catch (_: Exception) {}
            }

            if (!requestSent && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try {
                    lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                    requestSent = true
                } catch (_: Exception) {}
            }

            if (!requestSent) {
                if (bestLast != null) {
                    val coords = GpsCoordinates(
                        latitude = Math.round(bestLast.latitude * 10000.0) / 10000.0,
                        longitude = Math.round(bestLast.longitude * 10000.0) / 10000.0,
                        accuracy = Math.round((if (bestLast.hasAccuracy()) bestLast.accuracy.toDouble() else 15.0) * 10.0) / 10.0,
                        capturedAt = formatIsoUtc(if (bestLast.time > 0) bestLast.time else System.currentTimeMillis())
                    )
                    continuation.resume(GpsResult.Success(coords))
                } else {
                    continuation.resume(
                        GpsResult.Error(
                            GpsErrorType.POSITION_UNAVAILABLE,
                            "Position introuvable. Veuillez vérifier que le GPS est activé et réessayer."
                        )
                    )
                }
            } else if (isRecent && bestLast != null) {
                // If waiting for fresh fix takes time, we could have used it, but listener is active.
            }
        }
    }
}
