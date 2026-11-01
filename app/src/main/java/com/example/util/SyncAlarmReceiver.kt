package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.example.data.database.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.EblRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SyncAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    val pendingResult = goAsync()
    val appContext = context.applicationContext

    CoroutineScope(Dispatchers.IO).launch {
      try {
        // 1. Ensure notification channels exist
        NotificationHelper.createNotificationChannels(appContext)

        // 2. Directly trigger repository sync so heads-up alerts arrive immediately when app is closed
        val database = AppDatabase.getDatabase(appContext)
        val authRepository = AuthRepository(database)
        val authPrefs = appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val cachedRm = authPrefs.getString("last_logged_rm_code", "") ?: ""
        authRepository.restoreSessionFromLastLogged(cachedRm)
        val eblRepository = EblRepository(database, authRepository, appContext)

        // Direct poll Google Sheets for updates, chat messages, and SMS
        eblRepository.pullDataFromGoogleSheets()

        // Also trigger background worker as backup
        BackgroundSyncWorker.triggerImmediateSync(appContext)
      } catch (_: Exception) {
      } finally {
        // 3. Reschedule the next alarm to keep polling continuously even if app process is closed
        scheduleNextAlarm(appContext, 60_000L)
        try {
          pendingResult.finish()
        } catch (_: Exception) {}
      }
    }
  }

  companion object {
    private const val REQUEST_CODE = 8842

    fun scheduleNextAlarm(context: Context, delayMillis: Long = 60_000L) {
      try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SyncAlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
          PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

        val triggerAt = SystemClock.elapsedRealtime() + delayMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
              } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
              }
            } else {
              alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            }
          } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
          }
        } else {
          alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
        }
      } catch (_: Exception) {}
    }

    fun cancelAlarm(context: Context) {
      try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SyncAlarmReceiver::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
          PendingIntent.FLAG_NO_CREATE
        }
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        if (pendingIntent != null) {
          alarmManager.cancel(pendingIntent)
        }
      } catch (_: Exception) {}
    }
  }
}
