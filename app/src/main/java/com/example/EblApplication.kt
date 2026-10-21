package com.example

import android.app.Application
import com.example.util.BackgroundSyncWorker
import com.example.util.NotificationHelper

class EblApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    // 1. Immediately create and register high-importance notification channels on process startup
    NotificationHelper.createNotificationChannels(this)

    // 2. Schedule reliable periodic background sync so notifications and Google Sheets sync run even when app is closed/killed
    BackgroundSyncWorker.schedulePeriodicSync(this)
  }
}
