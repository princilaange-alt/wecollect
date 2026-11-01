package com.example.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.repository.SurveyRepository
import com.example.data.sync.SyncService
import com.example.util.AndroidNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AnnouncementSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val surveyDao = db.surveyDao()
            val repository = SurveyRepository(surveyDao, applicationContext)
            val syncService = SyncService(repository)

            val oldAnnonces = surveyDao.getAllAnnoncesList()
            val oldIds = oldAnnonces.map { it.idAnnonce }.toSet()

            syncService.synchronizeAll()

            val newAnnonces = surveyDao.getAllAnnoncesList()

            for (annonce in newAnnonces) {
                if (!oldIds.contains(annonce.idAnnonce) && oldIds.isNotEmpty()) {
                    val title = annonce.titre.takeIf { !it.isNullOrBlank() } ?: "Nouvelle annonce"
                    val content = annonce.contenu.takeIf { !it.isNullOrBlank() } ?: "Une nouvelle directive a été publiée."
                    AndroidNotificationHelper.showAnnouncementNotification(
                        applicationContext,
                        title,
                        content,
                        annonce.idAnnonce.hashCode()
                    )
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
