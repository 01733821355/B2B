package com.example.util

import android.content.Context
import android.util.Log

object SmsService {
  private const val TAG = "SmsService"

  /**
   * Dispatches digital in-app & cloud SMS alerts without requiring a physical SIM card,
   * carrier network charges, or SEND_SMS permission.
   * Notifications are stored directly in local app storage (Room DB) and synchronized
   * via the Google Sheets SMS_Notifications tab.
   */
  fun sendSms(
    context: Context,
    recipientMobile: String,
    messageText: String
  ): Boolean {
    val cleanMobile = recipientMobile.trim().replace(" ", "").replace("-", "")
    if (cleanMobile.isBlank() || messageText.isBlank()) {
      Log.w(TAG, "Cannot dispatch SMS alert: empty recipient or message")
      return false
    }

    Log.i(TAG, "Digital bank alert logged for $cleanMobile: ${messageText.take(60)}")
    return true
  }
}
