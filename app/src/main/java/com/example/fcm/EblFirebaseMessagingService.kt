package com.example.fcm

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.model.SmsNotificationEntity
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EblFirebaseMessagingService : FirebaseMessagingService() {

  companion object {
    private const val TAG = "EblFcmService"
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "New FCM Token received: $token")
    FcmTokenManager.onNewToken(applicationContext, token)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

    val data = remoteMessage.data
    val title = remoteMessage.notification?.title
      ?: data["title"]
      ?: "EBL RM Alert"

    val body = remoteMessage.notification?.body
      ?: data["body"]
      ?: data["message"]
      ?: "New high priority notification"

    val targetScreen = data["targetScreen"] ?: data["screen"]
    val targetFileId = data["targetFileId"] ?: data["fileId"]
    val actionType = data["actionType"] ?: "PUSH_ALERT"
    val triggeredByRole = data["triggeredByRole"] ?: "ADMIN"
    val triggeredByCode = data["triggeredByCode"] ?: "SYSTEM"

    // 1. Show Instant Heads-Up Push Notification (Wakes device, plays sound, vibrates)
    NotificationHelper.showFcmPushNotification(
      context = applicationContext,
      title = title,
      body = body,
      targetScreen = targetScreen,
      targetFileId = targetFileId,
      extras = data
    )

    // 2. Persist in local notification history (Room DB)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = AppDatabase.getDatabase(applicationContext)
        val now = DateUtils.currentDhakaMillis()
        val lastRm = db.appSettingDao().getSetting("last_logged_rm_code")?.settingValue?.trim() ?: "ALL"

        val entity = SmsNotificationEntity(
          recipientRmCode = data["recipientRmCode"] ?: lastRm,
          recipientMobile = data["recipientMobile"] ?: "",
          recipientName = data["recipientName"] ?: "Officer",
          triggeredByRole = triggeredByRole,
          triggeredByCode = triggeredByCode,
          actionType = actionType,
          targetType = if (!targetFileId.isNullOrBlank()) "CUSTOMER_FILE" else "SYSTEM_ALERT",
          fileId = targetFileId ?: "",
          customerName = data["customerName"] ?: "",
          messageText = body,
          sentTimestamp = now,
          status = "DELIVERED",
          isRead = false
        )
        db.smsNotificationDao().insertSms(entity)
      } catch (e: Exception) {
        Log.e(TAG, "Error saving incoming push to SMS history", e)
      }
    }
  }
}
