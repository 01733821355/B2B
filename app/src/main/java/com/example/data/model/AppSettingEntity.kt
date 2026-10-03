package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingEntity(
  @PrimaryKey
  val settingKey: String,
  val settingValue: String,
  val updatedBy: String,
  val updatedAt: Long
)

@Entity(tableName = "sync_status")
data class SyncStatusEntity(
  @PrimaryKey
  val id: Int = 1,
  val spreadsheetId: String = "1lb9Wou10ecl28EUgaXD2cA3YCNY7nNHp1BOFrrLezqI",
  val lastSyncTimestamp: Long? = null,
  val lastSyncStatus: String = "IDLE", // IDLE, IN_PROGRESS, SUCCESS, ERROR
  val lastSyncMessage: String = "Initial state. Ready for synchronization.",
  val pendingRecordsCount: Int = 0,
  val appsScriptUrl: String = "https://script.google.com/macros/s/AKfycbzxQ2GtKwhT8UjUdvqPTWielndlsMu9d_rVFf2ro4sI5-uCRrvj8uQXFKpVnBF7g9r0NQ/exec",
  val syncSecretKey: String = ""
)
