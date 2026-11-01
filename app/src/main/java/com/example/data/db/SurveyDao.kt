package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {

    // --- Users ---
    @Query("SELECT * FROM users LIMIT 1")
    fun getFirstUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getFirstUserDirect(): UserEntity?

    @Query("SELECT * FROM users WHERE id_user = :userId")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("""
        SELECT * FROM users 
        WHERE (
            LOWER(email) = LOWER(:identifier) 
            OR telephone = :identifier 
            OR LOWER(matricule) = LOWER(:identifier)
            OR LOWER(nom) = LOWER(:identifier) 
            OR LOWER(prenom) = LOWER(:identifier)
            OR LOWER(nom || ' ' || prenom) = LOWER(:identifier)
            OR LOWER(prenom || ' ' || nom) = LOWER(:identifier)
            OR LOWER(email) LIKE '%' || LOWER(:identifier) || '%'
            OR telephone LIKE '%' || :identifier || '%'
            OR LOWER(matricule) LIKE '%' || LOWER(:identifier) || '%'
        ) 
        LIMIT 1
    """)
    suspend fun findUserByEmailOrPhone(identifier: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Query("UPDATE users SET mot_de_passe = :newPassword, updated_at = :updatedAt WHERE id_user = :userId")
    suspend fun updatePassword(userId: Long, newPassword: String, updatedAt: String = System.currentTimeMillis().toString()): Int

    @Query("UPDATE users SET photo = :photo, photo_url = :photoUrl, updated_at = :updatedAt WHERE id_user = :userId")
    suspend fun updateUserPhoto(userId: Long, photo: String?, photoUrl: String?, updatedAt: String = System.currentTimeMillis().toString()): Int

    @Query("UPDATE users SET region = :region, matricule = :matricule, updated_at = :updatedAt WHERE id_user = :userId")
    suspend fun updateUserRegionAndMatricule(userId: Long, region: String, matricule: String?, updatedAt: String = System.currentTimeMillis().toString()): Int

    @Query("UPDATE users SET statut = :statut, updated_at = :updatedAt WHERE id_user = :userId")
    suspend fun updateUserStatut(userId: Long, statut: String, updatedAt: String = System.currentTimeMillis().toString()): Int

    @Query("UPDATE users SET statut = :statut, updated_at = :updatedAt WHERE LOWER(email) = LOWER(:email)")
    suspend fun updateUserStatutByEmail(email: String, statut: String, updatedAt: String = System.currentTimeMillis().toString()): Int

    @Query("DELETE FROM users WHERE id_user = :userId")
    suspend fun deleteUserById(userId: Long): Int

    @Update
    suspend fun updateUser(user: UserEntity): Int

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY nom ASC, prenom ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    // --- Personnes ---
    @Query("SELECT * FROM personnes WHERE id_personne = :id LIMIT 1")
    suspend fun getPersonneById(id: Long): PersonneEntity?

    @Query("SELECT * FROM personnes WHERE code_unique = :code LIMIT 1")
    suspend fun getPersonneByCode(code: String): PersonneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonne(personne: PersonneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonnes(personnes: List<PersonneEntity>)

    @Query("SELECT * FROM personnes ORDER BY id_personne DESC")
    suspend fun getAllPersonnes(): List<PersonneEntity>

    @Query("SELECT * FROM personnes ORDER BY id_personne DESC")
    fun getAllPersonnesFlow(): Flow<List<PersonneEntity>>

    @Query("SELECT COUNT(*) FROM personnes")
    suspend fun countPersonnes(): Int

    @Query("SELECT COUNT(*) FROM personnes WHERE code_unique = :code")
    suspend fun countPersonnesByCode(code: String): Int

    @Query("DELETE FROM personnes WHERE code_unique = :code")
    suspend fun deletePersonneByCode(code: String)

    // --- Questionnaires & Questions ---
    @Query("SELECT * FROM questionnaires ORDER BY id_questionnaire ASC LIMIT 1")
    fun getDefaultQuestionnaire(): Flow<QuestionnaireEntity?>

    @Query("SELECT * FROM questionnaires WHERE id_questionnaire = :id LIMIT 1")
    suspend fun getQuestionnaireById(id: Long): QuestionnaireEntity?

    @Query("""
        SELECT * FROM questionnaires 
        WHERE statut IS NULL 
           OR (
               statut != 'BROUILLON' 
               AND LOWER(TRIM(statut)) NOT LIKE '%brouill%' 
               AND LOWER(TRIM(statut)) NOT LIKE '%draft%'
           )
        ORDER BY id_questionnaire ASC
    """)
    fun getAllQuestionnaires(): Flow<List<QuestionnaireEntity>>

    @Query("SELECT * FROM questionnaires ORDER BY id_questionnaire ASC")
    suspend fun getAllQuestionnairesList(): List<QuestionnaireEntity>

    @Query("SELECT * FROM questionnaires ORDER BY id_questionnaire ASC")
    suspend fun getAllQuestionnairesRaw(): List<QuestionnaireEntity>

    @Query("""
        UPDATE questionnaires 
        SET statut = 'BROUILLON'
        WHERE statut IS NOT NULL 
          AND (
              LOWER(TRIM(statut)) LIKE '%brouill%'
              OR LOWER(TRIM(statut)) LIKE '%draft%'
              OR LOWER(TRIM(statut)) LIKE '%inact%'
              OR LOWER(TRIM(statut)) LIKE '%non_publi%'
              OR LOWER(TRIM(statut)) LIKE '%archiv%'
              OR LOWER(TRIM(statut)) LIKE '%supprim%'
          )
    """)
    suspend fun markDraftQuestionnairesAsBrouillon()

    @Query("""
        DELETE FROM questionnaires 
        WHERE statut IS NOT NULL 
          AND (
              LOWER(TRIM(statut)) LIKE '%brouill%'
              OR LOWER(TRIM(statut)) LIKE '%draft%'
              OR LOWER(TRIM(statut)) LIKE '%inact%'
              OR LOWER(TRIM(statut)) LIKE '%non_publi%'
              OR LOWER(TRIM(statut)) LIKE '%archiv%'
              OR LOWER(TRIM(statut)) LIKE '%supprim%'
          )
    """)
    suspend fun deleteDraftQuestionnaires()

    @Query("SELECT COUNT(*) FROM questionnaires")
    suspend fun countQuestionnaires(): Int

    @Query("UPDATE questionnaires SET statut = :newStatut WHERE id_questionnaire = :id")
    suspend fun updateQuestionnaireStatut(id: Long, newStatut: String)

    @Query("UPDATE questionnaires SET statut = 'PUBLIÉ' WHERE id_questionnaire = :id")
    suspend fun markQuestionnaireAsPublished(id: Long)

    @Query("UPDATE questionnaires SET statut = 'BROUILLON' WHERE id_questionnaire = :id")
    suspend fun markQuestionnaireAsDraft(id: Long)

    @Query("UPDATE questionnaires SET statut = 'BROUILLON' WHERE id_questionnaire NOT IN (:ids)")
    suspend fun markOtherQuestionnairesAsDraft(ids: List<Long>)

    @Query("UPDATE questionnaires SET statut = 'BROUILLON'")
    suspend fun markAllQuestionnairesAsDraft()

    @Query("DELETE FROM questionnaires WHERE id_questionnaire = :id")
    suspend fun deleteQuestionnaireById(id: Long)

    @Query("DELETE FROM questions WHERE id_question = :id")
    suspend fun deleteQuestionById(id: Long)

    @Query("DELETE FROM options WHERE id_option = :id")
    suspend fun deleteOptionById(id: Long)

    @Query("DELETE FROM questions WHERE id_questionnaire = :id")
    suspend fun deleteQuestionsByQuestionnaireId(id: Long)

    @Query("DELETE FROM options WHERE id_question IN (SELECT id_question FROM questions WHERE id_questionnaire = :id)")
    suspend fun deleteOptionsByQuestionnaireId(id: Long)

    @Query("DELETE FROM questionnaires WHERE id_questionnaire NOT IN (:ids)")
    suspend fun deleteQuestionnairesNotIn(ids: List<Long>)

    @Query("DELETE FROM questions WHERE id_questionnaire NOT IN (:ids)")
    suspend fun deleteQuestionsNotInQuestionnaires(ids: List<Long>)

    @Query("DELETE FROM options WHERE id_question NOT IN (SELECT id_question FROM questions)")
    suspend fun deleteOrphanOptions()

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestionsList(): List<QuestionEntity>

    @Query("SELECT * FROM options")
    suspend fun getAllOptionsList(): List<OptionEntity>

    @Query("SELECT * FROM enquetes")
    suspend fun getAllEnquetesList(): List<EnqueteEntity>

    @Transaction
    @Query("SELECT * FROM questions WHERE id_questionnaire = :questionnaireId ORDER BY ordre ASC")
    fun getQuestionsWithOptions(questionnaireId: Long): Flow<List<QuestionWithOptions>>

    @Transaction
    @Query("SELECT * FROM questions WHERE id_questionnaire = :questionnaireId ORDER BY ordre ASC")
    suspend fun getQuestionsWithOptionsDirect(questionnaireId: Long): List<QuestionWithOptions>

    @Transaction
    @Query("SELECT * FROM questions ORDER BY ordre ASC")
    suspend fun getAllQuestionsWithOptionsDirect(): List<QuestionWithOptions>

    @Query("SELECT * FROM questions WHERE id_question = :questionId LIMIT 1")
    suspend fun getQuestionById(questionId: Long): QuestionEntity?

    @Query("SELECT * FROM options WHERE id_option = :optionId LIMIT 1")
    suspend fun getOptionById(optionId: Long): OptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionnaire(questionnaire: QuestionnaireEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestionnaires(questionnaires: List<QuestionnaireEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT COUNT(*) FROM questions WHERE id_questionnaire = :questionnaireId")
    suspend fun countQuestionsByQuestionnaire(questionnaireId: Long): Int

    @Query("SELECT COUNT(*) FROM questions WHERE id_questionnaire = :questionnaireId AND type = :type")
    suspend fun countQuestionsByType(questionnaireId: Long, type: String): Int

    @Query("SELECT MAX(ordre) FROM questions WHERE id_questionnaire = :questionnaireId")
    suspend fun getMaxQuestionOrder(questionnaireId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOption(option: OptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptions(options: List<OptionEntity>)

    @Query("""
        SELECT * FROM questionnaires 
        WHERE (statut IS NOT NULL AND (
            LOWER(TRIM(statut)) IN ('publie', 'publié', 'public', 'actif', 'active', 'validé', 'valide')
            OR (
                LOWER(TRIM(statut)) NOT IN ('brouillon', 'draft', 'en brouillon', 'en_brouillon', 'archive', 'archivé', 'supprime', 'supprimé', 'inactif', 'inactive', 'non publie', 'non publié', 'non-publie', 'non-publié', 'désactivé', 'desactive', 'cloture', 'clôturé', 'ferme', 'fermé', 'attente')
                AND LOWER(TRIM(statut)) NOT LIKE '%brouill%'
                AND LOWER(TRIM(statut)) NOT LIKE '%draft%'
                AND LOWER(TRIM(statut)) NOT LIKE '%inact%'
                AND LOWER(TRIM(statut)) NOT LIKE '%non%'
                AND LOWER(TRIM(statut)) NOT LIKE '%desactiv%'
                AND LOWER(TRIM(statut)) NOT LIKE '%désactiv%'
                AND LOWER(TRIM(statut)) NOT LIKE '%archiv%'
                AND LOWER(TRIM(statut)) NOT LIKE '%supprim%'
                AND LOWER(TRIM(statut)) NOT LIKE '%attente%'
                AND LOWER(TRIM(statut)) NOT LIKE '%ferm%'
                AND LOWER(TRIM(statut)) NOT LIKE '%clos%'
                AND LOWER(TRIM(statut)) NOT LIKE '%clotur%'
                AND LOWER(TRIM(statut)) NOT LIKE '%clôtur%'
            )
        ))
        ORDER BY id_questionnaire ASC 
        LIMIT 1
    """)
    suspend fun getLatestQuestionnaireSync(): QuestionnaireEntity?

    @Query("UPDATE enquetes SET id_questionnaire = :newQuestionnaireId")
    suspend fun updateAllEnquetesQuestionnaireId(newQuestionnaireId: Long)

    // --- Enquetes ---
    @Query("SELECT * FROM enquetes WHERE id_questionnaire = :questionnaireId")
    suspend fun getEnquetesByQuestionnaireId(questionnaireId: Long): List<EnqueteEntity>

    @Query("SELECT * FROM enquetes ORDER BY id_enquete DESC")
    fun getAllEnquetes(): Flow<List<EnqueteEntity>>

    @Query("""
        SELECT 
            e.id_enquete AS idEnquete,
            p.code_unique AS codeUnique,
            e.id_questionnaire AS idQuestionnaire,
            e.id_enqueteur AS idEnqueteur,
            e.date_enquete AS dateEnquete,
            e.statut AS statut,
            e.sync_status AS syncStatus,
            e.created_at AS createdAt,
            e.updated_at AS updatedAt,
            COALESCE(
                (
                    SELECT GROUP_CONCAT(r.valeur, ' ')
                    FROM reponses r
                    LEFT JOIN questions q ON r.id_question = q.id_question
                    WHERE r.id_enquete = e.id_enquete
                      AND (
                          LOWER(COALESCE(q.libelle, '')) LIKE '%nom%' 
                          OR LOWER(COALESCE(q.libelle, '')) LIKE '%prenom%' 
                          OR LOWER(COALESCE(q.libelle, '')) LIKE '%prénom%'
                          OR LOWER(COALESCE(q.libelle, '')) LIKE '%anarana%'
                          OR LOWER(COALESCE(q.libelle, '')) LIKE '%repondant%'
                          OR LOWER(COALESCE(q.libelle, '')) LIKE '%répondant%'
                      )
                ),
                (
                    SELECT r.valeur
                    FROM reponses r
                    WHERE r.id_enquete = e.id_enquete AND r.valeur IS NOT NULL AND TRIM(r.valeur) != ''
                    ORDER BY r.id_reponse ASC
                    LIMIT 1
                ),
                p.code_unique
            ) AS nomComplet,
            (
                SELECT u.prenom || ' ' || u.nom
                FROM users u
                WHERE u.id_user = e.id_enqueteur
            ) AS enqueteurNom,
            (
                SELECT qn.titre
                FROM questionnaires qn
                WHERE qn.id_questionnaire = e.id_questionnaire
            ) AS questionnaireTitre,
            (
                SELECT r.valeur
                FROM reponses r
                LEFT JOIN questions q ON r.id_question = q.id_question
                WHERE r.id_enquete = e.id_enquete
                  AND (LOWER(COALESCE(q.libelle, '')) LIKE '%region%' OR LOWER(COALESCE(q.libelle, '')) LIKE '%région%' OR LOWER(COALESCE(q.libelle, '')) LIKE '%faritra%')
                LIMIT 1
            ) AS region,
            (
                SELECT r.valeur
                FROM reponses r
                LEFT JOIN questions q ON r.id_question = q.id_question
                WHERE r.id_enquete = e.id_enquete
                  AND (LOWER(COALESCE(q.libelle, '')) LIKE '%commune%' OR LOWER(COALESCE(q.libelle, '')) LIKE '%kaominina%')
                LIMIT 1
            ) AS commune
        FROM enquetes e
        LEFT JOIN personnes p ON e.id_personne = p.id_personne
        ORDER BY e.updated_at DESC, e.id_enquete DESC
    """)
    fun getAllEnquetesAudit(): Flow<List<SyncedAuditRecord>>

    @Query("SELECT * FROM enquetes WHERE id_enquete = :id")
    suspend fun getEnqueteById(id: Long): EnqueteEntity?

    @Query("""
        SELECT e.* FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        WHERE p.code_unique = :code 
        LIMIT 1
    """)
    suspend fun getEnqueteByCode(code: String): EnqueteEntity?

    @Query("""
        SELECT e.* FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        WHERE p.code_unique = :code AND e.id_questionnaire = :questionnaireId 
        LIMIT 1
    """)
    suspend fun getEnqueteByCodeAndQuestionnaire(code: String, questionnaireId: Long): EnqueteEntity?

    @Query("SELECT * FROM enquetes WHERE id_personne = :personneId AND id_questionnaire = :questionnaireId LIMIT 1")
    suspend fun getEnqueteByPersonneAndQuestionnaire(personneId: Long, questionnaireId: Long): EnqueteEntity?

    @Query("""
        SELECT e.* FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        WHERE p.code_unique = :code 
        ORDER BY e.id_questionnaire ASC, e.id_enquete ASC
    """)
    suspend fun getEnquetesByCode(code: String): List<EnqueteEntity>

    @Query("""
        SELECT e.* FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        WHERE p.code_unique = :code 
        ORDER BY e.id_questionnaire ASC, e.id_enquete ASC
    """)
    fun getEnquetesByCodeFlow(code: String): Flow<List<EnqueteEntity>>

    @Query("SELECT * FROM enquetes WHERE id_personne = :personneId ORDER BY id_questionnaire ASC, id_enquete ASC")
    suspend fun getEnquetesByPersonneId(personneId: Long): List<EnqueteEntity>

    @Query("""
        SELECT COUNT(DISTINCT e.id_questionnaire) FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        WHERE p.code_unique = :code
    """)
    suspend fun countQuestionnairesForCode(code: String): Int

    @Query("""
        SELECT 
            e.id_questionnaire AS idQuestionnaire,
            q.titre AS titre,
            e.date_enquete AS dateEnquete,
            e.id_enquete AS idEnquete,
            e.sync_status AS syncStatus
        FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        INNER JOIN questionnaires q ON e.id_questionnaire = q.id_questionnaire
        WHERE p.code_unique = :code
        ORDER BY e.date_enquete DESC, e.id_enquete DESC
    """)
    suspend fun getAnsweredQuestionnairesByCode(code: String): List<QuestionnaireAnsweredSummary>

    @Query("""
        SELECT 
            e.id_questionnaire AS idQuestionnaire,
            q.titre AS titre,
            e.date_enquete AS dateEnquete,
            e.id_enquete AS idEnquete,
            e.sync_status AS syncStatus
        FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        INNER JOIN questionnaires q ON e.id_questionnaire = q.id_questionnaire
        WHERE p.code_unique = :code
        ORDER BY e.date_enquete DESC, e.id_enquete DESC
    """)
    fun getAnsweredQuestionnairesByCodeFlow(code: String): Flow<List<QuestionnaireAnsweredSummary>>

    @Query("""
        DELETE FROM enquetes 
        WHERE id_personne IN (SELECT id_personne FROM personnes WHERE code_unique = :code)
    """)
    suspend fun deleteEnquetesByCode(code: String)

    @Query("DELETE FROM questionnaires WHERE LOWER(titre) LIKE '%ménage%' OR LOWER(titre) LIKE '%menage%'")
    suspend fun deleteLegacyTemplateQuestionnaires()

    @Query("SELECT COUNT(*) FROM personnes WHERE code_unique = :code")
    suspend fun countByCode(code: String): Int

    @Query("SELECT COUNT(*) FROM enquetes")
    suspend fun countEnquetes(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnquete(enquete: EnqueteEntity): Long

    @Update
    suspend fun updateEnquete(enquete: EnqueteEntity)

    @Query("UPDATE enquetes SET sync_status = :syncStatus, updated_at = :updatedAt WHERE id_enquete = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String, updatedAt: String)

    @Delete
    suspend fun deleteEnquete(enquete: EnqueteEntity)

    @Query("DELETE FROM enquetes WHERE id_enquete = :id")
    suspend fun deleteEnqueteById(id: Long)

    @Query("DELETE FROM reponses WHERE id_enquete IN (SELECT id_enquete FROM enquetes WHERE sync_status = 'SYNCHRONISEE' AND id_enquete NOT IN (:remoteIds))")
    suspend fun deleteSyncedReponsesNotIn(remoteIds: List<Long>)

    @Query("DELETE FROM enquetes WHERE sync_status = 'SYNCHRONISEE' AND id_enquete NOT IN (:remoteIds)")
    suspend fun deleteSyncedEnquetesNotIn(remoteIds: List<Long>)

    @Transaction
    suspend fun reconcileRemoteDeletedEnquetes(remoteIds: List<Long>) {
        if (remoteIds.isNotEmpty()) {
            deleteSyncedReponsesNotIn(remoteIds)
            deleteSyncedEnquetesNotIn(remoteIds)
        }
    }

    @Query("SELECT * FROM enquetes WHERE sync_status = 'EN_ATTENTE' ORDER BY id_enquete DESC")
    fun getPendingSurveysFlow(): Flow<List<EnqueteEntity>>

    @Query("SELECT COUNT(*) FROM enquetes WHERE sync_status = 'EN_ATTENTE'")
    suspend fun countPendingSurveys(): Int

    // --- Reponses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReponse(reponse: ReponseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReponses(reponses: List<ReponseEntity>)

    @Query("SELECT * FROM reponses WHERE id_enquete = :enqueteId")
    suspend fun getReponsesForEnquete(enqueteId: Long): List<ReponseEntity>

    @Transaction
    @Query("SELECT * FROM reponses WHERE id_enquete = :enqueteId")
    fun getReponsesWithQuestion(enqueteId: Long): Flow<List<ReponseWithQuestion>>

    @Query("DELETE FROM reponses WHERE id_enquete = :enqueteId")
    suspend fun deleteReponsesForEnquete(enqueteId: Long)

    @Query("DELETE FROM reponses WHERE id_reponse = :id")
    suspend fun deleteReponseById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnquetes(enquetes: List<EnqueteEntity>)

    // --- Notifications ---
    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id_user = :userId ORDER BY created_at DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE id_user = :userId AND lu = 0")
    fun getUnreadNotificationsCount(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET lu = 1, lu_at = :luAt WHERE id_notification = :id")
    suspend fun markNotificationAsRead(id: Long, luAt: String = System.currentTimeMillis().toString())

    @Query("UPDATE notifications SET lu = 1, lu_at = :luAt WHERE id_user = :userId AND lu = 0")
    suspend fun markAllNotificationsAsRead(userId: Long, luAt: String = System.currentTimeMillis().toString())

    @Query("DELETE FROM notifications WHERE id_notification = :id")
    suspend fun deleteNotificationById(id: Long)

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotificationsList(): List<NotificationEntity>

    // --- Fanadiovana ireo données par défaut / seed data teo aloha ---
    @Query("""
        DELETE FROM reponses 
        WHERE id_enquete IN (
            SELECT e.id_enquete FROM enquetes e
            INNER JOIN personnes p ON e.id_personne = p.id_personne
            WHERE p.code_unique = 'DUMMY-SAMPLE-2026'
        ) 
        OR valeur LIKE '%RAZAFY%' 
        OR valeur LIKE '%Mamy%'
    """)
    suspend fun deleteDummyReponses()

    @Query("""
        DELETE FROM enquetes 
        WHERE id_personne IN (SELECT id_personne FROM personnes WHERE code_unique = 'DUMMY-SAMPLE-2026')
    """)
    suspend fun deleteDummyEnquetes()

    @Query("DELETE FROM personnes WHERE code_unique = 'DUMMY-SAMPLE-2026'")
    suspend fun deleteDummyPersonnes()

    @Query("""
        DELETE FROM options 
        WHERE id_question IN (
            SELECT id_question FROM questions 
            WHERE id_questionnaire IN (
                SELECT id_questionnaire FROM questionnaires 
                WHERE titre = '__TEMP_DUMMY_DELETE_ONLY__'
            )
        )
    """)
    suspend fun deleteDummyOptions()

    @Query("""
        DELETE FROM questions 
        WHERE id_questionnaire IN (
            SELECT id_questionnaire FROM questionnaires 
            WHERE titre = '__TEMP_DUMMY_DELETE_ONLY__'
        )
    """)
    suspend fun deleteDummyQuestions()

    @Query("DELETE FROM questionnaires WHERE titre = '__TEMP_DUMMY_DELETE_ONLY__'")
    suspend fun deleteDummyQuestionnaires()

    // --- Duplicate and Correlation Checks ---
    @Query("""
        SELECT DISTINCT p.code_unique FROM enquetes e
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        INNER JOIN reponses r ON e.id_enquete = r.id_enquete
        WHERE r.id_question = :questionId AND LOWER(TRIM(r.valeur)) = LOWER(TRIM(:value))
        LIMIT 1
    """)
    suspend fun findExistingCodeByResponseValue(questionId: Long, value: String): String?

    @Query("""
        SELECT e.id_enquete, p.code_unique, e.date_enquete, q.titre as questionnaire_titre, qu.libelle as question_libelle, r.valeur
        FROM reponses r
        INNER JOIN enquetes e ON r.id_enquete = e.id_enquete
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        INNER JOIN questions qu ON r.id_question = qu.id_question
        LEFT JOIN questionnaires q ON e.id_questionnaire = q.id_questionnaire
        WHERE (qu.type = 'TELEPHONE' OR LOWER(qu.libelle) LIKE '%téléphone%' OR LOWER(qu.libelle) LIKE '%telephone%' OR LOWER(qu.libelle) LIKE '%finday%' OR LOWER(qu.libelle) LIKE '%contact%')
        AND (
            LOWER(TRIM(r.valeur)) = LOWER(TRIM(:rawValue))
            OR REPLACE(REPLACE(REPLACE(REPLACE(LOWER(TRIM(r.valeur)), ' ', ''), '-', ''), '.', ''), '+261', '0') = :normalizedValue
        )
        LIMIT 1
    """)
    suspend fun findExistingSurveyByPhone(rawValue: String, normalizedValue: String): com.example.data.model.ExistingPhoneDuplicateInfo?

    @Query("""
        SELECT e.id_enquete, p.code_unique, e.date_enquete, q.titre as questionnaire_titre, qu.libelle as question_libelle, r.valeur
        FROM reponses r
        INNER JOIN enquetes e ON r.id_enquete = e.id_enquete
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        INNER JOIN questions qu ON r.id_question = qu.id_question
        LEFT JOIN questionnaires q ON e.id_questionnaire = q.id_questionnaire
        WHERE r.id_question = :questionId
        AND (
            LOWER(TRIM(r.valeur)) = LOWER(TRIM(:rawValue))
            OR REPLACE(REPLACE(REPLACE(LOWER(TRIM(r.valeur)), ' ', ''), '-', ''), '.', '') = :normalizedValue
        )
        LIMIT 1
    """)
    suspend fun findExistingSurveyByQuestionResponse(questionId: Long, rawValue: String, normalizedValue: String): com.example.data.model.ExistingPhoneDuplicateInfo?

    @Query("""
        SELECT e.id_enquete, p.code_unique, e.date_enquete, q.titre as questionnaire_titre, qu.libelle as question_libelle, r.valeur
        FROM reponses r
        INNER JOIN enquetes e ON r.id_enquete = e.id_enquete
        INNER JOIN personnes p ON e.id_personne = p.id_personne
        INNER JOIN questions qu ON r.id_question = qu.id_question
        LEFT JOIN questionnaires q ON e.id_questionnaire = q.id_questionnaire
        WHERE (
            r.id_question = :questionId
            OR (LOWER(qu.libelle) LIKE '%cin%' AND :isCinQuestion = 1)
            OR (LOWER(qu.libelle) LIKE '%passeport%' AND :isPassportQuestion = 1)
        )
        AND (
            LOWER(TRIM(r.valeur)) = LOWER(TRIM(:value))
            OR REPLACE(REPLACE(REPLACE(LOWER(TRIM(r.valeur)), ' ', ''), '-', ''), '.', '') = :normalizedValue
        )
        LIMIT 1
    """)
    suspend fun findExistingSurveyByCriticalValue(
        questionId: Long,
        value: String,
        normalizedValue: String,
        isCinQuestion: Int,
        isPassportQuestion: Int
    ): com.example.data.model.ExistingPhoneDuplicateInfo?

    @Query("""
        DELETE FROM questions 
        WHERE (id_question >= 100 AND id_question <= 999 AND (
            libelle = 'Code unique de la personne' 
            OR libelle = 'Nom de famille' 
            OR (libelle = 'Prénom(s)' AND id_question % 10 = 3)
            OR libelle = 'Genre / Sexe' 
            OR libelle = 'Tranche d''âge' 
            OR libelle = 'Observation / Remarques de terrain'
        ))
    """)
    suspend fun deleteSyntheticQuestions()

    // --- Annonces ---
    @Query("SELECT * FROM annonces ORDER BY created_at DESC")
    fun getAllAnnonces(): Flow<List<AnnonceEntity>>

    @Query("SELECT * FROM annonces ORDER BY created_at DESC")
    suspend fun getAllAnnoncesList(): List<AnnonceEntity>

    @Query("SELECT * FROM annonces WHERE id_annonce = :idAnnonce LIMIT 1")
    suspend fun getAnnonceById(idAnnonce: String): AnnonceEntity?

    @Query("SELECT COUNT(*) FROM annonces")
    suspend fun countAnnonces(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnonces(annonces: List<AnnonceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnonce(annonce: AnnonceEntity)

    @Query("DELETE FROM annonces WHERE id_annonce = :idAnnonce")
    suspend fun deleteAnnonceById(idAnnonce: String): Int

    @Query("DELETE FROM annonces WHERE id_annonce NOT IN (:ids)")
    suspend fun deleteAnnoncesNotIn(ids: List<String>): Int

    @Query("DELETE FROM annonces WHERE id_annonce LIKE 'ann-default%'")
    suspend fun deleteDefaultAnnonces(): Int

    @Query("DELETE FROM annonces")
    suspend fun deleteAllAnnonces()

    // --- Annonce Fichiers ---
    @Query("SELECT * FROM annonce_fichiers WHERE id_annonce = :idAnnonce")
    fun getFichiersForAnnonce(idAnnonce: String): Flow<List<AnnonceFichierEntity>>

    @Query("SELECT * FROM annonce_fichiers WHERE id_annonce = :idAnnonce")
    suspend fun getFichiersForAnnonceList(idAnnonce: String): List<AnnonceFichierEntity>

    @Query("SELECT * FROM annonce_fichiers")
    fun getAllAnnonceFichiers(): Flow<List<AnnonceFichierEntity>>

    @Query("SELECT * FROM annonce_fichiers")
    suspend fun getAllAnnonceFichiersList(): List<AnnonceFichierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnonceFichiers(fichiers: List<AnnonceFichierEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnonceFichier(fichier: AnnonceFichierEntity)

    @Query("DELETE FROM annonce_fichiers WHERE id_fichier = :idFichier")
    suspend fun deleteAnnonceFichier(idFichier: String): Int

    @Query("DELETE FROM annonce_fichiers WHERE id_annonce = :idAnnonce")
    suspend fun deleteFichiersForAnnonce(idAnnonce: String): Int

    @Query("DELETE FROM annonce_fichiers WHERE id_annonce NOT IN (:ids)")
    suspend fun deleteAnnonceFichiersNotIn(ids: List<String>): Int

    @Query("DELETE FROM annonce_fichiers WHERE id_annonce LIKE 'ann-default%'")
    suspend fun deleteDefaultAnnonceFichiers(): Int

    @Query("DELETE FROM annonce_fichiers")
    suspend fun deleteAllAnnonceFichiers()

    // --- Annonce Destinataires ---
    @Query("SELECT * FROM annonce_destinataires WHERE id_annonce = :idAnnonce")
    fun getDestinatairesForAnnonce(idAnnonce: String): Flow<List<AnnonceDestinataireEntity>>

    @Query("SELECT * FROM annonce_destinataires WHERE id_annonce = :idAnnonce")
    suspend fun getDestinatairesForAnnonceList(idAnnonce: String): List<AnnonceDestinataireEntity>

    @Query("SELECT * FROM annonce_destinataires")
    fun getAllAnnonceDestinataires(): Flow<List<AnnonceDestinataireEntity>>

    @Query("SELECT * FROM annonce_destinataires")
    suspend fun getAllAnnonceDestinatairesList(): List<AnnonceDestinataireEntity>

    @Query("SELECT * FROM annonce_destinataires WHERE id_annonce = :idAnnonce AND id_user = :userId LIMIT 1")
    suspend fun getDestinataire(idAnnonce: String, userId: Long): AnnonceDestinataireEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnonceDestinataires(destinataires: List<AnnonceDestinataireEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnonceDestinataire(destinataire: AnnonceDestinataireEntity)

    @Query("UPDATE annonce_destinataires SET statut_lecture = :statut, date_lecture = :dateLecture WHERE id_annonce = :idAnnonce AND id_user = :userId")
    suspend fun updateAnnonceLecture(idAnnonce: String, userId: Long, statut: String = "LU", dateLecture: String): Int

    @Query("DELETE FROM annonce_destinataires WHERE id_annonce = :idAnnonce")
    suspend fun deleteDestinatairesForAnnonce(idAnnonce: String): Int

    @Query("DELETE FROM annonce_destinataires WHERE id_annonce NOT IN (:ids)")
    suspend fun deleteAnnonceDestinatairesNotIn(ids: List<String>): Int

    @Query("DELETE FROM annonce_destinataires WHERE id_annonce LIKE 'ann-default%'")
    suspend fun deleteDefaultAnnonceDestinataires(): Int

    @Query("DELETE FROM annonce_destinataires")
    suspend fun deleteAllAnnonceDestinataires()
}


