package com.example.data.repository

import com.example.data.db.SurveyDao
import com.example.data.model.AnnonceDestinataireEntity
import com.example.data.model.AnnonceEntity
import com.example.data.model.AnnonceFichierEntity
import com.example.data.model.EnqueteDetail
import com.example.data.model.EnqueteEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.OptionEntity
import com.example.data.model.PersonneEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionWithOptions
import com.example.data.model.QuestionnaireAnsweredSummary
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.ReponseEntity
import com.example.data.model.ReponseWithQuestion
import com.example.data.model.ResearchMetrics
import com.example.data.model.SyncedAuditRecord
import com.example.data.model.UserEntity
import com.example.data.sync.SyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import android.content.Context
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SurveyRepository(private val surveyDao: SurveyDao, private val context: Context? = null) {

    private var duplicatesPreventedCounter = 0
    private var validationsPassedCounter = 0

    val allEnquetes: Flow<List<EnqueteEntity>> = surveyDao.getAllEnquetes()

    val allAnnonces: Flow<List<AnnonceEntity>> = surveyDao.getAllAnnonces()
    val allAnnonceFichiers: Flow<List<AnnonceFichierEntity>> = surveyDao.getAllAnnonceFichiers()
    val allAnnonceDestinataires: Flow<List<AnnonceDestinataireEntity>> = surveyDao.getAllAnnonceDestinataires()

    val allPersonnes: Flow<List<PersonneEntity>> = surveyDao.getAllPersonnesFlow()

    val allEnquetesAudit: Flow<List<SyncedAuditRecord>> = surveyDao.getAllEnquetesAudit()

    val allQuestionnaires: Flow<List<QuestionnaireEntity>> = surveyDao.getAllQuestionnaires()

    val defaultQuestionnaire: Flow<QuestionnaireEntity?> = surveyDao.getDefaultQuestionnaire()

    val defaultUser: Flow<UserEntity?> = surveyDao.getFirstUser()

    val allUsersFlow: Flow<List<UserEntity>> = surveyDao.getAllUsersFlow()

    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>> {
        return surveyDao.getNotificationsForUser(userId)
    }

    fun getUnreadNotificationsCount(userId: Long): Flow<Int> {
        return surveyDao.getUnreadNotificationsCount(userId)
    }

    suspend fun markNotificationAsRead(id: Long) {
        val luAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        surveyDao.markNotificationAsRead(id, luAt)
    }

    suspend fun markAllNotificationsAsRead(userId: Long) {
        val luAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        surveyDao.markAllNotificationsAsRead(userId, luAt)
    }

    suspend fun addNotification(notification: NotificationEntity): Long {
        return surveyDao.insertNotification(notification)
    }

    fun getQuestionsWithOptions(questionnaireId: Long): Flow<List<QuestionWithOptions>> {
        return surveyDao.getQuestionsWithOptions(questionnaireId)
    }

    suspend fun getAllQuestionsWithOptionsDirect(): List<QuestionWithOptions> {
        return surveyDao.getAllQuestionsWithOptionsDirect()
    }

    fun getReponsesWithQuestion(enqueteId: Long): Flow<List<ReponseWithQuestion>> {
        return surveyDao.getReponsesWithQuestion(enqueteId)
    }

    suspend fun checkCodeDuplicate(code: String): Boolean {
        return surveyDao.countByCode(code.trim()) > 0
    }

    suspend fun getEnqueteByCode(code: String): EnqueteEntity? {
        val clean = code.trim()
        if (clean.isBlank()) return null
        return surveyDao.getEnqueteByCode(clean)
    }

    suspend fun getEnqueteByCodeAndQuestionnaire(code: String, questionnaireId: Long): EnqueteEntity? {
        val clean = code.trim()
        if (clean.isBlank()) return null
        return surveyDao.getEnqueteByCodeAndQuestionnaire(clean, questionnaireId)
    }

    suspend fun getEnquetesByCode(code: String): List<EnqueteEntity> {
        val clean = code.trim()
        if (clean.isBlank()) return emptyList()
        return surveyDao.getEnquetesByCode(clean)
    }

    suspend fun getReponsesForEnquete(enqueteId: Long): List<ReponseEntity> {
        return surveyDao.getReponsesForEnquete(enqueteId)
    }

    suspend fun checkResponseDuplicate(questionId: Long, value: String): String? {
        if (value.isBlank()) return null
        return surveyDao.findExistingCodeByResponseValue(questionId, value.trim())
    }

    fun recordDuplicateBlocked() {
        duplicatesPreventedCounter++
    }

    fun recordValidationPassed() {
        validationsPassedCounter++
    }

    suspend fun saveUser(user: UserEntity) {
        val existing = (if (user.email.isNotBlank()) surveyDao.findUserByEmailOrPhone(user.email) else null)
            ?: (if (user.idUser > 0L) surveyDao.getUserById(user.idUser) else null)

        if (existing != null && user.idUser > 0L && existing.idUser != user.idUser) {
            try {
                surveyDao.deleteUserById(existing.idUser)
            } catch (_: Exception) {}
        }

        var result = user
        if (result.motDePasse.isNullOrBlank() && existing?.motDePasse?.isNotBlank() == true) {
            result = result.copy(motDePasse = existing.motDePasse)
        }
        if (result.photo.isNullOrBlank() && existing?.photo?.isNotBlank() == true) {
            result = result.copy(photo = existing.photo)
        }
        if (result.region.isBlank() && existing?.region?.isNotBlank() == true) {
            result = result.copy(region = existing.region)
        }
        if (result.matricule.isNullOrBlank() && existing?.matricule?.isNotBlank() == true) {
            result = result.copy(matricule = existing.matricule)
        }
        surveyDao.insertUser(result)
        if (result.email.isNotBlank()) {
            surveyDao.updateUserStatutByEmail(result.email, result.statut)
        }
        if (result.idUser > 0L) {
            surveyDao.updateUserStatut(result.idUser, result.statut)
        }
    }

    suspend fun insertUser(user: UserEntity): Long {
        return surveyDao.insertUser(user)
    }

    suspend fun updateUserStatut(userId: Long, statut: String): Int {
        return surveyDao.updateUserStatut(userId, statut)
    }

    suspend fun updateUserStatutByEmail(email: String, statut: String): Int {
        return surveyDao.updateUserStatutByEmail(email, statut)
    }

    suspend fun findUserById(id: Long): UserEntity? {
        return surveyDao.getUserById(id)
    }

    suspend fun getPersonneByCode(code: String): PersonneEntity? {
        val clean = code.trim()
        if (clean.isBlank()) return null
        return surveyDao.getPersonneByCode(clean)
    }

    suspend fun getAnsweredQuestionnairesByCode(code: String): List<QuestionnaireAnsweredSummary> {
        val clean = code.trim()
        if (clean.isBlank()) return emptyList()
        return surveyDao.getAnsweredQuestionnairesByCode(clean)
    }

    suspend fun getOrCreatePersonne(codeUnique: String): PersonneEntity {
        val clean = codeUnique.trim()
        val existing = surveyDao.getPersonneByCode(clean)
        if (existing != null) return existing
        val newPersonne = PersonneEntity(
            codeUnique = clean,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        val newId = surveyDao.insertPersonne(newPersonne)
        return newPersonne.copy(idPersonne = newId)
    }

    private fun normalizePhoneNumber(raw: String): String {
        return raw.replace(Regex("[^0-9+]"), "").replace("+261", "0").trim()
    }

    suspend fun checkPhoneDuplicate(phone: String, excludeCode: String? = null): com.example.data.model.ExistingPhoneDuplicateInfo? {
        val clean = phone.trim()
        val norm = normalizePhoneNumber(clean)
        if (norm.length < 8) return null
        val match = surveyDao.findExistingSurveyByPhone(clean, norm) ?: return null
        if (excludeCode != null && match.code_unique.equals(excludeCode.trim(), ignoreCase = true)) return null
        return match
    }

    /**
     * Scans existing Room database tables (`personnes`, `enquetes`, `reponses`) before saving
     * to detect potential duplicates based on critical fields (code_unique, numéro de téléphone, identifiants).
     */
    suspend fun checkForDuplicatesInRoom(
        codeUnique: String,
        questionnaireId: Long,
        responses: Map<Long, String> = emptyMap(),
        excludeEnqueteId: Long? = null
    ): com.example.data.model.LocalDuplicateCheckResult {
        val cleanCode = codeUnique.trim()
        if (cleanCode.isBlank()) {
            return com.example.data.model.LocalDuplicateCheckResult(hasDuplicate = false)
        }

        // 1. Scan Room for existing Personne and Enquete for the same questionnaire (Règle UNIQUE personne-questionnaire)
        val personne = surveyDao.getPersonneByCode(cleanCode)
        if (personne != null) {
            val existingEnquete = surveyDao.getEnqueteByPersonneAndQuestionnaire(personne.idPersonne, questionnaireId)
            if (existingEnquete != null && existingEnquete.idEnquete != excludeEnqueteId) {
                val qEntity = surveyDao.getQuestionnaireById(questionnaireId)
                val qTitle = qEntity?.titre ?: "ce questionnaire"
                return com.example.data.model.LocalDuplicateCheckResult(
                    hasDuplicate = true,
                    duplicateType = com.example.data.model.LocalDuplicateCheckResult.DuplicateType.PERSON_QUESTIONNAIRE_DUPLICATE,
                    duplicateMessage = "Doublon refusé : La personne '$cleanCode' a déjà répondu au questionnaire « $qTitle » (Enquête #${existingEnquete.idEnquete} du ${existingEnquete.dateEnquete}).",
                    conflictingField = "ID unique / Code personne",
                    conflictingValue = cleanCode,
                    conflictingCode = cleanCode,
                    conflictingDate = existingEnquete.dateEnquete,
                    conflictingEnqueteId = existingEnquete.idEnquete,
                    existingEnquete = existingEnquete,
                    existingPersonne = personne
                )
            }
        }

        // 2. Scan Room for critical fields like telephone number, CIN, passport, or unique identifier
        for ((qId, value) in responses) {
            val cleanVal = value.trim()
            if (cleanVal.isBlank()) continue

            val qEntity = surveyDao.getQuestionById(qId)
            val qLabel = qEntity?.libelle ?: ""
            val qLabelLower = qLabel.lowercase()
            val isPhoneField = qEntity?.type == "TELEPHONE" ||
                    qLabelLower.contains("téléphone") || qLabelLower.contains("telephone") ||
                    qLabelLower.contains("contact") || qLabelLower.contains("finday") ||
                    qLabelLower.contains("mobile")

            val isCinField = qLabelLower.contains("cin") || qLabelLower.contains("carte d'identité") || qLabelLower.contains("carte d'identite")
            val isPassportField = qLabelLower.contains("passeport") || qLabelLower.contains("passport")
            val isCriticalIdentifier = isCinField || isPassportField || qEntity?.estCodeUnique == true ||
                    qLabelLower.contains("identifiant") || qLabelLower.contains("nif") || qLabelLower.contains("stat")

            // A. Check phone duplicate
            if (isPhoneField || (cleanVal.length in 8..15 && cleanVal.replace("+", "").all { it.isDigit() })) {
                val normPhone = normalizePhoneNumber(cleanVal)
                if (normPhone.length >= 8) {
                    val phoneDup = surveyDao.findExistingSurveyByPhone(cleanVal, normPhone)
                    if (phoneDup != null && !phoneDup.code_unique.equals(cleanCode, ignoreCase = true)) {
                        val fieldName = phoneDup.question_libelle ?: qLabel.ifBlank { "Numéro de téléphone" }
                        return com.example.data.model.LocalDuplicateCheckResult(
                            hasDuplicate = true,
                            duplicateType = com.example.data.model.LocalDuplicateCheckResult.DuplicateType.PHONE_OR_IDENTIFIER_DUPLICATE,
                            duplicateMessage = "Doublon détecté sur le champ critique « $fieldName » : Le numéro « $cleanVal » est déjà associé à l'enquête #${phoneDup.id_enquete} du ${phoneDup.date_enquete ?: "récente"} (Code: ${phoneDup.code_unique}).",
                            conflictingField = fieldName,
                            conflictingValue = cleanVal,
                            conflictingCode = phoneDup.code_unique,
                            conflictingDate = phoneDup.date_enquete,
                            conflictingEnqueteId = phoneDup.id_enquete
                        )
                    }
                }
            }

            // B. Check CIN, Passport, or critical identifier duplicate
            if (isCriticalIdentifier) {
                val normVal = cleanVal.replace(" ", "").replace("-", "")
                val idDup = surveyDao.findExistingSurveyByCriticalValue(
                    questionId = qId,
                    value = cleanVal,
                    normalizedValue = normVal,
                    isCinQuestion = if (isCinField) 1 else 0,
                    isPassportQuestion = if (isPassportField) 1 else 0
                )
                if (idDup != null && !idDup.code_unique.equals(cleanCode, ignoreCase = true)) {
                    val fieldName = idDup.question_libelle ?: qLabel.ifBlank { "ID unique" }
                    return com.example.data.model.LocalDuplicateCheckResult(
                        hasDuplicate = true,
                        duplicateType = com.example.data.model.LocalDuplicateCheckResult.DuplicateType.PHONE_OR_IDENTIFIER_DUPLICATE,
                        duplicateMessage = "Doublon détecté sur le champ critique « $fieldName » : La valeur « $cleanVal » est déjà enregistrée dans l'enquête #${idDup.id_enquete} (Code: ${idDup.code_unique}).",
                        conflictingField = fieldName,
                        conflictingValue = cleanVal,
                        conflictingCode = idDup.code_unique,
                        conflictingDate = idDup.date_enquete,
                        conflictingEnqueteId = idDup.id_enquete
                    )
                }
            }

            // C. General identifier check for responses >= 8 chars
            if (cleanVal.length >= 8) {
                val existingCode = surveyDao.findExistingCodeByResponseValue(qId, cleanVal)
                if (existingCode != null && !existingCode.equals(cleanCode, ignoreCase = true)) {
                    val fieldName = qLabel.ifBlank { "Identifiant / Réponse unique" }
                    return com.example.data.model.LocalDuplicateCheckResult(
                        hasDuplicate = true,
                        duplicateType = com.example.data.model.LocalDuplicateCheckResult.DuplicateType.PHONE_OR_IDENTIFIER_DUPLICATE,
                        duplicateMessage = "Doublon détecté sur le champ « $fieldName » : La valeur « $cleanVal » est déjà associée au code « $existingCode ».",
                        conflictingField = fieldName,
                        conflictingValue = cleanVal,
                        conflictingCode = existingCode
                    )
                }
            }
        }

        return com.example.data.model.LocalDuplicateCheckResult(hasDuplicate = false)
    }

    suspend fun saveEnquete(
        codeUnique: String,
        questionnaireId: Long,
        enqueteurId: Long,
        dateEnquete: String,
        responses: Map<Long, String>, // questionId -> value
        selectedOptions: Map<Long, List<Long>>, // questionId -> list of optionIds
        questions: List<QuestionWithOptions>,
        allowUpdate: Boolean = false
    ): Result<Long> {
        val cleanCode = codeUnique.trim()
        if (cleanCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Le code unique de la personne est obligatoire."))
        }

        // Local mandatory validation check
        val missingQuestions = mutableListOf<String>()
        for (qItem in questions) {
            val q = qItem.question
            if (q.obligatoire) {
                if (q.estCodeUnique) {
                    val textVal = responses[q.idQuestion]?.trim() ?: cleanCode
                    if (textVal.isBlank()) {
                        missingQuestions.add(q.libelle)
                    }
                } else {
                    when (q.type) {
                        "CHOIX_UNIQUE" -> {
                            val opts = selectedOptions[q.idQuestion]
                            val textVal = responses[q.idQuestion]
                            if ((opts == null || opts.isEmpty()) && textVal.isNullOrBlank()) {
                                missingQuestions.add(q.libelle)
                            }
                        }
                        "CHOIX_MULTIPLE" -> {
                            val opts = selectedOptions[q.idQuestion]
                            if (opts == null || opts.isEmpty()) {
                                missingQuestions.add(q.libelle)
                            }
                        }
                        else -> {
                            val textVal = responses[q.idQuestion]
                            if (textVal.isNullOrBlank()) {
                                missingQuestions.add(q.libelle)
                            }
                        }
                    }
                }
            }
        }

        if (missingQuestions.isNotEmpty()) {
            return Result.failure(
                IllegalArgumentException("Contrôle de complétude échoué : ${missingQuestions.size} question(s) obligatoire(s) non renseignée(s) :\n• " + missingQuestions.joinToString("\n• "))
            )
        }

        // Scan Room database for duplicates prior to save
        if (!allowUpdate) {
            val duplicateCheck = checkForDuplicatesInRoom(
                codeUnique = cleanCode,
                questionnaireId = questionnaireId,
                responses = responses
            )
            if (duplicateCheck.hasDuplicate) {
                recordDuplicateBlocked()
                return Result.failure(
                    IllegalStateException(duplicateCheck.duplicateMessage ?: "Doublon détecté dans les données locales Room.")
                )
            }
        }

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        
        // Obtenir ou créer la personne correspondante (table personnes)
        val personne = getOrCreatePersonne(cleanCode)

        val existingForQuestionnaire = surveyDao.getEnqueteByPersonneAndQuestionnaire(personne.idPersonne, questionnaireId)

        if (existingForQuestionnaire != null && !allowUpdate) {
            recordDuplicateBlocked()
            val qEntity = surveyDao.getQuestionnaireById(questionnaireId)
            val qTitle = qEntity?.titre ?: "ce questionnaire"
            return Result.failure(
                IllegalStateException("Doublon refusé : La personne '$cleanCode' a déjà répondu au questionnaire « $qTitle ». Une personne ne peut répondre qu'une seule fois à un même questionnaire.")
            )
        }

        // Serialize responses to JSON for dual storage & durability
        val responsesJsonObject = org.json.JSONObject()
        for ((qId, textVal) in responses) {
            if (textVal.isNotBlank()) {
                responsesJsonObject.put(qId.toString(), textVal)
            }
        }
        for ((qId, optList) in selectedOptions) {
            if (optList.isNotEmpty()) {
                val qItem = questions.find { it.question.idQuestion == qId }
                val optLabels = optList.mapNotNull { oId ->
                    qItem?.options?.find { it.idOption == oId }?.libelle
                }.joinToString(", ")
                if (optLabels.isNotBlank()) {
                    responsesJsonObject.put(qId.toString(), optLabels)
                }
            }
        }
        val responsesJsonStr = responsesJsonObject.toString()

        val enqueteId = if (existingForQuestionnaire != null) {
            // Fiche existante pour ce questionnaire et cette personne : mise à jour
            val updated = existingForQuestionnaire.copy(
                idEnqueteur = enqueteurId,
                dateEnquete = dateEnquete,
                statut = "TERMINEE",
                syncStatus = "EN_ATTENTE",
                responsesJson = responsesJsonStr,
                updatedAt = currentTime
            )
            surveyDao.updateEnquete(updated)
            // Nettoyage des anciennes réponses pour cette enquête avant réinsertion
            surveyDao.deleteReponsesForEnquete(existingForQuestionnaire.idEnquete)
            existingForQuestionnaire.idEnquete
        } else {
            // Nouvelle enquête pour cette personne et ce questionnaire
            val enquete = EnqueteEntity(
                idPersonne = personne.idPersonne,
                idQuestionnaire = questionnaireId,
                idEnqueteur = enqueteurId,
                dateEnquete = dateEnquete,
                statut = "TERMINEE",
                syncStatus = "EN_ATTENTE",
                responsesJson = responsesJsonStr,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            surveyDao.insertEnquete(enquete)
        }

        // Insert responses
        val reponsesToInsert = mutableListOf<ReponseEntity>()

        for (qItem in questions) {
            val qId = qItem.question.idQuestion
            val rawResp = responses[qId]?.trim()
            when (qItem.question.type) {
                "CHOIX_UNIQUE" -> {
                    val optId = selectedOptions[qId]?.firstOrNull()
                    val optObj = qItem.options.firstOrNull { it.idOption == optId }

                    val finalValuer = if (optObj != null) {
                        if (isAutreOption(optObj.libelle)) {
                            if (!rawResp.isNullOrBlank()) {
                                rawResp
                            } else {
                                optObj.libelle
                            }
                        } else {
                            optObj.libelle
                        }
                    } else {
                        rawResp ?: ""
                    }

                    if (optId != null || finalValuer.isNotBlank()) {
                        reponsesToInsert.add(
                            ReponseEntity(
                                idEnquete = enqueteId,
                                idQuestion = qId,
                                idOption = optId,
                                valeur = finalValuer,
                                createdAt = currentTime
                            )
                        )
                    }
                }
                "CHOIX_MULTIPLE" -> {
                    val opts = selectedOptions[qId] ?: emptyList()
                    if (opts.isNotEmpty()) {
                        val selectedNames = opts.mapNotNull { oId ->
                            val optObj = qItem.options.firstOrNull { it.idOption == oId }
                            if (optObj != null) {
                                if (isAutreOption(optObj.libelle)) {
                                    if (!rawResp.isNullOrBlank() && rawResp.contains(":")) {
                                        rawResp
                                    } else if (!rawResp.isNullOrBlank() && !rawResp.equals(optObj.libelle, ignoreCase = true)) {
                                        "${optObj.libelle}: $rawResp"
                                    } else {
                                        optObj.libelle
                                    }
                                } else {
                                    optObj.libelle
                                }
                            } else null
                        }.joinToString(", ")

                        reponsesToInsert.add(
                            ReponseEntity(
                                idEnquete = enqueteId,
                                idQuestion = qId,
                                idOption = opts.firstOrNull(),
                                valeur = selectedNames.ifBlank { rawResp },
                                createdAt = currentTime
                            )
                        )
                    } else if (!rawResp.isNullOrBlank()) {
                        reponsesToInsert.add(
                            ReponseEntity(
                                idEnquete = enqueteId,
                                idQuestion = qId,
                                idOption = null,
                                valeur = rawResp,
                                createdAt = currentTime
                            )
                        )
                    }
                }
                else -> {
                    val textVal = if (qItem.question.estCodeUnique && responses[qId].isNullOrBlank()) {
                        cleanCode
                    } else {
                        responses[qId]
                    }
                    if (!textVal.isNullOrBlank()) {
                        reponsesToInsert.add(
                            ReponseEntity(
                                idEnquete = enqueteId,
                                idQuestion = qId,
                                idOption = null,
                                valeur = textVal,
                                createdAt = currentTime
                            )
                        )
                    }
                }
            }
        }

        if (reponsesToInsert.isNotEmpty()) {
            surveyDao.insertReponses(reponsesToInsert)
        }

        // Créer une notification locale
        try {
            surveyDao.insertNotification(
                NotificationEntity(
                    idUser = enqueteurId,
                    type = "NOUVELLE_ENQUETE",
                    titre = "Fiche enregistrée ($cleanCode)",
                    message = "Fiche d'enquête enregistrée localement avec succès.",
                    idEnquete = enqueteId,
                    idQuestionnaire = questionnaireId,
                    lu = false,
                    createdAt = currentTime
                )
            )
        } catch (_: Exception) {}

        recordValidationPassed()
        return Result.success(enqueteId)
    }

    /**
     * Direct alias for saving a Survey and its Responses offline with local validation.
     */
    suspend fun saveSurvey(
        codeUnique: String,
        questionnaireId: Long,
        enqueteurId: Long,
        dateEnquete: String,
        responses: Map<Long, String>,
        selectedOptions: Map<Long, List<Long>>,
        questions: List<QuestionWithOptions>,
        allowUpdate: Boolean = false
    ): Result<Long> = saveEnquete(
        codeUnique = codeUnique,
        questionnaireId = questionnaireId,
        enqueteurId = enqueteurId,
        dateEnquete = dateEnquete,
        responses = responses,
        selectedOptions = selectedOptions,
        questions = questions,
        allowUpdate = allowUpdate
    )

    /**
     * Performs local validation on Survey and Response data before offline insertion.
     */
    fun validateSurveyLocally(
        codeUnique: String,
        dateEnquete: String,
        questions: List<QuestionWithOptions>,
        textResponses: Map<Long, String>,
        selectedOptions: Map<Long, List<Long>>
    ): com.example.data.model.SurveyValidationResult = com.example.data.model.LocalSurveyValidator.validateSurveyData(
        codeUnique = codeUnique,
        dateEnquete = dateEnquete,
        questions = questions,
        textResponses = textResponses,
        selectedOptions = selectedOptions
    )

    fun getPendingSurveysFlow(): Flow<List<EnqueteEntity>> = surveyDao.getPendingSurveysFlow()

    suspend fun updateEnqueteSyncStatus(id: Long, syncStatus: String) {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        surveyDao.updateSyncStatus(id, syncStatus, currentTime)
    }

    suspend fun deleteEnquete(id: Long) {
        surveyDao.deleteReponsesForEnquete(id)
        surveyDao.deleteEnqueteById(id)
    }

    suspend fun deleteEnquetesByCode(code: String) {
        val clean = code.trim()
        if (clean.isNotBlank()) {
            val enquetes = surveyDao.getEnquetesByCode(clean)
            enquetes.forEach { surveyDao.deleteReponsesForEnquete(it.idEnquete) }
            surveyDao.deleteEnquetesByCode(clean)
            surveyDao.deletePersonneByCode(clean)
        }
    }

    suspend fun getEnqueteDetail(enqueteId: Long): EnqueteDetail? {
        val enquete = surveyDao.getEnqueteById(enqueteId) ?: return null
        val personne = surveyDao.getPersonneById(enquete.idPersonne)
        var reponses = surveyDao.getReponsesForEnquete(enqueteId)
        val allQuestionsWithOptions = surveyDao.getAllQuestionsWithOptionsDirect()

        if (reponses.isEmpty() && !enquete.responsesJson.isNullOrBlank()) {
            try {
                val jsonMap = org.json.JSONObject(enquete.responsesJson)
                val fallbackList = mutableListOf<ReponseEntity>()
                val keys = jsonMap.keys()
                while (keys.hasNext()) {
                    val keyStr = keys.next()
                    val qId = keyStr.toLongOrNull()
                    val valStr = jsonMap.optString(keyStr, "")
                    if (qId != null && valStr.isNotBlank()) {
                        fallbackList.add(
                            ReponseEntity(
                                idEnquete = enqueteId,
                                idQuestion = qId,
                                valeur = valStr
                            )
                        )
                    }
                }
                reponses = fallbackList
            } catch (_: Exception) {}
        }

        var reponsesWithQuestions = reponses.map { rep ->
            val qOpt = allQuestionsWithOptions.firstOrNull { it.question.idQuestion == rep.idQuestion }
            val qEntity = qOpt?.question ?: surveyDao.getQuestionById(rep.idQuestion) ?: QuestionEntity(
                idQuestion = rep.idQuestion,
                idQuestionnaire = enquete.idQuestionnaire,
                libelle = "Question #${rep.idQuestion}",
                type = "TEXTE"
            )
            var optEntity = qOpt?.options?.firstOrNull { it.idOption == rep.idOption }
                ?: rep.idOption?.let { surveyDao.getOptionById(it) }

            if (optEntity == null && !rep.valeur.isNullOrBlank()) {
                val optId = rep.valeur.trim().toLongOrNull()
                if (optId != null) {
                    optEntity = qOpt?.options?.firstOrNull { it.idOption == optId }
                        ?: surveyDao.getOptionById(optId)
                }
            }

            ReponseWithQuestion(
                reponse = rep,
                question = qEntity,
                option = optEntity
            )
        }

        val questionnaire = surveyDao.getQuestionnaireById(enquete.idQuestionnaire)
        val enqueteur = surveyDao.getUserById(enquete.idEnqueteur)
        val derivedRespondentName: String = reponsesWithQuestions.firstOrNull {
            val lbl = it.question.libelle.lowercase()
            lbl.contains("nom") || lbl.contains("anarana") || lbl.contains("prenom") || lbl.contains("chef") || lbl.contains("repondant")
        }?.let { it.reponse.valeur ?: it.option?.libelle }?.trim()?.takeIf { it.isNotBlank() }
            ?: (personne?.codeUnique ?: "Respondant")

        return EnqueteDetail(
            enquete = enquete,
            reponses = reponsesWithQuestions,
            codeUnique = personne?.codeUnique ?: "ENQ-$enqueteId",
            questionnaireTitre = questionnaire?.titre ?: "Questionnaire #${enquete.idQuestionnaire}",
            enqueteurNom = enqueteur?.let { "${it.prenom} ${it.nom}".trim() } ?: "Enquêteur #${enquete.idEnqueteur}",
            respondentNom = derivedRespondentName,
            enqueteurPhoto = enqueteur?.photo,
            enqueteurPhotoUrl = enqueteur?.photoUrl
        )
    }

    suspend fun getEnqueteById(id: Long): EnqueteEntity? {
        return surveyDao.getEnqueteById(id)
    }

    suspend fun getPersonneById(id: Long): PersonneEntity? {
        return surveyDao.getPersonneById(id)
    }

    fun getResearchMetrics(): Flow<ResearchMetrics> {
        return allEnquetes.combine(defaultQuestionnaire) { enquetes, _ ->
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
                duplicatesBlocked = duplicatesPreventedCounter,
                fieldValidationsPassed = validationsPassedCounter
            )
        }
    }

    suspend fun findUserByEmailOrPhone(identifier: String): UserEntity? {
        return surveyDao.findUserByEmailOrPhone(identifier.trim())
    }

    suspend fun getUserById(userId: Long): UserEntity? {
        return surveyDao.getUserById(userId)
    }

    suspend fun getFirstUserDirect(): UserEntity? {
        return surveyDao.getFirstUserDirect()
    }

    suspend fun saveUsers(users: List<UserEntity>) {
        val mergedUsers = users.map { newUser ->
            val existing = (if (newUser.email.isNotBlank()) surveyDao.findUserByEmailOrPhone(newUser.email) else null)
                ?: (if (newUser.idUser > 0L) surveyDao.getUserById(newUser.idUser) else null)

            if (existing != null && newUser.idUser > 0L && existing.idUser != newUser.idUser) {
                try {
                    surveyDao.deleteUserById(existing.idUser)
                } catch (_: Exception) {}
            }

            var result = newUser
            if (result.motDePasse.isNullOrBlank() && existing?.motDePasse?.isNotBlank() == true) {
                result = result.copy(motDePasse = existing.motDePasse)
            }
            if (result.photo.isNullOrBlank() && existing?.photo?.isNotBlank() == true) {
                result = result.copy(photo = existing.photo)
            }
            if (result.photoUrl.isNullOrBlank() && existing?.photoUrl?.isNotBlank() == true) {
                result = result.copy(photoUrl = existing.photoUrl)
            }
            result
        }
        surveyDao.insertUsers(mergedUsers)
        for (u in mergedUsers) {
            if (u.email.isNotBlank()) {
                surveyDao.updateUserStatutByEmail(u.email, u.statut)
            }
            if (u.idUser > 0L) {
                surveyDao.updateUserStatut(u.idUser, u.statut)
            }
        }
    }

    suspend fun updateUserPassword(userId: Long, newPassword: String) {
        val timestamp = System.currentTimeMillis().toString()
        surveyDao.updatePassword(userId, newPassword, timestamp)
    }

    suspend fun updateUserPhoto(userId: Long, photo: String?, photoUrl: String?) {
        val timestamp = System.currentTimeMillis().toString()
        surveyDao.updateUserPhoto(userId, photo, photoUrl, timestamp)
    }

    suspend fun markAllQuestionnairesAsDraft() {
        try {
            surveyDao.markAllQuestionnairesAsDraft()
        } catch (_: Exception) {}
    }

    suspend fun markQuestionnaireAsDraft(id: Long) {
        try {
            surveyDao.markQuestionnaireAsDraft(id)
        } catch (_: Exception) {}
    }

    suspend fun getAllQuestionnairesRaw(): List<QuestionnaireEntity> {
        return surveyDao.getAllQuestionnairesRaw()
    }

    suspend fun markQuestionnaireAsPublished(id: Long) {
        try {
            surveyDao.markQuestionnaireAsPublished(id)
        } catch (_: Exception) {}
    }

    suspend fun deleteQuestionnaireCompletely(id: Long) {
        try {
            val enquetes = surveyDao.getEnquetesByQuestionnaireId(id)
            if (enquetes.isNotEmpty()) {
                // If there are existing surveys locally, only mark as BROUILLON to never lose data!
                surveyDao.updateQuestionnaireStatut(id, "BROUILLON")
            } else {
                surveyDao.deleteOptionsByQuestionnaireId(id)
                surveyDao.deleteQuestionsByQuestionnaireId(id)
                surveyDao.deleteQuestionnaireById(id)
            }
        } catch (_: Exception) {
            try {
                surveyDao.updateQuestionnaireStatut(id, "BROUILLON")
            } catch (_: Exception) {}
        }
    }

    suspend fun purgeDraftQuestionnaire(id: Long) {
        markQuestionnaireAsDraft(id)
    }

    suspend fun countQuestionsByQuestionnaire(id: Long): Int {
        return surveyDao.countQuestionsByQuestionnaire(id)
    }

    suspend fun deleteDraftQuestionnaire(id: Long) {
        markQuestionnaireAsDraft(id)
    }

    suspend fun cleanDraftQuestionnaires() {
        try {
            surveyDao.markDraftQuestionnairesAsBrouillon()
        } catch (_: Exception) {}
    }

    suspend fun savePulledForms(
        questionnaires: List<QuestionnaireEntity>,
        questions: List<QuestionEntity>,
        options: List<OptionEntity>,
        isFullSync: Boolean = true
    ) {
        try {
            surveyDao.markDraftQuestionnairesAsBrouillon()
        } catch (_: Exception) {}

        val nonDraftQuestionnaires = questionnaires.filter {
            !com.example.data.sync.SyncService.isDraftStatus(it.statut) && com.example.data.sync.SyncService.isPublicStatus(it.statut)
        }
        val draftQuestionnaires = questionnaires.filter {
            com.example.data.sync.SyncService.isDraftStatus(it.statut) || !com.example.data.sync.SyncService.isPublicStatus(it.statut)
        }

        // Marquer les autres questionnaires comme BROUILLON lors d'une synchronisation globale complète (isFullSync)
        // Seuls les questionnaires avec statut PUBLIÉ issus de Supabase restent actifs
        if (isFullSync) {
            val publicIds = nonDraftQuestionnaires.map { it.idQuestionnaire }.distinct()
            try {
                if (publicIds.isNotEmpty()) {
                    surveyDao.markOtherQuestionnairesAsDraft(publicIds)
                } else {
                    surveyDao.markAllQuestionnairesAsDraft()
                }
            } catch (_: Exception) {}
        }

        // Sauvegarder dans SQLite (Room)
        try {
            if (nonDraftQuestionnaires.isNotEmpty()) {
                surveyDao.insertQuestionnaires(nonDraftQuestionnaires)
            }
            if (draftQuestionnaires.isNotEmpty()) {
                surveyDao.insertQuestionnaires(draftQuestionnaires.map { it.copy(statut = "BROUILLON") })
            }
        } catch (e: Exception) {
            for (q in questionnaires) {
                try {
                    surveyDao.insertQuestionnaire(q)
                } catch (_: Exception) {}
            }
        }

        if (questions.isNotEmpty()) {
            val fallbackQId = nonDraftQuestionnaires.firstOrNull()?.idQuestionnaire ?: 1L
            val safeQuestions = questions.map { q ->
                if (q.idQuestionnaire <= 0L) q.copy(idQuestionnaire = fallbackQId) else q
            }
            try {
                // Delete any old synthetic/dummy questions so only authentic remote questions exist
                surveyDao.deleteSyntheticQuestions()
                surveyDao.deleteOrphanOptions()
                // Upsert directly without deleting existing questions to prevent breaking reponses foreign keys
                surveyDao.insertQuestions(safeQuestions)
            } catch (e: Exception) {
                for (q in safeQuestions) {
                    try {
                        surveyDao.insertQuestion(q)
                    } catch (_: Exception) {}
                }
            }
        }

        if (options.isNotEmpty()) {
            try {
                surveyDao.insertOptions(options)
            } catch (e: Exception) {
                for (opt in options) {
                    try {
                        surveyDao.insertOption(opt)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    suspend fun savePulledAnnonces(
        annonces: List<AnnonceEntity>,
        fichiers: List<AnnonceFichierEntity> = emptyList(),
        destinataires: List<AnnonceDestinataireEntity> = emptyList()
    ) {
        try {
            // Fafana avy hatrany ireo annonces par defaut na seed data taloha
            surveyDao.deleteDefaultAnnonceFichiers()
            surveyDao.deleteDefaultAnnonceDestinataires()
            surveyDao.deleteDefaultAnnonces()

            if (annonces.isNotEmpty()) {
                val validIds = annonces.map { it.idAnnonce }
                surveyDao.deleteAnnonceFichiersNotIn(validIds)
                surveyDao.deleteAnnonceDestinatairesNotIn(validIds)
                surveyDao.deleteAnnoncesNotIn(validIds)
                surveyDao.insertAnnonces(annonces)
            } else {
                surveyDao.deleteAllAnnonceFichiers()
                surveyDao.deleteAllAnnonceDestinataires()
                surveyDao.deleteAllAnnonces()
            }
            if (fichiers.isNotEmpty()) {
                surveyDao.insertAnnonceFichiers(fichiers)
            }
            if (destinataires.isNotEmpty()) {
                surveyDao.insertAnnonceDestinataires(destinataires)
            }
        } catch (_: Exception) {}
    }

    suspend fun saveAnnonce(
        annonce: AnnonceEntity,
        fichiers: List<AnnonceFichierEntity> = emptyList(),
        destinataires: List<AnnonceDestinataireEntity> = emptyList()
    ) {
        try {
            surveyDao.insertAnnonce(annonce)
            if (fichiers.isNotEmpty()) {
                surveyDao.insertAnnonceFichiers(fichiers)
            }
            if (destinataires.isNotEmpty()) {
                surveyDao.insertAnnonceDestinataires(destinataires)
            }
        } catch (_: Exception) {}
    }

    suspend fun saveAnnonceFichier(fichier: AnnonceFichierEntity) {
        try {
            surveyDao.insertAnnonceFichier(fichier)
        } catch (_: Exception) {}
    }

    suspend fun saveAnnonceDestinataire(destinataire: AnnonceDestinataireEntity) {
        try {
            surveyDao.insertAnnonceDestinataire(destinataire)
        } catch (_: Exception) {}
    }

    suspend fun deleteAnnonce(idAnnonce: String) {
        try {
            surveyDao.deleteFichiersForAnnonce(idAnnonce)
            surveyDao.deleteDestinatairesForAnnonce(idAnnonce)
            surveyDao.deleteAnnonceById(idAnnonce)
        } catch (_: Exception) {}
    }

    suspend fun deleteAnnonceFichier(idFichier: String) {
        try {
            surveyDao.deleteAnnonceFichier(idFichier)
        } catch (_: Exception) {}
    }

    suspend fun getAnnonceById(idAnnonce: String): AnnonceEntity? =
        surveyDao.getAnnonceById(idAnnonce)

    suspend fun getFichiersForAnnonce(idAnnonce: String): List<AnnonceFichierEntity> =
        surveyDao.getFichiersForAnnonceList(idAnnonce)

    suspend fun getDestinatairesForAnnonce(idAnnonce: String): List<AnnonceDestinataireEntity> =
        surveyDao.getDestinatairesForAnnonceList(idAnnonce)

    suspend fun markAnnonceAsRead(idAnnonce: String, userId: Long) {
        try {
            val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.getDefault()).format(java.util.Date())
            val existing = surveyDao.getDestinataire(idAnnonce, userId)
            if (existing != null) {
                surveyDao.updateAnnonceLecture(idAnnonce, userId, "LU", nowIso)
            } else {
                surveyDao.insertAnnonceDestinataire(
                    AnnonceDestinataireEntity(
                        idAnnonce = idAnnonce,
                        idUser = userId,
                        dateEnvoi = nowIso,
                        dateLecture = nowIso,
                        statutLecture = "LU"
                    )
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun markOtherQuestionnairesAsDraft(publicIds: List<Long>) {
        if (publicIds.isEmpty()) return
        try {
            surveyDao.markOtherQuestionnairesAsDraft(publicIds)
        } catch (_: Exception) {}
    }

    suspend fun getAllUsers(): List<UserEntity> = surveyDao.getAllUsers()
    suspend fun getAllPersonnes(): List<PersonneEntity> = surveyDao.getAllPersonnes()
    suspend fun getAllQuestionnairesList(): List<QuestionnaireEntity> = surveyDao.getAllQuestionnairesList()
    suspend fun getAllQuestionsList(): List<QuestionEntity> = surveyDao.getAllQuestionsList()
    suspend fun getAllOptionsList(): List<OptionEntity> = surveyDao.getAllOptionsList()
    suspend fun getAllEnquetesList(): List<EnqueteEntity> = surveyDao.getAllEnquetesList()

    suspend fun savePulledEnquetes(
        personnes: List<PersonneEntity>,
        enquetes: List<EnqueteEntity>,
        reponses: List<ReponseEntity>
    ) {
        try {
            // 1. Ensure all personnes exist in personnes table
            if (personnes.isNotEmpty()) {
                surveyDao.insertPersonnes(personnes)
            }

            // 2. Ensure all enqueteurs exist in users table to satisfy foreign keys
            val existingUsers = surveyDao.getAllUsers().map { it.idUser }.toSet()
            val missingUserIds = enquetes.map { it.idEnqueteur }.filter { !existingUsers.contains(it) }.distinct()
            if (missingUserIds.isNotEmpty()) {
                val stubUsers = missingUserIds.map { uid ->
                    UserEntity(
                        idUser = uid,
                        nom = "Enquêteur",
                        prenom = "N°$uid",
                        email = "enqueteur$uid@collecte.mg",
                        role = "ENQUETEUR",
                        statut = "ACTIF"
                    )
                }
                surveyDao.insertUsers(stubUsers)
            }

            // 3. Ensure questionnaires exist for foreign keys
            val existingQIds = surveyDao.getAllQuestionnairesList().map { it.idQuestionnaire }.toSet()
            val fallbackQId = existingQIds.firstOrNull() ?: 1L

            // Ensure personnes exist for foreign keys
            val existingPersonneIds = surveyDao.getAllPersonnes().map { it.idPersonne }.toSet()

            val enquetesToInsert = enquetes.filter { existingPersonneIds.contains(it.idPersonne) }.map { enq ->
                val safeQId = if (existingQIds.contains(enq.idQuestionnaire)) enq.idQuestionnaire else fallbackQId
                enq.copy(idQuestionnaire = safeQId)
            }

            // Insert / update enquetes in SQLite
            if (enquetesToInsert.isNotEmpty()) {
                surveyDao.insertEnquetes(enquetesToInsert)
            }

            // 4. Insert responses safely
            if (reponses.isNotEmpty()) {
                val refreshedEnquetes = surveyDao.getAllEnquetesList().associateBy { it.idEnquete }
                val existingQuesIds = surveyDao.getAllQuestionsList().map { it.idQuestion }.toSet()
                val existingOptIds = surveyDao.getAllOptionsList().map { it.idOption }.toSet()

                val validReponses = reponses.filter { rep ->
                    refreshedEnquetes.containsKey(rep.idEnquete) && existingQuesIds.contains(rep.idQuestion)
                }.map { rep ->
                    if (rep.idOption != null && !existingOptIds.contains(rep.idOption)) {
                        rep.copy(idOption = null)
                    } else {
                        rep
                    }
                }

                if (validReponses.isNotEmpty()) {
                    surveyDao.insertReponses(validReponses)
                }
            }

            // 5. Reconcile deleted enquetes from Supabase:
            // Any local enquete that was SYNCHRONISEE but no longer exists on Supabase was deleted remotely!
            // We safely delete it from SQLite without touching local pending EN_ATTENTE enquetes.
            val remoteEnqueteIds = enquetes.map { it.idEnquete }
            if (remoteEnqueteIds.isNotEmpty()) {
                try {
                    surveyDao.reconcileRemoteDeletedEnquetes(remoteEnqueteIds)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            // Fallback: insert individually if batch encounters an issue
            for (enq in enquetes) {
                try {
                    surveyDao.insertEnquetes(listOf(enq))
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun reconcileRemoteDeletedEnquetes(remoteIds: List<Long>) {
        try {
            surveyDao.reconcileRemoteDeletedEnquetes(remoteIds)
        } catch (_: Exception) {}
    }

    suspend fun insertQuestionnaire(q: QuestionnaireEntity) = surveyDao.insertQuestionnaire(q)
    suspend fun insertQuestionnaires(list: List<QuestionnaireEntity>) = surveyDao.insertQuestionnaires(list)
    suspend fun insertQuestion(q: QuestionEntity) = surveyDao.insertQuestion(q)
    suspend fun insertQuestions(questions: List<QuestionEntity>) = surveyDao.insertQuestions(questions)
    suspend fun insertOption(o: OptionEntity) = surveyDao.insertOption(o)
    suspend fun insertOptions(options: List<OptionEntity>) = surveyDao.insertOptions(options)
    suspend fun getQuestionnaireById(id: Long) = surveyDao.getQuestionnaireById(id)
    suspend fun insertEnquete(e: EnqueteEntity) = surveyDao.insertEnquete(e)
    suspend fun insertReponse(r: ReponseEntity) = surveyDao.insertReponse(r)
    suspend fun insertPersonne(p: PersonneEntity) = surveyDao.insertPersonne(p)
    suspend fun deleteQuestionById(id: Long) = surveyDao.deleteQuestionById(id)
    suspend fun deleteOptionById(id: Long) = surveyDao.deleteOptionById(id)
    suspend fun deleteReponseById(id: Long) = surveyDao.deleteReponseById(id)
    suspend fun deletePersonneByCode(code: String) = surveyDao.deletePersonneByCode(code)

    suspend fun clearDefaultDummyData() {
        try {
            // Keep clean state without deleting actual user data
        } catch (_: Exception) {}
    }

    suspend fun seedInitialDataIfEmpty() {
        if (surveyDao.countUsers() == 0) {
            surveyDao.insertUsers(
                listOf(
                    UserEntity(
                        idUser = 1,
                        nom = "RAHARINIRINA",
                        prenom = "Juvance",
                        email = "juvanceraharinirina@gmail.com",
                        telephone = "0345812888",
                        motDePasse = "261103",
                        role = "ADMIN",
                        statut = "ACTIF",
                        region = "Analamanga",
                        matricule = "ADM-001",
                        photo = com.example.data.util.RealUserPhotos.USER_1_PHOTO,
                        photoUrl = com.example.data.util.RealUserPhotos.USER_1_PHOTO
                    ),
                    UserEntity(
                        idUser = 2,
                        nom = "RAKOTO",
                        prenom = "Jean",
                        email = "jean.rakoto@wecollect.mg",
                        telephone = "0341111111",
                        motDePasse = "Enqueteur@123",
                        role = "ENQUETEUR",
                        statut = "ACTIF",
                        region = "Analamanga",
                        matricule = "ENQ-002",
                        photo = com.example.data.util.RealUserPhotos.USER_2_PHOTO,
                        photoUrl = com.example.data.util.RealUserPhotos.USER_2_PHOTO
                    ),
                    UserEntity(
                        idUser = 3,
                        nom = "RABE",
                        prenom = "Paul",
                        email = "paulrabe@gmail.com",
                        telephone = "0342222222",
                        motDePasse = "261103",
                        role = "ENQUETEUR",
                        statut = "ACTIF",
                        region = "Vakinankaratra",
                        matricule = "ENQ-003",
                        photo = com.example.data.util.RealUserPhotos.USER_3_PHOTO,
                        photoUrl = com.example.data.util.RealUserPhotos.USER_3_PHOTO
                    ),
                    UserEntity(
                        idUser = 4,
                        nom = "RASOLO",
                        prenom = "Marie",
                        email = "marie@gmail.com",
                        telephone = "0343333333",
                        motDePasse = "261103",
                        role = "ENQUETEUR",
                        statut = "ACTIF",
                        region = "Atsinanana",
                        matricule = "ENQ-004",
                        photo = com.example.data.util.RealUserPhotos.USER_4_PHOTO,
                        photoUrl = com.example.data.util.RealUserPhotos.USER_4_PHOTO
                    )
                )
            )
        } else {
            // Backfill : Manamarina sy mampiditra ny sary tena izy, ny région sy matricule
            val currentUsers = surveyDao.getAllUsers()
            for (u in currentUsers) {
                if (u.photo.isNullOrBlank() || u.photoUrl.isNullOrBlank()) {
                    val realP = when (u.idUser) {
                        2L -> com.example.data.util.RealUserPhotos.USER_2_PHOTO
                        3L -> com.example.data.util.RealUserPhotos.USER_3_PHOTO
                        4L -> com.example.data.util.RealUserPhotos.USER_4_PHOTO
                        else -> com.example.data.util.RealUserPhotos.USER_1_PHOTO
                    }
                    surveyDao.updateUserPhoto(u.idUser, realP, realP)
                }
                if (u.region.isBlank() || u.matricule.isNullOrBlank()) {
                    val assignedRegion = if (u.region.isNotBlank()) u.region else when (u.idUser) {
                        3L -> "Vakinankaratra"
                        4L -> "Atsinanana"
                        else -> "Analamanga"
                    }
                    val assignedMatricule = if (!u.matricule.isNullOrBlank()) u.matricule else when (u.idUser) {
                        1L -> "ADM-001"
                        2L -> "ENQ-002"
                        3L -> "ENQ-003"
                        4L -> "ENQ-004"
                        else -> "ENQ-%03d".format(u.idUser)
                    }
                    surveyDao.updateUserRegionAndMatricule(u.idUser, assignedRegion, assignedMatricule)
                }
            }
        }

        if (surveyDao.countPersonnes() == 0) {
            surveyDao.insertPersonnes(
                listOf(
                    PersonneEntity(
                        idPersonne = 1,
                        codeUnique = "PERS-001"
                    ),
                    PersonneEntity(
                        idPersonne = 2,
                        codeUnique = "PERS-002"
                    ),
                    PersonneEntity(
                        idPersonne = 3,
                        codeUnique = "PERS-003"
                    )
                )
            )
        }

        // Raha mbola tsy misy questionnaire voatahiry dia ampidirina ny questionnaire fototra "Enquête Terrain"
        if (surveyDao.countQuestionnaires() == 0) {
            seedDefaultQuestionnaireAndQuestions()
        }

        // Raha mbola tsy misy annonce dia ampidirina ireo annonce ofisialy avy amin'ny Supabase
        if (surveyDao.countAnnonces() == 0) {
            seedDefaultAnnonces()
        }
    }

    suspend fun ensureDefaultAnnoncesExist() {
        if (surveyDao.countAnnonces() == 0) {
            seedDefaultAnnonces()
        }
    }

    suspend fun ensureDefaultSurveyExists() {
        if (surveyDao.countQuestionnaires() == 0) {
            seedDefaultQuestionnaireAndQuestions()
        }
    }

    suspend fun seedDefaultQuestionnaireAndQuestions() {
        try {
            val q = QuestionnaireEntity(
                idQuestionnaire = 12L,
                titre = "Enquête Terrain",
                description = "Collecte des informations sur les personnes",
                version = "1.0",
                statut = "PUBLIÉ",
                createdBy = 1L
            )
            surveyDao.insertQuestionnaire(q)

            val questions = listOf(
                QuestionEntity(62L, 12L, "Nom", "TEXTE", obligatoire = true, ordre = 1),
                QuestionEntity(63L, 12L, "Prénom(s)", "TEXTE", obligatoire = true, ordre = 2),
                QuestionEntity(65L, 12L, "CIN : Numéro de Carte d’Identité Nationale", "TEXTE", obligatoire = false, ordre = 4),
                QuestionEntity(66L, 12L, "Sexe : Homme / Femme", "CHOIX_UNIQUE", obligatoire = true, ordre = 5),
                QuestionEntity(67L, 12L, "Âge", "NOMBRE", obligatoire = false, ordre = 6),
                QuestionEntity(68L, 12L, "Date de naissance", "DATE", obligatoire = true, ordre = 7),
                QuestionEntity(69L, 12L, "Téléphone", "TEXTE", obligatoire = true, ordre = 8),
                QuestionEntity(70L, 12L, "Situation matrimoniale : Célibataire / Marié(e) / Divorcé(e) / Veuf(ve)", "CHOIX_UNIQUE", obligatoire = false, ordre = 9),
                QuestionEntity(71L, 12L, "Nombre d’enfants", "NOMBRE", obligatoire = false, ordre = 10),
                QuestionEntity(72L, 12L, "Niveau d’instruction : Aucun / Primaire / Collège / Lycée / Universitaire", "CHOIX_UNIQUE", obligatoire = false, ordre = 11),
                QuestionEntity(73L, 12L, "Profession / Activité principale", "TEXTE", obligatoire = false, ordre = 12),
                QuestionEntity(74L, 12L, "Statut professionnel : Salarié / Indépendant / Agriculteur / Commerçant / Sans emploi / Autre", "CHOIX_UNIQUE", obligatoire = false, ordre = 13),
                QuestionEntity(75L, 12L, "Région", "CHOIX_UNIQUE", obligatoire = true, ordre = 14),
                QuestionEntity(76L, 12L, "District", "TEXTE", obligatoire = false, ordre = 15),
                QuestionEntity(77L, 12L, "Commune", "TEXTE", obligatoire = false, ordre = 16),
                QuestionEntity(78L, 12L, "Fokontany", "TEXTE", obligatoire = false, ordre = 17),
                QuestionEntity(79L, 12L, "Village / Quartier", "TEXTE", obligatoire = false, ordre = 18),
                QuestionEntity(80L, 12L, "Nombre de personnes dans le ménage", "NOMBRE", obligatoire = false, ordre = 19),
                QuestionEntity(81L, 12L, "Chef de ménage : Oui / Non", "CHOIX_UNIQUE", obligatoire = false, ordre = 20),
                QuestionEntity(82L, 12L, "Lien avec le chef de ménage : Lui-même / Époux(se) / Enfant / Parent / Autre", "CHOIX_UNIQUE", obligatoire = false, ordre = 21),
                QuestionEntity(89L, 12L, "Photo [PHOTO]", "TEXTE", obligatoire = false, ordre = 22)
            )
            surveyDao.insertQuestions(questions)

            val options = listOf(
                OptionEntity(54L, 66L, "Homme", 1),
                OptionEntity(55L, 66L, "Femme", 2),
                OptionEntity(56L, 70L, "Célibataire", 1),
                OptionEntity(57L, 70L, "Marié(e)", 2),
                OptionEntity(58L, 70L, "Divorcé(e)", 3),
                OptionEntity(59L, 70L, "Veuf(ve)", 4),
                OptionEntity(60L, 72L, "Aucun", 1),
                OptionEntity(61L, 72L, "Primaire", 2),
                OptionEntity(62L, 72L, "Collège", 3),
                OptionEntity(63L, 72L, "Lycée", 4),
                OptionEntity(64L, 72L, "Universitaire", 5),
                OptionEntity(65L, 74L, "Salarié", 1),
                OptionEntity(66L, 74L, "Indépendant", 2),
                OptionEntity(67L, 74L, "Agriculteur", 3),
                OptionEntity(68L, 74L, "Commerçant", 4),
                OptionEntity(69L, 74L, "Sans emploi", 5),
                OptionEntity(70L, 74L, "Autre", 6),
                OptionEntity(71L, 75L, "Analamanga", 1),
                OptionEntity(72L, 75L, "Atsimo-Andrefana", 2),
                OptionEntity(73L, 75L, "Atsinanana", 3),
                OptionEntity(74L, 75L, "Vakinankaratra", 4),
                OptionEntity(75L, 75L, "Boeny", 5),
                OptionEntity(76L, 75L, "Menabe", 6),
                OptionEntity(77L, 75L, "Diana", 7),
                OptionEntity(78L, 75L, "Autre", 8),
                OptionEntity(79L, 81L, "Oui", 1),
                OptionEntity(80L, 81L, "Non", 2),
                OptionEntity(81L, 82L, "Lui-même", 1),
                OptionEntity(82L, 82L, "Époux(se)", 2),
                OptionEntity(83L, 82L, "Enfant", 3),
                OptionEntity(84L, 82L, "Parent", 4),
                OptionEntity(85L, 82L, "Autre", 5)
            )
            surveyDao.insertOptions(options)
        } catch (_: Exception) {}
    }

    suspend fun seedDefaultAnnonces() {
        try {
            val annonces = listOf(
                AnnonceEntity(
                    idAnnonce = "c34de56f-78ab-4901-bcde-f12345678901",
                    titre = "Planning des enquêtes de terrain - Semaine 42",
                    contenu = "Le planning détaillé des missions de collecte dans les régions Analamanga, Menabe et Atsimo-Andrefana est disponible en pièce jointe. Veuillez le consulter avant vos déplacements.",
                    typeAnnonce = "MIXTE",
                    priorite = "NORMALE",
                    statut = "PUBLIEE",
                    cibleType = "TOUS",
                    datePublication = "2026-10-17T19:55:47.009+00:00",
                    createdBy = 1L,
                    authorName = "RAHARINIRINA Juvance",
                    authorRole = "ADMIN"
                ),
                AnnonceEntity(
                    idAnnonce = "c293a1e6-1af9-4e81-a0e7-13ee436d7469",
                    titre = "Rentre de pour tous des ecolo",
                    contenu = "izyb",
                    typeAnnonce = "MIXTE",
                    priorite = "IMPORTANTE",
                    statut = "PUBLIEE",
                    cibleType = "TOUS",
                    datePublication = "2026-10-15T08:07:53.853+00:00",
                    createdBy = 1L,
                    authorName = "RAHARINIRINA Juvance",
                    authorRole = "ADMIN"
                ),
                AnnonceEntity(
                    idAnnonce = "13e04e4a-9a0b-436f-8b4c-f44f7182ea96",
                    titre = "ssssss",
                    contenu = "",
                    typeAnnonce = "FICHIER",
                    priorite = "NORMALE",
                    statut = "PUBLIEE",
                    cibleType = "TOUS",
                    datePublication = "2026-10-19T05:24:30.309+00:00",
                    createdBy = 1L,
                    authorName = "RAHARINIRINA Juvance",
                    authorRole = "ADMIN"
                )
            )
            surveyDao.insertAnnonces(annonces)

            val fichiers = listOf(
                AnnonceFichierEntity(
                    idFichier = "cc882ea5-9753-4d89-b062-6d55b56b3ec4",
                    idAnnonce = "c34de56f-78ab-4901-bcde-f12345678901",
                    nomFichier = "Planning_Missions_Terrain_S42.pdf",
                    typeMime = "application/pdf",
                    tailleOctets = 239155L
                ),
                AnnonceFichierEntity(
                    idFichier = "36e532fe-847f-45ee-9c50-ccf20a8295af",
                    idAnnonce = "c293a1e6-1af9-4e81-a0e7-13ee436d7469",
                    nomFichier = "tampo.jpg",
                    typeMime = "image/jpeg",
                    tailleOctets = 345665L
                ),
                AnnonceFichierEntity(
                    idFichier = "13f06820-f681-479c-ba60-9231c47dcc02",
                    idAnnonce = "13e04e4a-9a0b-436f-8b4c-f44f7182ea96",
                    nomFichier = "006.png",
                    typeMime = "image/png",
                    tailleOctets = 1542310L
                )
            )
            surveyDao.insertAnnonceFichiers(fichiers)
        } catch (_: Exception) {}
    }

    private fun isAutreOption(libelle: String?): Boolean {
        if (libelle.isNullOrBlank()) return false
        val clean = libelle.trim().lowercase()
        return clean == "autre" || clean == "autres" || clean == "hafa" || clean == "other" ||
                clean.startsWith("autre") || clean.startsWith("hafa") ||
                clean.contains("préciser") || clean.contains("preciser") ||
                clean.contains("à préciser") || clean.contains("a preciser")
    }
}


