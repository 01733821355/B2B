package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "audit_logs",
  indices = [Index(value = ["timestamp"]), Index(value = ["rmCode"])]
)
data class AuditLogEntity(
  @PrimaryKey
  val logId: String,
  val userId: String,
  val role: String,
  val action: String, // LOGIN, FILE_CREATE, FILE_UPDATE, FILE_DELETE, RM_CREATE, RM_STATUS_CHANGE, PASSWORD_RESET, SYNC_SHEETS, EXPORT_REPORT
  val fileId: String? = null,
  val rmCode: String? = null,
  val timestamp: Long,
  val details: String
)
