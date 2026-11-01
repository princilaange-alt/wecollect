package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.AnnonceDestinataireEntity
import com.example.data.model.AnnonceEntity
import com.example.data.model.AnnonceFichierEntity
import com.example.data.model.DetailedAnnonce
import com.example.data.model.EnqueteDetail
import com.example.data.model.EnqueteEntity
import com.example.data.model.PersonneEntity
import com.example.data.model.QuestionWithOptions
import com.example.data.model.QuestionnaireAvailability
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.ResearchMetrics
import com.example.data.model.SyncedAuditRecord
import com.example.data.model.UserEntity
import com.example.data.repository.SurveyRepository
import com.example.data.session.UserSessionManager
import com.example.data.model.GlobalSyncState
import com.example.data.sync.SupabaseRealtimeManager
import com.example.data.sync.SyncLog
import com.example.data.sync.SyncService
import com.example.data.util.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FormValidationState(
    val completenessPercent: Int = 0,
    val answeredMandatoryCount: Int = 0,
    val totalMandatoryCount: Int = 0,
    val isReadyToSubmit: Boolean = false,
    val isExistingCode: Boolean = false,
    val personFound: Boolean = false,
    val person: PersonneEntity? = null,
    val isAlreadyAnsweredForCurrentQuestionnaire: Boolean = false,
    val answeredQuestionnaires: List<String> = emptyList(),
    val questionnaireAvailabilities: List<QuestionnaireAvailability> = emptyList(),
    val availableQuestionnaires: List<QuestionnaireEntity> = emptyList(),
    val answeredQuestionnairesCount: Int = 0,
    val availableQuestionnairesCount: Int = 0,
    val codeNotice: String? = null,
    val codeDuplicateError: String? = null,
    val respondentDuplicateWarning: String? = null,
    val phoneDuplicateWarning: String? = null,
    val duplicateCheckResult: com.example.data.model.LocalDuplicateCheckResult? = null,
    val fieldErrors: Map<Long, String> = emptyMap(),
    val codeError: String? = null,
    val dateError: String? = null
)

class SurveyViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SurveyRepository(database.surveyDao(), application)
    val syncService = SyncService(repository)
    val sessionManager = UserSessionManager(application)
    val realtimeManager = SupabaseRealtimeManager(repository, sessionManager, viewModelScope)

    // All Personnes stored locally
    val allPersonnes: StateFlow<List<PersonneEntity>> = repository.allPersonnes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Current Tab: 0 = Formulaire, 1 = Fiches Terrain, 2 = Synchronisation, 3 = Profil
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Network monitoring
    private val networkMonitor = NetworkMonitor(application)
    val isDeviceOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, networkMonitor.isCurrentlyConnected())

    // Dark Mode Toggle
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Offline mode simulation
    private val _isForceOffline = MutableStateFlow(false)
    val isForceOffline: StateFlow<Boolean> = _isForceOffline.asStateFlow()

    // Effective online state (Device is physically connected AND user has not enabled simulated offline)
    val isConnectedAndOnline: StateFlow<Boolean> = combine(
        isDeviceOnline,
        _isForceOffline
    ) { devOnline, forceOffline ->
        devOnline && !forceOffline
    }.stateIn(viewModelScope, SharingStarted.Eagerly, networkMonitor.isCurrentlyConnected())

    // All Questionnaires stored in local SQLite Room (persisted locally and available offline)
    val allQuestionnaires: StateFlow<List<QuestionnaireEntity>> = repository.allQuestionnaires
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // All Annonces stored in local SQLite Room
    val allAnnonces: StateFlow<List<AnnonceEntity>> = repository.allAnnonces
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAnnonceFichiers: StateFlow<List<AnnonceFichierEntity>> = repository.allAnnonceFichiers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allAnnonceDestinataires: StateFlow<List<AnnonceDestinataireEntity>> = repository.allAnnonceDestinataires
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    private val _selectedQuestionnaireId = MutableStateFlow<Long?>(null)
    val selectedQuestionnaireId: StateFlow<Long?> = _selectedQuestionnaireId.asStateFlow()

    // Real-time Supabase sync monitoring states
    private val _lastRealtimeSyncTime = MutableStateFlow(System.currentTimeMillis())
    val lastRealtimeSyncTime: StateFlow<Long> = _lastRealtimeSyncTime.asStateFlow()

    // Automatic 30-minute inactivity timeout for security
    private val _lastUserActivityTime = MutableStateFlow(System.currentTimeMillis())
    val lastUserActivityTime: StateFlow<Long> = _lastUserActivityTime.asStateFlow()

    fun onUserActivity() {
        _lastUserActivityTime.value = System.currentTimeMillis()
    }

    fun checkInactivityTimeout() {
        val current = _loggedInUser.value ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - _lastUserActivityTime.value
        val maxInactivityMs = 30 * 60 * 1000L // 30 minutes
        if (elapsed >= maxInactivityMs) {
            logoutDueToInactivity()
        }
    }

    fun logoutDueToInactivity() {
        val current = _loggedInUser.value ?: return
        val userName = "${current.nom} ${current.prenom}".trim().ifBlank { current.email }
        logout()
        _deactivationNotice.value = "🔒 Déconnexion automatique par sécurité : session expirée après 30 minutes d'inactivité."
        _statusChangeNotification.value = Pair("🔒 Compte « $userName » déconnecté automatiquement après 30 min d'inactivité.", false)
    }

    val isRealtimeListening: StateFlow<Boolean> = isConnectedAndOnline

    private val _liveUpdateNotification = MutableStateFlow<String?>(null)
    val liveUpdateNotification: StateFlow<String?> = _liveUpdateNotification.asStateFlow()

    fun dismissLiveUpdateNotification() {
        _liveUpdateNotification.value = null
    }

    // Active Questionnaire displayed in Form (reactively updated whenever Room changes)
    val questionnaire: StateFlow<QuestionnaireEntity?> = combine(
        allQuestionnaires,
        _selectedQuestionnaireId
    ) { list, selectedId ->
        if (list.isEmpty()) {
            null
        } else if (selectedId != null) {
            list.find { it.idQuestionnaire == selectedId } ?: list.firstOrNull()
        } else {
            list.firstOrNull()
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null
    )

    // Logged in User Session
    private val _loggedInUser = MutableStateFlow<UserEntity?>(sessionManager.getSession())
    val loggedInUser: StateFlow<UserEntity?> = _loggedInUser.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _loggedInUser.asStateFlow()

    // Annonces enrichies avec fichiers, statut de lecture par utilisateur et date relative
    val detailedAnnonces: StateFlow<List<DetailedAnnonce>> = combine(
        allAnnonces,
        allAnnonceFichiers,
        allAnnonceDestinataires,
        _loggedInUser
    ) { annoncesList, fichiersList, destList, user ->
        val userId = user?.idUser ?: 0L
        annoncesList.map { a ->
            val filesForThis = fichiersList.filter { it.idAnnonce == a.idAnnonce }
            val destForUser = destList.firstOrNull { it.idAnnonce == a.idAnnonce && it.idUser == userId }
            val isAuthor = a.createdBy != null && a.createdBy == userId
            val isRead = isAuthor || (destForUser?.statutLecture == "LU")

            val relativeTime = formatTimeAgo(a.datePublication ?: a.createdAt)
            val primaryFile = when {
                filesForThis.isNotEmpty() -> {
                    val jpg = filesForThis.firstOrNull { it.nomFichier.endsWith(".jpg", true) || it.nomFichier.endsWith(".jpeg", true) }
                    val img = jpg ?: filesForThis.firstOrNull { it.typeMime?.startsWith("image/") == true || it.nomFichier.endsWith(".png", true) }
                    val pdf = filesForThis.firstOrNull { it.nomFichier.endsWith(".pdf", true) || it.typeMime == "application/pdf" }
                    (img ?: pdf ?: filesForThis.first()).nomFichier
                }
                a.typeAnnonce == "IMAGE" -> "photo_terrain.jpg"
                a.typeAnnonce == "FICHIER" -> "document_joint.pdf"
                else -> "annonce.txt"
            }

            DetailedAnnonce(
                annonce = a,
                fichiers = filesForThis,
                isRead = isRead,
                timeAgo = relativeTime,
                primaryFileName = primaryFile
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun markAnnonceAsRead(idAnnonce: String) {
        val user = _loggedInUser.value ?: return
        viewModelScope.launch {
            repository.markAnnonceAsRead(idAnnonce, user.idUser)
            syncService.markAnnonceAsReadRemote(idAnnonce, user.idUser)
        }
    }

    fun deleteAnnonce(idAnnonce: String) {
        viewModelScope.launch {
            repository.deleteAnnonce(idAnnonce)
        }
    }

    fun publishAnnonce(
        titre: String,
        contenu: String,
        typeAnnonce: String = "TEXTE",
        priorite: String = "NORMALE",
        cibleType: String = "TOUS",
        cibleValeur: String? = null,
        fileName: String? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        val user = _loggedInUser.value
        val newId = java.util.UUID.randomUUID().toString()
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date())
        val authorName = if (user != null) "${user.prenom} ${user.nom}".trim() else "Direction de Recherche"
        val authorRole = user?.role ?: "COORDINATEUR"

        val isImageFile = fileName?.let {
            it.endsWith(".jpg", ignoreCase = true) ||
            it.endsWith(".jpeg", ignoreCase = true) ||
            it.endsWith(".png", ignoreCase = true) ||
            it.endsWith(".webp", ignoreCase = true)
        } ?: false

        val isPdfFile = fileName?.endsWith(".pdf", ignoreCase = true) ?: false

        val deducedType = when {
            isImageFile -> "IMAGE"
            !fileName.isNullOrBlank() -> "FICHIER"
            else -> typeAnnonce
        }

        val deducedMime = when {
            isPdfFile -> "application/pdf"
            isImageFile -> "image/jpeg"
            fileName?.endsWith(".xlsx", ignoreCase = true) == true || fileName?.endsWith(".xls", ignoreCase = true) == true -> "application/vnd.ms-excel"
            fileName?.endsWith(".mp3", ignoreCase = true) == true || fileName?.endsWith(".wav", ignoreCase = true) == true -> "audio/mpeg"
            else -> "application/octet-stream"
        }

        val entity = AnnonceEntity(
            idAnnonce = newId,
            titre = titre,
            contenu = contenu,
            typeAnnonce = deducedType,
            priorite = priorite,
            statut = "PUBLIEE",
            cibleType = cibleType,
            cibleValeur = cibleValeur,
            datePublication = nowIso,
            createdBy = user?.idUser,
            authorName = authorName,
            authorRole = authorRole,
            createdAt = nowIso,
            updatedAt = nowIso
        )

        val files = if (!fileName.isNullOrBlank()) {
            listOf(
                AnnonceFichierEntity(
                    idFichier = java.util.UUID.randomUUID().toString(),
                    idAnnonce = newId,
                    nomFichier = fileName,
                    cheminStorage = "annonces/$fileName",
                    typeMime = deducedMime,
                    tailleOctets = if (isImageFile) 1450000L else if (isPdfFile) 380000L else 45000L,
                    uploadedAt = nowIso
                )
            )
        } else emptyList()

        viewModelScope.launch {
            repository.saveAnnonce(entity, files)
            val ok = syncService.createAnnonceRemote(entity, files)
            onResult(ok)
        }
    }

    fun formatTimeAgo(dateString: String?): String {
        if (dateString.isNullOrBlank()) return "Récemment"
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            )
            var parsedDate: Date? = null
            if (dateString.toLongOrNull() != null) {
                parsedDate = Date(dateString.toLong())
            } else {
                for (f in formats) {
                    try {
                        parsedDate = f.parse(dateString)
                        if (parsedDate != null) break
                    } catch (_: Exception) {}
                }
            }
            if (parsedDate == null) return "Récemment"
            val diffMs = System.currentTimeMillis() - parsedDate.time
            val minutes = diffMs / (1000 * 60)
            val hours = minutes / 60
            val days = hours / 24
            when {
                minutes < 2 -> "À l'instant"
                minutes < 60 -> "il y a $minutes min"
                hours < 24 -> "il y a $hours h"
                days == 1L -> "il y a 1 jour"
                days in 2..30 -> "il y a $days jours"
                else -> "il y a ${days / 30} mois"
            }
        } catch (_: Exception) {
            "Récemment"
        }
    }

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Fanontaniana mifanaraka amin'ny questionnaire voafidy :
    // Rehefa mifidy questionnaire hafa ny mpampiasa dia miova avy hatrany ny fanontaniana (samy manana ny azy manokana)
    @OptIn(ExperimentalCoroutinesApi::class)
    val questions: StateFlow<List<QuestionWithOptions>> = questionnaire
        .flatMapLatest { q ->
            if (q != null) {
                repository.getQuestionsWithOptions(q.idQuestionnaire)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Fiarovana sy fitahirizana SQLite (na Hors-ligne aza) :
    // Tsy ny enqueteur rehetra no mahita ny reponse enquete,
    // fa izay enqueteur izay nanao enquete ihany no mahita amin'ny alalan'ny id_enqueteur ihany
    val allEnquetes: StateFlow<List<EnqueteEntity>> = combine(
        repository.allEnquetes,
        currentUser
    ) { list, user ->
        if (user == null) {
            emptyList()
        } else {
            list.filter { it.idEnqueteur == user.idUser }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allEnquetesAudit: StateFlow<List<SyncedAuditRecord>> = combine(
        repository.allEnquetesAudit,
        currentUser
    ) { records, user ->
        if (user == null) {
            emptyList()
        } else {
            records.filter { it.idEnqueteur == user.idUser }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Filter & Search for Audit Log History
    private val _auditSearchQuery = MutableStateFlow("")
    val auditSearchQuery: StateFlow<String> = _auditSearchQuery.asStateFlow()

    private val _auditFilter = MutableStateFlow("SYNCHRONISEE") // "SYNCHRONISEE", "TOUS", "EN_ATTENTE"
    val auditFilter: StateFlow<String> = _auditFilter.asStateFlow()

    val filteredAuditRecords: StateFlow<List<SyncedAuditRecord>> = combine(
        allEnquetesAudit,
        _auditSearchQuery,
        _auditFilter
    ) { records, query, filter ->
        records.filter { record ->
            val matchesFilter = when (filter) {
                "SYNCHRONISEE" -> record.syncStatus == "SYNCHRONISEE"
                "EN_ATTENTE" -> record.syncStatus == "EN_ATTENTE"
                "ERREUR" -> record.syncStatus == "ERREUR"
                else -> true
            }
            val q = query.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                    record.codeUnique.lowercase().contains(q) ||
                    (record.nomComplet?.lowercase()?.contains(q) == true) ||
                    (record.enqueteurNom?.lowercase()?.contains(q) == true) ||
                    (record.region?.lowercase()?.contains(q) == true) ||
                    (record.commune?.lowercase()?.contains(q) == true) ||
                    record.dateEnquete.contains(q)

            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setAuditSearchQuery(query: String) {
        _auditSearchQuery.value = query
    }

    fun setAuditFilter(filter: String) {
        _auditFilter.value = filter
    }

    val researchMetrics: StateFlow<ResearchMetrics> = combine(
        allEnquetes,
        repository.getResearchMetrics()
    ) { enquetes, baseMetrics ->
        val total = enquetes.size
        val completed = enquetes.count { it.statut == "TERMINEE" || it.statut == "VALIDEE" }
        val synced = enquetes.count { it.syncStatus == "SYNCHRONISEE" }
        val pending = enquetes.count { it.syncStatus == "EN_ATTENTE" }
        val error = enquetes.count { it.syncStatus == "ERREUR" }
        val rate = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 100f

        ResearchMetrics(
            totalEnquetes = total,
            completedEnquetes = completed,
            syncedEnquetes = synced,
            pendingEnquetes = pending,
            errorEnquetes = error,
            completenessRate = rate,
            duplicatesBlocked = baseMetrics.duplicatesBlocked,
            fieldValidationsPassed = baseMetrics.fieldValidationsPassed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResearchMetrics())

    val isSyncing: StateFlow<Boolean> = syncService.isSyncing
    val syncLogs: StateFlow<List<SyncLog>> = syncService.syncLogs

    // Manual / Pull-to-refresh active loading indicator state
    private val _isRefreshingData = MutableStateFlow(false)
    val isRefreshingData: StateFlow<Boolean> = _isRefreshingData.asStateFlow()

    // Filter & Search for Enquetes List
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("TOUS") // TOUS, EN_ATTENTE, SYNCHRONISEE, ERREUR
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    val filteredEnquetes: StateFlow<List<SyncedAuditRecord>> = combine(
        allEnquetesAudit,
        _searchQuery,
        _statusFilter
    ) { enquetes, query, filter ->
        enquetes.filter { enquete ->
            val matchesFilter = when (filter) {
                "EN_ATTENTE" -> enquete.syncStatus == "EN_ATTENTE"
                "SYNCHRONISEE" -> enquete.syncStatus == "SYNCHRONISEE"
                "ERREUR" -> enquete.syncStatus == "ERREUR"
                else -> true
            }
            val q = query.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                    enquete.codeUnique.lowercase().contains(q) ||
                    enquete.dateEnquete.contains(q) ||
                    (enquete.nomComplet?.lowercase()?.contains(q) == true) ||
                    (enquete.region?.lowercase()?.contains(q) == true) ||
                    (enquete.commune?.lowercase()?.contains(q) == true)
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Form State
    private val _codeUnique = MutableStateFlow(generateUniqueCode())
    val codeUnique: StateFlow<String> = _codeUnique.asStateFlow()

    private val _dateEnquete = MutableStateFlow(getTodayDate())
    val dateEnquete: StateFlow<String> = _dateEnquete.asStateFlow()

    private val _textResponses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val textResponses: StateFlow<Map<Long, String>> = _textResponses.asStateFlow()

    private val _selectedOptions = MutableStateFlow<Map<Long, List<Long>>>(emptyMap())
    val selectedOptions: StateFlow<Map<Long, List<Long>>> = _selectedOptions.asStateFlow()

    private val _autreResponses = MutableStateFlow<Map<Long, String>>(emptyMap())
    val autreResponses: StateFlow<Map<Long, String>> = _autreResponses.asStateFlow()

    private val _validationState = MutableStateFlow(FormValidationState())
    val validationState: StateFlow<FormValidationState> = _validationState.asStateFlow()

    private val _formMessage = MutableStateFlow<Pair<String, Boolean>?>(null) // message to isError
    val formMessage: StateFlow<Pair<String, Boolean>?> = _formMessage.asStateFlow()

    // Dialog d'alerte doublons sur champs critiques
    private val _duplicateDetectionDialog = MutableStateFlow<com.example.data.model.LocalDuplicateCheckResult?>(null)
    val duplicateDetectionDialog: StateFlow<com.example.data.model.LocalDuplicateCheckResult?> = _duplicateDetectionDialog.asStateFlow()

    fun dismissDuplicateDialog() {
        _duplicateDetectionDialog.value = null
    }

    // Enquete Detail Preview Dialog
    private val _selectedEnqueteDetail = MutableStateFlow<EnqueteDetail?>(null)
    val selectedEnqueteDetail: StateFlow<EnqueteDetail?> = _selectedEnqueteDetail.asStateFlow()

    // Lisitry ny kaonty rehetra efa tafiditra teo aloha (Saved / Remembered Accounts)
    private val _savedAccounts = MutableStateFlow<List<UserEntity>>(emptyList())
    val savedAccounts: StateFlow<List<UserEntity>> = _savedAccounts.asStateFlow()

    fun refreshSavedAccounts() {
        val list = sessionManager.getSavedAccounts().toMutableList()
        val roomUsers = allUsers.value
        if (roomUsers.isNotEmpty()) {
            for (i in list.indices) {
                val acc = list[i]
                val match = roomUsers.find { 
                    (acc.email.isNotBlank() && it.email.equals(acc.email, ignoreCase = true)) ||
                    (!acc.telephone.isNullOrBlank() && !it.telephone.isNullOrBlank() && it.telephone!!.trim() == acc.telephone!!.trim()) ||
                    (acc.idUser > 0L && it.idUser == acc.idUser) ||
                    (acc.nom.isNotBlank() && acc.prenom.isNotBlank() && it.nom.trim().equals(acc.nom.trim(), ignoreCase = true) && it.prenom.trim().equals(acc.prenom.trim(), ignoreCase = true))
                }
                if (match != null) {
                    val effectiveStatut = if (SyncService.isInactiveStatut(match.statut)) "INACTIF" else "ACTIF"
                    list[i] = acc.copy(
                        nom = match.nom,
                        prenom = match.prenom,
                        statut = effectiveStatut,
                        photo = match.photo ?: acc.photo,
                        photoUrl = match.photoUrl ?: acc.photoUrl,
                        role = match.role,
                        region = match.region.ifBlank { acc.region },
                        matricule = match.matricule ?: acc.matricule
                    )
                }
            }
        }
        _savedAccounts.value = list
    }

    fun removeSavedAccount(user: UserEntity) {
        sessionManager.removeSavedAccount(user.idUser, user.email)
        refreshSavedAccounts()
    }

    fun createNewUser(user: UserEntity, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = syncService.createUserRemote(user)
                if (result.isSuccess) {
                    val savedUser = result.getOrThrow()
                    sessionManager.saveAccount(savedUser)
                    refreshSavedAccounts()
                    onComplete(true, "Utilisateur « ${savedUser.prenom} ${savedUser.nom} » (${savedUser.region}) ajouté avec succès !")
                } else {
                    onComplete(false, result.exceptionOrNull()?.message ?: "Erreur lors de l'enregistrement de l'utilisateur")
                }
            } catch (e: Exception) {
                onComplete(false, e.localizedMessage ?: "Erreur inattendue")
            }
        }
    }

    fun updateUser(user: UserEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveUser(user)
            _loggedInUser.value = user
            sessionManager.saveSession(user)
            sessionManager.saveAccount(user)
            refreshSavedAccounts()
            _codeUnique.value = generateUniqueCode()
            checkCodeDuplicateDebounced(_codeUnique.value)

            val remoteSuccess = syncService.updateUserProfileRemote(user)
            if (remoteSuccess) {
                _statusChangeNotification.value = Pair("✅ Profil mis à jour et synchronisé avec la base de données !", true)
            } else {
                _statusChangeNotification.value = Pair("Profil enregistré en local (sera synchronisé une fois connecté).", true)
            }
            onComplete?.invoke(remoteSuccess)
        }
    }

    private val _isChangingPassword = MutableStateFlow(false)
    val isChangingPassword: StateFlow<Boolean> = _isChangingPassword.asStateFlow()

    fun updatePassword(
        oldPassword: String?,
        newPassword: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val current = _loggedInUser.value
        if (current == null) {
            onError("Aucun utilisateur connecté")
            return
        }
        val cleanNew = newPassword.trim()
        if (cleanNew.length < 4) {
            onError("Le mot de passe doit comporter au moins 4 caractères.")
            return
        }

        viewModelScope.launch {
            _isChangingPassword.value = true
            val result = syncService.changePassword(
                user = current,
                oldPasswordInput = oldPassword,
                newPasswordInput = cleanNew
            )
            _isChangingPassword.value = false

            result.onSuccess { (updatedUser, message) ->
                _loggedInUser.value = updatedUser
                sessionManager.saveSession(updatedUser)
                sessionManager.saveAccount(updatedUser)
                refreshSavedAccounts()
                onSuccess(message)
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Échec de la modification du mot de passe.")
            }
        }
    }

    private val _isUpdatingPhoto = MutableStateFlow(false)
    val isUpdatingPhoto: StateFlow<Boolean> = _isUpdatingPhoto.asStateFlow()

    private val _deactivationNotice = MutableStateFlow<String?>(null)
    val deactivationNotice: StateFlow<String?> = _deactivationNotice.asStateFlow()

    // Fampandrenesana mivantana ny fiovan'ny satan'ny kaonty (ACTIF <-> INACTIF)
    private val _statusChangeNotification = MutableStateFlow<Pair<String, Boolean>?>(null)
    val statusChangeNotification: StateFlow<Pair<String, Boolean>?> = _statusChangeNotification.asStateFlow()

    fun dismissStatusChangeNotification() {
        _statusChangeNotification.value = null
    }

    fun triggerStatusNotification(message: String, isPositive: Boolean) {
        _statusChangeNotification.value = Pair(message, isPositive)
    }

    fun clearDeactivationNotice() {
        _deactivationNotice.value = null
    }

    /**
     * Mamoaka avy hatrany ilay mpanadihady rehefa natao inactif na désactivé ny kaontiny
     * (Déconnexion immédiate automatique vers l'écran de login).
     */
    fun forceLogoutDueToDeactivation(reason: String = "Votre compte a été désactivé par l'administrateur.") {
        val current = _loggedInUser.value
        val userName = if (current != null) "${current.nom} ${current.prenom}".trim().ifBlank { current.email } else "de l'enquêteur"
        if (current != null) {
            viewModelScope.launch {
                if (current.idUser > 0L) {
                    repository.updateUserStatut(current.idUser, "INACTIF")
                }
                if (current.email.isNotBlank()) {
                    repository.updateUserStatutByEmail(current.email, "INACTIF")
                }
            }
            sessionManager.updateSavedAccountStatut(current.idUser, current.email, "INACTIF")
        }
        sessionManager.clearSession()
        _loggedInUser.value = null
        _selectedEnqueteDetail.value = null
        _loginError.value = null
        _deactivationNotice.value = reason
        _statusChangeNotification.value = Pair("⛔ Compte « $userName » désactivé par l'administrateur.", false)
        refreshSavedAccounts()
        _codeUnique.value = generateUniqueCode()
    }

    /**
     * Fitaovana ahafahana manandrana (Tester) eo no ho eo ny fivoahana hoazy rehefa désactivé ny kaonty
     */
    fun simulateDeactivateCurrentUserForTesting() {
        val current = _loggedInUser.value ?: return
        viewModelScope.launch {
            if (current.idUser > 0L) {
                repository.updateUserStatut(current.idUser, "INACTIF")
            } else if (current.email.isNotBlank()) {
                repository.updateUserStatutByEmail(current.email, "INACTIF")
            }
            forceLogoutDueToDeactivation("Test de désactivation du compte enquêteur.")
        }
    }

    /**
     * Fitaovana ahafahana manandrana (Tester) eo no ho eo ny fiverenana ho ACTIF indray
     */
    fun simulateReactivateUserForTesting(userId: Long, email: String) {
        viewModelScope.launch {
            if (userId > 0L) {
                repository.updateUserStatut(userId, "ACTIF")
            }
            if (email.isNotBlank()) {
                repository.updateUserStatutByEmail(email, "ACTIF")
            }
            sessionManager.updateSavedAccountStatut(userId, email, "ACTIF")
            refreshSavedAccounts()
            _statusChangeNotification.value = Pair("✅ Statut actualisé : Compte réactivé (ACTIF) !", true)
        }
    }

    fun updateUserPhoto(photoPathOrUrl: String, onResult: (Boolean, String) -> Unit) {
        val current = _loggedInUser.value ?: currentUser.value ?: return
        // Tsy azon'ny mpanadihady ovaina ny sary ("de io photo io tsy azo avainy enqueteur")
        if (!current.role.equals("ADMIN", ignoreCase = true)) {
            onResult(false, "La photo de profil est officielle et ne peut pas être modifiée par l'enquêteur.")
            return
        }
        viewModelScope.launch {
            _isUpdatingPhoto.value = true
            val result = syncService.updateUserPhoto(current, photoPathOrUrl)
            _isUpdatingPhoto.value = false
            result.onSuccess { (updatedUser, msg) ->
                _loggedInUser.value = updatedUser
                sessionManager.saveSession(updatedUser)
                sessionManager.saveAccount(updatedUser)
                refreshSavedAccounts()
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Erreur de mise à jour de la photo")
            }
        }
    }

    suspend fun checkCurrentAccountActiveStatus(): Boolean {
        val current = _loggedInUser.value ?: return true

        // 1. Raha mifandray amin'ny aterineto (En ligne), jereo mivantana ao amin'ny Supabase aloha
        // satria ny Supabase no manan-kery voalohany (Source de vérité)
        if (isConnectedAndOnline.value && !_isForceOffline.value) {
            val statusResult = syncService.checkUserAccountStatus(current)
            if (statusResult is com.example.data.sync.UserAccountStatusResult.Inactive) {
                forceLogoutDueToDeactivation(statusResult.reason)
                return false
            } else if (statusResult is com.example.data.sync.UserAccountStatusResult.Active) {
                val updated = statusResult.user
                if (_loggedInUser.value?.statut != updated.statut ||
                    _loggedInUser.value?.photo != updated.photo ||
                    _loggedInUser.value?.photoUrl != updated.photoUrl ||
                    _loggedInUser.value?.nom != updated.nom ||
                    _loggedInUser.value?.prenom != updated.prenom ||
                    _loggedInUser.value?.role != updated.role) {
                    _loggedInUser.value = updated
                    sessionManager.saveSession(updated)
                    sessionManager.saveAccount(updated)
                    refreshSavedAccounts()
                }
                return true
            }
        }

        // 2. Raha tsy misy connexion ihany (Hors-ligne) no manamarina ny SQLite Room
        val localUser = if (current.idUser > 0L) {
            repository.findUserById(current.idUser)
        } else {
            repository.findUserByEmailOrPhone(current.email)
        }

        if (localUser != null && SyncService.isInactiveStatut(localUser.statut)) {
            forceLogoutDueToDeactivation("Ce compte a été désactivé dans la base locale.")
            return false
        }

        return true
    }

    private suspend fun refreshLoggedInUserInternal() {
        try {
            val current = _loggedInUser.value ?: return
            val fresh = if (current.idUser > 0L) {
                repository.getUserById(current.idUser)
            } else if (current.email.isNotBlank()) {
                repository.findUserByEmailOrPhone(current.email)
            } else null

            if (fresh != null) {
                if (SyncService.isInactiveStatut(fresh.statut)) {
                    forceLogoutDueToDeactivation("Ce compte a été désactivé.")
                } else {
                    _loggedInUser.value = fresh
                    sessionManager.saveSession(fresh)
                    sessionManager.saveAccount(fresh)
                    refreshSavedAccounts()
                }
            }
        } catch (_: Exception) {}
    }

    fun refreshLoggedInUser() {
        viewModelScope.launch {
            refreshLoggedInUserInternal()
            checkCurrentAccountActiveStatus()
        }
    }

    fun switchUser(user: UserEntity) {
        if (SyncService.isInactiveStatut(user.statut)) {
            forceLogoutDueToDeactivation("Ce compte est désactivé.")
            return
        }
        onUserActivity()
        _loggedInUser.value = user
        sessionManager.saveSession(user)
        sessionManager.saveAccount(user)
        refreshSavedAccounts()
    }

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Form Pull Sync State
    private val _isPullingForm = MutableStateFlow(false)
    val isPullingForm: StateFlow<Boolean> = _isPullingForm.asStateFlow()

    private val _pullFormStatus = MutableStateFlow<Pair<String, Boolean>?>(null)
    val pullFormStatus: StateFlow<Pair<String, Boolean>?> = _pullFormStatus.asStateFlow()

    // Supabase Realtime WebSocket connection status
    val isRealtimeConnected: StateFlow<Boolean> = realtimeManager.isConnected

    // Indicator of sync errors & background active syncs
    private val _hasSyncError = MutableStateFlow(false)
    val hasSyncError: StateFlow<Boolean> = _hasSyncError.asStateFlow()

    private val _isRealtimeSyncing = MutableStateFlow(false)
    val isRealtimeSyncing: StateFlow<Boolean> = _isRealtimeSyncing.asStateFlow()

    // Indicateur de synchronisation global (Synchronisé / Synchronisation en cours / Hors ligne / Erreur)
    val globalSyncState: StateFlow<GlobalSyncState> = combine(
        isConnectedAndOnline,
        syncService.isSyncing,
        _isPullingForm,
        _isRealtimeSyncing,
        _hasSyncError
    ) { isOnline, syncing, pulling, realtimeSync, hasErr ->
        when {
            !isOnline -> GlobalSyncState.HORS_LIGNE
            syncing || pulling || realtimeSync -> GlobalSyncState.EN_COURS
            hasErr -> GlobalSyncState.ERREUR
            else -> GlobalSyncState.SYNCHRONISE
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GlobalSyncState.HORS_LIGNE)

    suspend fun processPendingDeletions() {
        if (!isConnectedAndOnline.value || _isForceOffline.value) return
        val pending = sessionManager.getPendingDeletions()
        if (pending.isNotEmpty()) {
            for (id in pending) {
                val ok = syncService.deleteRemoteEnquete(id)
                if (ok) {
                    sessionManager.removePendingDeletion(id)
                }
            }
        }
    }

    private var duplicateCheckJob: Job? = null

    init {
        realtimeManager.syncService = syncService

        // Wire Realtime Callbacks
        realtimeManager.onUserDeactivated = { user ->
            forceLogoutDueToDeactivation("Ce compte a été désactivé par l'administrateur.")
        }
        realtimeManager.onDataChanged = { table, action ->
            _lastRealtimeSyncTime.value = System.currentTimeMillis()
            _hasSyncError.value = false
            if (table == "users" || table == "utilisateurs") {
                refreshSavedAccounts()
                viewModelScope.launch { refreshLoggedInUserInternal() }
            } else if (table == "questionnaires" || table == "formulaires" || table == "questions" || table == "options") {
                viewModelScope.launch { syncService.quickCheckQuestionnairesRealtime() }
            }
        }

        viewModelScope.launch {
            realtimeManager.eventNotification.collect { notif ->
                if (notif != null) {
                    _liveUpdateNotification.value = notif
                    _lastRealtimeSyncTime.value = System.currentTimeMillis()
                    _hasSyncError.value = false
                    if (notif.contains("annonce", ignoreCase = true) || notif.contains("reçue", ignoreCase = true)) {
                        com.example.util.AndroidNotificationHelper.showAnnouncementNotification(
                            getApplication(),
                            "Nouvelle Annonce • Enquête Terrain",
                            notif
                        )
                    }
                }
            }
        }

        // 0. Averina ny fidirana voatahiry eo an-toerana (Local User Session Storage)
        val savedUser = sessionManager.getSession()
        if (savedUser != null) {
            if (SyncService.isInactiveStatut(savedUser.statut)) {
                forceLogoutDueToDeactivation("Ce compte a été désactivé.")
            } else {
                _loggedInUser.value = savedUser
                sessionManager.saveAccount(savedUser)
                _codeUnique.value = generateUniqueCode()
            }
        }
        refreshSavedAccounts()

        // 0.1. Fanaraha-maso d'inactivité de 30 minutes (Sécurité automatique vers l'écran de login)
        viewModelScope.launch {
            while (isActive) {
                checkInactivityTimeout()
                kotlinx.coroutines.delay(10000) // Vérification périodique toutes les 10s
            }
        }

        // 0.4. Fanaraha-maso sy fitantanana ny lisitry ny questionnaires mavitrika (PUBLIÉ <-> BROUILLON) :
        // - Rehefa miova ho BROUILLON any amin'ny admin dia miala automatique (sivana sy fampahafantarana eo no ho eo).
        // - Raha ilay questionnaire teo am-pelatanana no lasa brouillon dia mifindra hoazy amin'ny questionnaire manaraka.
        // - Raha miova ho PUBLIÉ indray dia miverina automatique ary misafidy azy hoazy raha tsy nisy formulaire mavitrika.
        viewModelScope.launch {
            var previousPublishedList: List<QuestionnaireEntity>? = null
            var lastDraftedQuestionnaireId: Long? = null

            allQuestionnaires.collect { currentList ->
                val prev = previousPublishedList
                previousPublishedList = currentList

                if (prev != null) {
                    val prevIds = prev.map { it.idQuestionnaire }.toSet()
                    val currentIds = currentList.map { it.idQuestionnaire }.toSet()

                    // 1. Raha nisy questionnaire lasa BROUILLON tany amin'ny admin
                    val removedIds = prevIds - currentIds
                    if (removedIds.isNotEmpty()) {
                        val currentSelected = _selectedQuestionnaireId.value

                        // Raha ilay questionnaire teo am-pelatanana mihitsy no novana ho brouillon
                        if (currentSelected != null && removedIds.contains(currentSelected)) {
                            lastDraftedQuestionnaireId = currentSelected
                            // Raha tsy mbola nisy zavatra nosoratana ihany no afindra hoazy
                            if (_textResponses.value.isEmpty()) {
                                val nextQ = currentList.firstOrNull()
                                if (nextQ != null) {
                                    _selectedQuestionnaireId.value = nextQ.idQuestionnaire
                                } else {
                                    _selectedQuestionnaireId.value = null
                                }
                            }
                        }
                    }

                    // 2. Raha nisy questionnaire navoaka (PUBLIÉ) indray tany amin'ny admin
                    val addedIds = currentIds - prevIds
                    if (addedIds.isNotEmpty()) {
                        val currentSelected = _selectedQuestionnaireId.value

                        if (lastDraftedQuestionnaireId != null && addedIds.contains(lastDraftedQuestionnaireId)) {
                            val targetQ = currentList.find { it.idQuestionnaire == lastDraftedQuestionnaireId }
                            if (targetQ != null && _textResponses.value.isEmpty()) {
                                _selectedQuestionnaireId.value = targetQ.idQuestionnaire
                                lastDraftedQuestionnaireId = null
                            }
                        } else if (currentSelected == null || !currentIds.contains(currentSelected)) {
                            val targetQ = currentList.find { addedIds.contains(it.idQuestionnaire) } ?: currentList.firstOrNull()
                            if (targetQ != null && _textResponses.value.isEmpty()) {
                                _selectedQuestionnaireId.value = targetQ.idQuestionnaire
                            }
                        }
                    }
                } else {
                    // Initialization
                    if (_selectedQuestionnaireId.value == null && currentList.isNotEmpty()) {
                        _selectedQuestionnaireId.value = currentList.first().idQuestionnaire
                    }
                }
            }
        }

        // 0.5. Fanaraha-maso tsy an-kijanona ny satan'ny kaonty sy ny fiovana rehetra (Watcher temps réel isa segondra) :
        // - Rehefa tafiditra : raha ovaina ho INACTIF ny kaonty ao amin'ny Supabase dia mivoaka hoazy avy hatrany (Déconnexion automatique).
        // - Rehefa eo amin'ny Login Screen : raha naverina natao ACTIF indray ilay kaonty tao amin'ny Supabase
        //   dia manavao hoazy ny lisitry ny kaonty (Saved Accounts) sy ny SQLite mba hahafahana miditra avy hatrany.
        // - Manaraka isa segondra ny fiovan'ny questionnaire (Brouillon <-> Publié) ao amin'ny Supabase
        viewModelScope.launch {
            while (isActive) {
                try {
                    if (isConnectedAndOnline.value && !_isForceOffline.value) {
                        // Reconnect realtime WebSocket if dropped
                        if (!realtimeManager.isConnected.value) {
                            realtimeManager.start(syncService.supabaseUrl, syncService.supabaseAnonKey, false)
                        }

                        // Process any pending deletions from offline mode
                        processPendingDeletions()

                        // 1. Manaraka haingana isa segondra ny questionnaire (PUBLIÉ <-> BROUILLON)
                        syncService.quickCheckQuestionnairesRealtime()

                        // 2. Manaraka haingana isa segondra ny mpampiasa (ACTIF <-> INACTIF)
                        val freshUsers = syncService.pullUsersFromSupabase()
                        if (freshUsers.isNotEmpty()) {
                            repository.saveUsers(freshUsers)

                            // Fanamarinana eo no ho eo ilay mpampiasa mavitrika tafiditra (Instant Logout if Inactive)
                            val current = _loggedInUser.value
                            if (current != null) {
                                val currentMatch = freshUsers.find { u ->
                                    (current.idUser > 0L && u.idUser > 0L && u.idUser == current.idUser) ||
                                    (current.email.isNotBlank() && u.email.isNotBlank() && u.email.trim().equals(current.email.trim(), ignoreCase = true)) ||
                                    (!current.telephone.isNullOrBlank() && !u.telephone.isNullOrBlank() && u.telephone!!.trim() == current.telephone!!.trim()) ||
                                    (current.nom.isNotBlank() && current.prenom.isNotBlank() && u.nom.trim().equals(current.nom.trim(), ignoreCase = true) && u.prenom.trim().equals(current.prenom.trim(), ignoreCase = true))
                                }
                                if (currentMatch != null) {
                                    if (SyncService.isInactiveStatut(currentMatch.statut)) {
                                        forceLogoutDueToDeactivation("Ce compte a été désactivé.")
                                    } else {
                                        val hasProfileChanged = current.statut != "ACTIF" ||
                                                current.nom != currentMatch.nom ||
                                                current.prenom != currentMatch.prenom ||
                                                current.role != currentMatch.role ||
                                                current.photo != currentMatch.photo ||
                                                current.photoUrl != currentMatch.photoUrl
                                        if (hasProfileChanged) {
                                            val updated = current.copy(
                                                statut = "ACTIF",
                                                nom = currentMatch.nom,
                                                prenom = currentMatch.prenom,
                                                role = currentMatch.role,
                                                photo = currentMatch.photo,
                                                photoUrl = currentMatch.photoUrl
                                            )
                                            _loggedInUser.value = updated
                                            sessionManager.saveSession(updated)
                                            sessionManager.saveAccount(updated)
                                        }
                                    }
                                }
                            }

                            // Fanamarinana eo no ho eo ireo kaonty voatahiry rehetra (Saved Accounts)
                            var anyChanged = false
                            val saved = sessionManager.getSavedAccounts()
                            for (s in saved) {
                                val match = freshUsers.find { fresh ->
                                    (s.email.isNotBlank() && fresh.email.isNotBlank() && fresh.email.trim().equals(s.email.trim(), ignoreCase = true)) ||
                                    (!s.telephone.isNullOrBlank() && !fresh.telephone.isNullOrBlank() && fresh.telephone!!.trim() == s.telephone!!.trim()) ||
                                    (s.idUser > 0L && fresh.idUser > 0L && fresh.idUser == s.idUser) ||
                                    (s.nom.isNotBlank() && s.prenom.isNotBlank() && fresh.nom.trim().equals(s.nom.trim(), ignoreCase = true) && fresh.prenom.trim().equals(s.prenom.trim(), ignoreCase = true))
                                }
                                if (match != null) {
                                    val isNowActive = !SyncService.isInactiveStatut(match.statut)
                                    val wasInactive = SyncService.isInactiveStatut(s.statut)
                                    if (match.statut != s.statut || isNowActive != !wasInactive) {
                                        val effectiveStatut = if (isNowActive) "ACTIF" else "INACTIF"
                                        sessionManager.updateSavedAccountStatut(s.idUser, s.email, effectiveStatut, s.telephone, s.nom, s.prenom)
                                        if (s.email.isNotBlank()) {
                                            repository.updateUserStatutByEmail(s.email, effectiveStatut)
                                        }
                                        if (s.idUser > 0L) {
                                            repository.updateUserStatut(s.idUser, effectiveStatut)
                                        }
                                        if (match.idUser > 0L) {
                                            repository.updateUserStatut(match.idUser, effectiveStatut)
                                        }
                                        val userDisplayName = "${match.nom} ${match.prenom}".trim().ifBlank { s.email }
                                        if (isNowActive) {
                                            _statusChangeNotification.value = Pair("✅ Compte « $userDisplayName » réactivé par l'administrateur ! Vous pouvez vous connecter.", true)
                                        } else {
                                            _statusChangeNotification.value = Pair("⛔ Compte « $userDisplayName » désactivé par l'administrateur.", false)
                                        }
                                        anyChanged = true
                                    }
                                }
                            }
                            if (anyChanged) {
                                refreshSavedAccounts()
                            }
                        }
                    }
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(15000) // Fallback watcher périodique (WebSocket Realtime gère déjà les événements instantanés)
            }
        }

        // 1. Initialise Room SQLite (users, code unique) & Auto-pull Supabase forms if online
        viewModelScope.launch {
            try {
                repository.seedInitialDataIfEmpty()
                refreshLoggedInUserInternal()
                _codeUnique.value = generateUniqueCode()
                checkCodeDuplicateDebounced(_codeUnique.value)
                if (!_isForceOffline.value) {
                    pullFormsFromSupabase()
                }
            } catch (_: Throwable) {
                // Ignore seed error
            }
        }

        // Mihaino ny fanovana ny mpampiasa rehetra mba hivoahana amin'ny kaonty (Logout) raha tena lasa INACTIF
        // na mba hanavaozana ny mombamomba azy raha lasa ACTIF indray
        viewModelScope.launch {
            allUsers.collect { userList ->
                refreshSavedAccounts()
                val current = _loggedInUser.value ?: return@collect
                val matching = userList.filter { 
                    (it.idUser > 0L && it.idUser == current.idUser) || 
                    (it.email.isNotBlank() && it.email.equals(current.email, ignoreCase = true)) 
                }
                if (matching.isNotEmpty()) {
                    val isAnyActive = matching.any { !SyncService.isInactiveStatut(it.statut) }
                    if (!isAnyActive) {
                        forceLogoutDueToDeactivation("Ce compte a été désactivé.")
                    } else {
                        val activeMatch = matching.find { !SyncService.isInactiveStatut(it.statut) }
                        if (activeMatch != null && (
                            _loggedInUser.value?.statut != activeMatch.statut ||
                            _loggedInUser.value?.photo != activeMatch.photo ||
                            _loggedInUser.value?.photoUrl != activeMatch.photoUrl
                        )) {
                            _loggedInUser.value = activeMatch
                            sessionManager.saveSession(activeMatch)
                            sessionManager.saveAccount(activeMatch)
                        }
                    }
                }
            }
        }


        // 2. Mihaino ny fanovana rehetra (Code unique, valinteny rehetra, safidy, fanontaniana) mba hanavaozana hoazy sy avy hatrany ny validation (ohatra: 1/5 obligatoire, 2/5 obligatoire, etc.)
        viewModelScope.launch {
            try {
                combine(
                    _codeUnique,
                    _textResponses,
                    _selectedOptions,
                    questions
                ) { _, _, _, _ ->
                    recalculateValidation()
                }.collect { }
            } catch (_: Throwable) {
                // Flow error
            }
        }

        // 3. Listener réseau ConnectivityManager (Auto-sync quand la connexion est rétablie) :
        // Dès que la connexion internet est rétablie, met à jour automatiquement
        // l'indicateur visuel de synchronisation de chaque enquête en synchronisant
        // immédiatement les fiches locales en attente vers Supabase.
        viewModelScope.launch {
            var wasOnline: Boolean? = null
            isConnectedAndOnline.collect { isOnlineNow ->
                val previous = wasOnline
                if (isOnlineNow) {
                    realtimeManager.start(syncService.supabaseUrl, syncService.supabaseAnonKey, _isForceOffline.value)
                    processPendingDeletions()
                    syncAllPendingSilently()
                    pullFormsFromSupabase()
                    if (previous == false) {
                        _liveUpdateNotification.value = "Connexion internet rétablie • Synchronisation automatique en cours..."
                    }
                } else {
                    realtimeManager.stop()
                    if (previous == true) {
                        _liveUpdateNotification.value = "Connexion internet interrompue • Mode hors-ligne actif (SQLite)"
                    }
                }
                wasOnline = isOnlineNow
            }
        }
    }

    fun ensureDefaultSurveyExists() {
        pullFormsFromSupabase()
    }


    fun login(identifier: String, motDePasse: String) {
        viewModelScope.launch {
            try {
                _isLoggingIn.value = true
                _loginError.value = null
                val result = syncService.loginUser(identifier, motDePasse)
                result.onSuccess { user ->
                    onUserActivity()
                    _loggedInUser.value = user
                    sessionManager.saveSession(user)
                    refreshSavedAccounts()
                    _loginError.value = null
                    _codeUnique.value = generateUniqueCode()
                    checkCodeDuplicateDebounced(_codeUnique.value)
                    // Pull forms automatically after successful login if not force offline
                    if (!_isForceOffline.value) {
                        pullFormsFromSupabase()
                    }
                }.onFailure { err ->
                    _loginError.value = err.localizedMessage ?: "Identifiant ou mot de passe incorrect."
                }
            } catch (e: Throwable) {
                _loginError.value = e.localizedMessage ?: "Erreur de connexion."
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _loggedInUser.value = null
        _loginError.value = null
        _selectedEnqueteDetail.value = null
        _codeUnique.value = generateUniqueCode()
        checkCodeDuplicateDebounced(_codeUnique.value)
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun restoreDefaultSurvey() {
        pullFormsFromSupabase()
    }

    fun pullFormsFromSupabase() {
        viewModelScope.launch {
            try {
                _isPullingForm.value = true
                val user = _loggedInUser.value ?: currentUser.value
                val enqueteurId = if (user != null && !user.role.equals("ADMIN", ignoreCase = true)) user.idUser else null
                val userRole = user?.role
                val result = syncService.pullFormsAndSchemaFromSupabase(enqueteurId, userRole)
                _lastRealtimeSyncTime.value = System.currentTimeMillis()

                _pullFormStatus.value = Pair(result.message, !result.success)
                refreshLoggedInUserInternal()
                _isPullingForm.value = false

                // Manamarina raha mbola ao amin'ny lisitry ny questionnaires navoaka (PUBLIE) ilay voafidy
                val updatedList = allQuestionnaires.value
                if (updatedList.isNotEmpty()) {
                    val currentSelected = _selectedQuestionnaireId.value
                    if (currentSelected == null || updatedList.none { it.idQuestionnaire == currentSelected }) {
                        _selectedQuestionnaireId.value = updatedList.first().idQuestionnaire
                    }
                } else {
                    _selectedQuestionnaireId.value = null
                }
            } catch (e: Throwable) {
                _isPullingForm.value = false
                _pullFormStatus.value = Pair("Erreur de synchronisation des formulaires.", true)
            }
        }
    }

    fun clearPullFormStatus() {
        _pullFormStatus.value = null
    }

    /**
     * Rafraîchit les formulaires et synchronise les données en attente (Pull-To-Refresh)
     * Direct sy haingana tsy misy fahatarana (sans délai artificiel).
     */
    fun refreshAllData() {
        viewModelScope.launch {
            _isRefreshingData.value = true
            _isPullingForm.value = true
            try {
                try {
                    repository.cleanDraftQuestionnaires()
                } catch (_: Exception) {}

                val user = _loggedInUser.value
                val enqueteurId = if (user != null && !user.role.equals("ADMIN", ignoreCase = true)) user.idUser else null
                val userRole = user?.role
                // 1. Pull questions & options from Supabase immédiatement
                val result = syncService.pullFormsAndSchemaFromSupabase(enqueteurId, userRole)
                _pullFormStatus.value = Pair(result.message, !result.success)

                // 2. Sync pending surveys for this enqueteur if online
                if (isConnectedAndOnline.value) {
                    syncService.synchronizeAll(enqueteurId)
                }

                refreshLoggedInUserInternal()

                // Mitazona ny questionnaire efa nofidian'ny mpampiasa na mifidy ny voalohany raha vao misokatra
                val currentList = allQuestionnaires.value
                if (currentList.isNotEmpty()) {
                    val currentSelected = _selectedQuestionnaireId.value
                    if (currentSelected == null || currentList.none { it.idQuestionnaire == currentSelected }) {
                        _selectedQuestionnaireId.value = currentList.first().idQuestionnaire
                    }
                } else {
                    _selectedQuestionnaireId.value = null
                }
            } catch (e: Throwable) {
                _pullFormStatus.value = Pair("Actualisation terminée", false)
            } finally {
                _isPullingForm.value = false
                _isRefreshingData.value = false
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _currentTab.value = tabIndex
        // Fakana avy hatrany ny questionnaires vaovao avy ao amin'ny Supabase rehefa miova pejy ny mpampiasa
        if (isConnectedAndOnline.value && !_isForceOffline.value && !_isPullingForm.value) {
            pullFormsFromSupabase()
        }
    }

    fun startNewSurvey() {
        resetForm()
        setTab(2)
        if (isConnectedAndOnline.value && !_isForceOffline.value && !_isPullingForm.value) {
            pullFormsFromSupabase()
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleOfflineSimulation() {
        _isForceOffline.value = !_isForceOffline.value
        syncService.isSimulatedOffline = _isForceOffline.value
        if (_isForceOffline.value) {
            realtimeManager.stop()
            syncService.addLog("Mode hors-ligne forcé activé (Zone blanche).", false)
        } else {
            realtimeManager.start(syncService.supabaseUrl, syncService.supabaseAnonKey, false)
            syncService.addLog("Mode en ligne rétabli. Prêt pour synchronisation.", true)
            viewModelScope.launch {
                processPendingDeletions()
                syncAllPendingSilently()
                pullFormsFromSupabase()
            }
        }
    }

    fun updateSupabaseConfig(url: String, anonKey: String) {
        syncService.supabaseUrl = url
        syncService.supabaseAnonKey = anonKey
        realtimeManager.updateConfig(url, anonKey, _isForceOffline.value)
        syncService.addLog("Configuration Supabase mise à jour : $url", true)
    }

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<Pair<Boolean, String>?>(null)
    val connectionTestResult: StateFlow<Pair<Boolean, String>?> = _connectionTestResult.asStateFlow()

    fun testSupabaseConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            val result = syncService.testLiveConnection()
            _connectionTestResult.value = result
            _isTestingConnection.value = false
        }
    }

    fun clearSyncLogs() {
        syncService.clearLogs()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    private var lastSubmittedCodeTracker: String? = null

    fun onCodeChanged(newCode: String) {
        _codeUnique.value = newCode
        val currentQ = questions.value
        val codeQuestion = currentQ.find { it.question.estCodeUnique }
        if (codeQuestion != null) {
            val updated = _textResponses.value.toMutableMap()
            if (newCode.isBlank()) {
                updated.remove(codeQuestion.question.idQuestion)
            } else {
                updated[codeQuestion.question.idQuestion] = newCode
            }
            _textResponses.value = updated
        }
        checkCodeDuplicateDebounced(newCode)
    }

    fun selectPersonne(code: String) {
        onCodeChanged(code)
    }

    fun generateNewCode() {
        val next = generateUniqueCode(forceNextSequence = true)
        onCodeChanged(next)
    }

    fun onDateChanged(newDate: String) {
        _dateEnquete.value = newDate
        recalculateValidation()
    }

    fun onTextChanged(questionId: Long, text: String) {
        val updated = _textResponses.value.toMutableMap()
        if (text.isBlank()) {
            updated.remove(questionId)
        } else {
            updated[questionId] = text
        }
        _textResponses.value = updated

        // If the question modified is the code unique question, update _codeUnique
        val currentQ = questions.value.find { it.question.idQuestion == questionId }
        if (currentQ?.question?.estCodeUnique == true && _codeUnique.value != text) {
            _codeUnique.value = text
            checkCodeDuplicateDebounced(text)
        } else if (currentQ != null && isPhoneQuestion(currentQ.question.libelle, currentQ.question.type) && text.isNotBlank()) {
            checkPhoneDuplicateDebounced(questionId, text)
        } else {
            recalculateValidation()
        }
    }

    fun isAutreOption(libelle: String): Boolean {
        val clean = libelle.trim().lowercase()
        return clean == "autre" || clean == "autres" || clean == "hafa" || clean == "other" ||
                clean.startsWith("autre") || clean.startsWith("hafa") ||
                clean.contains("préciser") || clean.contains("preciser") ||
                clean.contains("à préciser") || clean.contains("a preciser")
    }

    fun onAutreTextChanged(questionId: Long, customText: String) {
        val updatedAutre = _autreResponses.value.toMutableMap()
        if (customText.isBlank()) {
            updatedAutre.remove(questionId)
        } else {
            updatedAutre[questionId] = customText
        }
        _autreResponses.value = updatedAutre

        // Update main text response
        val updatedText = _textResponses.value.toMutableMap()
        val qItem = questions.value.find { it.question.idQuestion == questionId }
        val selectedOptIds = _selectedOptions.value[questionId] ?: emptyList()

        if (qItem?.question?.type == "CHOIX_MULTIPLE") {
            val labels = selectedOptIds.mapNotNull { oId ->
                val oObj = qItem.options.find { it.idOption == oId }
                if (oObj != null) {
                    if (isAutreOption(oObj.libelle)) {
                        if (customText.isNotBlank()) "${oObj.libelle}: $customText" else oObj.libelle
                    } else {
                        oObj.libelle
                    }
                } else null
            }
            if (labels.isNotEmpty()) {
                updatedText[questionId] = labels.joinToString(", ")
            } else {
                updatedText.remove(questionId)
            }
        } else {
            val selectedOptId = selectedOptIds.firstOrNull()
            val opt = qItem?.options?.find { it.idOption == selectedOptId }
            val optLabel = opt?.libelle ?: "Autre"

            if (customText.isNotBlank()) {
                updatedText[questionId] = "$optLabel: $customText"
            } else {
                updatedText[questionId] = optLabel
            }
        }
        _textResponses.value = updatedText

        recalculateValidation()
    }

    fun onSingleOptionSelected(questionId: Long, optionId: Long, optionText: String) {
        val updatedOptions = _selectedOptions.value.toMutableMap()
        updatedOptions[questionId] = listOf(optionId)
        _selectedOptions.value = updatedOptions

        val updatedText = _textResponses.value.toMutableMap()
        val isAutre = isAutreOption(optionText)
        if (isAutre) {
            val existingAutre = _autreResponses.value[questionId] ?: ""
            if (existingAutre.isNotBlank()) {
                updatedText[questionId] = "$optionText: $existingAutre"
            } else {
                updatedText[questionId] = optionText
            }
        } else {
            val updatedAutre = _autreResponses.value.toMutableMap()
            updatedAutre.remove(questionId)
            _autreResponses.value = updatedAutre
            updatedText[questionId] = optionText
        }
        _textResponses.value = updatedText

        recalculateValidation()
    }

    fun onMultiOptionToggled(questionId: Long, optionId: Long) {
        val updatedOptions = _selectedOptions.value.toMutableMap()
        val currentList = updatedOptions[questionId]?.toMutableList() ?: mutableListOf()
        if (currentList.contains(optionId)) {
            currentList.remove(optionId)
        } else {
            currentList.add(optionId)
        }
        if (currentList.isEmpty()) {
            updatedOptions.remove(questionId)
        } else {
            updatedOptions[questionId] = currentList
        }
        _selectedOptions.value = updatedOptions

        val qItem = questions.value.find { it.question.idQuestion == questionId }
        val opt = qItem?.options?.find { it.idOption == optionId }
        if (opt != null && isAutreOption(opt.libelle)) {
            if (!currentList.contains(optionId)) {
                val updatedAutre = _autreResponses.value.toMutableMap()
                updatedAutre.remove(questionId)
                _autreResponses.value = updatedAutre
            }
        }

        // Update textResponses for CHOIX_MULTIPLE
        val updatedText = _textResponses.value.toMutableMap()
        if (qItem != null) {
            val autreText = _autreResponses.value[questionId] ?: ""
            val labels = currentList.mapNotNull { oId ->
                val oObj = qItem.options.find { it.idOption == oId }
                if (oObj != null) {
                    if (isAutreOption(oObj.libelle)) {
                        if (autreText.isNotBlank()) "${oObj.libelle}: $autreText" else oObj.libelle
                    } else {
                        oObj.libelle
                    }
                } else null
            }
            if (labels.isNotEmpty()) {
                updatedText[questionId] = labels.joinToString(", ")
            } else {
                updatedText.remove(questionId)
            }
            _textResponses.value = updatedText
        }

        recalculateValidation()
    }

    fun checkCodeDuplicateDebounced(code: String) {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = viewModelScope.launch {
            val clean = code.trim()
            val currentQ = questionnaire.value
            val currentQId = currentQ?.idQuestionnaire
            val currentQTitre = currentQ?.titre ?: "ce questionnaire"
            val allQ = allQuestionnaires.value

            if (clean.isNotBlank()) {
                val person = repository.getPersonneByCode(clean)
                val answeredList = repository.getAnsweredQuestionnairesByCode(clean)
                val alreadyAnsweredCurrent = currentQId != null && answeredList.any { it.idQuestionnaire == currentQId }

                val availabilities = allQ.map { q ->
                    val answered = answeredList.find { it.idQuestionnaire == q.idQuestionnaire }
                    QuestionnaireAvailability(
                        questionnaire = q,
                        isAnswered = answered != null,
                        dateAnswered = answered?.dateEnquete,
                        idEnquete = answered?.idEnquete,
                        syncStatus = answered?.syncStatus
                    )
                }

                val availableQList = availabilities.filter { !it.isAnswered }.map { it.questionnaire }
                val answeredQList = availabilities.filter { it.isAnswered }

                val otherAnswered = answeredList.filter { it.idQuestionnaire != currentQId }
                val otherTitles = otherAnswered.map { it.titre }

                val current = _validationState.value
                if (alreadyAnsweredCurrent) {
                    val answeredRecord = answeredList.find { it.idQuestionnaire == currentQId }
                    val dateInfo = answeredRecord?.dateEnquete?.let { " (effectuée le $it)" } ?: ""
                    _validationState.value = current.copy(
                        isExistingCode = true,
                        personFound = true,
                        person = person,
                        isAlreadyAnsweredForCurrentQuestionnaire = true,
                        answeredQuestionnaires = answeredList.map { it.titre },
                        questionnaireAvailabilities = availabilities,
                        availableQuestionnaires = availableQList,
                        answeredQuestionnairesCount = answeredQList.size,
                        availableQuestionnairesCount = availableQList.size,
                        codeNotice = null,
                        codeDuplicateError = "🚫 Règle UNIQUE(id_personne, id_questionnaire) : Cette personne ($clean) a déjà répondu au questionnaire « $currentQTitre »$dateInfo. Une personne ne peut répondre qu'une seule fois à un même questionnaire."
                    )
                } else if (person != null || answeredList.isNotEmpty()) {
                    val notice = if (otherTitles.isNotEmpty()) {
                        "👤 Personne identifiée ($clean) • A déjà répondu à ${otherTitles.size} questionnaire(s) : ${otherTitles.joinToString(", ")}. Éligible pour « $currentQTitre » (${availableQList.size} questionnaire(s) disponible(s))."
                    } else {
                        "👤 Personne identifiée ($clean) • Éligible pour « $currentQTitre » (${availableQList.size} questionnaire(s) disponible(s))."
                    }
                    _validationState.value = current.copy(
                        isExistingCode = true,
                        personFound = true,
                        person = person,
                        isAlreadyAnsweredForCurrentQuestionnaire = false,
                        answeredQuestionnaires = answeredList.map { it.titre },
                        questionnaireAvailabilities = availabilities,
                        availableQuestionnaires = availableQList,
                        answeredQuestionnairesCount = answeredQList.size,
                        availableQuestionnairesCount = availableQList.size,
                        codeNotice = notice,
                        codeDuplicateError = null
                    )
                } else {
                    _validationState.value = current.copy(
                        isExistingCode = false,
                        personFound = false,
                        person = null,
                        isAlreadyAnsweredForCurrentQuestionnaire = false,
                        answeredQuestionnaires = emptyList(),
                        questionnaireAvailabilities = availabilities,
                        availableQuestionnaires = allQ,
                        answeredQuestionnairesCount = 0,
                        availableQuestionnairesCount = allQ.size,
                        codeNotice = "Nouveau répondant : Le code $clean identifiera cette personne pour l'ensemble de ses questionnaires futurs.",
                        codeDuplicateError = null
                    )
                }
            } else {
                val availabilities = allQ.map { QuestionnaireAvailability(questionnaire = it, isAnswered = false) }
                val current = _validationState.value
                _validationState.value = current.copy(
                    isExistingCode = false,
                    personFound = false,
                    person = null,
                    isAlreadyAnsweredForCurrentQuestionnaire = false,
                    answeredQuestionnaires = emptyList(),
                    questionnaireAvailabilities = availabilities,
                    availableQuestionnaires = allQ,
                    answeredQuestionnairesCount = 0,
                    availableQuestionnairesCount = allQ.size,
                    codeNotice = null,
                    codeDuplicateError = null
                )
            }
            recalculateValidation()
        }
    }

    private var phoneDuplicateCheckJob: Job? = null
    fun checkPhoneDuplicateDebounced(questionId: Long, phoneText: String) {
        phoneDuplicateCheckJob?.cancel()
        phoneDuplicateCheckJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350)
            val currentCode = _codeUnique.value.trim()
            val match = repository.checkPhoneDuplicate(phoneText, excludeCode = currentCode)
            if (match != null) {
                val warning = "⚠️ Doublon critique : Ce numéro est déjà enregistré dans l'enquête #${match.id_enquete} (Code: ${match.code_unique})"
                val updatedErrors = _validationState.value.fieldErrors.toMutableMap()
                updatedErrors[questionId] = warning
                _validationState.value = _validationState.value.copy(
                    fieldErrors = updatedErrors,
                    respondentDuplicateWarning = warning,
                    phoneDuplicateWarning = warning,
                    isReadyToSubmit = false
                )
            } else {
                val updatedErrors = _validationState.value.fieldErrors.toMutableMap()
                if (updatedErrors[questionId]?.contains("Doublon critique") == true) {
                    updatedErrors.remove(questionId)
                }
                _validationState.value = _validationState.value.copy(
                    fieldErrors = updatedErrors,
                    respondentDuplicateWarning = null,
                    phoneDuplicateWarning = null
                )
                recalculateValidation()
            }
        }
    }

    private fun checkRespondentDuplicate(questionId: Long, value: String) {
        viewModelScope.launch {
            val existingCode = repository.checkResponseDuplicate(questionId, value)
            val current = _validationState.value
            _validationState.value = current.copy(
                respondentDuplicateWarning = if (existingCode != null) {
                    "Doublon potentiel : Ce répondant/téléphone est déjà enregistré dans l'enquête '$existingCode' !"
                } else null
            )
        }
    }

    private fun isPhoneQuestion(libelle: String, type: String): Boolean {
        val l = libelle.lowercase()
        val t = type.uppercase()
        if (t == "TELEPHONE") return true
        if (t != "TEXTE" && t != "NOMBRE") return false
        return l.contains("téléphone") || l.contains("telephone") ||
                l.contains("contact") || l.contains("phone") ||
                l.contains("mobile") || l.contains("finday") ||
                (l.contains("numéro") && (l.contains("tel") || l.contains("appel") || l.contains("joindre")))
    }

    private fun validatePhoneNumber(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.any { it.isLetter() }) {
            return "Le numéro de téléphone ne doit contenir que des chiffres"
        }
        val cleaned = trimmed.replace(Regex("[\\s\\-\\.\\(\\)]"), "")
        if (!cleaned.matches(Regex("^\\+?\\d+$"))) {
            return "Format invalide : caractères non autorisés"
        }

        val digitsOnly = if (cleaned.startsWith("+")) cleaned.substring(1) else cleaned

        if (cleaned.startsWith("+261")) {
            val nationalPart = digitsOnly.substring(3)
            if (nationalPart.length < 9) {
                return "Numéro incomplet : +261 suivi de 9 chiffres (${nationalPart.length}/9)"
            }
            if (nationalPart.length > 9) {
                return "Numéro trop long : 9 chiffres attendus après +261 (${nationalPart.length}/9)"
            }
            val prefix = nationalPart.take(2)
            if (prefix !in listOf("32", "33", "34", "37", "38", "20")) {
                return "Préfixe opérateur non reconnu (ex: +261 34...)"
            }
            return null
        }

        if (cleaned.startsWith("0")) {
            if (cleaned.length < 10) {
                return "Numéro incomplet : 10 chiffres requis (${cleaned.length}/10)"
            }
            if (cleaned.length > 10) {
                return "Numéro trop long : 10 chiffres attendus (${cleaned.length}/10)"
            }
            val prefix = cleaned.take(3)
            if (prefix !in listOf("032", "033", "034", "037", "038", "020")) {
                return "Opérateur non reconnu (attendu: 032, 033, 034, 037, 038 ou 020)"
            }
            return null
        }

        if (cleaned.length == 9) {
            val prefix = cleaned.take(2)
            if (prefix !in listOf("32", "33", "34", "37", "38", "20")) {
                return "Numéro invalide (ex: 0345812888)"
            }
            return null
        }

        if (cleaned.length < 10) {
            return "Numéro incomplet : 10 chiffres requis (${cleaned.length}/10)"
        }
        if (cleaned.length > 10) {
            return "Numéro trop long (10 chiffres attendus)"
        }

        return null
    }

    private fun recalculateValidation() {
        val qList = questions.value
        val mandatoryQuestions = qList.filter { it.question.obligatoire }
        val totalMandatory = mandatoryQuestions.size

        var answeredCount = 0
        val fieldErrors = mutableMapOf<Long, String>()
        val currentCode = _codeUnique.value.trim()
        val currentDate = _dateEnquete.value.trim()

        val codeError = when {
            currentCode.isBlank() -> "Code unique obligatoire (identifie la personne)"
            _validationState.value.codeDuplicateError != null -> _validationState.value.codeDuplicateError
            _validationState.value.isAlreadyAnsweredForCurrentQuestionnaire -> "Cette personne a déjà répondu à ce questionnaire"
            else -> null
        }

        val dateError = when {
            currentDate.isBlank() -> "La date de l'enquête est obligatoire"
            !currentDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) -> "Format attendu : AAAA-MM-JJ"
            else -> {
                val parts = currentDate.split("-")
                val y = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val d = parts.getOrNull(2)?.toIntOrNull() ?: 0
                if (y < 1900 || y > 2100 || m !in 1..12 || d !in 1..31) {
                    "Date invalide (vérifiez l'année, le mois et le jour)"
                } else null
            }
        }

        for (qItem in qList) {
            val q = qItem.question
            val isMandatory = q.obligatoire
            val rawText = _textResponses.value[q.idQuestion]?.trim() ?: ""
            val selectedOpts = _selectedOptions.value[q.idQuestion] ?: emptyList()
            val autreText = _autreResponses.value[q.idQuestion]?.trim() ?: ""

            // Check if an "Autre" option is selected in this question
            val selectedAutreOption = qItem.options.find { opt ->
                selectedOpts.contains(opt.idOption) && isAutreOption(opt.libelle)
            }
            val hasAutreError = selectedAutreOption != null && autreText.isBlank()

            val isAnswered = when {
                q.estCodeUnique -> currentCode.isNotBlank() || rawText.isNotBlank()
                q.type == "CHOIX_UNIQUE" -> (selectedOpts.isNotEmpty() || rawText.isNotBlank()) && !hasAutreError
                q.type == "CHOIX_MULTIPLE" -> selectedOpts.isNotEmpty() && !hasAutreError
                else -> rawText.isNotBlank()
            }

            if (hasAutreError) {
                fieldErrors[q.idQuestion] = "Veuillez préciser votre réponse pour l'option « ${selectedAutreOption?.libelle ?: "Autre"} »"
            } else if (isMandatory && isAnswered) {
                answeredCount++
            }

            // Real-time format validation rules when filled
            if (rawText.isNotBlank()) {
                val qType = q.type.uppercase()
                val qLabel = q.libelle.lowercase()

                // Rule 1: Phone number format validation
                if (isPhoneQuestion(q.libelle, q.type)) {
                    val phoneErr = validatePhoneNumber(rawText)
                    if (phoneErr != null) {
                        fieldErrors[q.idQuestion] = phoneErr
                    }
                }
                // Rule 2: Email format validation
                else if (qLabel.contains("email") || qLabel.contains("courriel") || qLabel.contains("e-mail")) {
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(rawText).matches()) {
                        fieldErrors[q.idQuestion] = "Format d'adresse e-mail invalide (ex: nom@domaine.com)"
                    }
                }
                // Rule 3: Number format validation
                else if (qType == "NOMBRE") {
                    if (rawText.toDoubleOrNull() == null) {
                        fieldErrors[q.idQuestion] = "Veuillez saisir un nombre valide"
                    }
                }
                // Rule 4: Date format validation
                else if (qType == "DATE") {
                    if (!rawText.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                        fieldErrors[q.idQuestion] = "Format de date attendu : AAAA-MM-JJ"
                    }
                }
            }
        }

        val percent = if (totalMandatory > 0) {
            ((answeredCount.toFloat() / totalMandatory.toFloat()) * 100).toInt()
        } else 100

        val ready = percent == 100 && codeError == null && dateError == null && fieldErrors.isEmpty()

        _validationState.value = _validationState.value.copy(
            completenessPercent = percent,
            answeredMandatoryCount = answeredCount,
            totalMandatoryCount = totalMandatory,
            isReadyToSubmit = ready,
            fieldErrors = fieldErrors,
            codeError = codeError,
            dateError = dateError
        )
    }

    fun submitSurvey(andSync: Boolean = true) {
        val q = questionnaire.value ?: return
        val u = _loggedInUser.value ?: return
        val qList = questions.value
        val submittedCode = _codeUnique.value.trim()

        if (submittedCode.isBlank()) {
            _formMessage.value = Pair("Le code unique est obligatoire.", true)
            return
        }

        viewModelScope.launch {
            if (!checkCurrentAccountActiveStatus()) {
                _formMessage.value = Pair("Opération impossible : Ce compte a été désactivé par l'administrateur.", true)
                return@launch
            }

            // --- CONTRÔLE CRITIQUE DE DOUBLONS AVANT SAUVEGARDE FINALE ---
            val duplicateCheck = repository.checkForDuplicatesInRoom(
                codeUnique = submittedCode,
                questionnaireId = q.idQuestionnaire,
                responses = _textResponses.value
            )

            if (duplicateCheck.hasDuplicate) {
                _duplicateDetectionDialog.value = duplicateCheck
                _formMessage.value = Pair(duplicateCheck.duplicateMessage ?: "Doublon détecté sur un champ critique.", true)
                return@launch
            }

            val result = repository.saveEnquete(
                codeUnique = submittedCode,
                questionnaireId = q.idQuestionnaire,
                enqueteurId = u.idUser,
                dateEnquete = _dateEnquete.value,
                responses = _textResponses.value,
                selectedOptions = _selectedOptions.value,
                questions = qList
            )

            result.onSuccess { newEnqueteId ->
                lastSubmittedCodeTracker = submittedCode
                val isOnlineNow = isConnectedAndOnline.value
                resetForm(submittedCode = submittedCode)
                _formMessage.value = null

                if (isOnlineNow) {
                    val msg = "Enquête $submittedCode enregistrée et transmise avec succès."
                    _formMessage.value = Pair(msg, false)
                    syncService.addLog("Fiche $submittedCode enregistrée en local (SQLite) et synchronisée.", true)
                    syncService.syncSingleById(newEnqueteId)
                } else {
                    val msg = "Enquête $submittedCode enregistrée localement dans SQLite sans perte. Elle sera synchronisée dès le retour du réseau."
                    _formMessage.value = Pair(msg, false)
                    syncService.addLog("Fiche $submittedCode enregistrée avec succès dans SQLite (Hors-ligne).", false)
                }
            }.onFailure { err ->
                _formMessage.value = Pair(err.localizedMessage ?: "Erreur d'enregistrement", true)
            }
        }
    }

    fun clearFormMessage() {
        _formMessage.value = null
    }

    fun resetForm(submittedCode: String? = null) {
        val nextCode = generateUniqueCode(forceNextSequence = submittedCode != null, excludeCode = submittedCode)
        _codeUnique.value = nextCode
        _dateEnquete.value = getTodayDate()
        val initialResponses = mutableMapOf<Long, String>()
        val codeQuestion = questions.value.find { it.question.estCodeUnique }
        if (codeQuestion != null && nextCode.isNotBlank()) {
            initialResponses[codeQuestion.question.idQuestion] = nextCode
        }
        _textResponses.value = initialResponses
        _selectedOptions.value = emptyMap()
        _autreResponses.value = emptyMap()
        _validationState.value = FormValidationState()
        checkCodeDuplicateDebounced(nextCode)
    }

    fun viewEnqueteDetail(enqueteId: Long) {
        viewModelScope.launch {
            val detail = repository.getEnqueteDetail(enqueteId)
            _selectedEnqueteDetail.value = detail
        }
    }

    fun closeEnqueteDetail() {
        _selectedEnqueteDetail.value = null
    }

    fun syncEnqueteById(enqueteId: Long) {
        viewModelScope.launch {
            val user = _loggedInUser.value
            if (user != null && !user.role.equals("ADMIN", ignoreCase = true)) {
                val enquete = repository.getEnqueteById(enqueteId)
                if (enquete != null && enquete.idEnqueteur != user.idUser) {
                    return@launch
                }
            }
            syncService.syncSingleById(enqueteId)
        }
    }

    fun deleteEnquete(enqueteId: Long) {
        viewModelScope.launch {
            val user = _loggedInUser.value
            val enquete = repository.getEnqueteById(enqueteId)
            if (user != null && !user.role.equals("ADMIN", ignoreCase = true)) {
                if (enquete != null && enquete.idEnqueteur != user.idUser) {
                    return@launch
                }
            }
            repository.deleteEnquete(enqueteId)
            if (_selectedEnqueteDetail.value?.enquete?.idEnquete == enqueteId) {
                _selectedEnqueteDetail.value = null
            }

            // Bidirectionnel : Fafana koa ao amin'ny Supabase
            if (isConnectedAndOnline.value && !_isForceOffline.value) {
                val ok = syncService.deleteRemoteEnquete(enqueteId)
                if (!ok) {
                    sessionManager.addPendingDeletion(enqueteId)
                }
            } else {
                sessionManager.addPendingDeletion(enqueteId)
            }
            syncService.addLog("Fiche #$enqueteId supprimée en local et synchronisée.", true)
        }
    }

    fun deleteMultipleEnquetes(enqueteIds: Collection<Long>) {
        viewModelScope.launch {
            val user = _loggedInUser.value
            for (id in enqueteIds) {
                if (user != null && !user.role.equals("ADMIN", ignoreCase = true)) {
                    val eq = repository.getEnqueteById(id)
                    if (eq?.idEnqueteur != user.idUser) continue
                }
                repository.deleteEnquete(id)
                if (isConnectedAndOnline.value && !_isForceOffline.value) {
                    val ok = syncService.deleteRemoteEnquete(id)
                    if (!ok) {
                        sessionManager.addPendingDeletion(id)
                    }
                } else {
                    sessionManager.addPendingDeletion(id)
                }
            }
            if (_selectedEnqueteDetail.value?.enquete?.idEnquete in enqueteIds) {
                _selectedEnqueteDetail.value = null
            }
            syncService.addLog("${enqueteIds.size} fiches supprimées et synchronisées.", true)
        }
    }

    fun selectQuestionnaire(id: Long) {
        _selectedQuestionnaireId.value = id
        _textResponses.value = emptyMap()
        _selectedOptions.value = emptyMap()
        _autreResponses.value = emptyMap()
        checkCodeDuplicateDebounced(_codeUnique.value)
    }

    fun checkQuestionnairesRealtimeNow() {
        viewModelScope.launch {
            syncService.quickCheckQuestionnairesRealtime()
        }
    }

    private fun syncAllPendingSilently() {
        viewModelScope.launch {
            val user = _loggedInUser.value ?: currentUser.value
            val enqueteurId = if (user != null && !user.role.equals("ADMIN", ignoreCase = true)) user.idUser else null
            val result = syncService.synchronizeAll(enqueteurId)
            if (result.success && result.syncedCount > 0) {
                _liveUpdateNotification.value = "${result.syncedCount} enquête(s) synchronisée(s) avec succès vers le serveur."
            }
        }
    }

    fun syncAllPending() {
        viewModelScope.launch {
            val user = _loggedInUser.value ?: currentUser.value
            val enqueteurId = if (user != null && !user.role.equals("ADMIN", ignoreCase = true)) user.idUser else null
            syncService.synchronizeAll(enqueteurId)
        }
    }

    private fun generateUniqueCode(forceNextSequence: Boolean = false, excludeCode: String? = null): String {
        val user = _loggedInUser.value ?: currentUser.value ?: sessionManager.getSession()
        val userId = user?.idUser ?: 1
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val prefix = "ENQ-U$userId-$year"
        val prefixWithDash = "$prefix-"

        val existingCodes = mutableSetOf<String>()
        allPersonnes.value.forEach { existingCodes.add(it.codeUnique.trim()) }
        allEnquetesAudit.value.forEach { existingCodes.add(it.codeUnique.trim()) }
        lastSubmittedCodeTracker?.let { existingCodes.add(it.trim()) }
        excludeCode?.let { existingCodes.add(it.trim()) }

        if (forceNextSequence) {
            val current = _codeUnique.value.trim()
            if (current.startsWith(prefixWithDash, ignoreCase = true)) {
                existingCodes.add(current)
            }
        }

        var maxSeq = 0
        for (code in existingCodes) {
            if (code.startsWith(prefixWithDash, ignoreCase = true)) {
                val suffix = code.substring(prefixWithDash.length)
                val digits = suffix.takeWhile { it.isDigit() }
                val num = digits.toIntOrNull()
                if (num != null && num > maxSeq) {
                    maxSeq = num
                }
            }
        }

        val nextSeq = maxSeq + 1
        return String.format(Locale.US, "%s-%03d", prefix, nextSeq)
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
