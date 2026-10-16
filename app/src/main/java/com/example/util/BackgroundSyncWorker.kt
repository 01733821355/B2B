package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.EblRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BackgroundSyncWorker(
  private val appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
      val database = AppDatabase.getDatabase(appContext)
      val authRepository = AuthRepository(database)
      authRepository.restoreSessionFromLastLogged()

      val eblRepository = EblRepository(database, authRepository, appContext)
      // Pull latest cloud updates from Google Sheets in the background even if the app is closed!
      // This checks for new file updates, team events, or incoming SMS and triggers notifications.
      eblRepository.pullDataFromGoogleSheets()
      Result.success()
    } catch (_: Exception) {
      Result.retry()
    }
  }

  companion object {
    private const val WORK_NAME = "EblBackgroundSyncWorker"

    fun schedulePeriodicSync(context: Context) {
      try {
        val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

        val workRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(
          15, TimeUnit.MINUTES,
          5, TimeUnit.MINUTES
        )
          .setConstraints(constraints)
          .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
          WORK_NAME,
          ExistingPeriodicWorkPolicy.KEEP,
          workRequest
        )
      } catch (_: Exception) {}
    }
  }
}
