package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.SurveyDao
import com.example.data.model.LocalSurveyValidator
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionWithOptions
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.UserEntity
import com.example.data.repository.SurveyRepository
import com.example.data.util.GpsCoordinates
import com.example.data.util.GpsLocationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GpsQuestionValidationTest {

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

        runBlocking {
            surveyDao.insertUser(
                UserEntity(
                    idUser = 1L,
                    nom = "Enquêteur",
                    prenom = "Test",
                    email = "test@example.com",
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
    fun testGpsJsonSerializationAndDeserialization() {
        val coords = GpsCoordinates(
            latitude = -25.0297,
            longitude = 46.9753,
            accuracy = 8.5,
            capturedAt = "2026-11-03T10:30:00Z"
        )

        val json = coords.toJson()
        assertTrue(json.contains("-25.0297"))
        assertTrue(json.contains("46.9753"))
        assertTrue(json.contains("8.5"))
        assertTrue(json.contains("2026-11-03T10:30:00Z"))

        val parsed = GpsLocationHelper.parseCoordinates(json)
        assertNotNull(parsed)
        assertEquals(-25.0297, parsed!!.latitude, 0.0001)
        assertEquals(46.9753, parsed.longitude, 0.0001)
        assertEquals(8.5, parsed.accuracy, 0.1)
        assertEquals("2026-11-03T10:30:00Z", parsed.capturedAt)
    }

    @Test
    fun testGpsQuestionDetection() {
        assertTrue(GpsLocationHelper.isGpsQuestion("GPS", "Localisation"))
        assertTrue(GpsLocationHelper.isGpsQuestion("LOCALISATION", "Point terrain"))
        assertTrue(GpsLocationHelper.isGpsQuestion("GEOLOCALISATION", "Position"))
        assertTrue(GpsLocationHelper.isGpsQuestion("TEXTE", "Coordonnées GPS [GPS]"))
        assertTrue(GpsLocationHelper.isGpsQuestion("TEXTE", "Position GPS du ménage"))

        assertFalse(GpsLocationHelper.isGpsQuestion("TEXTE", "Nom et Prénom"))
        assertFalse(GpsLocationHelper.isGpsQuestion("NOMBRE", "Âge"))
    }

    @Test
    fun testMandatoryGpsQuestionValidationFailsWhenEmpty() {
        val qGps = QuestionEntity(
            idQuestion = 201L,
            idQuestionnaire = 1L,
            libelle = "Coordonnées GPS du ménage",
            type = "GPS",
            obligatoire = true,
            ordre = 1
        )
        val questions = listOf(QuestionWithOptions(question = qGps))

        // Empty response
        val result = LocalSurveyValidator.validateSurveyData(
            codeUnique = "ENQ-001",
            dateEnquete = "2026-11-03",
            questions = questions,
            textResponses = emptyMap(),
            selectedOptions = emptyMap()
        )

        assertFalse(result.isValid)
        assertTrue(result.missingMandatoryQuestions.contains("Coordonnées GPS du ménage"))
    }

    @Test
    fun testMandatoryGpsQuestionValidationSucceedsWhenValidCoordinates() {
        val qGps = QuestionEntity(
            idQuestion = 201L,
            idQuestionnaire = 1L,
            libelle = "Coordonnées GPS du ménage",
            type = "GPS",
            obligatoire = true,
            ordre = 1
        )
        val questions = listOf(QuestionWithOptions(question = qGps))

        val gpsJson = """
            {
              "latitude": -25.0297,
              "longitude": 46.9753,
              "accuracy": 8.5,
              "captured_at": "2026-11-03T10:30:00Z"
            }
        """.trimIndent()

        val result = LocalSurveyValidator.validateSurveyData(
            codeUnique = "ENQ-001",
            dateEnquete = "2026-11-03",
            questions = questions,
            textResponses = mapOf(201L to gpsJson),
            selectedOptions = emptyMap()
        )

        assertTrue(result.isValid)
        assertEquals(0, result.missingMandatoryQuestions.size)
        assertEquals(100, result.completenessPercent)
    }

    @Test
    fun testOfflineStorageOfGpsResponseInRoomDatabase() = runBlocking {
        val questionnaire = QuestionnaireEntity(
            idQuestionnaire = 30L,
            titre = "Enquête Environnementale",
            statut = "PUBLIÉ",
            createdBy = 1L
        )
        surveyDao.insertQuestionnaire(questionnaire)

        val qGps = QuestionEntity(
            idQuestion = 301L,
            idQuestionnaire = 30L,
            libelle = "Position GPS du point d'eau",
            type = "GPS",
            obligatoire = true,
            ordre = 1
        )
        surveyDao.insertQuestions(listOf(qGps))

        val coords = GpsCoordinates(
            latitude = -25.0297,
            longitude = 46.9753,
            accuracy = 8.5,
            capturedAt = "2026-11-03T10:30:00Z"
        )

        val saveResult = repository.saveEnquete(
            codeUnique = "POINT-EAU-01",
            questionnaireId = 30L,
            enqueteurId = 1L,
            dateEnquete = "2026-11-03",
            responses = mapOf(301L to coords.toJson()),
            selectedOptions = emptyMap(),
            questions = listOf(QuestionWithOptions(question = qGps))
        )

        assertTrue(saveResult.isSuccess)
        val enqueteId = saveResult.getOrThrow()

        val reponses = surveyDao.getReponsesForEnquete(enqueteId)
        assertEquals(1, reponses.size)
        val savedVal = reponses[0].valeur
        assertNotNull(savedVal)

        val retrievedCoords = GpsLocationHelper.parseCoordinates(savedVal)
        assertNotNull(retrievedCoords)
        assertEquals(-25.0297, retrievedCoords!!.latitude, 0.0001)
        assertEquals(46.9753, retrievedCoords.longitude, 0.0001)
        assertEquals(8.5, retrievedCoords.accuracy, 0.1)
    }
}
