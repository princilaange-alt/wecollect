package com.example.data.sync

import android.util.Log
import com.example.data.model.AnnonceDestinataireEntity
import com.example.data.model.AnnonceEntity
import com.example.data.model.AnnonceFichierEntity
import com.example.data.model.EnqueteEntity
import com.example.data.model.OptionEntity
import com.example.data.model.PersonneEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.ReponseEntity
import com.example.data.model.UserEntity
import com.example.data.repository.SurveyRepository
import com.example.data.session.UserSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mpitantana ny fifandraisana temps réel amin'ny Supabase (Supabase Realtime via WebSocket).
 * - Mihaino ny fanovana rehetra (INSERT, UPDATE, DELETE) amin'ireo tables:
 *   users, questionnaires, questions, options, enquetes, reponses, personnes.
 * - Manavao avy hatrany ny SQLite Room eo no ho eo tsy mila manao refresh manuel na reload application.
 * - Rehefa tapaka ny connexion dia miato, ary rehefa miverina dia mampifandray ho azy (auto-reconnect).
 */
class SupabaseRealtimeManager(
    private val repository: SurveyRepository,
    private val sessionManager: UserSessionManager? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    var syncService: SyncService? = null

    private val TAG = "SupabaseRealtime"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for WebSocket
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private val messageCounter = AtomicInteger(1)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _lastEventTime = MutableStateFlow(System.currentTimeMillis())
    val lastEventTime: StateFlow<Long> = _lastEventTime.asStateFlow()

    private val _eventNotification = MutableStateFlow<String?>(null)
    val eventNotification: StateFlow<String?> = _eventNotification.asStateFlow()

    var onUserDeactivated: ((UserEntity) -> Unit)? = null
    var onDataChanged: ((table: String, action: String) -> Unit)? = null

    private var isStarted = false
    private var currentUrl: String = ""
    private var currentKey: String = ""
    private var isSimulatedOffline: Boolean = false

    fun start(supabaseUrl: String, anonKey: String, isForceOffline: Boolean = false) {
        currentUrl = supabaseUrl
        currentKey = anonKey
        isSimulatedOffline = isForceOffline

        if (isForceOffline) {
            disconnect()
            return
        }

        isStarted = true
        connectWebSocket()
    }

    fun updateConfig(supabaseUrl: String, anonKey: String, isForceOffline: Boolean) {
        val urlChanged = currentUrl != supabaseUrl || currentKey != anonKey
        currentUrl = supabaseUrl
        currentKey = anonKey
        isSimulatedOffline = isForceOffline

        if (isForceOffline) {
            disconnect()
        } else if (urlChanged || webSocket == null || !_isConnected.value) {
            disconnect()
            isStarted = true
            connectWebSocket()
        }
    }

    fun stop() {
        isStarted = false
        disconnect()
    }

    fun clearNotification() {
        _eventNotification.value = null
    }

    private fun disconnect() {
        try {
            heartbeatJob?.cancel()
            heartbeatJob = null
            reconnectJob?.cancel()
            reconnectJob = null

            webSocket?.close(1000, "App paused or disconnected")
            webSocket = null
        } catch (_: Exception) {}
        _isConnected.value = false
    }

    private fun connectWebSocket() {
        if (!isStarted || isSimulatedOffline) return
        if (currentUrl.isBlank() || currentKey.isBlank()) return

        try {
            val cleanHost = currentUrl.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .trimEnd('/')

            val wsUrl = "wss://$cleanHost/realtime/v1/websocket?apikey=$currentKey&vsn=1.0.0"
            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("apikey", currentKey)
                .addHeader("Authorization", "Bearer $currentKey")
                .build()

            webSocket = client.newWebSocket(request, createWebSocketListener())
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating WebSocket: ${e.message}")
            scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (!isStarted || isSimulatedOffline) return
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            _isConnected.value = false
            delay(5000)
            if (isStarted && !isSimulatedOffline) {
                connectWebSocket()
            }
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _isConnected.value = true
                Log.d(TAG, "Supabase Realtime connected successfully!")

                // 1. Join public channel with postgres_changes for all critical tables
                val joinAllMsg = JSONObject().apply {
                    put("topic", "realtime:public")
                    put("event", "phx_join")
                    put("payload", JSONObject().apply {
                        put("config", JSONObject().apply {
                            put("broadcast", JSONObject().apply { put("self", true) })
                            put("presence", JSONObject().apply { put("key", "") })
                            val changesArray = JSONArray()
                            listOf(
                                "users", "utilisateurs", "questionnaires", "formulaires", "questions",
                                "options", "enquetes", "reponses", "personnes",
                                "annonces", "annonce_fichiers", "annonce_destinataires"
                            ).forEach { table ->
                                changesArray.put(JSONObject().apply {
                                    put("event", "*")
                                    put("schema", "public")
                                    put("table", table)
                                })
                            }
                            put("postgres_changes", changesArray)
                        })
                    })
                    put("ref", "sub_${messageCounter.incrementAndGet()}")
                }
                ws.send(joinAllMsg.toString())

                // 2. Also join table-specific channels as fallback
                listOf(
                    "users", "questionnaires", "questions", "options", "enquetes", "reponses", "personnes",
                    "annonces", "annonce_fichiers", "annonce_destinataires"
                ).forEach { table ->
                    val singleJoin = JSONObject().apply {
                        put("topic", "realtime:public:$table")
                        put("event", "phx_join")
                        put("payload", JSONObject())
                        put("ref", "sub_${table}_${messageCounter.incrementAndGet()}")
                    }
                    ws.send(singleJoin.toString())
                }

                // 3. Heartbeat loop (every 25 seconds)
                heartbeatJob?.cancel()
                heartbeatJob = scope.launch {
                    while (isActive) {
                        delay(25000)
                        try {
                            val hb = JSONObject().apply {
                                put("topic", "phoenix")
                                put("event", "heartbeat")
                                put("payload", JSONObject())
                                put("ref", "hb_${messageCounter.incrementAndGet()}")
                            }
                            ws.send(hb.toString())
                        } catch (_: Exception) {}
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val event = root.optString("event")
                    val topic = root.optString("topic")
                    val payload = root.optJSONObject("payload") ?: JSONObject()

                    if (event == "postgres_changes") {
                        val dataObj = payload.optJSONObject("data") ?: payload
                        val type = (dataObj.optString("type").takeIf { it.isNotBlank() }
                            ?: payload.optString("type")).uppercase()
                        val table = (dataObj.optString("table").takeIf { it.isNotBlank() }
                            ?: payload.optString("table")).lowercase()
                        val record = dataObj.optJSONObject("record")
                            ?: payload.optJSONObject("record")
                            ?: dataObj.optJSONObject("new")
                            ?: payload.optJSONObject("new")
                        val oldRecord = dataObj.optJSONObject("old_record")
                            ?: payload.optJSONObject("old_record")
                            ?: dataObj.optJSONObject("old")
                            ?: payload.optJSONObject("old")
                        processRealtimeChange(table, type, record, oldRecord)
                    } else if (event.equals("INSERT", true) || event.equals("UPDATE", true) || event.equals("DELETE", true)) {
                        val type = event.uppercase()
                        val inferredTable = when {
                            payload.has("table") -> payload.optString("table")
                            topic.contains(":") -> topic.substringAfterLast(":")
                            else -> ""
                        }
                        val record = payload.optJSONObject("record")
                            ?: payload.optJSONObject("new")
                        val oldRecord = payload.optJSONObject("old_record")
                            ?: payload.optJSONObject("old")
                        processRealtimeChange(inferredTable, type, record, oldRecord)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing realtime message: ${e.message}")
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Supabase Realtime disconnected: ${t.message}")
                _isConnected.value = false
                scheduleReconnect()
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                if (isStarted && !isSimulatedOffline) {
                    scheduleReconnect()
                }
            }
        }
    }

    /**
     * Mamerina sy mampihatra avy hatrany ny fanovana ao amin'ny SQLite Room (Actualisation ciblée).
     * Tsy manao reload ny pejy fa ny données voakasika ihany no havaozina.
     */
    private fun processRealtimeChange(
        table: String,
        action: String,
        record: JSONObject?,
        oldRecord: JSONObject?
    ) {
        val cleanTable = table.lowercase().trim()
        val cleanAction = action.uppercase().trim()
        if (cleanTable.isBlank()) return

        scope.launch {
            try {
                when (cleanTable) {
                    "users", "utilisateurs" -> handleUserChange(cleanAction, record, oldRecord)
                    "questionnaires", "formulaires" -> handleQuestionnaireChange(cleanAction, record, oldRecord)
                    "questions" -> handleQuestionChange(cleanAction, record, oldRecord)
                    "options" -> handleOptionChange(cleanAction, record, oldRecord)
                    "enquetes" -> handleEnqueteChange(cleanAction, record, oldRecord)
                    "reponses" -> handleReponseChange(cleanAction, record, oldRecord)
                    "personnes" -> handlePersonneChange(cleanAction, record, oldRecord)
                    "annonces" -> handleAnnonceChange(cleanAction, record, oldRecord)
                    "annonce_fichiers" -> handleAnnonceFichierChange(cleanAction, record, oldRecord)
                    "annonce_destinataires" -> handleAnnonceDestinataireChange(cleanAction, record, oldRecord)
                }

                _lastEventTime.value = System.currentTimeMillis()
                onDataChanged?.invoke(cleanTable, cleanAction)
            } catch (e: Exception) {
                Log.e(TAG, "Error applying realtime change for $cleanTable: ${e.message}")
            }
        }
    }

    private suspend fun handleAnnonceChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optString("id_annonce", oldRecord.optString("id", ""))
                    ?: record?.optString("id_annonce", record.optString("id", ""))
                    ?: ""
                if (id.isNotBlank()) {
                    repository.deleteAnnonce(id)
                    _eventNotification.value = "Annonce supprimée sur Supabase"
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
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

                val entity = AnnonceEntity(
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
                repository.saveAnnonce(entity)
                if (action == "INSERT") {
                    _eventNotification.value = "Nouvelle annonce reçue : « $titre » ($priorite)"
                } else {
                    _eventNotification.value = "Annonce actualisée : « $titre »"
                }
            }
        }
    }

    private suspend fun handleAnnonceFichierChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optString("id_fichier", oldRecord.optString("id", ""))
                    ?: record?.optString("id_fichier", record.optString("id", ""))
                    ?: ""
                if (id.isNotBlank()) {
                    repository.deleteAnnonceFichier(id)
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
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
                    val fichier = AnnonceFichierEntity(
                        idFichier = idFichier,
                        idAnnonce = idAnnonce,
                        nomFichier = nomFichier,
                        cheminStorage = cheminStorage,
                        typeMime = typeMime,
                        tailleOctets = tailleOctets,
                        dataUrl = dataUrl,
                        uploadedAt = uploadedAt
                    )
                    repository.saveAnnonceFichier(fichier)
                }
            }
        }
    }

    private suspend fun handleAnnonceDestinataireChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = obj.optLong("id", 0L)
                val idAnnonce = obj.optString("id_annonce", "")
                val idUser = obj.optLong("id_user", 0L)
                val dateEnvoi = if (obj.isNull("date_envoi")) null else obj.optString("date_envoi")
                val dateLecture = if (obj.isNull("date_lecture")) null else obj.optString("date_lecture")
                val statutLecture = obj.optString("statut_lecture", "NON_LU")

                if (idAnnonce.isNotBlank() && idUser > 0L) {
                    val dest = AnnonceDestinataireEntity(
                        id = id,
                        idAnnonce = idAnnonce,
                        idUser = idUser,
                        dateEnvoi = dateEnvoi,
                        dateLecture = dateLecture,
                        statutLecture = statutLecture
                    )
                    repository.saveAnnonceDestinataire(dest)
                }
            }
        }
    }

    private suspend fun handleUserChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optLong("id_user", oldRecord.optLong("id", 0L)) ?: record?.optLong("id_user", record.optLong("id", 0L)) ?: 0L
                val email = oldRecord?.optString("email") ?: record?.optString("email") ?: ""
                if (id > 0L) {
                    repository.updateUserStatut(id, "INACTIF")
                    sessionManager?.updateSavedAccountStatut(id, email, "INACTIF")
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = when {
                    obj.has("id_user") && !obj.isNull("id_user") -> obj.optLong("id_user", 0L)
                    obj.has("id") && !obj.isNull("id") -> obj.optLong("id", 0L)
                    obj.has("user_id") && !obj.isNull("user_id") -> obj.optLong("user_id", 0L)
                    else -> 0L
                }
                val nom = obj.optString("nom", "")
                val prenom = obj.optString("prenom", "")
                val email = obj.optString("email", "")
                val telephone = if (obj.isNull("telephone")) null else obj.optString("telephone")
                val role = obj.optString("role", "ENQUETEUR")
                val motDePasse = if (obj.isNull("mot_de_passe")) null else obj.optString("mot_de_passe")
                val photo = if (obj.isNull("photo")) null else obj.optString("photo")
                val photoUrl = if (obj.isNull("photo_url")) null else obj.optString("photo_url")
                val region = if (obj.isNull("region") || obj.optString("region").isBlank()) "Analamanga" else obj.optString("region")
                val matricule = if (obj.isNull("matricule") || obj.optString("matricule").isBlank()) null else obj.optString("matricule")
                val rawStatut = SyncService.extractUserStatut(obj)
                val effectiveStatut = if (SyncService.isInactiveStatut(rawStatut)) "INACTIF" else "ACTIF"

                val user = UserEntity(
                    idUser = id,
                    nom = nom,
                    prenom = prenom,
                    email = email,
                    telephone = telephone,
                    role = role,
                    statut = effectiveStatut,
                    region = region,
                    matricule = matricule,
                    motDePasse = motDePasse,
                    photo = photo,
                    photoUrl = photoUrl
                )

                repository.saveUser(user)
                if (email.isNotBlank()) {
                    repository.updateUserStatutByEmail(email, effectiveStatut)
                }
                if (id > 0L) {
                    repository.updateUserStatut(id, effectiveStatut)
                }
                sessionManager?.updateSavedAccountStatut(id, email, effectiveStatut, telephone, nom, prenom)

                val currentSession = sessionManager?.getSession()
                val isSessionMatch = currentSession != null && (
                    (id > 0L && id == currentSession.idUser) || 
                    (email.isNotBlank() && email.equals(currentSession.email, ignoreCase = true)) ||
                    (!telephone.isNullOrBlank() && !currentSession.telephone.isNullOrBlank() && telephone.trim() == currentSession.telephone!!.trim()) ||
                    (nom.isNotBlank() && prenom.isNotBlank() && nom.trim().equals(currentSession.nom.trim(), ignoreCase = true) && prenom.trim().equals(currentSession.prenom.trim(), ignoreCase = true))
                )

                if (isSessionMatch) {
                    if (SyncService.isInactiveStatut(effectiveStatut)) {
                        onUserDeactivated?.invoke(user)
                    } else {
                        sessionManager.saveSession(user.copy(idUser = if (user.idUser > 0L) user.idUser else currentSession!!.idUser))
                    }
                }

                _eventNotification.value = "Utilisateur « $prenom $nom » actualisé ($effectiveStatut)"
            }
        }
    }

    private suspend fun handleQuestionnaireChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optLong("id_questionnaire") ?: record?.optLong("id_questionnaire") ?: 0L
                if (id > 0L) {
                    repository.deleteQuestionnaireCompletely(id)
                    _eventNotification.value = "Questionnaire #$id supprimé en direct de Supabase"
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = when {
                    obj.has("id_questionnaire") && !obj.isNull("id_questionnaire") -> obj.optLong("id_questionnaire", 0L)
                    obj.has("id") && !obj.isNull("id") -> obj.optLong("id", 0L)
                    obj.has("questionnaire_id") && !obj.isNull("questionnaire_id") -> obj.optLong("questionnaire_id", 0L)
                    else -> 0L
                }
                if (id <= 0L) return

                val titre = when {
                    obj.has("titre") && !obj.isNull("titre") -> obj.optString("titre")
                    obj.has("title") && !obj.isNull("title") -> obj.optString("title")
                    obj.has("nom") && !obj.isNull("nom") -> obj.optString("nom")
                    obj.has("label") && !obj.isNull("label") -> obj.optString("label")
                    else -> "Questionnaire #$id"
                }
                val desc = obj.optString("description", "")
                val rawStatut = SyncService.extractQuestionnaireStatut(obj)
                val version = obj.optString("version", "v1.0")
                val createdBy = obj.optLong("created_by", 1L)

                val isDraft = SyncService.isDraftStatus(rawStatut) || !SyncService.isPublicStatus(rawStatut)

                if (isDraft) {
                    // Publié -> Brouillon : Le questionnaire disparaît immédiatement de la liste publiée
                    // sans supprimer les questions ou les options de SQLite
                    repository.markQuestionnaireAsDraft(id)
                } else {
                    // Brouillon -> Publié : Le questionnaire réapparaît automatiquement dans la liste publiée
                    // sans supprimer ni recréer le questionnaire
                    val normalizedStatus = "PUBLIÉ"
                    val q = QuestionnaireEntity(
                        idQuestionnaire = id,
                        titre = titre,
                        description = desc,
                        version = version,
                        statut = normalizedStatus,
                        createdBy = createdBy
                    )
                    repository.insertQuestionnaire(q)
                    repository.markQuestionnaireAsPublished(id)

                    val questionsCount = repository.countQuestionsByQuestionnaire(id)
                    if (questionsCount == 0 && syncService != null) {
                        scope.launch {
                            try {
                                syncService?.fetchAndSaveQuestionsForQuestionnaireDirect(id)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleQuestionChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optLong("id_question") ?: record?.optLong("id_question") ?: 0L
                if (id > 0L) {
                    repository.deleteQuestionById(id)
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = obj.optLong("id_question", 0L)
                if (id <= 0L) return

                val q = QuestionEntity(
                    idQuestion = id,
                    idQuestionnaire = obj.optLong("id_questionnaire", 1L),
                    libelle = obj.optString("libelle", ""),
                    type = obj.optString("type", "TEXTE"),
                    ordre = obj.optInt("ordre", 1),
                    obligatoire = obj.optBoolean("obligatoire", false),
                    estCodeUnique = obj.optBoolean("est_code_unique", false)
                )
                repository.insertQuestion(q)
            }
        }
    }

    private suspend fun handleOptionChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optLong("id_option") ?: record?.optLong("id_option") ?: 0L
                if (id > 0L) {
                    repository.deleteOptionById(id)
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = obj.optLong("id_option", 0L)
                if (id <= 0L) return

                val opt = OptionEntity(
                    idOption = id,
                    idQuestion = obj.optLong("id_question", 0L),
                    libelle = if (obj.has("libelle")) obj.optString("libelle", "") else obj.optString("valeur", ""),
                    ordre = obj.optInt("ordre", 1)
                )
                repository.insertOption(opt)
            }
        }
    }

    private suspend fun handleEnqueteChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optLong("id_enquete") ?: record?.optLong("id_enquete") ?: 0L
                if (id > 0L) {
                    repository.deleteEnquete(id)
                    _eventNotification.value = "Fiche #$id supprimée de Supabase"
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = obj.optLong("id_enquete", 0L)
                if (id <= 0L) return

                // Fiarovana ny angona eo an-toerana : raha misy enquête EN_ATTENTE eo an-toerana
                // dia tsy potehina na soloina sao very ny fanovana mbola tsy tafakatra
                val existing = repository.getEnqueteById(id)
                if (existing != null && existing.syncStatus == "EN_ATTENTE") {
                    return
                }

                val enq = EnqueteEntity(
                    idEnquete = id,
                    idPersonne = obj.optLong("id_personne", id),
                    idQuestionnaire = obj.optLong("id_questionnaire", 1L),
                    idEnqueteur = obj.optLong("id_enqueteur", 1L),
                    dateEnquete = obj.optString("date_enquete", ""),
                    statut = obj.optString("statut", "TERMINEE"),
                    syncStatus = "SYNCHRONISEE",
                    createdAt = obj.optString("created_at", ""),
                    updatedAt = obj.optString("updated_at", "")
                )
                repository.insertEnquete(enq)
                _eventNotification.value = "Enquête #$id mise à jour en temps réel"
            }
        }
    }

    private suspend fun handleReponseChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val id = oldRecord?.optLong("id_reponse") ?: record?.optLong("id_reponse") ?: 0L
                if (id > 0L) {
                    repository.deleteReponseById(id)
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = obj.optLong("id_reponse", 0L)
                if (id <= 0L) return

                val rep = ReponseEntity(
                    idReponse = id,
                    idEnquete = obj.optLong("id_enquete", 0L),
                    idQuestion = obj.optLong("id_question", 0L),
                    idOption = if (obj.isNull("id_option")) null else obj.optLong("id_option"),
                    valeur = if (obj.isNull("valeur")) "" else obj.getString("valeur")
                )
                repository.insertReponse(rep)
            }
        }
    }

    private suspend fun handlePersonneChange(action: String, record: JSONObject?, oldRecord: JSONObject?) {
        when (action) {
            "DELETE" -> {
                val code = oldRecord?.optString("code_unique") ?: record?.optString("code_unique") ?: ""
                if (code.isNotBlank()) {
                    repository.deletePersonneByCode(code)
                }
            }
            "INSERT", "UPDATE" -> {
                val obj = record ?: return
                val id = obj.optLong("id_personne", 0L)
                val code = obj.optString("code_unique", "")
                if (code.isNotBlank()) {
                    val p = PersonneEntity(
                        idPersonne = if (id > 0L) id else System.currentTimeMillis(),
                        codeUnique = code,
                        createdAt = obj.optString("created_at", ""),
                        updatedAt = obj.optString("updated_at", "")
                    )
                    repository.insertPersonne(p)
                }
            }
        }
    }
}
