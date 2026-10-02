package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_notifications")
data class SmsNotificationEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val recipientRmCode: String,     // e.g. "104393"
  val recipientMobile: String,     // e.g. "01711223344"
  val recipientName: String,       // e.g. "Tanvir Ahmed"
  val triggeredByRole: String,     // "ADMIN" or "MENTOR"
  val triggeredByCode: String,     // "Admin0" or "12345"
  val actionType: String,          // "UPDATE" or "DELETE" or "RESTORE" or "TARGET" or "PROFILE"
  val targetType: String,          // "CUSTOMER_FILE", "RM_TARGET", "RM_PROFILE"
  val fileId: String? = null,
  val customerName: String? = null,
  val messageText: String,         // Full text of the SMS sent
  val sentTimestamp: Long,         // DateUtils.currentDhakaMillis()
  val status: String = "DELIVERED", // "DELIVERED", "SENT_DEVICE", "STORED"
  val isRead: Boolean = false
)
