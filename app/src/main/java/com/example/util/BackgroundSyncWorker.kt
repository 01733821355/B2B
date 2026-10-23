package com.example.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
      // 1. Ensure notification channels are registered
      NotificationHelper.createNotificationChannels(appContext)

      val database = AppDatabase.getDatabase(appContext)
      val authRepository = AuthRepository(database)

      // Restore user session from database or SharedPreferences so recipient matching works in background
      val authPrefs = appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
      val cachedRm = authPrefs.getString("last_logged_rm_code", "") ?: ""
      authRepository.restoreSessionFromLastLogged(cachedRm)

      val eblRepository = EblRepository(database, authRepository, appContext)

      // 2. Pull latest cloud updates from Google Sheets even if app is completely closed/killed!
      // This retrieves new files, updates, SMS alerts, and team events and fires high-importance device notifications.
      eblRepository.pullDataFromGoogleSheets()

      // 3. Location beacon update if location permission is active
      val lastRm = database.appSettingDao().getSetting("last_logged_rm_code")?.settingValue ?: cachedRm
      if (lastRm.isNotBlank() && LocationHelper.hasLocationPermission(appContext)) {
        try {
          val loc = LocationHelper.getCurrentLocation(appContext)
          eblRepository.updateUserLocation(lastRm, loc.latitude, loc.longitude, loc.address, "BACKGROUND_WORKER_BEACON")
        } catch (_: Exception) {}
      }

      // Schedule next fast check so that when app is closed, sheet SMS notifications are caught promptly
      scheduleNextFastSync(appContext)

      Result.success()
    } catch (_: Exception) {
      scheduleNextFastSync(appContext)
      Result.retry()
    }
  }

  companion object {
    private const val WORK_NAME = "EblBackgroundSyncWorker"
    private const val FAST_WORK_NAME = "EblFastBackgroundSync"

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
          ExistingPeriodicWorkPolicy.UPDATE,
          workRequest
        )

        scheduleNextFastSync(context)
      } catch (_: Exception) {}
    }

    fun scheduleNextFastSync(context: Context) {
      try {
        val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

        val fastWork = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
          .setConstraints(constraints)
          .setInitialDelay(20, TimeUnit.SECONDS)
          .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
          FAST_WORK_NAME,
          ExistingWorkPolicy.REPLACE,
          fastWork
        )
      } catch (_: Exception) {}
    }

    fun triggerImmediateSync(context: Context) {
      try {
        val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

        val workRequest = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
          .setConstraints(constraints)
          .build()

        WorkManager.getInstance(context).enqueue(workRequest)
      } catch (_: Exception) {}
    }
  }
}
