package com.example

import android.app.Application
import android.util.Log
import com.example.util.BackgroundSyncWorker
import com.example.util.NotificationHelper

class EblApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    // Global crash protection: log unexpected background thread uncaught exceptions
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      Log.e("EblApplication", "Uncaught exception in thread ${thread.name}", throwable)
      defaultHandler?.uncaughtException(thread, throwable)
    }

    // 1. Immediately create and register high-importance notification channels on process startup
    NotificationHelper.createNotificationChannels(this)

    // 2. Schedule reliable periodic background sync so notifications and Google Sheets sync run even when app is closed/killed
    BackgroundSyncWorker.schedulePeriodicSync(this)
    com.example.util.SyncAlarmReceiver.scheduleNextAlarm(this, 15 * 60 * 1000L)
  }
}
