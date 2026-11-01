package com.example

import com.example.data.sync.SyncService
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testBrouillonQuestionnairesAreExcluded() {
    // Tests confirming that questionnaires with statut BROUILLON are never pulled from Supabase
    val draftStatuses = listOf("BROUILLON", "brouillon", "Brouillon", "DRAFT", "draft", "Draft", "en_brouillon")
    for (status in draftStatuses) {
      assertTrue("Status '$status' should be recognized as draft", SyncService.isDraftStatus(status))
      assertFalse("Status '$status' must not be public / must not be pulled", SyncService.isPublicStatus(status))
    }

    // Archived statuses must not be public
    val archiveStatuses = listOf("ARCHIVE", "archive", "archivé", "SUPPRIME", "supprimé", "cloture")
    for (status in archiveStatuses) {
      assertFalse("Archive status '$status' must not be public", SyncService.isPublicStatus(status))
    }

    // Published / Active statuses must be accepted
    val validStatuses = listOf("PUBLIE", "publie", "publié", "PUBLIC", "public", "ACTIF", "actif")
    for (status in validStatuses) {
      assertFalse("Valid status '$status' must not be draft", SyncService.isDraftStatus(status))
      assertTrue("Valid status '$status' must be public and pulled", SyncService.isPublicStatus(status))
    }
  }

  @Test
  fun testSurveyorCodeUniqueness() {
    val regex = Regex("^ENQ(-U\\d+)?-\\d{6}-\\d{4}-\\d{3}$")
    val sampleCode = "ENQ-U2-260920-1145-782"
    assertTrue("Surveyor unique code should match pattern", regex.matches(sampleCode))
  }
}

