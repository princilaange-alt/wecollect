package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Standard typealiases for Survey and Response entities to support English & French naming
 * consistently throughout Room database offline storage and local validation logic.
 */
typealias Survey = EnqueteEntity
typealias SurveyEntity = EnqueteEntity
typealias Response = ReponseEntity
typealias ResponseEntity = ReponseEntity
typealias SurveyDetail = EnqueteDetail

data class QuestionWithOptions(
    @Embedded val question: QuestionEntity,
    @Relation(
        parentColumn = "id_question",
        entityColumn = "id_question"
    )
    val options: List<OptionEntity> = emptyList()
)

data class ReponseWithQuestion(
    @Embedded val reponse: ReponseEntity,
    @Relation(
        parentColumn = "id_question",
        entityColumn = "id_question"
    )
    val question: QuestionEntity,
    @Relation(
        parentColumn = "id_option",
        entityColumn = "id_option"
    )
    val option: OptionEntity? = null
)

data class EnqueteDetail(
    val enquete: EnqueteEntity,
    val reponses: List<ReponseWithQuestion>,
    val codeUnique: String = "",
    val questionnaireTitre: String = "",
    val enqueteurNom: String = "",
    val respondentNom: String = "",
    val enqueteurPhoto: String? = null,
    val enqueteurPhotoUrl: String? = null
)

data class SyncResult(
    val success: Boolean,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ResearchMetrics(
    val totalEnquetes: Int = 0,
    val completedEnquetes: Int = 0,
    val syncedEnquetes: Int = 0,
    val pendingEnquetes: Int = 0,
    val errorEnquetes: Int = 0,
    val completenessRate: Float = 0f,
    val duplicatesBlocked: Int = 0,
    val fieldValidationsPassed: Int = 0
)

data class SyncedAuditRecord(
    val idEnquete: Long,
    val codeUnique: String,
    val idQuestionnaire: Long,
    val idEnqueteur: Long,
    val dateEnquete: String,
    val statut: String,
    val syncStatus: String,
    val createdAt: String,
    val updatedAt: String,
    val nomComplet: String? = null,
    val enqueteurNom: String? = null,
    val questionnaireTitre: String? = null,
    val region: String? = null,
    val commune: String? = null
)

data class QuestionnaireAnsweredSummary(
    val idQuestionnaire: Long,
    val titre: String,
    val dateEnquete: String,
    val idEnquete: Long,
    val syncStatus: String
)

data class QuestionnaireAvailability(
    val questionnaire: QuestionnaireEntity,
    val isAnswered: Boolean,
    val dateAnswered: String? = null,
    val idEnquete: Long? = null,
    val syncStatus: String? = null
)

/**
 * Result of local duplicate scanning in Room database prior to saving a survey.
 */
data class LocalDuplicateCheckResult(
    val hasDuplicate: Boolean,
    val duplicateType: DuplicateType = DuplicateType.NONE,
    val duplicateMessage: String? = null,
    val conflictingField: String? = null,
    val conflictingValue: String? = null,
    val conflictingCode: String? = null,
    val conflictingDate: String? = null,
    val conflictingEnqueteId: Long? = null,
    val existingEnquete: EnqueteEntity? = null,
    val existingPersonne: PersonneEntity? = null
) {
    enum class DuplicateType {
        NONE,
        PERSON_QUESTIONNAIRE_DUPLICATE,
        PHONE_OR_IDENTIFIER_DUPLICATE,
        EXACT_SURVEY_DUPLICATE
    }
}

data class ExistingPhoneDuplicateInfo(
    val id_enquete: Long,
    val code_unique: String,
    val date_enquete: String?,
    val questionnaire_titre: String?,
    val question_libelle: String?,
    val valeur: String?
)

/**
 * Result of local validation prior to saving survey & responses into Room offline database.
 */
data class SurveyValidationResult(
    val isValid: Boolean,
    val completenessPercent: Int,
    val answeredCount: Int,
    val totalMandatoryCount: Int,
    val missingMandatoryQuestions: List<String> = emptyList(),
    val validationErrors: List<String> = emptyList()
)

/**
 * Validator supporting local validation of Survey and Response data prior to offline Room persistence.
 */
object LocalSurveyValidator {

    fun validateSurveyData(
        codeUnique: String,
        dateEnquete: String,
        questions: List<QuestionWithOptions>,
        textResponses: Map<Long, String>,
        selectedOptions: Map<Long, List<Long>>
    ): SurveyValidationResult {
        val errors = mutableListOf<String>()
        val missingMandatory = mutableListOf<String>()

        val cleanCode = codeUnique.trim()
        if (cleanCode.isBlank()) {
            errors.add("Le code unique de la personne est obligatoire.")
        }

        val cleanDate = dateEnquete.trim()
        if (cleanDate.isBlank()) {
            errors.add("La date de l'enquête est obligatoire.")
        } else if (!cleanDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            errors.add("Format de date invalide. Format attendu : AAAA-MM-JJ.")
        }

        var mandatoryCount = 0
        var answeredMandatoryCount = 0

        for (item in questions) {
            val q = item.question
            val isMandatory = q.obligatoire
            if (isMandatory) mandatoryCount++

            val isAnswered = when {
                q.estCodeUnique -> {
                    cleanCode.isNotBlank() || !textResponses[q.idQuestion].isNullOrBlank()
                }
                q.type == "CHOIX_UNIQUE" -> {
                    val opts = selectedOptions[q.idQuestion]
                    val txt = textResponses[q.idQuestion]
                    (!opts.isNullOrEmpty()) || !txt.isNullOrBlank()
                }
                q.type == "CHOIX_MULTIPLE" -> {
                    val opts = selectedOptions[q.idQuestion]
                    !opts.isNullOrEmpty()
                }
                q.type == "NOMBRE" -> {
                    val txt = textResponses[q.idQuestion]?.trim()
                    if (!txt.isNullOrBlank()) {
                        val num = txt.toDoubleOrNull()
                        if (num == null) {
                            errors.add("La question « ${q.libelle} » requiert un nombre valide.")
                            false
                        } else true
                    } else false
                }
                else -> {
                    val txt = textResponses[q.idQuestion]?.trim()
                    !txt.isNullOrBlank()
                }
            }

            if (isAnswered) {
                if (isMandatory) answeredMandatoryCount++
            } else if (isMandatory) {
                missingMandatory.add(q.libelle)
            }
        }

        val percent = if (mandatoryCount == 0) 100 else ((answeredMandatoryCount * 100) / mandatoryCount)

        if (missingMandatory.isNotEmpty()) {
            errors.add("${missingMandatory.size} question(s) obligatoire(s) non renseignée(s) : ${missingMandatory.joinToString(", ")}")
        }

        return SurveyValidationResult(
            isValid = errors.isEmpty(),
            completenessPercent = percent,
            answeredCount = answeredMandatoryCount,
            totalMandatoryCount = mandatoryCount,
            missingMandatoryQuestions = missingMandatory,
            validationErrors = errors
        )
    }
}

data class DetailedAnnonce(
    val annonce: AnnonceEntity,
    val fichiers: List<AnnonceFichierEntity> = emptyList(),
    val isRead: Boolean = false,
    val timeAgo: String = "Récemment",
    val primaryFileName: String = if (annonce.typeAnnonce == "FICHIER") "document_joint.pdf" else "annonce.txt"
)


