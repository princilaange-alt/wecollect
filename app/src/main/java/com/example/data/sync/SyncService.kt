package com.example.data.sync

import android.util.Log
import com.example.data.model.AnnonceDestinataireEntity
import com.example.data.model.AnnonceEntity
import com.example.data.model.AnnonceFichierEntity
import com.example.data.model.EnqueteDetail
import com.example.data.model.EnqueteEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.OptionEntity
import com.example.data.model.PersonneEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.ReponseEntity
import com.example.data.model.SupabaseQuestionnaireStatus
import com.example.data.model.SyncResult
import com.example.data.model.UserEntity
import com.example.data.repository.SurveyRepository
import com.example.data.util.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class SyncLog(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val message: String,
    val isSuccess: Boolean,
    val recordsCount: Int = 0,
    val codeUnique: String? = null,
    val nomComplet: String? = null
)

sealed class UserAccountStatusResult {
    data class Active(val user: UserEntity) : UserAccountStatusResult()
    data class Inactive(val reason: String) : UserAccountStatusResult()
}

class SyncService(private val repository: SurveyRepository) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Real live Supabase credentials configured by user
    var supabaseUrl: String = "https://hcqhrzdskyzdqzdrhisc.supabase.co"
    var supabaseAnonKey: String = "sb_publishable_9xsDUlHz9EkDtdbXqMAA7Q_lpAf4p1e"
    var isSimulatedOffline: Boolean = false

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<SyncLog>>(
        listOf(
            SyncLog(
                message = "Module de synchronisation Supabase connecté en direct (Projet hcqhrzdskyzdqzdrhisc).",
                isSuccess = true
            )
        )
    )
    val syncLogs: StateFlow<List<SyncLog>> = _syncLogs.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    // Liste des questionnaires sur Supabase en direct (PUBLIÉ sy BROUILLON)
    private val _remoteQuestionnaires = MutableStateFlow<List<SupabaseQuestionnaireStatus>>(emptyList())
    val remoteQuestionnaires: StateFlow<List<SupabaseQuestionnaireStatus>> = _remoteQuestionnaires.asStateFlow()

    fun addLog(
        message: String,
        isSuccess: Boolean,
        count: Int = 0,
        codeUnique: String? = null,
        nomComplet: String? = null
    ) {
        val newLog = SyncLog(
            message = message,
            isSuccess = isSuccess,
            recordsCount = count,
            codeUnique = codeUnique,
            nomComplet = nomComplet
        )
        _syncLogs.value = listOf(newLog) + _syncLogs.value.take(45)
    }

    fun clearLogs() {
        _syncLogs.value = emptyList()
    }

    fun extractRespondentName(reponses: List<com.example.data.model.ReponseWithQuestion>): String {
        val nom = reponses.firstOrNull { 
            val lib = it.question.libelle.lowercase()
            ((lib.contains("nom") && !lib.contains("prénom") && !lib.contains("prenom")) || lib.contains("anarana") || lib.contains("anaran'ny"))
        }?.reponse?.valeur?.trim() ?: ""
        
        val prenom = reponses.firstOrNull { 
            val lib = it.question.libelle.lowercase()
            lib.contains("prénom") || lib.contains("prenom") || lib.contains("fanampiny")
        }?.reponse?.valeur?.trim() ?: ""
        
        return when {
            nom.isNotBlank() && prenom.isNotBlank() -> "$nom $prenom"
            nom.isNotBlank() -> nom
            prenom.isNotBlank() -> prenom
            else -> reponses.firstOrNull { it.reponse.valeur?.isNotBlank() == true }?.reponse?.valeur?.trim() ?: "Enquêté"
        }
    }

    /**
     * Effectue un test de connexion direct (Ping HTTP) vers l'API REST de Supabase
     */
    suspend fun testLiveConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) {
            return@withContext Pair(false, "Mode zone blanche activé : Connexion réseau bloquée intentionnellement.")
        }
        val startTime = System.currentTimeMillis()
        try {
            val url = "${supabaseUrl.trimEnd('/')}/rest/v1/questionnaires?select=*"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val jsonArr = JSONArray(body)
                var publicCount = 0
                var draftCount = 0
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val st = (if (obj.has("statut") && !obj.isNull("statut")) obj.getString("statut")
                              else if (obj.has("status") && !obj.isNull("status")) obj.getString("status")
                              else "").trim()
                    if (isDraftStatus(st)) {
                        draftCount++
                    } else if (isPublicStatus(st)) {
                        publicCount++
                    }
                }
                val draftInfo = if (draftCount > 0) " ($draftCount brouillons ignorés)" else ""
                val msg = "Connexion Supabase en direct RÉUSSIE (HTTP ${response.code} en ${latency}ms) ! $publicCount questionnaire(s) disponible(s)$draftInfo."
                addLog(msg, true)
                Pair(true, msg)
            } else {
                val errBody = response.body?.string() ?: response.message
                val msg = "Échec connexion Supabase (HTTP ${response.code}) : $errBody"
                addLog(msg, false)
                Pair(false, msg)
            }
        } catch (e: Exception) {
            val msg = "Erreur de connexion : ${e.localizedMessage ?: "Réseau inaccessible"}"
            addLog(msg, false)
            Pair(false, msg)
        }
    }

    /**
     * Synchronise l'ensemble des fiches en attente vers la base distante Supabase
     */
    suspend fun synchronizeAll(enqueteurId: Long? = null): SyncResult = withContext(Dispatchers.IO) {
        if (_isSyncing.value) {
            return@withContext SyncResult(false, message = "Une synchronisation est déjà en cours.")
        }

        _isSyncing.value = true

        try {
            if (isSimulatedOffline) {
                val msg = "Échec : Connexion réseau indisponible (Zone blanche activée). Les fiches restent protégées dans SQLite."
                addLog(msg, false)
                val res = SyncResult(false, message = msg)
                _lastSyncResult.value = res
                return@withContext res
            }

            val allEnquetes = repository.allEnquetes.firstOrNull() ?: emptyList()
            val pending = allEnquetes.filter { 
                (it.syncStatus == "EN_ATTENTE" || it.syncStatus == "ERREUR") &&
                (enqueteurId == null || it.idEnqueteur == enqueteurId)
            }

            if (pending.isEmpty()) {
                val msg = "Aucune fiche en attente. Toutes les fiches locales sont synchronisées."
                addLog(msg, true)
                val res = SyncResult(true, 0, 0, msg)
                _lastSyncResult.value = res
                return@withContext res
            }

            addLog("Début synchronisation de ${pending.size} fiche(s) vers Supabase ($supabaseUrl)...", true, pending.size)

            var successCount = 0
            var failCount = 0

            for (enquete in pending) {
                val detail = repository.getEnqueteDetail(enquete.idEnquete)
                if (detail != null) {
                    val respondent = extractRespondentName(detail.reponses)
                    val code = detail.codeUnique
                    val syncSuccess = pushEnqueteToSupabase(detail)
                    if (syncSuccess) {
                        repository.updateEnqueteSyncStatus(enquete.idEnquete, "SYNCHRONISEE")
                        addLog(
                            message = "Fiche $code ($respondent) transmise à Supabase avec succès.",
                            isSuccess = true,
                            count = 1,
                            codeUnique = code,
                            nomComplet = respondent
                        )
                        successCount++
                    } else {
                        repository.updateEnqueteSyncStatus(enquete.idEnquete, "ERREUR")
                        addLog(
                            message = "Échec transmission pour $code ($respondent).",
                            isSuccess = false,
                            count = 0,
                            codeUnique = code,
                            nomComplet = respondent
                        )
                        failCount++
                    }
                }
            }

            val isFullSuccess = failCount == 0
            val msg = if (isFullSuccess) {
                "Succès : $successCount fiche(s) transmise(s) et confirmée(s) dans Supabase !"
            } else {
                "$successCount synchronisée(s), $failCount en échec. Vérifiez la connectivité."
            }

            addLog(msg, isFullSuccess, successCount)
            val result = SyncResult(isFullSuccess, successCount, failCount, msg)
            _lastSyncResult.value = result
            return@withContext result
        } catch (e: Exception) {
            val msg = "Erreur globale de synchronisation : ${e.localizedMessage ?: "Réseau indisponible"}"
            addLog(msg, false)
            val result = SyncResult(false, 0, 0, msg)
            _lastSyncResult.value = result
            return@withContext result
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Envoie une fiche d'enquête et ses réponses réelles vers Supabase
     * (y compris les photos et enregistrements vocaux réels vers Supabase Storage / Base64 Data URI)
     */
    private suspend fun pushEnqueteToSupabase(detail: EnqueteDetail): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = supabaseUrl.trimEnd('/')

            val cleanCode = detail.codeUnique.ifBlank { "ENQ-${detail.enquete.idEnquete}" }

            // 1. Envoyer ou récupérer la personne (Table `personnes`)
            var remotePersonId: Long = detail.enquete.idPersonne
            try {
                val personPayload = JSONObject().apply {
                    put("code_unique", cleanCode)
                }
                val personReqBody = personPayload.toString().toRequestBody("application/json".toMediaType())
                val personRequest = Request.Builder()
                    .url("$cleanUrl/rest/v1/personnes")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                    .post(personReqBody)
                    .build()
                val personResponse = client.newCall(personRequest).execute()
                if (personResponse.isSuccessful) {
                    val respStr = personResponse.body?.string() ?: "[]"
                    val pArray = JSONArray(respStr)
                    if (pArray.length() > 0) {
                        remotePersonId = pArray.getJSONObject(0).optLong("id_personne", remotePersonId)
                    }
                } else {
                    val getPReq = Request.Builder()
                        .url("$cleanUrl/rest/v1/personnes?code_unique=eq.$cleanCode&select=id_personne")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .get()
                        .build()
                    val getPResp = client.newCall(getPReq).execute()
                    if (getPResp.isSuccessful) {
                        val arr = JSONArray(getPResp.body?.string() ?: "[]")
                        if (arr.length() > 0) {
                            remotePersonId = arr.getJSONObject(0).optLong("id_personne", remotePersonId)
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. Envoyer l'en-tête de l'enquête (Table `enquetes`)
            val enquetePayload = JSONObject().apply {
                put("id_personne", remotePersonId)
                put("id_questionnaire", detail.enquete.idQuestionnaire)
                put("id_enqueteur", detail.enquete.idEnqueteur)
                put("date_enquete", detail.enquete.dateEnquete)
                put("statut", detail.enquete.statut)
                put("sync_status", "SYNCHRONISEE")
            }

            val enqueteReqBody = enquetePayload.toString().toRequestBody("application/json".toMediaType())
            val enqueteRequest = Request.Builder()
                .url("$cleanUrl/rest/v1/enquetes")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(enqueteReqBody)
                .build()

            val enqueteResponse = client.newCall(enqueteRequest).execute()
            if (!enqueteResponse.isSuccessful) {
                val err = enqueteResponse.body?.string() ?: enqueteResponse.message
                addLog("Erreur Supabase (table enquetes) pour $cleanCode : $err", false)
                return@withContext false
            }

            val respBodyStr = enqueteResponse.body?.string() ?: "[]"
            val respJsonArray = JSONArray(respBodyStr)
            val remoteEnqueteId = if (respJsonArray.length() > 0) {
                respJsonArray.getJSONObject(0).optLong("id_enquete", detail.enquete.idEnquete)
            } else {
                detail.enquete.idEnquete
            }

            // 2. Nettoyer les anciennes réponses pour cette enquête pour éviter tout doublon dans Supabase
            try {
                val deleteOldReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/reponses?id_enquete=eq.$remoteEnqueteId")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .delete()
                    .build()
                client.newCall(deleteOldReq).execute().close()
            } catch (_: Exception) {}

            // 3. Envoyer les réponses (Table `reponses`) avec synchronisation réelle des médias
            if (detail.reponses.isNotEmpty()) {
                val reponsesArray = JSONArray()
                var mediaCount = 0

                for (rwq in detail.reponses) {
                    val rawVal = rwq.reponse.valeur?.trim() ?: ""

                    // Règles photo et voix : Si vide, non répondu ou absent, ne pas envoyer
                    val isPhoto = MediaUtils.isImage(rawVal, rwq.question.type, rwq.question.libelle)
                    val isAudio = MediaUtils.isAudio(rawVal, rwq.question.type, rwq.question.libelle)
                    val isMedia = isPhoto || isAudio || MediaUtils.isMediaValue(rawVal)

                    val qTypeUpper = rwq.question.type.uppercase()
                    val isMediaTypeQuestion = qTypeUpper in listOf("IMAGE", "PHOTO", "AUDIO", "VOCAL", "SON", "VOICE", "ENREGISTREMENT", "MICRO") ||
                            (qTypeUpper == "TEXTE" && (rwq.question.libelle.lowercase().let {
                                it.contains("photo") || it.contains("sary") || it.contains("image") || it.contains("audio") || it.contains("vocal") || it.contains("feo")
                            }))

                    // Si c'est une question média (photo ou voix) et qu'il n'y a pas de réponse, on ne l'envoie pas
                    if (isMediaTypeQuestion && (rawVal.isBlank() || rawVal == "null" || rawVal == "[]" || rawVal.equals("aucun", true))) {
                        continue
                    }

                    // Si la réponse est totalement vide pour une valeur média, ne pas envoyer
                    if (isMedia && rawVal.isBlank()) {
                        continue
                    }

                    // Ignorer les réponses vides générales sans option
                    if (rawVal.isBlank() && rwq.reponse.idOption == null) {
                        continue
                    }

                    val syncedVal = if (isMedia && rawVal.isNotBlank()) {
                        mediaCount++
                        MediaUtils.prepareMediaForSupabaseSync(
                            client = client,
                            supabaseUrl = cleanUrl,
                            supabaseKey = supabaseAnonKey,
                            codeUnique = detail.codeUnique,
                            questionId = rwq.reponse.idQuestion,
                            valeur = rawVal
                        )
                    } else {
                        rawVal
                    }

                    val rJson = JSONObject().apply {
                        put("id_enquete", remoteEnqueteId)
                        put("id_question", rwq.reponse.idQuestion)
                        if (rwq.reponse.idOption != null) {
                            put("id_option", rwq.reponse.idOption)
                        } else {
                            put("id_option", JSONObject.NULL)
                        }
                        put("valeur", syncedVal)
                    }
                    reponsesArray.put(rJson)
                }

                if (mediaCount > 0) {
                    addLog("Envoi réel de $mediaCount média(s) (photo/voix) vers Supabase pour ${detail.codeUnique}...", true)
                }

                if (reponsesArray.length() > 0) {
                    val repReqBody = reponsesArray.toString().toRequestBody("application/json".toMediaType())
                    val repRequest = Request.Builder()
                        .url("$cleanUrl/rest/v1/reponses")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                        .post(repReqBody)
                        .build()

                    val repResponse = client.newCall(repRequest).execute()
                    if (!repResponse.isSuccessful) {
                        val err = repResponse.body?.string() ?: repResponse.message
                        addLog("Alerte Supabase (table reponses) pour ${detail.codeUnique} : $err", false)
                        return@withContext repResponse.code == 409
                    }
                }
            }

            return@withContext true
        } catch (e: Exception) {
            addLog("Exception réseau Supabase : ${e.localizedMessage}", false)
            return@withContext false
        }
    }

    suspend fun syncSingleById(enqueteId: Long): Boolean = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) {
            addLog("Échec : Mode zone blanche actif. La fiche reste dans la file d'attente locale.", false)
            return@withContext false
        }
        val detail = repository.getEnqueteDetail(enqueteId) ?: return@withContext false
        val respondent = extractRespondentName(detail.reponses)
        val code = detail.codeUnique
        addLog(
            message = "Synchronisation de la fiche $code ($respondent) vers Supabase...",
            isSuccess = true,
            count = 0,
            codeUnique = code,
            nomComplet = respondent
        )

        val success = pushEnqueteToSupabase(detail)
        if (success) {
            repository.updateEnqueteSyncStatus(enqueteId, "SYNCHRONISEE")
            addLog(
                message = "Fiche $code ($respondent) synchronisée avec succès dans Supabase.",
                isSuccess = true,
                count = 1,
                codeUnique = code,
                nomComplet = respondent
            )
        } else {
            repository.updateEnqueteSyncStatus(enqueteId, "ERREUR")
            addLog(
                message = "Échec de synchronisation pour la fiche $code ($respondent).",
                isSuccess = false,
                count = 0,
                codeUnique = code,
                nomComplet = respondent
            )
        }
        success
    }

    fun getPayloadPreview(detail: EnqueteDetail): String {
        val root = JSONObject()
        val enqueteJson = JSONObject().apply {
            put("code_unique", detail.codeUnique)
            put("id_questionnaire", detail.enquete.idQuestionnaire)
            put("id_enqueteur", detail.enquete.idEnqueteur)
            put("date_enquete", detail.enquete.dateEnquete)
            put("statut", detail.enquete.statut)
            put("sync_status", "SYNCHRONISEE")
        }
        root.put("enquete", enqueteJson)

        val reponsesArray = JSONArray()
        for (rwq in detail.reponses) {
            val rJson = JSONObject().apply {
                put("question_id", rwq.reponse.idQuestion)
                put("question_libelle", rwq.question.libelle)
                if (rwq.reponse.idOption != null) {
                    put("id_option", rwq.reponse.idOption)
                } else {
                    put("id_option", JSONObject.NULL)
                }
                put("valeur", rwq.reponse.valeur ?: "")
            }
            reponsesArray.put(rJson)
        }
        root.put("reponses", reponsesArray)

        return root.toString(2)
    }

    /**
     * Mampiditra / maka ny questionnaires, questions, options, ary users avy any amin'ny Supabase
     * ary manao stock azy mivantana ao amin'ny SQLite database (Room).
     */
    suspend fun pullFormsAndSchemaFromSupabase(
        enqueteurId: Long? = null,
        userRole: String? = null
    ): SyncResult = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) {
            val msg = "Mode zone blanche actif : Impossible de récupérer les formulaires depuis Supabase (Hors ligne)."
            addLog(msg, false)
            return@withContext SyncResult(false, message = msg)
        }

        val cleanUrl = supabaseUrl.trimEnd('/')
        try {
            addLog("Récupération automatique des formulaires depuis Supabase ($cleanUrl)...", true)

            // Famakiana miaraka (Parallel async HTTP calls) ho an'ny Users, Questionnaires, Questions, Options, Enquetes, Reponses
            val rawResults = coroutineScope {
                val usersDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<UserEntity>()
                    try {
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/users?select=*")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .addHeader("Pragma", "no-cache")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val rawPhoto = if (obj.isNull("photo")) null else obj.optString("photo").takeIf { it.isNotBlank() }
                                val rawPhotoUrl = when {
                                    !obj.isNull("photo_url") -> obj.optString("photo_url").takeIf { it.isNotBlank() }
                                    !obj.isNull("photo") -> obj.optString("photo").takeIf { it.isNotBlank() }
                                    else -> null
                                }
                                val rawStatut = extractUserStatut(obj)
                                list.add(
                                    UserEntity(
                                        idUser = if (obj.has("id_user")) obj.getLong("id_user") else obj.optLong("id", (i + 1).toLong()),
                                        nom = obj.optString("nom", "Nom"),
                                        prenom = obj.optString("prenom", "Prénom"),
                                        email = obj.optString("email", "user$i@survey.mg"),
                                        telephone = if (obj.isNull("telephone")) null else obj.getString("telephone"),
                                        motDePasse = if (obj.isNull("mot_de_passe")) null else obj.getString("mot_de_passe"),
                                        role = obj.optString("role", "ENQUETEUR"),
                                        statut = rawStatut,
                                        photo = rawPhoto,
                                        photoUrl = rawPhotoUrl,
                                        createdAt = obj.optString("created_at", System.currentTimeMillis().toString()),
                                        updatedAt = obj.optString("updated_at", System.currentTimeMillis().toString())
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore user pull error
                    }
                    list
                }

                val qDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<QuestionnaireEntity>()
                    val allSupabaseStatusList = mutableListOf<SupabaseQuestionnaireStatus>()
                    try {
                        var req = Request.Builder()
                            .url("$cleanUrl/rest/v1/questionnaires?select=*")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .addHeader("Pragma", "no-cache")
                            .get()
                            .build()
                        var resp = client.newCall(req).execute()
                        
                        // Fallback raha "formulaires" na "formulaire" ny anaran'ny table
                        if (!resp.isSuccessful && resp.code == 404) {
                            req = Request.Builder()
                                .url("$cleanUrl/rest/v1/formulaires?select=*")
                                .addHeader("apikey", supabaseAnonKey)
                                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                                .get()
                                .build()
                            resp = client.newCall(req).execute()
                        }

                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val rawStatut = extractQuestionnaireStatut(obj)

                                val id = when {
                                    obj.has("id_questionnaire") && !obj.isNull("id_questionnaire") -> obj.getLong("id_questionnaire")
                                    obj.has("id") && !obj.isNull("id") -> obj.getLong("id")
                                    obj.has("questionnaire_id") && !obj.isNull("questionnaire_id") -> obj.getLong("questionnaire_id")
                                    else -> (i + 1).toLong()
                                }
                                val title = when {
                                    obj.has("titre") -> obj.getString("titre")
                                    obj.has("title") -> obj.getString("title")
                                    obj.has("nom") -> obj.getString("nom")
                                    obj.has("label") -> obj.getString("label")
                                    else -> "Questionnaire #$id"
                                }

                                val desc = when {
                                    obj.has("description") && !obj.isNull("description") -> obj.getString("description")
                                    obj.has("desc") && !obj.isNull("desc") -> obj.getString("desc")
                                    else -> null
                                }
                                val ver = obj.optString("version", "v1.0")
                                val createdBy = when {
                                    obj.has("created_by") && !obj.isNull("created_by") -> obj.getLong("created_by")
                                    obj.has("id_user") && !obj.isNull("id_user") -> obj.getLong("id_user")
                                    obj.has("user_id") && !obj.isNull("user_id") -> obj.getLong("user_id")
                                    else -> 1L
                                }
                                val updatedAt = obj.optString("updated_at", System.currentTimeMillis().toString())

                                val isPub = isPublicStatus(rawStatut)
                                allSupabaseStatusList.add(
                                    SupabaseQuestionnaireStatus(
                                        idQuestionnaire = id,
                                        titre = title,
                                        description = desc,
                                        version = ver,
                                        statut = if (isPub) "PUBLIÉ" else rawStatut.ifBlank { "BROUILLON" },
                                        isPublished = isPub,
                                        updatedAt = updatedAt
                                    )
                                )

                                if (!isPub) {
                                    addLog("Questionnaire « $title » (#$id) avec statut « ${rawStatut.ifBlank { "BROUILLON" }} » masqué (brouillon).", isSuccess = true)
                                    try {
                                        repository.markQuestionnaireAsDraft(id)
                                    } catch (_: Exception) {}
                                    continue
                                }

                                list.add(
                                    QuestionnaireEntity(
                                        idQuestionnaire = id,
                                        titre = title,
                                        description = desc,
                                        version = ver,
                                        statut = "PUBLIÉ",
                                        createdBy = createdBy,
                                        createdAt = obj.optString("created_at", System.currentTimeMillis().toString()),
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                            _remoteQuestionnaires.value = allSupabaseStatusList
                        } else {
                            addLog("Supabase /questionnaires HTTP ${resp.code} : ${resp.message}", false)
                        }
                    } catch (e: Exception) {
                        addLog("Erreur questionnaires Supabase : ${e.localizedMessage}", false)
                    }
                    list
                }

                val questDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<QuestionEntity>()
                    try {
                        var req = Request.Builder()
                            .url("$cleanUrl/rest/v1/questions?select=*")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .addHeader("Pragma", "no-cache")
                            .get()
                            .build()
                        var resp = client.newCall(req).execute()

                        if (!resp.isSuccessful && resp.code == 404) {
                            req = Request.Builder()
                                .url("$cleanUrl/rest/v1/question?select=*")
                                .addHeader("apikey", supabaseAnonKey)
                                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                                .get()
                                .build()
                            resp = client.newCall(req).execute()
                        }

                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val qId = when {
                                    obj.has("id_question") && !obj.isNull("id_question") -> obj.optLong("id_question", (i + 1).toLong())
                                    obj.has("question_id") && !obj.isNull("question_id") -> obj.optLong("question_id", (i + 1).toLong())
                                    obj.has("id") && !obj.isNull("id") -> obj.optLong("id", (i + 1).toLong())
                                    else -> (i + 1).toLong()
                                }
                                val questionnaireId = when {
                                    obj.has("id_questionnaire") && !obj.isNull("id_questionnaire") -> obj.optLong("id_questionnaire", 0L)
                                    obj.has("questionnaire_id") && !obj.isNull("questionnaire_id") -> obj.optLong("questionnaire_id", 0L)
                                    obj.has("idQuestionnaire") && !obj.isNull("idQuestionnaire") -> obj.optLong("idQuestionnaire", 0L)
                                    obj.has("id_formulaire") && !obj.isNull("id_formulaire") -> obj.optLong("id_formulaire", 0L)
                                    obj.has("formulaire_id") && !obj.isNull("formulaire_id") -> obj.optLong("formulaire_id", 0L)
                                    else -> 0L
                                }
                                val libelle = when {
                                    obj.has("libelle") -> obj.getString("libelle")
                                    obj.has("label") -> obj.getString("label")
                                    obj.has("texte") -> obj.getString("texte")
                                    obj.has("title") -> obj.getString("title")
                                    obj.has("name") -> obj.getString("name")
                                    else -> "Question #$qId"
                                }
                                val type = when {
                                    obj.has("type") -> obj.getString("type")
                                    obj.has("question_type") -> obj.getString("question_type")
                                    else -> "TEXTE"
                                }
                                val oblig = if (obj.has("obligatoire")) obj.getBoolean("obligatoire") else obj.optBoolean("is_required", false)
                                val ordre = if (obj.has("ordre")) obj.getInt("ordre") else obj.optInt("order", i + 1)
                                val isCodeUnique = if (obj.has("est_code_unique")) obj.getBoolean("est_code_unique")
                                    else if (obj.has("is_code_unique")) obj.getBoolean("is_code_unique")
                                    else false

                                list.add(
                                    QuestionEntity(
                                        idQuestion = qId,
                                        idQuestionnaire = questionnaireId,
                                        libelle = libelle,
                                        type = type.uppercase(),
                                        obligatoire = oblig,
                                        ordre = ordre,
                                        estCodeUnique = isCodeUnique,
                                        createdAt = obj.optString("created_at", System.currentTimeMillis().toString()),
                                        updatedAt = obj.optString("updated_at", System.currentTimeMillis().toString())
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore question pull error
                    }
                    list
                }

                val optDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<OptionEntity>()
                    try {
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/options?select=*")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .addHeader("Pragma", "no-cache")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val optId = when {
                                    obj.has("id_option") && !obj.isNull("id_option") -> obj.optLong("id_option", (i + 1).toLong())
                                    obj.has("option_id") && !obj.isNull("option_id") -> obj.optLong("option_id", (i + 1).toLong())
                                    obj.has("id") && !obj.isNull("id") -> obj.optLong("id", (i + 1).toLong())
                                    else -> (i + 1).toLong()
                                }
                                val questionId = when {
                                    obj.has("id_question") && !obj.isNull("id_question") -> obj.optLong("id_question", 0L)
                                    obj.has("question_id") && !obj.isNull("question_id") -> obj.optLong("question_id", 0L)
                                    obj.has("idQuestion") && !obj.isNull("idQuestion") -> obj.optLong("idQuestion", 0L)
                                    else -> 0L
                                }
                                val libelle = when {
                                    obj.has("libelle") -> obj.getString("libelle")
                                    obj.has("label") -> obj.getString("label")
                                    obj.has("texte") -> obj.getString("texte")
                                    obj.has("value") -> obj.getString("value")
                                    else -> "Option #$optId"
                                }
                                val ordre = if (obj.has("ordre")) obj.getInt("ordre") else obj.optInt("order", i + 1)
                                list.add(
                                    OptionEntity(
                                        idOption = optId,
                                        idQuestion = questionId,
                                        libelle = libelle,
                                        ordre = ordre
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore option pull error
                    }
                    list
                }

                val personnesDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<PersonneEntity>()
                    try {
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/personnes?select=*&order=id_personne.asc")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val pId = obj.optLong("id_personne", (i + 1).toLong())
                                val code = obj.optString("code_unique", "ENQ-$pId")
                                val created = obj.optString("created_at", "")
                                val updated = obj.optString("updated_at", "")
                                list.add(
                                    PersonneEntity(
                                        idPersonne = pId,
                                        codeUnique = code,
                                        createdAt = created,
                                        updatedAt = updated
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                    list
                }

                val enquetesDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<EnqueteEntity>()
                    try {
                        val queryParam = if (enqueteurId != null && !userRole.equals("ADMIN", ignoreCase = true)) {
                            "id_enqueteur=eq.$enqueteurId&select=*&order=id_enquete.desc"
                        } else {
                            "select=*&order=id_enquete.desc"
                        }
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/enquetes?$queryParam")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optLong("id_enquete", (i + 1).toLong())
                                val idP = obj.optLong("id_personne", id)
                                val idQ = obj.optLong("id_questionnaire", 1L)
                                val idEnq = obj.optLong("id_enqueteur", enqueteurId ?: 1L)
                                val dateE = obj.optString("date_enquete", "")
                                val st = obj.optString("statut", "TERMINEE")
                                val syncSt = obj.optString("sync_status", "SYNCHRONISEE")
                                val created = obj.optString("created_at", "")
                                val updated = obj.optString("updated_at", "")
                                list.add(
                                    EnqueteEntity(
                                        idEnquete = id,
                                        idPersonne = idP,
                                        idQuestionnaire = idQ,
                                        idEnqueteur = idEnq,
                                        dateEnquete = dateE,
                                        statut = st,
                                        syncStatus = syncSt,
                                        createdAt = created,
                                        updatedAt = updated
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore enquete pull error
                    }
                    list
                }

                val reponsesDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<ReponseEntity>()
                    try {
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/reponses?select=*")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val valStr = if (obj.isNull("valeur")) "" else obj.getString("valeur")

                                val id = obj.optLong("id_reponse", (i + 1).toLong())
                                val idEnq = obj.optLong("id_enquete", 0L)
                                val idQues = obj.optLong("id_question", 0L)
                                val idOpt = if (obj.isNull("id_option")) null else obj.optLong("id_option")
                                list.add(
                                    ReponseEntity(
                                        idReponse = id,
                                        idEnquete = idEnq,
                                        idQuestion = idQues,
                                        idOption = idOpt,
                                        valeur = valStr
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore reponse pull error
                    }
                    list
                }

                val annoncesDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<AnnonceEntity>()
                    try {
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/annonces?select=*&order=created_at.desc")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = when {
                                    obj.has("id_annonce") && !obj.isNull("id_annonce") -> obj.getString("id_annonce")
                                    obj.has("id") && !obj.isNull("id") -> obj.getString("id")
                                    else -> java.util.UUID.randomUUID().toString()
                                }
                                val titre = obj.optString("titre", "Annonce")
                                val contenu = if (obj.isNull("contenu")) null else obj.optString("contenu")
                                val typeAnnonce = obj.optString("type_annonce", "TEXTE")
                                val priorite = obj.optString("priorite", "NORMALE")
                                val statut = obj.optString("statut", "PUBLIEE")
                                val cibleType = obj.optString("cible_type", "TOUS")
                                val cibleValeur = if (obj.isNull("cible_valeur")) null else obj.optString("cible_valeur")
                                val datePub = if (obj.isNull("date_publication")) null else obj.optString("date_publication")
                                val dateExp = if (obj.isNull("date_expiration")) null else obj.optString("date_expiration")
                                val createdBy = if (obj.isNull("created_by")) null else obj.optLong("created_by")
                                val authorName = if (obj.isNull("author_name")) null else obj.optString("author_name")
                                val authorRole = if (obj.isNull("author_role")) null else obj.optString("author_role")
                                val createdAt = obj.optString("created_at", System.currentTimeMillis().toString())
                                val updatedAt = obj.optString("updated_at", System.currentTimeMillis().toString())

                                list.add(
                                    AnnonceEntity(
                                        idAnnonce = id,
                                        titre = titre,
                                        contenu = contenu,
                                        typeAnnonce = typeAnnonce,
                                        priorite = priorite,
                                        statut = statut,
                                        cibleType = cibleType,
                                        cibleValeur = cibleValeur,
                                        datePublication = datePub,
                                        dateExpiration = dateExp,
                                        createdBy = createdBy,
                                        authorName = authorName,
                                        authorRole = authorRole,
                                        createdAt = createdAt,
                                        updatedAt = updatedAt
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore annonce pull error
                    }
                    list
                }

                val annonceFichiersDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<AnnonceFichierEntity>()
                    try {
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/annonce_fichiers?select=*")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val idFichier = when {
                                    obj.has("id_fichier") && !obj.isNull("id_fichier") -> obj.getString("id_fichier")
                                    obj.has("id") && !obj.isNull("id") -> obj.getString("id")
                                    else -> java.util.UUID.randomUUID().toString()
                                }
                                val idAnnonce = obj.optString("id_annonce", "")
                                val nomFichier = obj.optString("nom_fichier", "fichier")
                                val cheminStorage = if (obj.isNull("chemin_storage")) null else obj.optString("chemin_storage")
                                val typeMime = if (obj.isNull("type_mime")) null else obj.optString("type_mime")
                                val tailleOctets = obj.optLong("taille_octets", 0L)
                                val dataUrl = if (obj.isNull("data_url")) null else obj.optString("data_url")
                                val uploadedAt = obj.optString("uploaded_at", System.currentTimeMillis().toString())

                                if (idAnnonce.isNotBlank()) {
                                    list.add(
                                        AnnonceFichierEntity(
                                            idFichier = idFichier,
                                            idAnnonce = idAnnonce,
                                            nomFichier = nomFichier,
                                            cheminStorage = cheminStorage,
                                            typeMime = typeMime,
                                            tailleOctets = tailleOctets,
                                            dataUrl = dataUrl,
                                            uploadedAt = uploadedAt
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                    list
                }

                val annonceDestinatairesDeferred = async(Dispatchers.IO) {
                    val list = mutableListOf<AnnonceDestinataireEntity>()
                    try {
                        val req = Request.Builder()
                            .url("$cleanUrl/rest/v1/annonce_destinataires?select=*")
                            .addHeader("apikey", supabaseAnonKey)
                            .addHeader("Authorization", "Bearer $supabaseAnonKey")
                            .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                            .get()
                            .build()
                        val resp = client.newCall(req).execute()
                        if (resp.isSuccessful) {
                            val arr = JSONArray(resp.body?.string() ?: "[]")
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                val id = obj.optLong("id", 0L)
                                val idAnnonce = obj.optString("id_annonce", "")
                                val idUser = obj.optLong("id_user", 0L)
                                val dateEnvoi = if (obj.isNull("date_envoi")) null else obj.optString("date_envoi")
                                val dateLecture = if (obj.isNull("date_lecture")) null else obj.optString("date_lecture")
                                val statutLecture = obj.optString("statut_lecture", "NON_LU")

                                if (idAnnonce.isNotBlank()) {
                                    list.add(
                                        AnnonceDestinataireEntity(
                                            id = id,
                                            idAnnonce = idAnnonce,
                                            idUser = idUser,
                                            dateEnvoi = dateEnvoi,
                                            dateLecture = dateLecture,
                                            statutLecture = statutLecture
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                    list
                }

                listOf(
                    usersDeferred.await(),
                    qDeferred.await(),
                    questDeferred.await(),
                    optDeferred.await(),
                    personnesDeferred.await(),
                    enquetesDeferred.await(),
                    reponsesDeferred.await(),
                    annoncesDeferred.await(),
                    annonceFichiersDeferred.await(),
                    annonceDestinatairesDeferred.await()
                )
            }

            @Suppress("UNCHECKED_CAST")
            val users = rawResults[0] as List<UserEntity>
            @Suppress("UNCHECKED_CAST")
            val questionnaires = rawResults[1] as List<QuestionnaireEntity>
            @Suppress("UNCHECKED_CAST")
            val questions = rawResults[2] as List<QuestionEntity>
            @Suppress("UNCHECKED_CAST")
            val options = rawResults[3] as List<OptionEntity>
            @Suppress("UNCHECKED_CAST")
            val personnes = rawResults[4] as List<PersonneEntity>
            @Suppress("UNCHECKED_CAST")
            val pulledEnquetes = rawResults[5] as List<EnqueteEntity>
            @Suppress("UNCHECKED_CAST")
            val pulledReponses = rawResults[6] as List<ReponseEntity>
            @Suppress("UNCHECKED_CAST")
            val pulledAnnonces = rawResults[7] as List<AnnonceEntity>
            @Suppress("UNCHECKED_CAST")
            val pulledAnnonceFichiers = rawResults[8] as List<AnnonceFichierEntity>
            @Suppress("UNCHECKED_CAST")
            val pulledAnnonceDestinataires = rawResults[9] as List<AnnonceDestinataireEntity>

            // 1. Tehirizina aloha ny mpampiasa (Users)
            if (users.isNotEmpty()) {
                try {
                    repository.saveUsers(users)
                } catch (e: Exception) {
                    // Ignore user save error
                }
            }

            val publicQuestionnaires = questionnaires
            val publicQuestionnaireIds = publicQuestionnaires.map { it.idQuestionnaire }.toSet()

            // Fanavahana ny fanontanian'ny questionnaires tsirairay :
            val publicQuestions = if (publicQuestionnaires.size == 1 && questions.isNotEmpty()) {
                val singleId = publicQuestionnaires.first().idQuestionnaire
                questions.filter { q ->
                    q.idQuestionnaire == singleId || q.idQuestionnaire <= 0
                }.map { q ->
                    if (q.idQuestionnaire <= 0) q.copy(idQuestionnaire = singleId) else q
                }
            } else {
                questions.filter { q ->
                    publicQuestionnaireIds.contains(q.idQuestionnaire)
                }
            }

            // Sivana ho an'ny safidy (options)
            val validQuestionIds = publicQuestions.map { it.idQuestion }.toSet()
            val publicOptions = options.filter { opt ->
                validQuestionIds.contains(opt.idQuestion)
            }

            // 2. Tehirizina ao amin'ny SQLite Room ny questionnaires navoaka (PUBLIE), questions, ary options
            // Antsoina foana na dia foana aza ny publicQuestionnaires mba hanalana avy hatrany izay questionnaires lasa BROUILLON
            repository.savePulledForms(publicQuestionnaires, publicQuestions, publicOptions)

            // 3. Tehirizina ny personnes, enquetes sy reponses efa vita
            if (pulledEnquetes.isNotEmpty() || personnes.isNotEmpty()) {
                try {
                    repository.savePulledEnquetes(personnes, pulledEnquetes, pulledReponses)
                } catch (e: Exception) {
                    // Ignore
                }
            }

            // 4. Tehirizina ny annonces, fichiers, ary destinataires avy ao amin'ny Supabase
            if (pulledAnnonces.isNotEmpty() || pulledAnnonceFichiers.isNotEmpty() || pulledAnnonceDestinataires.isNotEmpty()) {
                try {
                    repository.savePulledAnnonces(pulledAnnonces, pulledAnnonceFichiers, pulledAnnonceDestinataires)
                } catch (_: Exception) {}
            }

            val msg = when {
                publicQuestionnaires.size > 1 -> "${publicQuestionnaires.size} formulaires synchronisés (${publicQuestions.size} questions au total)."
                publicQuestionnaires.size == 1 -> "Formulaire « ${publicQuestionnaires.first().titre} » actualisé (${publicQuestions.size} questions)."
                else -> "Aucun formulaire avec le statut PUBLIÉ disponible sur Supabase."
            }
            addLog(msg, true, publicQuestions.size)
            SyncResult(success = true, syncedCount = publicQuestions.size, failedCount = 0, message = msg)
        } catch (e: Exception) {
            val err = "Impossible de récupérer les formulaires depuis Supabase : ${e.localizedMessage ?: "Problème réseau"}"
            addLog(err, false)
            SyncResult(false, message = err)
        }
    }

    /**
     * Téléchargement direct des questions & options pour un questionnaire publié
     */
    suspend fun fetchAndSaveQuestionsForQuestionnaireDirect(questionnaireId: Long) = withContext(Dispatchers.IO) {
        val cleanUrl = supabaseUrl.trimEnd('/')
        if (cleanUrl.isBlank() || isSimulatedOffline) return@withContext
        try {
            val (qList, optList) = fetchQuestionsAndOptionsForQuestionnaire(cleanUrl, questionnaireId)
            if (qList.isNotEmpty()) {
                repository.insertQuestions(qList)
            }
            if (optList.isNotEmpty()) {
                repository.insertOptions(optList)
            }
        } catch (e: Exception) {
            Log.w("SyncService", "fetchAndSaveQuestionsForQuestionnaireDirect error: ${e.message}")
        }
    }

    /**
     * Maka ny questions sy options an'ny questionnaire iray avy amin'ny Supabase
     */
    private suspend fun fetchQuestionsAndOptionsForQuestionnaire(
        cleanUrl: String,
        targetQuestionnaireId: Long
    ): Pair<List<QuestionEntity>, List<OptionEntity>> = withContext(Dispatchers.IO) {
        val questions = mutableListOf<QuestionEntity>()
        val options = mutableListOf<OptionEntity>()
        try {
            var reqQ = Request.Builder()
                .url("$cleanUrl/rest/v1/questions?select=*")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .get()
                .build()
            var respQ = client.newCall(reqQ).execute()
            if (!respQ.isSuccessful && respQ.code == 404) {
                reqQ = Request.Builder()
                    .url("$cleanUrl/rest/v1/question?select=*")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()
                respQ = client.newCall(reqQ).execute()
            }
            val bodyQ = if (respQ.isSuccessful) respQ.body?.string() else null

            if (!bodyQ.isNullOrBlank()) {
                val arr = JSONArray(bodyQ)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val qId = if (obj.has("id_question") && !obj.isNull("id_question")) obj.optLong("id_question", (i + 1).toLong())
                        else if (obj.has("question_id") && !obj.isNull("question_id")) obj.optLong("question_id", (i + 1).toLong())
                        else if (obj.has("id") && !obj.isNull("id")) obj.optLong("id", (i + 1).toLong())
                        else (i + 1).toLong()

                    val qQuestionnaireId = when {
                        obj.has("id_questionnaire") && !obj.isNull("id_questionnaire") -> obj.optLong("id_questionnaire", 0L)
                        obj.has("questionnaire_id") && !obj.isNull("questionnaire_id") -> obj.optLong("questionnaire_id", 0L)
                        obj.has("idQuestionnaire") && !obj.isNull("idQuestionnaire") -> obj.optLong("idQuestionnaire", 0L)
                        obj.has("id_formulaire") && !obj.isNull("id_formulaire") -> obj.optLong("id_formulaire", 0L)
                        obj.has("formulaire_id") && !obj.isNull("formulaire_id") -> obj.optLong("formulaire_id", 0L)
                        else -> 0L
                    }

                    // Sivana raha manana questionnaire_id manokana, na raisina raha latsaky ny 1 ny isan'ny questionnaires
                    if (qQuestionnaireId > 0 && qQuestionnaireId != targetQuestionnaireId) {
                        continue
                    }

                    val libelle = when {
                        obj.has("libelle") -> obj.getString("libelle")
                        obj.has("label") -> obj.getString("label")
                        obj.has("texte") -> obj.getString("texte")
                        obj.has("title") -> obj.getString("title")
                        obj.has("name") -> obj.getString("name")
                        else -> "Question #$qId"
                    }
                    val type = when {
                        obj.has("type") -> obj.getString("type")
                        obj.has("question_type") -> obj.getString("question_type")
                        else -> "TEXTE"
                    }
                    val oblig = if (obj.has("obligatoire")) obj.getBoolean("obligatoire") else obj.optBoolean("is_required", false)
                    val ordre = if (obj.has("ordre")) obj.getInt("ordre") else obj.optInt("order", i + 1)
                    val isCodeUnique = if (obj.has("est_code_unique")) obj.getBoolean("est_code_unique")
                        else if (obj.has("is_code_unique")) obj.getBoolean("is_code_unique")
                        else false

                    questions.add(
                        QuestionEntity(
                            idQuestion = qId,
                            idQuestionnaire = targetQuestionnaireId,
                            libelle = libelle,
                            type = type.uppercase(),
                            obligatoire = oblig,
                            ordre = ordre,
                            estCodeUnique = isCodeUnique,
                            createdAt = obj.optString("created_at", System.currentTimeMillis().toString()),
                            updatedAt = obj.optString("updated_at", System.currentTimeMillis().toString())
                        )
                    )
                }
            }

            // Options
            val qIds = questions.map { it.idQuestion }
            if (qIds.isNotEmpty()) {
                var reqOpt = Request.Builder()
                    .url("$cleanUrl/rest/v1/options?select=*")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .get()
                    .build()
                var respOpt = client.newCall(reqOpt).execute()
                if (!respOpt.isSuccessful && respOpt.code == 404) {
                    reqOpt = Request.Builder()
                        .url("$cleanUrl/rest/v1/option?select=*")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .get()
                        .build()
                    respOpt = client.newCall(reqOpt).execute()
                }
                if (respOpt.isSuccessful) {
                    val arr = JSONArray(respOpt.body?.string() ?: "[]")
                    val qIdSet = qIds.toSet()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val optId = if (obj.has("id_option") && !obj.isNull("id_option")) obj.optLong("id_option", (i + 1).toLong())
                            else if (obj.has("option_id") && !obj.isNull("option_id")) obj.optLong("option_id", (i + 1).toLong())
                            else obj.optLong("id", (i + 1).toLong())

                        val questionId = when {
                            obj.has("id_question") && !obj.isNull("id_question") -> obj.optLong("id_question", 0L)
                            obj.has("question_id") && !obj.isNull("question_id") -> obj.optLong("question_id", 0L)
                            obj.has("idQuestion") && !obj.isNull("idQuestion") -> obj.optLong("idQuestion", 0L)
                            else -> 0L
                        }

                        if (qIdSet.contains(questionId)) {
                            val libelle = when {
                                obj.has("libelle") -> obj.getString("libelle")
                                obj.has("label") -> obj.getString("label")
                                obj.has("texte") -> obj.getString("texte")
                                obj.has("value") -> obj.getString("value")
                                else -> "Option #$optId"
                            }
                            val ordre = if (obj.has("ordre")) obj.getInt("ordre") else obj.optInt("order", i + 1)
                            options.add(
                                OptionEntity(
                                    idOption = optId,
                                    idQuestion = questionId,
                                    libelle = libelle,
                                    ordre = ordre
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        Pair(questions, options)
    }

    /**
     * Fanamarinana en direct sy haingana dia haingana (Real-time fast watcher)
     * Mahita avy hatrany ny fiovan'ny questionnaire (PUBLIÉ <-> BROUILLON) ao amin'ny Supabase
     * Tsy misy fahatarana ary manavao ny SQLite Room eo no ho eo.
     */
    suspend fun quickCheckQuestionnairesRealtime(): Boolean = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) return@withContext false
        val cleanUrl = supabaseUrl.trimEnd('/')

        try {
            var req = Request.Builder()
                .url("$cleanUrl/rest/v1/questionnaires?select=*")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .get()
                .build()

            var resp = client.newCall(req).execute()
            if (!resp.isSuccessful && resp.code == 404) {
                req = Request.Builder()
                    .url("$cleanUrl/rest/v1/formulaires?select=*")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()
                resp = client.newCall(req).execute()
            }
            if (!resp.isSuccessful && resp.code == 404) {
                req = Request.Builder()
                    .url("$cleanUrl/rest/v1/surveys?select=*")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()
                resp = client.newCall(req).execute()
            }
            if (!resp.isSuccessful) {
                return@withContext false
            }

            val bodyStr = resp.body?.string() ?: ""
            val arr = try { JSONArray(bodyStr) } catch (_: Exception) { JSONArray() }

            val parsedList = mutableListOf<SupabaseQuestionnaireStatus>()
            val publishedToSave = mutableListOf<QuestionnaireEntity>()
            val draftIdsToPurge = mutableListOf<Pair<Long, String>>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = when {
                    obj.has("id_questionnaire") && !obj.isNull("id_questionnaire") -> obj.getLong("id_questionnaire")
                    obj.has("id") && !obj.isNull("id") -> obj.getLong("id")
                    obj.has("questionnaire_id") && !obj.isNull("questionnaire_id") -> obj.getLong("questionnaire_id")
                    else -> (i + 1).toLong()
                }
                val title = when {
                    obj.has("titre") && !obj.isNull("titre") -> obj.getString("titre")
                    obj.has("title") && !obj.isNull("title") -> obj.getString("title")
                    obj.has("nom") && !obj.isNull("nom") -> obj.getString("nom")
                    obj.has("label") && !obj.isNull("label") -> obj.getString("label")
                    else -> "Questionnaire #$id"
                }
                val rawStatut = extractQuestionnaireStatut(obj)

                val desc = when {
                    obj.has("description") && !obj.isNull("description") -> obj.getString("description")
                    obj.has("desc") && !obj.isNull("desc") -> obj.getString("desc")
                    else -> null
                }
                val ver = obj.optString("version", "v1.0")
                val createdBy = when {
                    obj.has("created_by") && !obj.isNull("created_by") -> obj.getLong("created_by")
                    obj.has("id_user") && !obj.isNull("id_user") -> obj.getLong("id_user")
                    obj.has("user_id") && !obj.isNull("user_id") -> obj.getLong("user_id")
                    else -> 1L
                }
                val updatedAt = obj.optString("updated_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

                val isPub = isPublicStatus(rawStatut)

                parsedList.add(
                    SupabaseQuestionnaireStatus(
                        idQuestionnaire = id,
                        titre = title,
                        description = desc,
                        version = ver,
                        statut = if (isPub) "PUBLIÉ" else rawStatut.ifBlank { "BROUILLON" },
                        isPublished = isPub,
                        updatedAt = updatedAt
                    )
                )

                if (isPub) {
                    publishedToSave.add(
                        QuestionnaireEntity(
                            idQuestionnaire = id,
                            titre = title,
                            description = desc,
                            version = ver,
                            statut = "PUBLIÉ",
                            createdBy = createdBy,
                            createdAt = obj.optString("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())),
                            updatedAt = updatedAt
                        )
                    )
                } else {
                    draftIdsToPurge.add(Pair(id, title))
                }
            }

            var hasChanged = false
            val allLocalRaw = repository.getAllQuestionnairesRaw()
            val currentLocalMap = allLocalRaw.associateBy { it.idQuestionnaire }

            // 1. Marquer comme BROUILLON les questionnaires devenus brouillons
            for ((draftId, draftTitle) in draftIdsToPurge) {
                val localQ = currentLocalMap[draftId]
                val wasActive = localQ != null && !SyncService.isDraftStatus(localQ.statut)
                if (wasActive) {
                    hasChanged = true
                    addLog("⚡ [En direct Supabase] Le formulaire « $draftTitle » (#$draftId) est passé en statut BROUILLON. Masqué de l'application.", true)
                }
                try {
                    repository.markQuestionnaireAsDraft(draftId)
                } catch (_: Exception) {}
            }

            // 2. Supprimer uniquement les questionnaires réellement supprimés dans Supabase
            if (parsedList.isNotEmpty()) {
                val allRemoteIds = parsedList.map { it.idQuestionnaire }.toSet()
                for (localQ in allLocalRaw) {
                    if (localQ.idQuestionnaire != 1L && !allRemoteIds.contains(localQ.idQuestionnaire)) {
                        hasChanged = true
                        try {
                            repository.deleteQuestionnaireCompletely(localQ.idQuestionnaire)
                            addLog("⚡ [En direct Supabase] Le formulaire « ${localQ.titre} » (#${localQ.idQuestionnaire}) a été supprimé.", true)
                        } catch (_: Exception) {}
                    }
                }
            }

            // 3. Manampy na manavao ireo izay lasa PUBLIÉ
            for (pubQ in publishedToSave) {
                val localQ = currentLocalMap[pubQ.idQuestionnaire]
                val localQuestionsCount = repository.countQuestionsByQuestionnaire(pubQ.idQuestionnaire)
                
                // Mamaritra avy hatrany ho PUBLIÉ ao amin'ny SQLite ihany raha mbola tsy nisy na niova
                val needsUpdate = localQ == null || localQ.statut != pubQ.statut || localQ.titre != pubQ.titre || localQ.description != pubQ.description || localQ.version != pubQ.version
                if (needsUpdate) {
                    try {
                        repository.insertQuestionnaire(pubQ)
                        repository.markQuestionnaireAsPublished(pubQ.idQuestionnaire)
                    } catch (_: Exception) {}
                }

                val wasDraft = localQ != null && SyncService.isDraftStatus(localQ.statut)
                if (localQ == null || wasDraft) {
                    hasChanged = true
                    addLog("⚡ [En direct Supabase] Le formulaire « ${pubQ.titre} » (#${pubQ.idQuestionnaire}) est de nouveau PUBLIÉ.", true)
                }

                val needsQuestions = localQuestionsCount == 0 || wasDraft
                if (needsQuestions) {
                    try {
                        val (qList, optList) = fetchQuestionsAndOptionsForQuestionnaire(cleanUrl, pubQ.idQuestionnaire)
                        // Tsy mamorona fallback questions intsony! Izay avy any amin'ny Supabase ihany no tehirizina.
                        if (qList.isNotEmpty()) {
                            hasChanged = true
                            repository.insertQuestions(qList)
                            if (optList.isNotEmpty()) {
                                repository.insertOptions(optList)
                            }
                            addLog("⚡ [En direct Supabase] Le formulaire « ${pubQ.titre} » (#${pubQ.idQuestionnaire}) est disponible (${qList.size} questions).", true)
                        }
                    } catch (e: Exception) {
                        Log.w("SyncService", "Error loading questions for ${pubQ.idQuestionnaire}: ${e.message}")
                    }
                }
            }

            // 4. Mamaritra ny questionnaires rehetra navoaka ho PUBLIÉ ao amin'ny SQLite
            val allPublishedIds = publishedToSave.map { it.idQuestionnaire }
            if (allPublishedIds.isNotEmpty()) {
                repository.markOtherQuestionnairesAsDraft(allPublishedIds)
            }
            // Tsy manao markAllQuestionnairesAsDraft() raha sendra foana na nisy olana ny réseau!
            for ((draftId, _) in draftIdsToPurge) {
                repository.markQuestionnaireAsDraft(draftId)
            }

            // Manisa ny questions ho an'ny lisitry ny remote
            val updatedParsedList = parsedList.map { item ->
                if (item.isPublished) {
                    val count = repository.countQuestionsByQuestionnaire(item.idQuestionnaire)
                    item.copy(questionsCount = count)
                } else {
                    item
                }
            }

            val previousList = _remoteQuestionnaires.value
            if (previousList != updatedParsedList) {
                _remoteQuestionnaires.value = updatedParsedList
                hasChanged = true
            }

            return@withContext hasChanged
        } catch (_: Exception) {
            return@withContext false
        }
    }

    /**
     * Fanamarinana haingana ny satan'ny kaonty (Vérification en direct du statut du compte)
     * Raha lasa INACTIF na DESACTIVE ilay kaonty ao amin'ny Supabase, dia miverina avy hatrany amin'ny login.
     * Raha lasa ACTIF indray, dia manavao ny SQLite eo no ho eo.
     */
    suspend fun checkUserAccountStatus(user: UserEntity): UserAccountStatusResult = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) {
            val local = if (user.idUser > 0L) repository.findUserById(user.idUser) else repository.findUserByEmailOrPhone(user.email)
            return@withContext if (local != null && isInactiveStatut(local.statut)) {
                UserAccountStatusResult.Inactive("Compte désactivé (Mode local)")
            } else {
                UserAccountStatusResult.Active(local ?: user)
            }
        }

        try {
            val cleanUrl = supabaseUrl.trimEnd('/')
            var req = Request.Builder()
                .url("$cleanUrl/rest/v1/users?select=*")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .get()
                .build()

            var resp = client.newCall(req).execute()
            if (!resp.isSuccessful && resp.code == 404) {
                req = Request.Builder()
                    .url("$cleanUrl/rest/v1/utilisateurs?select=*")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .get()
                    .build()
                resp = client.newCall(req).execute()
            }

            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: "[]"
                val arr = JSONArray(body)
                var matchedObj: JSONObject? = null

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val rEmail = o.optString("email", "").trim()
                    val rTel = o.optString("telephone", "").trim()
                    val rNom = o.optString("nom", "").trim()
                    val rPrenom = o.optString("prenom", "").trim()
                    val rId = when {
                        o.has("id_user") && !o.isNull("id_user") -> o.optLong("id_user", -1L)
                        o.has("id") && !o.isNull("id") -> o.optLong("id", -1L)
                        o.has("user_id") && !o.isNull("user_id") -> o.optLong("user_id", -1L)
                        else -> -1L
                    }
                    val emailMatch = user.email.isNotBlank() && rEmail.equals(user.email.trim(), ignoreCase = true)
                    val telMatch = !user.telephone.isNullOrBlank() && rTel.isNotBlank() && rTel == user.telephone!!.trim()
                    val idMatch = user.idUser > 0L && rId > 0L && rId == user.idUser
                    val nameMatch = user.nom.isNotBlank() && user.prenom.isNotBlank() &&
                            rNom.equals(user.nom.trim(), ignoreCase = true) &&
                            rPrenom.equals(user.prenom.trim(), ignoreCase = true)

                    if (emailMatch || telMatch || idMatch || nameMatch) {
                        matchedObj = o
                        break
                    }
                }

                if (matchedObj != null) {
                    val parsedStatut = extractUserStatut(matchedObj)

                    if (isInactiveStatut(parsedStatut)) {
                        // Update local Room database so offline mode also locks it
                        if (user.idUser > 0L) {
                            repository.updateUserStatut(user.idUser, "INACTIF")
                        }
                        if (user.email.isNotBlank()) {
                            repository.updateUserStatutByEmail(user.email, "INACTIF")
                        }
                        return@withContext UserAccountStatusResult.Inactive("Ce compte est désactivé.")
                    } else {
                        // Efa ACTIF indray! Update SQLite Room to ACTIF immediately
                        if (user.idUser > 0L) {
                            repository.updateUserStatut(user.idUser, "ACTIF")
                        }
                        if (user.email.isNotBlank()) {
                            repository.updateUserStatutByEmail(user.email, "ACTIF")
                        }
                        val rawPhoto = if (matchedObj.isNull("photo")) null else matchedObj.optString("photo").takeIf { it.isNotBlank() }
                        val rawPhotoUrl = when {
                            !matchedObj.isNull("photo_url") -> matchedObj.optString("photo_url").takeIf { it.isNotBlank() }
                            !matchedObj.isNull("photo") -> matchedObj.optString("photo").takeIf { it.isNotBlank() }
                            else -> null
                        }
                        val updated = user.copy(
                            nom = matchedObj.optString("nom", user.nom),
                            prenom = matchedObj.optString("prenom", user.prenom),
                            role = matchedObj.optString("role", user.role).uppercase(),
                            statut = "ACTIF",
                            photo = rawPhoto ?: user.photo,
                            photoUrl = rawPhotoUrl ?: user.photoUrl
                        )
                        repository.saveUser(updated)
                        return@withContext UserAccountStatusResult.Active(updated)
                    }
                }
            }
        } catch (_: Exception) {}

        val local = if (user.idUser > 0L) repository.findUserById(user.idUser) else repository.findUserByEmailOrPhone(user.email)
        return@withContext if (local != null && isInactiveStatut(local.statut)) {
            UserAccountStatusResult.Inactive("Compte désactivé (Mode local)")
        } else {
            UserAccountStatusResult.Active(local ?: user)
        }
    }

    /**
     * Maka ny mpampiasa rehetra avy ao amin'ny Supabase sy manavao ny SQLite Room
     */
    suspend fun pullUsersFromSupabase(): List<UserEntity> = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) return@withContext emptyList()
        val cleanUrl = supabaseUrl.trimEnd('/')
        try {
            var req = Request.Builder()
                .url("$cleanUrl/rest/v1/users?select=*")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .get()
                .build()

            var resp = client.newCall(req).execute()
            if (!resp.isSuccessful && resp.code == 404) {
                req = Request.Builder()
                    .url("$cleanUrl/rest/v1/utilisateurs?select=*")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()
                resp = client.newCall(req).execute()
            }

            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: "[]"
                val jsonArr = JSONArray(bodyStr)
                val list = mutableListOf<UserEntity>()
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val rawPhoto = if (obj.isNull("photo")) null else obj.optString("photo").takeIf { it.isNotBlank() }
                    val rawPhotoUrl = when {
                        !obj.isNull("photo_url") -> obj.optString("photo_url").takeIf { it.isNotBlank() }
                        !obj.isNull("photo") -> obj.optString("photo").takeIf { it.isNotBlank() }
                        else -> null
                    }
                    val rawStatut = extractUserStatut(obj)
                    val effectiveStatut = if (isInactiveStatut(rawStatut)) "INACTIF" else "ACTIF"

                    val email = obj.optString("email", "").trim()
                    val idUser = when {
                        obj.has("id_user") && !obj.isNull("id_user") -> obj.optLong("id_user", (i + 1).toLong())
                        obj.has("id") && !obj.isNull("id") -> obj.optLong("id", (i + 1).toLong())
                        obj.has("user_id") && !obj.isNull("user_id") -> obj.optLong("user_id", (i + 1).toLong())
                        else -> (i + 1).toLong()
                    }
                    val rawRegion = if (obj.isNull("region") || obj.optString("region").isBlank()) "Analamanga" else obj.optString("region")
                    val rawMatricule = if (obj.isNull("matricule") || obj.optString("matricule").isBlank()) null else obj.optString("matricule")

                    val user = UserEntity(
                        idUser = idUser,
                        nom = obj.optString("nom", "Nom"),
                        prenom = obj.optString("prenom", "Prénom"),
                        email = email,
                        telephone = if (obj.isNull("telephone")) null else obj.getString("telephone"),
                        motDePasse = if (obj.isNull("mot_de_passe")) null else obj.getString("mot_de_passe"),
                        role = obj.optString("role", "ENQUETEUR"),
                        statut = effectiveStatut,
                        region = rawRegion,
                        matricule = rawMatricule,
                        photo = rawPhoto,
                        photoUrl = rawPhotoUrl,
                        createdAt = obj.optString("created_at", System.currentTimeMillis().toString()),
                        updatedAt = obj.optString("updated_at", System.currentTimeMillis().toString())
                    )
                    list.add(user)
                }
                if (list.isNotEmpty()) {
                    repository.saveUsers(list)
                }
                return@withContext list
            }
        } catch (_: Exception) {}
        return@withContext emptyList()
    }

    companion object {
        /**
         * Famakafakana ny satan'ny kaonty avy amin'ny JSON Supabase na SQLite
         * Mandray an-tanana ny tranga rehetra (boolean, int, text, anarana samihafa toy ny:
         * statut, status, etat, is_active, actif, active, enabled, compte_actif, sns.)
         */
        /**
         * Famakafakana ny satan'ny kaonty avy amin'ny JSON Supabase na SQLite
         * Mandray an-tanana ny tranga rehetra (boolean, int, text, anarana samihafa toy ny:
         * statut, status, etat, is_active, actif, active, enabled, compte_actif, is_enabled, disabled, sns.)
         * Fitsipika fototra : Raha misy indicator milaza mazava fa INACTIF / DÉSACTIVÉ na FALSE na 0 dia INACTIF avy hatrany.
         */
        fun extractUserStatut(obj: JSONObject): String {
            // 1. ZAHANA VOALOHANY RAHA MISY FAMANTARANA INACTIF / DESACTIVE (Security first)
            val booleanKeys = listOf("is_active", "isactive", "actif", "active", "enabled", "compte_actif", "is_enabled")
            for (key in booleanKeys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val raw = obj.opt(key)
                    if (raw is Boolean && !raw) return "INACTIF"
                    val str = raw.toString().trim().lowercase()
                    if (str == "false" || str == "0" || str == "no" || str == "non" || str == "f" || str == "off" || str == "inactif" || str == "inactive") {
                        return "INACTIF"
                    }
                }
            }

            val disabledKeys = listOf("disabled", "is_disabled", "bloque", "bloqué", "is_blocked", "supprime", "supprimé", "is_deleted", "suspendu", "is_suspended")
            for (key in disabledKeys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val raw = obj.opt(key)
                    if (raw is Boolean && raw) return "INACTIF"
                    val str = raw.toString().trim().lowercase()
                    if (str == "true" || str == "1" || str == "yes" || str == "oui" || str == "t") {
                        return "INACTIF"
                    }
                }
            }

            val stringKeys = listOf("statut", "status", "etat", "state", "account_status", "user_status", "compte_statut")
            for (key in stringKeys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val raw = obj.opt(key)
                    if (raw is Boolean && !raw) return "INACTIF"
                    val s = raw.toString().trim()
                    if (isInactiveStatut(s)) return "INACTIF"
                }
            }

            // 2. RAHA TSY NISY FAMANTARANA INACTIF, ZAHANA RAHA MISY FAMANTARANA MAZAVA FA ACTIF
            for (key in booleanKeys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val raw = obj.opt(key)
                    if (raw is Boolean && raw) return "ACTIF"
                    val str = raw.toString().trim().lowercase()
                    if (str == "true" || str == "1" || str == "yes" || str == "oui" || str == "t" || str == "actif" || str == "active") {
                        return "ACTIF"
                    }
                }
            }

            for (key in stringKeys) {
                if (obj.has(key) && !obj.isNull(key)) {
                    val raw = obj.opt(key)
                    if (raw is Boolean && raw) return "ACTIF"
                    val s = raw.toString().trim()
                    if (s.isNotBlank() && !isInactiveStatut(s)) {
                        return "ACTIF"
                    }
                }
            }

            return "ACTIF"
        }

        /**
         * Fanamarinana feno raha INACTIF / DESACTIVE ny kaonty iray
         */
        fun isInactiveStatut(statutStr: String?): Boolean {
            if (statutStr.isNullOrBlank()) return false
            val s = statutStr.trim().lowercase()

            // Mazava tsara fa actif
            if (s == "actif" || s == "active" || s == "activee" || s == "activé" || s == "activer" || 
                s == "actif/active" || s == "valide" || s == "validé" || s == "true" || s == "1" || 
                s == "ok" || s == "open" || s == "enable" || s == "enabled" || s == "oui" || s == "yes") {
                return false
            }

            // Mazava tsara fa inactif / désactivé
            if (s == "inactif" || s == "inactive" || s == "desactive" || s == "désactivé" || 
                s == "desactiver" || s == "désactiver" || s == "bloque" || s == "bloqué" || 
                s == "suspendu" || s == "suspended" || s == "disabled" || s == "disable" || 
                s == "refuse" || s == "refusé" || s == "revoque" || s == "révoqué" || 
                s == "archive" || s == "archivé" || s == "supprime" || s == "supprimé" || 
                s == "ferme" || s == "fermé" || s == "banni" || s == "banned" || 
                s == "cloture" || s == "clôturé" || s == "false" || s == "0" || s == "no" || s == "non" || s == "off") {
                return true
            }

            // Substring detection ho an'ireo fiovaovana sy lantom-peo
            return s.contains("inact") || 
                   s.contains("desactiv") || 
                   s.contains("désactiv") || 
                   s.contains("disabl") || 
                   s.contains("bloqu") || 
                   s.contains("suspend") || 
                   s.contains("revoq") || 
                   s.contains("révoq") || 
                   s.contains("bann") ||
                   s.contains("non_actif") ||
                   s.contains("non-actif") ||
                   s.contains("non actif")
        }

        /**
         * Famoahana ny statut marina avy amin'ny JSONObject Supabase
         */
        fun extractQuestionnaireStatut(obj: JSONObject): String {
            return when {
                obj.has("statut") && !obj.isNull("statut") -> obj.optString("statut")
                obj.has("status") && !obj.isNull("status") -> obj.optString("status")
                obj.has("etat") && !obj.isNull("etat") -> obj.optString("etat")
                obj.has("state") && !obj.isNull("state") -> obj.optString("state")
                obj.has("is_published") && !obj.isNull("is_published") -> if (obj.optBoolean("is_published")) "PUBLIE" else "BROUILLON"
                obj.has("publie") && !obj.isNull("publie") -> if (obj.optBoolean("publie")) "PUBLIE" else "BROUILLON"
                obj.has("is_draft") && !obj.isNull("is_draft") -> if (obj.optBoolean("is_draft")) "BROUILLON" else "PUBLIE"
                else -> "PUBLIE"
            }.trim()
        }

        /**
         * Fanamarinana raha BROUILLON na DRAFT ny questionnaire ao amin'ny Supabase :
         * Tsy heverina ho draft raha tsy voafaritra mazava tsara (brouillon, draft, inactif, supprimé).
         */
        fun isDraftStatus(statutStr: String?): Boolean {
            if (statutStr.isNullOrBlank()) return false
            val s = statutStr.trim().lowercase()
            return s.contains("brouill") ||
                    s.contains("draft") ||
                    s.contains("non_publi") ||
                    s.contains("non-publi") ||
                    s.contains("non publi") ||
                    s.contains("inact") ||
                    s.contains("desactiv") ||
                    s.contains("désactiv") ||
                    s.contains("archiv") ||
                    s.contains("supprim") ||
                    s.contains("clotur") ||
                    s.contains("clôtur") ||
                    s.contains("ferm") ||
                    s.contains("attent") ||
                    s.contains("en_cours") ||
                    s.contains("en cours") ||
                    s.contains("cree") ||
                    s.contains("créé") ||
                    s.contains("nouveau") ||
                    s == "false" ||
                    s == "0"
        }

        /**
         * Fanamarinana raha azo ampiasaina (valide / publie) ny questionnaire ao amin'ny Supabase :
         * Tsy ekena ny brouillons, archivés, supprimés, clôturés, sns.
         */
        fun isPublicStatus(statutStr: String?): Boolean {
            if (statutStr.isNullOrBlank()) return true
            val s = statutStr.trim().lowercase()
            if (isDraftStatus(s)) return false
            if (s.contains("publi") || s.contains("public") || s.contains("actif") || s.contains("active") || s.contains("valide") || s == "true" || s == "1") {
                return true
            }
            return !isDraftStatus(s)
        }
    }

    /**
     * Authentification de l'utilisateur (Enquêteur / Admin)
     * - En ligne : vérifie directement dans la table `users` de Supabase et met à jour SQLite.
     * - Hors ligne : vérifie dans la table `users` locale de SQLite.
     * - Prise en charge automatique de tous les formats d'identifiants (Email, Téléphone, Nom).
     */
    suspend fun loginUser(identifier: String, motDePasse: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        val cleanPass = motDePasse.trim()

        if (cleanId.isEmpty()) {
            return@withContext Result.failure(Exception("Veuillez saisir votre identifiant ou email."))
        }
        if (cleanPass.isEmpty()) {
            return@withContext Result.failure(Exception("Veuillez saisir votre mot de passe."))
        }

        // 1. Fanamarinana en direct ao amin'ny Supabase aloha raha en ligne (Source de vérité absolue)
        val localUser = repository.findUserByEmailOrPhone(cleanId)

        if (!isSimulatedOffline) {
            try {
                val cleanUrl = supabaseUrl.trimEnd('/')
                var req = Request.Builder()
                    .url("$cleanUrl/rest/v1/users?select=*")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                    .addHeader("Pragma", "no-cache")
                    .get()
                    .build()
                var resp = client.newCall(req).execute()
                if (!resp.isSuccessful && resp.code == 404) {
                    req = Request.Builder()
                        .url("$cleanUrl/rest/v1/utilisateurs?select=*")
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                        .addHeader("Pragma", "no-cache")
                        .get()
                        .build()
                    resp = client.newCall(req).execute()
                }

                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: "[]"
                    val jsonArr = JSONArray(bodyStr)
                    val cleanPhone = cleanId.replace(" ", "").replace("-", "").replace(".", "")
                    for (i in 0 until jsonArr.length()) {
                        val obj = jsonArr.getJSONObject(i)
                        val email = obj.optString("email", "").trim()
                        val nom = obj.optString("nom", "").trim()
                        val prenom = obj.optString("prenom", "").trim()
                        val telephone = if (obj.isNull("telephone")) "" else obj.optString("telephone", "").trim()
                        val cleanObjPhone = telephone.replace(" ", "").replace("-", "").replace(".", "")
                        val identifiant = obj.optString("identifiant", obj.optString("username", "")).trim()
                        val matricule = if (obj.isNull("matricule")) "" else obj.optString("matricule", "").trim()

                        val matches = email.equals(cleanId, ignoreCase = true) ||
                                      (cleanObjPhone.isNotBlank() && cleanObjPhone.equals(cleanPhone, ignoreCase = true)) ||
                                      (email.isNotBlank() && email.substringBefore("@").equals(cleanId, ignoreCase = true)) ||
                                      (matricule.isNotBlank() && matricule.equals(cleanId, ignoreCase = true)) ||
                                      identifiant.equals(cleanId, ignoreCase = true) ||
                                      nom.equals(cleanId, ignoreCase = true) ||
                                      prenom.equals(cleanId, ignoreCase = true) ||
                                      "$prenom $nom".trim().equals(cleanId, ignoreCase = true) ||
                                      "$nom $prenom".trim().equals(cleanId, ignoreCase = true)

                        if (matches) {
                            val rawStatut = extractUserStatut(obj)
                            val isInactive = isInactiveStatut(rawStatut)

                            val userId = when {
                                obj.has("id_user") && !obj.isNull("id_user") -> obj.optLong("id_user", (i + 1).toLong())
                                obj.has("id") && !obj.isNull("id") -> obj.optLong("id", (i + 1).toLong())
                                obj.has("user_id") && !obj.isNull("user_id") -> obj.optLong("user_id", (i + 1).toLong())
                                else -> (i + 1).toLong()
                            }

                            // Raha DÉSACTIVÉ / INACTIF ao amin'ny database : havaozy eo no ho eo ny SQLite ary tsipaho ny fidirana
                            if (isInactive) {
                                try {
                                    if (email.isNotBlank()) repository.updateUserStatutByEmail(email, "INACTIF")
                                    if (userId > 0L) repository.updateUserStatut(userId, "INACTIF")
                                } catch (_: Exception) {}
                                return@withContext Result.failure(Exception("Ce compte est désactivé par l'administrateur. Veuillez contacter votre superviseur."))
                            }

                            val expectedPass = when {
                                obj.has("mot_de_passe") && !obj.isNull("mot_de_passe") -> obj.getString("mot_de_passe").trim()
                                obj.has("password") && !obj.isNull("password") -> obj.getString("password").trim()
                                obj.has("mdp") && !obj.isNull("mdp") -> obj.getString("mdp").trim()
                                obj.has("pass") && !obj.isNull("pass") -> obj.getString("pass").trim()
                                else -> ""
                            }

                            // Fanamarinana teny miafina
                            if (expectedPass.isNotBlank() && expectedPass != cleanPass) {
                                return@withContext Result.failure(Exception("Mot de passe incorrect."))
                            }

                            val rawPhoto = if (obj.isNull("photo")) null else obj.optString("photo").takeIf { it.isNotBlank() }
                            val rawPhotoUrl = when {
                                !obj.isNull("photo_url") -> obj.optString("photo_url").takeIf { it.isNotBlank() }
                                !obj.isNull("photo") -> obj.optString("photo").takeIf { it.isNotBlank() }
                                else -> null
                            }

                            val rawRegion = if (obj.isNull("region") || obj.optString("region").isBlank()) "Analamanga" else obj.optString("region")
                            val rawMatricule = if (obj.isNull("matricule") || obj.optString("matricule").isBlank()) null else obj.optString("matricule")

                            val onlineUser = UserEntity(
                                idUser = userId,
                                nom = if (nom.isNotBlank()) nom else "Utilisateur",
                                prenom = if (prenom.isNotBlank()) prenom else cleanId.substringBefore("@"),
                                email = if (email.isNotBlank()) email else cleanId,
                                telephone = telephone.ifBlank { null },
                                motDePasse = if (expectedPass.isNotBlank()) expectedPass else cleanPass,
                                role = obj.optString("role", if (cleanId.contains("admin", ignoreCase = true)) "ADMIN" else "ENQUETEUR").uppercase(),
                                statut = "ACTIF",
                                region = rawRegion,
                                matricule = rawMatricule,
                                photo = rawPhoto,
                                photoUrl = rawPhotoUrl,
                                createdAt = obj.optString("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())),
                                updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            )

                            // Sauvegarde dans SQLite local ho an'ny fampiasana maharitra sy fidirana hors-ligne
                            repository.saveUsers(listOf(onlineUser))
                            if (onlineUser.email.isNotBlank()) {
                                repository.updateUserStatutByEmail(onlineUser.email, "ACTIF")
                            }
                            if (onlineUser.idUser > 0L) {
                                repository.updateUserStatut(onlineUser.idUser, "ACTIF")
                            }
                            addLog("Connexion en ligne réussie : ${onlineUser.prenom} ${onlineUser.nom} (${onlineUser.role})", true)
                            return@withContext Result.success(onlineUser)
                        }
                    }
                }
            } catch (e: Exception) {
                addLog("Vérification en ligne (${e.localizedMessage}), recherche dans la base locale...", false)
            }
        }

        // 2. Fallback SQLite local (Mode hors-ligne na tsy nety ny fifandraisana)
        if (localUser != null) {
            if (isInactiveStatut(localUser.statut)) {
                return@withContext Result.failure(Exception("Ce compte est désactivé par l'administrateur. Veuillez contacter votre superviseur."))
            }

            if (!localUser.motDePasse.isNullOrBlank() && localUser.motDePasse != cleanPass) {
                return@withContext Result.failure(Exception("Mot de passe incorrect."))
            }

            addLog("Connexion locale réussie : ${localUser.prenom} ${localUser.nom} (${localUser.role})", true)
            return@withContext Result.success(localUser)
        }

        // 3. Compte non trouvé : Refus strict d'accès (Accès réservé aux utilisateurs enregistrés)
        Result.failure(Exception("Identifiant ou mot de passe incorrect. Seuls les comptes enregistrés peuvent se connecter."))
    }

    /**
     * Fanovana teny miafina (Changement de mot de passe)
     * - Fanamarinana ny teny miafina taloha (Ancien mot de passe)
     * - Fampitoviana mivantana any amin'ny Supabase REST API (`PATCH /rest/v1/users` na `/rest/v1/utilisateurs`)
     * - Fitehirizana maharitra ao amin'ny SQLite (Room Database)
     */
    suspend fun changePassword(
        user: UserEntity,
        oldPasswordInput: String?,
        newPasswordInput: String
    ): Result<Pair<UserEntity, String>> = withContext(Dispatchers.IO) {
        val cleanNewPass = newPasswordInput.trim()
        val cleanOldPass = oldPasswordInput?.trim() ?: ""

        if (cleanNewPass.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Le mot de passe doit comporter au moins 4 caractères."))
        }

        // Vérification de l'ancien mot de passe
        if (!user.motDePasse.isNullOrBlank() && cleanOldPass.isNotEmpty()) {
            if (user.motDePasse != cleanOldPass) {
                return@withContext Result.failure(IllegalArgumentException("Ancien mot de passe incorrect."))
            }
        }

        val updatedTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val updatedUser = user.copy(
            motDePasse = cleanNewPass,
            updatedAt = updatedTimestamp
        )

        var statusMessage = ""

        // 1. Envoi vers Supabase si en ligne
        if (!isSimulatedOffline) {
            try {
                val cleanUrl = supabaseUrl.trimEnd('/')
                val targetUrl = if (user.idUser > 0L) {
                    "$cleanUrl/rest/v1/users?id_user=eq.${user.idUser}"
                } else {
                    "$cleanUrl/rest/v1/users?email=eq.${user.email}"
                }

                val jsonBody = JSONObject().apply {
                    put("mot_de_passe", cleanNewPass)
                    put("password", cleanNewPass)
                    put("mdp", cleanNewPass)
                    put("updated_at", updatedTimestamp)
                }.toString()

                val request = Request.Builder()
                    .url(targetUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                var response = client.newCall(request).execute()
                if (!response.isSuccessful && response.code == 404) {
                    val altUrl = if (user.idUser > 0L) {
                        "$cleanUrl/rest/v1/utilisateurs?id_user=eq.${user.idUser}"
                    } else {
                        "$cleanUrl/rest/v1/utilisateurs?email=eq.${user.email}"
                    }
                    val altRequest = Request.Builder()
                        .url(altUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    response = client.newCall(altRequest).execute()
                }

                if (response.isSuccessful) {
                    statusMessage = "Mot de passe modifié et synchronisé avec succès dans Supabase !"
                    addLog("Nouveau mot de passe synchronisé (${user.prenom} ${user.nom})", true)
                } else {
                    val code = response.code
                    statusMessage = "Mot de passe enregistré localement (Erreur Supabase HTTP $code)."
                    addLog("Synchronisation Supabase différée (HTTP $code)", false)
                }
            } catch (e: Exception) {
                statusMessage = "Mot de passe enregistré localement. Synchronisation ultérieure."
                addLog("Synchronisation ultérieure (${e.localizedMessage})", false)
            }
        } else {
            statusMessage = "Mot de passe enregistré localement (Mode Hors-ligne)."
            addLog("Mot de passe modifié localement (Mode Hors-ligne)", true)
        }

        // 2. Sauvegarde SQLite (Room)
        repository.saveUser(updatedUser)
        if (user.idUser > 0L) {
            repository.updateUserPassword(user.idUser, cleanNewPass)
        }

        Result.success(Pair(updatedUser, statusMessage))
    }

    /**
     * Fanavaozana ny sarin'ny mpampiasa (Photo de l'enquêteur)
     * - Tsy azon'ny mpanadihady ovaina ny sary (verrouillé pour les enquêteurs)
     */
    suspend fun updateUserPhoto(
        user: UserEntity,
        photoPathOrUrl: String
    ): Result<Pair<UserEntity, String>> = withContext(Dispatchers.IO) {
        // Tsy azon'ny mpanadihady ovaina ny sariny ("de io photo io tsy azo avainy enqueteur")
        if (!user.role.equals("ADMIN", ignoreCase = true)) {
            return@withContext Result.failure(IllegalAccessException("La photo de profil est officielle et ne peut pas être modifiée par l'enquêteur."))
        }
        val updatedTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val updatedUser = user.copy(
            photo = photoPathOrUrl,
            photoUrl = photoPathOrUrl,
            updatedAt = updatedTimestamp
        )

        var statusMessage = ""

        if (!isSimulatedOffline) {
            try {
                val cleanUrl = supabaseUrl.trimEnd('/')
                val targetUrl = if (user.idUser > 0L) {
                    "$cleanUrl/rest/v1/users?id_user=eq.${user.idUser}"
                } else {
                    "$cleanUrl/rest/v1/users?email=eq.${user.email}"
                }

                val jsonBody = JSONObject().apply {
                    put("photo", photoPathOrUrl)
                    put("photo_url", photoPathOrUrl)
                    put("avatar_url", photoPathOrUrl)
                    put("image_url", photoPathOrUrl)
                    put("updated_at", updatedTimestamp)
                }.toString()

                val request = Request.Builder()
                    .url(targetUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                var response = client.newCall(request).execute()
                if (!response.isSuccessful && response.code == 404) {
                    val altUrl = if (user.idUser > 0L) {
                        "$cleanUrl/rest/v1/utilisateurs?id_user=eq.${user.idUser}"
                    } else {
                        "$cleanUrl/rest/v1/utilisateurs?email=eq.${user.email}"
                    }
                    val altRequest = Request.Builder()
                        .url(altUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    response = client.newCall(altRequest).execute()
                }

                if (response.isSuccessful) {
                    statusMessage = "Photo de profil mise à jour et synchronisée avec Supabase !"
                    addLog("Photo synchronisée vers Supabase (${user.prenom} ${user.nom})", true)
                } else {
                    val code = response.code
                    statusMessage = "Photo enregistrée localement (Supabase HTTP $code)."
                    addLog("Synchronisation Supabase photo différée (HTTP $code)", false)
                }
            } catch (e: Exception) {
                statusMessage = "Photo enregistrée localement. Synchronisation Supabase ultérieure."
                addLog("Synchronisation photo ultérieure (${e.localizedMessage})", false)
            }
        } else {
            statusMessage = "Photo enregistrée localement (Mode Hors-ligne)."
            addLog("Photo enregistrée localement (Mode Hors-ligne)", true)
        }

        // Sauvegarde SQLite (Room)
        repository.saveUser(updatedUser)
        if (user.idUser > 0L) {
            repository.updateUserPhoto(user.idUser, photoPathOrUrl, photoPathOrUrl)
        }

        Result.success(Pair(updatedUser, statusMessage))
    }

    /**
     * Mamafa enquête ao amin'ny Supabase en temps réel (Bidirectionnel: APK -> Supabase).
     * Fafana aloha ny valinteny rehetra ao amin'ny table `reponses`, avy eo fafana ny `enquetes`.
     */
    suspend fun deleteRemoteEnquete(remoteEnqueteId: Long): Boolean = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) return@withContext false
        try {
            val cleanUrl = supabaseUrl.trimEnd('/')

            // 1. Fafana ny valintenin'io enquête io ao amin'ny table reponses
            try {
                val delRepReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/reponses?id_enquete=eq.$remoteEnqueteId")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .delete()
                    .build()
                client.newCall(delRepReq).execute().close()
            } catch (_: Exception) {}

            // 2. Fafana ilay enquête ao amin'ny table enquetes
            val delEnqReq = Request.Builder()
                .url("$cleanUrl/rest/v1/enquetes?id_enquete=eq.$remoteEnqueteId")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .delete()
                .build()
            val resp = client.newCall(delEnqReq).execute()
            val isSuccess = resp.isSuccessful
            resp.close()

            if (isSuccess) {
                addLog("Fiche #$remoteEnqueteId supprimée de Supabase en direct.", true)
            } else {
                addLog("Échec suppression fiche #$remoteEnqueteId sur Supabase (HTTP ${resp.code})", false)
            }
            isSuccess
        } catch (e: Exception) {
            addLog("Erreur suppression fiche sur Supabase: ${e.message}", false)
            false
        }
    }

    suspend fun deleteMultipleRemoteEnquetes(ids: Collection<Long>): Boolean = withContext(Dispatchers.IO) {
        var allOk = true
        for (id in ids) {
            val ok = deleteRemoteEnquete(id)
            if (!ok) allOk = false
        }
        allOk
    }

    /**
     * Mandefa fanavaozana ny satan'ny famakiana annonce any amin'ny Supabase (table annonce_destinataires).
     */
    suspend fun markAnnonceAsReadRemote(idAnnonce: String, userId: Long): Boolean = withContext(Dispatchers.IO) {
        if (isSimulatedOffline || userId <= 0L || idAnnonce.isBlank()) return@withContext false
        try {
            val cleanUrl = supabaseUrl.trimEnd('/')
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())

            val payload = JSONObject().apply {
                put("id_annonce", idAnnonce)
                put("id_user", userId)
                put("statut_lecture", "LU")
                put("date_lecture", nowIso)
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$cleanUrl/rest/v1/annonce_destinataires")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val resp = client.newCall(req).execute()
            val ok = resp.isSuccessful
            resp.close()
            ok
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Mamorona sy mandefa annonce vaovao miaraka amin'ny fichiers mankany amin'ny Supabase.
     */
    suspend fun createAnnonceRemote(
        annonce: AnnonceEntity,
        fichiers: List<AnnonceFichierEntity> = emptyList(),
        destinatairesUserIds: List<Long> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) return@withContext false
        try {
            val cleanUrl = supabaseUrl.trimEnd('/')
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())

            // 1. Insert into public.annonces
            val annonceJson = JSONObject().apply {
                put("id_annonce", annonce.idAnnonce)
                put("titre", annonce.titre)
                if (annonce.contenu != null) put("contenu", annonce.contenu)
                put("type_annonce", annonce.typeAnnonce)
                put("priorite", annonce.priorite)
                put("statut", annonce.statut)
                put("cible_type", annonce.cibleType)
                if (annonce.cibleValeur != null) put("cible_valeur", annonce.cibleValeur)
                if (annonce.datePublication != null) put("date_publication", annonce.datePublication)
                if (annonce.dateExpiration != null) put("date_expiration", annonce.dateExpiration)
                if (annonce.createdBy != null) put("created_by", annonce.createdBy)
                if (annonce.authorName != null) put("author_name", annonce.authorName)
                if (annonce.authorRole != null) put("author_role", annonce.authorRole)
            }

            val annonceBody = annonceJson.toString().toRequestBody("application/json".toMediaType())
            val annonceReq = Request.Builder()
                .url("$cleanUrl/rest/v1/annonces")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(annonceBody)
                .build()

            val annonceResp = client.newCall(annonceReq).execute()
            val annonceSuccess = annonceResp.isSuccessful
            annonceResp.close()

            if (!annonceSuccess) {
                addLog("Échec publication annonce sur Supabase", false)
                return@withContext false
            }

            // 2. Insert into public.annonce_fichiers
            if (fichiers.isNotEmpty()) {
                val fichiersArr = JSONArray()
                fichiers.forEach { f ->
                    fichiersArr.put(JSONObject().apply {
                        put("id_fichier", f.idFichier)
                        put("id_annonce", annonce.idAnnonce)
                        put("nom_fichier", f.nomFichier)
                        if (f.cheminStorage != null) put("chemin_storage", f.cheminStorage)
                        if (f.typeMime != null) put("type_mime", f.typeMime)
                        put("taille_octets", f.tailleOctets)
                        if (f.dataUrl != null) put("data_url", f.dataUrl)
                    })
                }

                val fReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/annonce_fichiers")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(fichiersArr.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(fReq).execute().close()
            }

            // 3. Insert into public.annonce_destinataires if any
            if (destinatairesUserIds.isNotEmpty()) {
                val destArr = JSONArray()
                destinatairesUserIds.forEach { uid ->
                    destArr.put(JSONObject().apply {
                        put("id_annonce", annonce.idAnnonce)
                        put("id_user", uid)
                        put("statut_lecture", "NON_LU")
                        put("date_envoi", nowIso)
                    })
                }

                val destReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/annonce_destinataires")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(destArr.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(destReq).execute().close()
            }

            addLog("Annonce « ${annonce.titre} » publiée avec succès sur Supabase !", true)
            true
        } catch (e: Exception) {
            addLog("Erreur publication annonce: ${e.message}", false)
            false
        }
    }

    /**
     * Mampifandray sy manavao ny mombamomba ny mpampiasa (nom, prénom, téléphone, statut, etc.)
     * mivantana any amin'ny Supabase (table users / utilisateurs)
     */
    suspend fun updateUserProfileRemote(user: UserEntity): Boolean = withContext(Dispatchers.IO) {
        if (isSimulatedOffline) return@withContext false
        try {
            val cleanUrl = supabaseUrl.trimEnd('/')
            val updatedTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

            val targetUrl = if (user.idUser > 0L) {
                "$cleanUrl/rest/v1/users?id_user=eq.${user.idUser}"
            } else {
                "$cleanUrl/rest/v1/users?email=eq.${user.email}"
            }

            val jsonBody = JSONObject().apply {
                put("nom", user.nom)
                put("prenom", user.prenom)
                if (user.telephone != null) put("telephone", user.telephone)
                put("role", user.role)
                put("statut", user.statut)
                put("region", user.region.ifBlank { "Analamanga" })
                if (user.matricule != null) put("matricule", user.matricule)
                if (user.photo != null) put("photo", user.photo)
                if (user.photoUrl != null) put("photo_url", user.photoUrl)
                put("updated_at", updatedTimestamp)
            }.toString()

            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()

            if (success) {
                addLog("Mise à jour du profil réussie sur Supabase pour ${user.prenom} ${user.nom}", true)
                true
            } else {
                // Try fallback to "utilisateurs" table
                val altUrl = if (user.idUser > 0L) {
                    "$cleanUrl/rest/v1/utilisateurs?id_user=eq.${user.idUser}"
                } else {
                    "$cleanUrl/rest/v1/utilisateurs?email=eq.${user.email}"
                }
                val altReq = Request.Builder()
                    .url(altUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .patch(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()
                val altResp = client.newCall(altReq).execute()
                val altOk = altResp.isSuccessful
                altResp.close()
                if (altOk) {
                    addLog("Mise à jour du profil réussie sur table utilisateurs", true)
                }
                altOk
            }
        } catch (e: Exception) {
            Log.w("SyncService", "Erreur synchronisation profil vers Supabase: ${e.message}")
            false
        }
    }

    /**
     * Mampiditra mpampiasa vaovao (Enquêteur / Admin) ao amin'ny Supabase sy SQLite (Room)
     */
    suspend fun createUserRemote(user: UserEntity): Result<UserEntity> = withContext(Dispatchers.IO) {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val userToSave = user.copy(
            createdAt = if (user.createdAt.isNotBlank()) user.createdAt else nowIso,
            updatedAt = nowIso,
            region = user.region.ifBlank { "Analamanga" }
        )

        // 1. Sauvegarde SQLite (Room) an-toerana aloha
        repository.saveUser(userToSave)
        val localUser = if (userToSave.email.isNotBlank()) repository.findUserByEmailOrPhone(userToSave.email) else null
        val userWithId = localUser ?: userToSave

        if (isSimulatedOffline) {
            addLog("Utilisateur créé localement (Mode Hors-ligne) : ${userWithId.prenom} ${userWithId.nom}", true)
            return@withContext Result.success(userWithId)
        }

        try {
            val cleanUrl = supabaseUrl.trimEnd('/')
            val jsonBody = JSONObject().apply {
                if (userWithId.idUser > 0L) put("id_user", userWithId.idUser)
                put("nom", userWithId.nom)
                put("prenom", userWithId.prenom)
                put("email", userWithId.email)
                if (!userWithId.telephone.isNullOrBlank()) put("telephone", userWithId.telephone)
                if (!userWithId.motDePasse.isNullOrBlank()) put("mot_de_passe", userWithId.motDePasse)
                put("role", userWithId.role)
                put("statut", userWithId.statut)
                put("region", userWithId.region.ifBlank { "Analamanga" })
                if (!userWithId.matricule.isNullOrBlank()) put("matricule", userWithId.matricule)
                if (!userWithId.photo.isNullOrBlank()) put("photo", userWithId.photo)
                if (!userWithId.photoUrl.isNullOrBlank()) put("photo_url", userWithId.photoUrl)
                put("created_at", userWithId.createdAt)
                put("updated_at", userWithId.updatedAt)
            }.toString()

            val request = Request.Builder()
                .url("$cleanUrl/rest/v1/users")
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            var response = client.newCall(request).execute()
            if (!response.isSuccessful && response.code == 404) {
                val altReq = Request.Builder()
                    .url("$cleanUrl/rest/v1/utilisateurs")
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()
                response = client.newCall(altReq).execute()
            }

            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                response.close()
                addLog("Utilisateur « ${userWithId.prenom} ${userWithId.nom} » enregistré sur Supabase !", true)

                var finalUser = userWithId
                if (bodyStr.startsWith("[")) {
                    val arr = JSONArray(bodyStr)
                    if (arr.length() > 0) {
                        val returnedObj = arr.getJSONObject(0)
                        val returnedId = when {
                            returnedObj.has("id_user") && !returnedObj.isNull("id_user") -> returnedObj.optLong("id_user", userWithId.idUser)
                            returnedObj.has("id") && !returnedObj.isNull("id") -> returnedObj.optLong("id", userWithId.idUser)
                            else -> userWithId.idUser
                        }
                        finalUser = userWithId.copy(idUser = returnedId)
                        repository.saveUser(finalUser)
                    }
                }
                Result.success(finalUser)
            } else {
                val code = response.code
                response.close()
                addLog("Utilisateur enregistré localement (Erreur Supabase HTTP $code)", false)
                Result.success(userWithId)
            }
        } catch (e: Exception) {
            addLog("Erreur création utilisateur Supabase: ${e.message}", false)
            Result.success(userWithId)
        }
    }
}
