package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.SurveyDao
import com.example.data.model.*
import com.example.data.repository.SurveyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseOfflineValidationTest {

    private lateinit var db: AppDatabase
    private lateinit var surveyDao: SurveyDao
    private lateinit var repository: SurveyRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        surveyDao = db.surveyDao()
        repository = SurveyRepository(surveyDao)

        // Base user required for questionnaire created_by foreign key
        runBlocking {
            surveyDao.insertUser(
                UserEntity(
                    idUser = 1L,
                    nom = "Rabe",
                    prenom = "Jean",
                    email = "jean@example.com",
                    role = "ENQUETEUR"
                )
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testOfflineSurveyAndResponseStorage() = runBlocking {
        val questionnaire = QuestionnaireEntity(
            idQuestionnaire = 10L,
            titre = "Recensement Agricole",
            description = "Enquête auprès des ménages agricoles",
            statut = "PUBLIE",
            version = "v1.0",
            createdBy = 1L
        )
        surveyDao.insertQuestionnaire(questionnaire)

        val q1 = QuestionEntity(
            idQuestion = 101L,
            idQuestionnaire = 10L,
            libelle = "Nom du chef de ménage",
            type = "TEXTE",
            obligatoire = true,
            ordre = 1
        )
        val q2 = QuestionEntity(
            idQuestion = 102L,
            idQuestionnaire = 10L,
            libelle = "Superficie exploitée (ha)",
            type = "NOMBRE",
            obligatoire = true,
            ordre = 2
        )
        surveyDao.insertQuestions(listOf(q1, q2))

        val questionsWithOptions = listOf(
            QuestionWithOptions(question = q1),
            QuestionWithOptions(question = q2)
        )

        // 2. Perform offline local validation and save survey
        val responses = mapOf(
            101L to "Randria Michel",
            102L to "2.5"
        )

        val saveResult = repository.saveSurvey(
            codeUnique = "ENQ-MG-001",
            questionnaireId = 10L,
            enqueteurId = 1L,
            dateEnquete = "2026-09-22",
            responses = responses,
            selectedOptions = emptyMap(),
            questions = questionsWithOptions
        )

        assertTrue("Survey should be saved successfully offline", saveResult.isSuccess)
        val surveyId = saveResult.getOrThrow()

        // 3. Verify Survey entity in Room
        val storedSurvey = surveyDao.getEnqueteById(surveyId)
        assertNotNull("Stored survey should exist in Room", storedSurvey)
        assertEquals("Survey must have offline pending status", "EN_ATTENTE", storedSurvey?.syncStatus)
        assertEquals("Survey must be marked as TERMINEE", "TERMINEE", storedSurvey?.statut)
        assertEquals("Survey date must match", "2026-09-22", storedSurvey?.dateEnquete)

        // 4. Verify Responses entities in Room
        val storedResponses = surveyDao.getReponsesForEnquete(surveyId)
        assertEquals("Should have 2 responses saved in Room", 2, storedResponses.size)
        val r1 = storedResponses.find { it.idQuestion == 101L }
        val r2 = storedResponses.find { it.idQuestion == 102L }
        assertEquals("Randria Michel", r1?.valeur)
        assertEquals("2.5", r2?.valeur)

        // 5. Verify pending surveys Flow
        val pendingList = surveyDao.getPendingSurveysFlow().first()
        assertEquals(1, pendingList.size)
        assertEquals(surveyId, pendingList[0].idEnquete)
    }

    @Test
    fun testLocalValidationBlocksIncompleteMandatoryFields() = runBlocking {
        val q1 = QuestionEntity(
            idQuestion = 201L,
            idQuestionnaire = 20L,
            libelle = "Numéro d'identification",
            type = "TEXTE",
            obligatoire = true,
            ordre = 1
        )
        val q2 = QuestionEntity(
            idQuestion = 202L,
            idQuestionnaire = 20L,
            libelle = "Source d'énergie principale",
            type = "CHOIX_UNIQUE",
            obligatoire = true,
            ordre = 2
        )
        val questionsWithOptions = listOf(
            QuestionWithOptions(question = q1),
            QuestionWithOptions(question = q2)
        )

        // Only answer q1, leaving mandatory q2 unanswered
        val partialResponses = mapOf(201L to "ID-999")
        val validation = LocalSurveyValidator.validateSurveyData(
            codeUnique = "RESP-100",
            dateEnquete = "2026-09-22",
            questions = questionsWithOptions,
            textResponses = partialResponses,
            selectedOptions = emptyMap()
        )

        assertFalse("Validation must fail when mandatory field is missing", validation.isValid)
        assertEquals(50, validation.completenessPercent)
        assertEquals(1, validation.answeredCount)
        assertEquals(2, validation.totalMandatoryCount)
        assertTrue(validation.missingMandatoryQuestions.contains("Source d'énergie principale"))

        // Attempting to save incomplete survey into Room must be blocked
        val saveResult = repository.saveSurvey(
            codeUnique = "RESP-100",
            questionnaireId = 20L,
            enqueteurId = 1L,
            dateEnquete = "2026-09-22",
            responses = partialResponses,
            selectedOptions = emptyMap(),
            questions = questionsWithOptions
        )
        assertTrue("Repository must reject survey with missing mandatory questions", saveResult.isFailure)
    }

    @Test
    fun testCascadeDeletionOfResponses() = runBlocking {
        // Setup metadata
        surveyDao.insertUser(UserEntity(idUser = 2L, nom = "Test", prenom = "User", email = "user2@example.com", role = "ENQUETEUR"))
        surveyDao.insertQuestionnaire(QuestionnaireEntity(idQuestionnaire = 30L, titre = "Test Questionnaire", statut = "PUBLIE", createdBy = 1L))
        val q = QuestionEntity(idQuestion = 301L, idQuestionnaire = 30L, libelle = "Nom", type = "TEXTE", obligatoire = false)
        surveyDao.insertQuestion(q)

        val saveResult = repository.saveSurvey(
            codeUnique = "ENQ-DEL-1",
            questionnaireId = 30L,
            enqueteurId = 2L,
            dateEnquete = "2026-09-22",
            responses = mapOf(301L to "Test Valeur"),
            selectedOptions = emptyMap(),
            questions = listOf(QuestionWithOptions(question = q))
        )
        val surveyId = saveResult.getOrThrow()

        val responsesBefore = surveyDao.getReponsesForEnquete(surveyId)
        assertEquals(1, responsesBefore.size)

        // Delete survey
        surveyDao.deleteEnqueteById(surveyId)

        // Responses must be cascade deleted by Room SQLite ForeignKey
        val responsesAfter = surveyDao.getReponsesForEnquete(surveyId)
        assertEquals(0, responsesAfter.size)
    }

    @Test
    fun testDuplicateSurveyPreventionForSameRespondent() = runBlocking {
        surveyDao.insertUser(UserEntity(idUser = 3L, nom = "Admin", prenom = "Enq", email = "user3@example.com", role = "ENQUETEUR"))
        surveyDao.insertQuestionnaire(QuestionnaireEntity(idQuestionnaire = 40L, titre = "Santé Publique", statut = "PUBLIE", createdBy = 1L))
        val q = QuestionEntity(idQuestion = 401L, idQuestionnaire = 40L, libelle = "Age", type = "TEXTE", obligatoire = false)
        surveyDao.insertQuestion(q)
        val qList = listOf(QuestionWithOptions(question = q))

        // First submission succeeds
        val firstSave = repository.saveSurvey(
            codeUnique = "PERS-UNIQUE-1",
            questionnaireId = 40L,
            enqueteurId = 3L,
            dateEnquete = "2026-09-22",
            responses = mapOf(401L to "32"),
            selectedOptions = emptyMap(),
            questions = qList
        )
        assertTrue(firstSave.isSuccess)

        // Second submission with same person code and questionnaire without allowUpdate must be rejected
        val duplicateSave = repository.saveSurvey(
            codeUnique = "PERS-UNIQUE-1",
            questionnaireId = 40L,
            enqueteurId = 3L,
            dateEnquete = "2026-09-22",
            responses = mapOf(401L to "33"),
            selectedOptions = emptyMap(),
            questions = qList,
            allowUpdate = false
        )
        assertTrue("Duplicate submission for same respondent must be blocked", duplicateSave.isFailure)
    }

    @Test
    fun testDeactivatedUserDetectionAndLock() = runBlocking {
        // Create an active surveyor
        val surveyor = UserEntity(
            idUser = 50L,
            nom = "Rakotomalala",
            prenom = "Hery",
            email = "hery@enquete.mg",
            motDePasse = "pass123",
            role = "ENQUETEUR",
            statut = "ACTIF"
        )
        repository.saveUser(surveyor)

        // Verify initially active
        assertFalse(com.example.data.sync.SyncService.isInactiveStatut(surveyor.statut))

        // Deactivate account
        repository.updateUserStatut(50L, "INACTIF")
        val updatedUser = repository.findUserById(50L)
        assertNotNull(updatedUser)
        assertEquals("INACTIF", updatedUser?.statut)
        assertTrue("User must be identified as inactive", com.example.data.sync.SyncService.isInactiveStatut(updatedUser?.statut))

        // Test login rejection for deactivated user
        val syncService = com.example.data.sync.SyncService(repository)
        syncService.isSimulatedOffline = true
        val loginResult = syncService.loginUser("hery@enquete.mg", "pass123")
        assertTrue("Login for deactivated user must fail", loginResult.isFailure)
    }

    @Test
    fun testSurveyorCannotChangeProfilePhoto() = runBlocking {
        val surveyor = UserEntity(
            idUser = 60L,
            nom = "Ravelo",
            prenom = "Tiana",
            email = "tiana@enquete.mg",
            role = "ENQUETEUR",
            photo = "https://example.com/initial.jpg"
        )
        repository.saveUser(surveyor)

        val syncService = com.example.data.sync.SyncService(repository)
        syncService.isSimulatedOffline = true

        // Enquêteur attempting to change photo must be blocked
        val result = syncService.updateUserPhoto(surveyor, "https://example.com/new_photo.jpg")
        assertTrue("Surveyor must not be allowed to change their profile photo", result.isFailure)
    }

    @Test
    fun testQuestionnaireDisappearsWhenDraftAndReappearsWhenPublished() = runBlocking {
        val q1 = QuestionnaireEntity(
            idQuestionnaire = 17L,
            titre = "Enquête Terrain 4",
            description = "Test questionnaire",
            version = "v1.0",
            statut = "PUBLIÉ",
            createdBy = 1L
        )
        repository.insertQuestionnaire(q1)

        // 1. Initial state: PUBLIÉ -> present in allQuestionnaires
        var list = repository.allQuestionnaires.first()
        assertTrue("Questionnaire 17 must be present when PUBLIÉ", list.any { it.idQuestionnaire == 17L })

        // 2. Admin sets status to BROUILLON -> leaves/disappears automatically
        repository.markQuestionnaireAsDraft(17L)
        list = repository.allQuestionnaires.first()
        assertFalse("Questionnaire 17 must disappear when marked BROUILLON", list.any { it.idQuestionnaire == 17L })

        // 3. Admin sets status back to PUBLIÉ -> returns/reappears automatically
        repository.markQuestionnaireAsPublished(17L)
        list = repository.allQuestionnaires.first()
        assertTrue("Questionnaire 17 must reappear automatically when PUBLIÉ", list.any { it.idQuestionnaire == 17L })
    }

    @Test
    fun testDuplicateDetectionOnCriticalFieldsPhoneAndId() = runBlocking {
        val q = QuestionnaireEntity(
            idQuestionnaire = 25L,
            titre = "Recensement Rural",
            statut = "PUBLIÉ",
            createdBy = 1L
        )
        repository.insertQuestionnaire(q)

        val qPhone = QuestionEntity(
            idQuestion = 251L,
            idQuestionnaire = 25L,
            libelle = "Numéro de téléphone",
            type = "TELEPHONE",
            obligatoire = true,
            ordre = 1
        )
        repository.insertQuestions(listOf(qPhone))

        // Save first survey with phone 0341234567 and code PERS-001
        val result1 = repository.saveEnquete(
            codeUnique = "PERS-001",
            questionnaireId = 25L,
            enqueteurId = 1L,
            dateEnquete = "2026-10-12",
            responses = mapOf(251L to "034 12 345 67"),
            selectedOptions = emptyMap(),
            questions = listOf(QuestionWithOptions(qPhone, emptyList()))
        )
        assertTrue("Première enquête doit réussir", result1.isSuccess)

        // Try to save duplicate person on same questionnaire
        val dupPersonCheck = repository.checkForDuplicatesInRoom(
            codeUnique = "PERS-001",
            questionnaireId = 25L,
            responses = mapOf(251L to "0329999999")
        )
        assertTrue("Doit détecter doublon sur ID unique/personne pour le même questionnaire", dupPersonCheck.hasDuplicate)
        assertEquals(LocalDuplicateCheckResult.DuplicateType.PERSON_QUESTIONNAIRE_DUPLICATE, dupPersonCheck.duplicateType)

        // Try to save another person (PERS-002) with the SAME phone number (0341234567)
        val dupPhoneCheck = repository.checkForDuplicatesInRoom(
            codeUnique = "PERS-002",
            questionnaireId = 25L,
            responses = mapOf(251L to "+261 34 12 345 67")
        )
        assertTrue("Doit détecter doublon sur le numéro de téléphone critique", dupPhoneCheck.hasDuplicate)
        assertEquals(LocalDuplicateCheckResult.DuplicateType.PHONE_OR_IDENTIFIER_DUPLICATE, dupPhoneCheck.duplicateType)
        assertEquals("PERS-001", dupPhoneCheck.conflictingCode)
    }

    @Test
    fun testAnnoncesFichiersAndDestinatairesPersistence() = runBlocking {
        val testAnnonceId = "ann-uuid-test-01"
        val annonce = AnnonceEntity(
            idAnnonce = testAnnonceId,
            titre = "Mission Terrain Région Analamanga",
            contenu = "Consignes prioritaires pour la collecte de cette semaine.",
            typeAnnonce = "FICHIER",
            priorite = "URGENTE",
            statut = "PUBLIEE",
            cibleType = "REGION",
            cibleValeur = "Analamanga",
            createdBy = 1L,
            authorName = "Rabe Jean",
            authorRole = "SUPERVISEUR"
        )

        val fichier = AnnonceFichierEntity(
            idFichier = "file-uuid-001",
            idAnnonce = testAnnonceId,
            nomFichier = "parcours_collecte.pdf",
            cheminStorage = "annonces/parcours_collecte.pdf",
            typeMime = "application/pdf",
            tailleOctets = 1048576L
        )

        val destinataire = AnnonceDestinataireEntity(
            idAnnonce = testAnnonceId,
            idUser = 1L,
            statutLecture = "NON_LU"
        )

        // 1. Sauvegarder dans Room SQLite
        repository.saveAnnonce(annonce, listOf(fichier), listOf(destinataire))

        // 2. Vérifier que l'annonce est correctement enregistrée
        val fetchedAnnonce = repository.getAnnonceById(testAnnonceId)
        assertNotNull("L'annonce doit être persistée dans SQLite", fetchedAnnonce)
        assertEquals("Mission Terrain Région Analamanga", fetchedAnnonce?.titre)
        assertEquals("URGENTE", fetchedAnnonce?.priorite)
        assertEquals("Analamanga", fetchedAnnonce?.cibleValeur)
        assertEquals("Rabe Jean", fetchedAnnonce?.authorName)
        assertEquals("SUPERVISEUR", fetchedAnnonce?.authorRole)

        // 3. Vérifier les fichiers joints
        val fichiers = repository.getFichiersForAnnonce(testAnnonceId)
        assertEquals(1, fichiers.size)
        assertEquals("parcours_collecte.pdf", fichiers.first().nomFichier)
        assertEquals(1048576L, fichiers.first().tailleOctets)

        // 4. Tester la mise à jour du statut de lecture (NON_LU -> LU)
        repository.markAnnonceAsRead(testAnnonceId, 1L)
        val destList = repository.getDestinatairesForAnnonce(testAnnonceId)
        assertEquals(1, destList.size)
        assertEquals("LU", destList.first().statutLecture)
        assertNotNull(destList.first().dateLecture)
    }
}
