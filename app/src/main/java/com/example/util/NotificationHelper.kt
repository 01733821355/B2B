package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {
  const val CHANNEL_ID = "rm_mentor_updates_channel"
  const val CHANNEL_RM_APPROVAL_ID = "rm_mentor_approvals_channel"
  private const val PREF_NAME = "app_notification_prefs"
  private const val KEY_NOTIFICATION_ASKED = "notification_permission_asked_once"

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
        description = "Instant notifications with distinct sound when data is updated by Admin or Mentor"
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 150, 100, 150, 100, 300)
        setSound(soundUri, audioAttributes)
      }

      val channel2 = NotificationChannel(
        CHANNEL_RM_APPROVAL_ID,
        "RM Assignments & Approvals",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts to Mentor when a new RM is assigned by Admin pending approval"
        enableLights(true)
        enableVibration(true)
        vibrationPattern = longArrayOf(0, 250, 150, 250)
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
    updatedByRole: String
  ) {
    try {
      createNotificationChannels(context)

      // Emit distinct unique alert sound tone
      try {
        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 350)
      } catch (_: Exception) {}

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        (ccNumber + targetRmCode).hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val refDisplay = if (ccNumber.isNotBlank()) "CC: $ccNumber" else "Customer: $customerName"

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("File Updated ($refDisplay)")
        .setContentText("RM $targetRmCode: Data updated by $updatedByRole ($changeDetails)")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("Attention RM $targetRmCode:\nYour customer file data was modified by $updatedByRole.\n\nRecord: $refDisplay\nCustomer: $customerName\nModifications: $changeDetails")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSound(soundUri)
        .setVibrate(longArrayOf(0, 150, 100, 150, 100, 300))
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

      // Emit distinct alert tone for Mentor
      try {
        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
      } catch (_: Exception) {}

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        rmCode.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val builder = NotificationCompat.Builder(context, CHANNEL_RM_APPROVAL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("New RM Assignment Pending")
        .setContentText("new rm assingne pending for your approval ($rmName, Code: $rmCode)")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("new rm assingne pending for your approval\n\nRM: $rmName\nCode: $rmCode\nAssigned by: Admin\nStatus: Pending Mentor Approval")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSound(soundUri)
        .setVibrate(longArrayOf(0, 200, 150, 200, 150, 350))
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

      // Emit distinct unique alert tone for RM
      try {
        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 350)
      } catch (_: Exception) {}

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        targetRmCode.hashCode() + 99,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )

      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

      val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Account Data Updated")
        .setContentText("RM $targetRmCode: Your data was updated by $updatedByRole")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("Attention RM $targetRmCode ($rmName):\nYour account information was updated by $updatedByRole.\n\nDetails: $details")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSound(soundUri)
        .setVibrate(longArrayOf(0, 150, 100, 150, 100, 250))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

      val notificationManager = NotificationManagerCompat.from(context)
      val notificationId = (System.currentTimeMillis() % 100000).toInt() + 2
      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {}
  }
}
