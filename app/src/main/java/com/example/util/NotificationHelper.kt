package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
  const val CHANNEL_ID = "b2b_alerts_channel_v2"
  const val CHANNEL_RM_APPROVAL_ID = "b2b_approvals_channel_v2"
  private const val PREF_NAME = "app_notification_prefs"
  private const val KEY_NOTIFICATION_ASKED = "notification_permission_asked_once"

  fun areNotificationsEnabled(context: Context): Boolean {
    return try {
      NotificationManagerCompat.from(context).areNotificationsEnabled()
    } catch (_: Exception) {
      true
    }
  }

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

      val channel1 = NotificationChannel(
        CHANNEL_ID,
        "File & Data Updates",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "High priority team and file update alerts"
        enableLights(true)
        lightColor = Color.BLUE
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 250, 150, 250)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setShowBadge(true)
        setSound(soundUri, audioAttributes)
      }

      val channel2 = NotificationChannel(
        CHANNEL_RM_APPROVAL_ID,
        "RM Assignments & Approvals",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "High priority alerts when an RM is assigned by Admin"
        enableLights(true)
        lightColor = Color.GREEN
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 250, 150, 250)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setShowBadge(true)
        setSound(soundUri, audioAttributes)
      }

      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      manager?.createNotificationChannel(channel1)
      manager?.createNotificationChannel(channel2)
    }
  }

  fun hasAskedPermissionOnce(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_NOTIFICATION_ASKED, false)
  }

  fun markPermissionAsked(context: Context) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_NOTIFICATION_ASKED, true).apply()
  }

  fun sendRmFileUpdateNotification(
    context: Context,
    targetRmCode: String,
    ccNumber: String,
    customerName: String,
    changeDetails: String,
    updatedByRole: String,
    customMessage: String? = null,
    dedupeKey: String? = null
  ) {
    try {
      val key = dedupeKey ?: "rm_file_${targetRmCode}_${ccNumber}_${changeDetails.take(30).hashCode()}"
      if (hasNotificationBeenShown(context, key)) {
        return
      }
      markNotificationShown(context, key)

      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        (ccNumber + targetRmCode).hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val refDisplay = if (ccNumber.isNotBlank()) "CC: $ccNumber" else "Customer: $customerName"
      val displayTitle = if (!customMessage.isNullOrBlank()) "File Update: $customerName" else "File Updated ($refDisplay)"
      val displayText = customMessage ?: "RM $targetRmCode: Data updated by $updatedByRole ($changeDetails)"

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(displayTitle)
        .setContentText(displayText)
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText(if (!customMessage.isNullOrBlank()) customMessage else "Attention RM $targetRmCode:\nYour customer file data was modified by $updatedByRole.\n\nRecord: $refDisplay\nCustomer: $customerName\nModifications: $changeDetails")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      val notificationId = (System.currentTimeMillis() % 100000).toInt()
      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }

  fun sendNewRmApprovalNotification(
    context: Context,
    rmName: String,
    rmCode: String
  ) {
    try {
      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        rmCode.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_RM_APPROVAL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("New RM Assignment Pending")
        .setContentText("New RM assigned pending your approval ($rmName, Code: $rmCode)")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("New RM assigned pending your approval\n\nRM: $rmName\nCode: $rmCode\nAssigned by: Admin\nStatus: Pending Mentor Approval")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      val notificationId = (System.currentTimeMillis() % 100000).toInt() + 1
      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }

  fun sendRmProfileUpdateNotification(
    context: Context,
    targetRmCode: String,
    rmName: String,
    updatedByRole: String,
    details: String
  ) {
    try {
      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        targetRmCode.hashCode() + 99,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Account Data Updated")
        .setContentText("RM $targetRmCode: Your data was updated by $updatedByRole")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("Attention RM $targetRmCode ($rmName):\nYour account information was updated by $updatedByRole.\n\nDetails: $details")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      val notificationId = (System.currentTimeMillis() % 100000).toInt() + 2
      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }

  fun sendIncomingSmsNotification(
    context: Context,
    senderTitle: String,
    messageText: String,
    dedupeKey: String? = null
  ) {
    try {
      val key = dedupeKey ?: "sms_incoming_${senderTitle}_${messageText.take(40).hashCode()}"
      if (hasNotificationBeenShown(context, key)) {
        return
      }
      markNotificationShown(context, key)

      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.sym_action_chat)
        .setContentTitle("SMS / Team Hub: $senderTitle")
        .setContentText(messageText)
        .setStyle(NotificationCompat.BigTextStyle().bigText("$senderTitle:\n$messageText"))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      val notificationId = (System.currentTimeMillis() % 100000).toInt() + 10
      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }

  fun hasNotificationBeenShown(context: Context, key: String): Boolean {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val shownSet = prefs.getStringSet("shown_notification_keys", emptySet()) ?: emptySet()
    return key in shownSet
  }

  fun markNotificationShown(context: Context, key: String) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val current = (prefs.getStringSet("shown_notification_keys", emptySet()) ?: emptySet()).toMutableSet()
    current.add(key)
    // Keep set bounded to last 1000 keys to avoid unlimited growth
    if (current.size > 1000) {
      val trimmed = current.toList().takeLast(600).toSet()
      prefs.edit().putStringSet("shown_notification_keys", trimmed).apply()
    } else {
      prefs.edit().putStringSet("shown_notification_keys", current).apply()
    }
  }

  fun sendAdminNewFileNotification(
    context: Context,
    rmCode: String,
    rmName: String,
    fileId: String,
    customerName: String,
    productType: String
  ) {
    try {
      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        fileId.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("New File: $customerName")
        .setContentText("RM $rmName ($rmCode) created $productType file ($fileId)")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("New Customer File Created!\n\nCustomer: $customerName\nProduct: $productType\nFile ID: $fileId\nEntered by: RM $rmName ($rmCode)")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      val notificationId = (System.currentTimeMillis() % 100000).toInt() + 15
      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }

  fun sendNewEventNotification(
    context: Context,
    eventTitle: String,
    creatorName: String,
    targetDate: String
  ) {
    try {
      createNotificationChannels(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        eventTitle.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Team Event: $eventTitle")
        .setContentText("Posted by $creatorName. Date: $targetDate")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("New Team Event Announcement!\n\nEvent: $eventTitle\nTarget Date: $targetDate\nPosted by: $creatorName")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_EVENT)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      val notificationId = (System.currentTimeMillis() % 100000).toInt() + 20
      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }

  fun sendTestAlertNotification(
    context: Context,
    title: String,
    message: String,
    notificationId: Int = 1001
  ) {
    try {
      createNotificationChannels(context)
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
      NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }
}
